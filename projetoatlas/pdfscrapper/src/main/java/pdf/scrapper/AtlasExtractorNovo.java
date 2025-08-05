package pdf.scrapper;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtlasExtractorNovo {
    private static final Set<String> encontrados = new HashSet<>();
    private static final Set<String> acronimosInvalidos = Set.of(
            "webpage", "acronim", "name", "institute", "medical", "linking", "social",
            "agricultural", "search", "about", "aspx", "humanities", "research-unit");

    private static final List<String> palavrasChave = List.of("center", "institute", "research");

    public record Centro(String acronimo, String nome, String website) {
    }

    public static void main(String[] args) {
        String pdfPath = Paths.get("src", "main", "resources", "atlas_2022.pdf").toString();
        String excelPath = Paths.get("src", "main", "resources", "KGR-RESEARCH-URI.xlsx").toString();
        List<Centro> centrosExcel = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Programas\\Tesseract-OCR\\tessdata");
            tesseract.setLanguage("por+eng");
            tesseract.setPageSegMode(1);
            tesseract.setTessVariable("tessedit_char_whitelist",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789:/.-_&’' ");

            int startPage = 194;
            int endPage = 198;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("centros_extraidos_fundido.csv"))) {
                for (int page = startPage; page <= endPage; page++) {
                    BufferedImage fullImage = pdfRenderer.renderImageWithDPI(page, 600);
                    int mid = fullImage.getWidth() / 2;
                    BufferedImage left = fullImage.getSubimage(0, 0, mid, fullImage.getHeight());
                    BufferedImage right = fullImage.getSubimage(mid, 0, mid, fullImage.getHeight());

                    processText(tesseract.doOCR(left), writer, centrosExcel);
                    processText(tesseract.doOCR(right), writer, centrosExcel);
                }
            }

            System.out.println("[DEBUG] Total de centros extraídos para Excel: " + centrosExcel.size());
            escreverParaExcel(centrosExcel, excelPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processText(String text, BufferedWriter writer, List<Centro> centrosExcel) throws Exception {
        String[] lines = text.split("\\R");

        Pattern mainPattern = Pattern.compile(
                "([A-ZÀ-ÿ0-9\\-/]{2,})[\\s\\-–—:]{1,5}(.{5,100}?)\\s+(https?://\\S+|www\\.\\S+|\\S+\\.(pt|org|com|eu|net)(/\\S*)?)",
                Pattern.CASE_INSENSITIVE);
        Pattern acronimoNomePattern = Pattern.compile("([A-Z][A-Z0-9\\-/]{2,})[\\s\\-–—:]{1,5}(.+)");
        Pattern urlPattern = Pattern.compile("(https?://\\S+|www\\.\\S+|\\S+\\.(pt|org|com|eu|net)(/\\S*)?)");

        for (int i = 0; i < lines.length - 2; i++) {
            String l1 = lines[i].trim();
            String l2 = lines[i + 1].trim();
            String l3 = lines[i + 2].trim();
            String combinado = l1 + " " + l2 + " " + l3;

            Matcher mainMatcher = mainPattern.matcher(combinado);
            if (mainMatcher.find()) {
                String acronimo = normalizarAcronimo(mainMatcher.group(1));
                String nome = corrigirErrosOCR(limparNome(mainMatcher.group(2)));
                String website = corrigirURL(mainMatcher.group(3));
                registrarCentro(acronimo, nome, website, writer, centrosExcel);
                continue;
            }

            Matcher urlMatcher = urlPattern.matcher(combinado);
            String url = "";
            if (urlMatcher.find()) {
                url = corrigirURL(urlMatcher.group());
                combinado = combinado.replace(urlMatcher.group(), "");
                l1 = l1.replace(urlMatcher.group(), "");
            }

            Matcher acronimoNomeMatcher = acronimoNomePattern.matcher(l1);
            if (acronimoNomeMatcher.find()) {
                String acronimo = normalizarAcronimo(acronimoNomeMatcher.group(1));
                String nome = corrigirErrosOCR(limparNome(acronimoNomeMatcher.group(2)));

                if (acronimo.equalsIgnoreCase("ACRONIM") && nome.matches(".*\\b([A-Z]{2,10}(?:-[A-Z]{2,10})?)\\b.*")) {
                    Matcher matcher = Pattern.compile("\\b([A-Z]{2,10}(?:-[A-Z]{2,10})?)\\b").matcher(nome);
                    if (matcher.find()) {
                        acronimo = matcher.group(1);
                        nome = nome.replaceFirst(acronimo, "").replaceFirst("(?i)NAME WEBPAGE", "").trim();
                    }
                }

                registrarCentro(acronimo, nome, url, writer, centrosExcel);
            }
        }
    }

    private static void registrarCentro(String acronimo, String nome, String website, BufferedWriter writer,
            List<Centro> centrosExcel)
            throws IOException {
        acronimo = tentarCorrigirAcronimo(acronimo, nome);
        website = corrigirErrosWebsite(website);

        if (!isValido(acronimo, website, nome)) {
            System.out.println("[REJEITADO] " + acronimo + " | " + nome + " | " + website);

            try (BufferedWriter rejeitadosWriter = new BufferedWriter(new FileWriter("rejeitados.csv", true))) {
                rejeitadosWriter.write("\"" + acronimo + "\",\"" + nome + "\",\"" + website + "\"\n");
            }
            return;
        }

        String key = acronimo + "|" + nome + "|" + website;
        if (encontrados.add(key)) {
            writer.write("\"" + acronimo + "\",\"" + nome + "\",\"" + website + "\"\n");
            centrosExcel.add(new Centro(acronimo, nome, website));
        }
    }

    public static void escreverParaExcel(List<Centro> centros, String caminhoExcel) {
        try {
            File file = new File(caminhoExcel);
            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheet("Organizations");

            // ⚠️ Limpar linhas antigas
            for (int i = sheet.getLastRowNum(); i > 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null)
                    sheet.removeRow(row);
            }

            int rowIndex = 1;
            for (Centro centro : centros) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(2).setCellValue("schema:Organization");
                row.createCell(3).setCellValue("schema:ResearchOrganization");
                row.createCell(4).setCellValue(centro.acronimo());
                row.createCell(5).setCellValue(centro.nome());
                row.createCell(9).setCellValue(centro.website());
            }

            fis.close();
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            workbook.close();
            fos.close();

            System.out.println("[EXCEL] Escrita concluída com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isValido(String acronimo, String website, String nome) {
        boolean urlOk = website.isEmpty() || isValidURL(website) || website.matches(".*\\.[a-zA-Z\\-]{2,}\\.?");
        boolean acronimoValido = acronimo.length() >= 2 && !acronimo.matches("^\\d+$");
        boolean nomeIndicaCentro = nome.toLowerCase()
                .matches(".*\\b(institute|center|centre|research|laboratory|group)\\b.*");

        return (acronimoValido && urlOk && !acronimosInvalidos.contains(acronimo.toLowerCase())) ||
                (urlOk && nomeIndicaCentro);
    }

    private static String tentarCorrigirAcronimo(String acronimoOriginal, String nome) {
        if (!acronimosInvalidos.contains(acronimoOriginal.toLowerCase())) {
            return acronimoOriginal;
        }

        Matcher matcher = Pattern.compile("([A-Z]{2,10}(?:-[A-Z]{2,10})?)").matcher(nome);
        if (matcher.find()) {
            String candidato = matcher.group(1).trim();
            if (!acronimosInvalidos.contains(candidato.toLowerCase())) {
                return candidato;
            }
        }

        return acronimoOriginal;
    }

    private static String corrigirErrosWebsite(String site) {
        site = site.trim();
        site = site.replace("ptpt", "pt").replace(".pt/pt", ".pt/").replace(".ptpt", ".pt")
                .replace("www.ifimup.up.", "www.ifimup.up.pt")
                .replace("cfcul.fc.ul.", "cfcul.fc.ul.pt");
        if (site.matches(".*\\.([a-zA-Z\\-]{2,})\\.$"))
            site += "pt";
        if (!site.contains("://"))
            site = "http://" + site;
        return site;
    }

    private static String corrigirURL(String url) {
        if (!url.contains("://") && !url.startsWith("www."))
            return "http://" + url;
        return url;
    }

    private static String normalizarAcronimo(String acronimo) {
        return acronimo.replaceAll("^(pt/|search/|about/|aspx/)?", "")
                .replaceAll("[^\\p{L}\\p{N}\\-/]", "")
                .trim();
    }

    private static boolean isValidURL(String url) {
        if (url == null || url.trim().equals("http://") || url.trim().equals("http://www."))
            return false;
        return url.matches("^(https?://|www\\.)[^\\s]+\\.[a-z]{2,6}(/[^\\s]*)?$") ||
                url.matches("^(https?://|www\\.)[^\\s]+\\.[a-z]{2,6}\\.?$");
    }

    private static String limparNome(String nome) {
        return nome.replaceAll("ç", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s\\-,\\.\\&’']", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String corrigirErrosOCR(String nome) {
        return nome.replaceAll("Environmentaland", "Environmental and")
                .replaceAll("Volcanologyand", "Volcanology and")
                .replaceAll("HealthSciences", "Health Sciences")
                .replaceAll("Microbiolog[iy]cal", "Microbiological")
                .replaceAll("andand", "and");
    }
}
