package pdf.scrapper;

import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.util.List;

public class ExcelWriter {
    public static void escreverParaExcel(List<Centro> centros, String caminhoExcel) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            File file = new File(caminhoExcel);
            fis = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheet("Organizations");

            // Limpar linhas antigas
            for (int i = sheet.getLastRowNum(); i > 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) sheet.removeRow(row);
            }

            // Escrever novos dados
            int rowIndex = 1;
            for (Centro centro : centros) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(2).setCellValue("schema:Organization");
                row.createCell(3).setCellValue("schema:ResearchOrganization");
                row.createCell(4).setCellValue(centro.acronimo());
                row.createCell(5).setCellValue(centro.nome());
                row.createCell(9).setCellValue(centro.website());
            }

            fos = new FileOutputStream(file);
            workbook.write(fos);
            workbook.close();
            System.out.println("[EXCEL] Escrita concluída com sucesso.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
