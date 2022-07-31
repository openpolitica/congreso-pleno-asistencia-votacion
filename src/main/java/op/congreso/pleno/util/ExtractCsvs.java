package op.congreso.pleno.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExtractCsvs {

  public static void main(String[] args) throws IOException {
    var dir = Path.of(args[0]);
    System.out.println("Directory: " + dir);
    try (final var ls = Files.list(dir)) {
      ls.forEach(path -> {
        try {
          if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            System.out.println("Archivo no existe o no se puede leer " + path);
          }
          if (path.toString().endsWith(".xlsx")) {
            System.out.println("Processing XLSX file: " + path);
            var filename = path.getFileName().toString().replace(".xlsx", "");
            var output = Path.of("target/" + filename);
            Files.createDirectories(output);
            // Open and existing XLSX file
            try (var workBook = new XSSFWorkbook(path.toFile())) {
              if (filename.endsWith("-asistencia")) {
                Files.writeString(output.resolve("metadatos.csv"), csvFromSheet(workBook, "metadatos"));
                Files.writeString(output.resolve("asistencias.csv"), csvFromSheet(workBook, "asistencias"));
                Files.writeString(output.resolve("resultados.csv"), csvFromSheet(workBook, "resultados"));
                Files.writeString(
                  output.resolve("resultados_partido.csv"),
                  csvFromSheet(workBook, "resultados_partido")
                );
                Files.writeString(output.resolve("notas.csv"), csvFromSheet(workBook, "notas"));
                Files.writeString(
                  output.resolve("datos_tipo_asistencia.csv"),
                  csvFromSheet(workBook, "datos_tipo_asistencia")
                );
                Files.writeString(
                  output.resolve("datos_grupo_parlamentario.csv"),
                  csvFromSheet(workBook, "datos_grupo_parlamentario")
                );
                Files.writeString(output.resolve("version.csv"), csvFromSheet(workBook, "version"));
              }
              if (filename.endsWith("-votacion")) {
                Files.writeString(output.resolve("metadatos.csv"), csvFromSheet(workBook, "metadatos"));
                Files.writeString(output.resolve("etiquetas.csv"), csvFromSheet(workBook, "etiquetas"));
                Files.writeString(output.resolve("votaciones.csv"), csvFromSheet(workBook, "votaciones"));
                Files.writeString(output.resolve("resultados.csv"), csvFromSheet(workBook, "resultados"));
                Files.writeString(
                  output.resolve("resultados_partido.csv"),
                  csvFromSheet(workBook, "resultados_partido")
                );
                Files.writeString(output.resolve("notas.csv"), csvFromSheet(workBook, "notas"));
                Files.writeString(
                  output.resolve("datos_tipo_votacion.csv"),
                  csvFromSheet(workBook, "datos_tipo_votacion")
                );
                Files.writeString(
                  output.resolve("datos_grupo_parlamentario.csv"),
                  csvFromSheet(workBook, "datos_grupo_parlamentario")
                );
                Files.writeString(output.resolve("version.csv"), csvFromSheet(workBook, "version"));
              }
            } catch (Exception e) {
              throw new RuntimeException("Error at " + path, e);
            }
          }
        } catch (Exception e) {
          throw new RuntimeException("Error at " + path, e);
        }
      });
    }
  }

  private static StringBuilder csvFromSheet(XSSFWorkbook workbook, String sheetName) {
    StringBuilder b = new StringBuilder();
    for (Row row : workbook.getSheet(sheetName)) {
      Iterator<Cell> cellIterator = row.cellIterator();
      StringBuilder builder = new StringBuilder();
      while (cellIterator.hasNext()) {
        var cell = cellIterator.next();
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
        // Escape quotes
        v = v.replace("\"", "'");
        if (v.contains(",")) {
          if (builder.length() != 0) {
            builder.append(",");
          }
          builder.append("\"").append(v).append("\"");
        } else if (!v.isBlank()) {
          if (builder.length() != 0) {
            builder.append(",");
          }
          builder.append(v);
        }
      }
      if (builder.isEmpty()) {
        break;
      } else {
        b.append(builder).append("\n");
      }
    }
    return b;
  }
}
