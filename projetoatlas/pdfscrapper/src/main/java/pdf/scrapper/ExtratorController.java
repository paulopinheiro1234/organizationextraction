package pdf.scrapper;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class ExtratorController {
    private final Set<String> encontrados = new HashSet<>();
    private final Set<String> acronimosInvalidos = Set.of(
        "webpage", "acronim", "name", "institute", "medical", "linking",
        "social", "agricultural", "search", "about", "aspx", "humanities",
        "research-unit", "engineering", "sciences", "associate", "and"
    );

    public void extrairEPreencherExcel(String pdfPath, String excelPath) {
        List<Centro> centrosExcel = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Users/BERNARDO/Documents/tesseract-5.5.1/tessdata");
            tesseract.setLanguage("por+eng");
            tesseract.setPageSegMode(1);
            tesseract.setTessVariable("tessedit_char_whitelist",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:/.-_&’' ");

            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter("centros_extraidos_fundido.csv"))) {
                for (int page = 194; page <= 198; page++) {
                    BufferedImage fullImage = pdfRenderer.renderImageWithDPI(page, 600);
                    int mid = fullImage.getWidth() / 2;
                    BufferedImage left = fullImage.getSubimage(0, 0, mid, fullImage.getHeight());
                    BufferedImage right = fullImage.getSubimage(mid, 0, mid, fullImage.getHeight());

                    processarTexto(tesseract.doOCR(left), writer, centrosExcel);
                    processarTexto(tesseract.doOCR(right), writer, centrosExcel);
                }
            }

            ExcelWriter.escreverParaExcel(centrosExcel, excelPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processarTexto(String text,
            BufferedWriter writer,
            List<Centro> centrosExcel) throws IOException {

        String[] lines = text.split("\\R");

        Pattern mainPattern = Pattern.compile(
            "([A-ZÀ-ÿ0-9\\-/]{2,})[\\s\\-–—:]{1,5}(.{5,100}?)\\s+" +
            "(https?://\\S+|www\\.\\S+|\\S+\\.(pt|org|com|eu|net)(/\\S*)?)",
            Pattern.CASE_INSENSITIVE
        );
        Pattern acronimoNomePattern = Pattern.compile(
            "([A-Z][A-Z0-9\\-/]{2,})[\\s\\-–—:]{1,5}(.+)"
        );
        Pattern urlPattern = Pattern.compile(
            "(https?://\\S+|www\\.\\S+|\\S+\\.(pt|org|com|eu|net)(/\\S*)?)"
        );

        for (int i = 0; i < lines.length - 2; i++) {
            String l1 = lines[i].trim();
            String l2 = lines[i + 1].trim();
            String l3 = lines[i + 2].trim();
            String combinado = String.join(" ", l1, l2, l3);

            Matcher m1 = mainPattern.matcher(combinado);
            if (m1.find()) {
                registrarCentro(
                    new Centro(m1.group(1), m1.group(2), m1.group(3)),
                    writer, centrosExcel
                );
                continue;
            }

            Matcher mu = urlPattern.matcher(combinado);
            String url = "";
            if (mu.find()) {
                url = mu.group();
                combinado = combinado.replace(url, "");
                l1 = l1.replace(url, "");
            }

            Matcher m2 = acronimoNomePattern.matcher(l1);
            if (m2.find()) {
                registrarCentro(
                    new Centro(m2.group(1), m2.group(2), url),
                    writer, centrosExcel
                );
            }
        }
    }

    private void registrarCentro(Centro c,
            BufferedWriter writer,
            List<Centro> centrosExcel) throws IOException {

        // 1) autocorrige acrónimo se placeholder
        Centro centro = corrigirAcronimoSePlaceholder(c);

        // 2) validação mínima
        if (centro.acronimo().length() < 2
         || centro.nome().isBlank()
         || centro.website().isBlank()) {
            return;
        }

        // 3) validação de padrão
        if (!centroValido(centro)) {
            return;
        }

        // 4) grava se novo
        if (encontrados.add(centro.toKey())) {
            writer.write(centro.toCSV());
            writer.newLine();
            centrosExcel.add(centro);
        }
    }

    private Centro corrigirAcronimoSePlaceholder(Centro c) {
        String acr = c.acronimo();
        if ("ACRONIM".equalsIgnoreCase(acr) || acr.contains("/")) {
            Matcher m = Pattern.compile("\\b([A-Z]{2,}(?:[-\\.][A-Z0-9]+)*)\\b")
                               .matcher(c.nome());
            if (m.find()) {
                String novoAcr = m.group(1);
                String novoNome = c.nome()
                    .replaceFirst("\\b" + Pattern.quote(novoAcr) + "\\b", "")
                    .trim();
                return new Centro(novoAcr, novoNome, c.website());
            }
        }
        return c;
    }

    private boolean centroValido(Centro c) {
        String acr = c.acronimo().toLowerCase();
        String nome = c.nome().toLowerCase();

        if (acronimosInvalidos.contains(acr)) return false;

        return nome.matches(
            ".*\\b(center|centre|laborat|institute|group|unit|lab|" +
            "science|research|technology|development|innovation|study|studies)\\b.*"
        );
    }
}
