package pdf.scrapper;
public class AtlasExtractorNovo {
    public static void main(String[] args) {
        String pdfPath = "src/main/resources/atlas_2022.pdf";
        String excelPath = "src/main/resources/KGR-RESEARCH-URI.xlsx";
        ExtratorController controller = new ExtratorController();
        controller.extrairEPreencherExcel(pdfPath, excelPath);
    }
}