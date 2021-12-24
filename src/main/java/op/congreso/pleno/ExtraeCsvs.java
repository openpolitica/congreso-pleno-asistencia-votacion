package op.congreso.pleno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExtraeCsvs {
  public static void main(String[] args) throws IOException {
    var dir = Path.of(args[0]);
    Files.list(dir).forEach(path -> {
      try {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
          throw new IllegalArgumentException("Archivo no existe o no se puede leer " + path);
        }
        var filename = path.getFileName().toString().replace(".xlsx", "");
        var output = Path.of("target/" + filename);
        Files.createDirectories(output);
        // Open and existing XLSX file
        try (var workBook = new XSSFWorkbook(path.toFile())) {
          if (filename.endsWith("-asistencia")) {
            Files.writeString(output.resolve("metadatos.csv"),
                csvFromSheet(workBook, "metadatos"));
            Files.writeString(output.resolve("asistencias.csv"),
                csvFromSheet(workBook, "asistencias"));
            Files.writeString(output.resolve("resultados.csv"),
                csvFromSheet(workBook, "resultados"));
            Files.writeString(output.resolve("resultados_partido.csv"),
                csvFromSheet(workBook, "resultados_partido"));
            Files.writeString(output.resolve("notas.csv"), csvFromSheet(workBook, "notas"));
            Files.writeString(output.resolve("datos_tipo_asistencia.csv"),
                csvFromSheet(workBook, "datos_tipo_asistencia"));
            Files.writeString(output.resolve("datos_grupo_parlamentario.csv"),
                csvFromSheet(workBook, "datos_grupo_parlamentario"));
            Files.writeString(output.resolve("version.csv"), csvFromSheet(workBook, "version"));
          }
          if (filename.endsWith("-votacion")) {
            Files.writeString(output.resolve("metadatos.csv"),
                csvFromSheet(workBook, "metadatos"));
            Files.writeString(output.resolve("etiquetas.csv"),
                csvFromSheet(workBook, "etiquetas"));
            Files.writeString(output.resolve("votaciones.csv"),
                csvFromSheet(workBook, "votaciones"));
            Files.writeString(output.resolve("resultados.csv"),
                csvFromSheet(workBook, "resultados"));
            Files.writeString(output.resolve("resultados_partido.csv"),
                csvFromSheet(workBook, "resultados_partido"));
            Files.writeString(output.resolve("notas.csv"), csvFromSheet(workBook, "notas"));
            Files.writeString(output.resolve("datos_tipo_votacion.csv"),
                csvFromSheet(workBook, "datos_tipo_votacion"));
            Files.writeString(output.resolve("datos_grupo_parlamentario.csv"),
                csvFromSheet(workBook, "datos_grupo_parlamentario"));
            Files.writeString(output.resolve("version.csv"), csvFromSheet(workBook, "version"));
          }
        } catch (InvalidFormatException e) {
          e.printStackTrace();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private static StringBuilder csvFromSheet(XSSFWorkbook workbook, String sheetName) {
    StringBuilder b = new StringBuilder();
    for (Row row : workbook.getSheet(sheetName)) {
      // Loop through all rows and add ","
      Iterator<Cell> cellIterator = row.cellIterator();
      StringBuilder stringBuffer = new StringBuilder();
      while (cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
        String v;
        try {
          if (cell.getCellType().equals(CellType.FORMULA)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            v = formatter.formatCellValue(cell, evaluator);
          } else {
            v = cell.getStringCellValue();
          }
        } catch (Exception e) {
          DataFormatter formatter = new DataFormatter();
          v = formatter.formatCellValue(cell).replace("\"", "");
        }
        if (v.contains(",")) {
          if (stringBuffer.length() != 0) {
            stringBuffer.append(",");
          }
          stringBuffer.append("\"").append(v).append("\"");
        } else {
          if (stringBuffer.length() != 0) {
            stringBuffer.append(",");
          }
          stringBuffer.append(v);
        }
      }
      if (stringBuffer.isEmpty()) {
        break;
      } else {
        b.append(stringBuffer).append("\n");
      }
    }
    return b;
  }
}
