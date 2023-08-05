package op.congreso.pleno.app;

import static op.congreso.pleno.textract.TextractRegistroPleno.processLines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import op.congreso.pleno.Rutas;
import op.congreso.pleno.db.SaveRegistroPlenoToCsv;
import op.congreso.pleno.textract.TextractRegistroPleno;

/** Vuelve a procesar el texto bajo {@code Constantes.TMP_DIR} */
public class RetryProcessRegistroPleno {

  public static void main(String[] args) throws IOException {
    var pleno = TextractRegistroPleno.plenoToRetry(Rutas.TMP_DIR);
    var lines = TextractRegistroPleno.retryProcessRegistroPleno(Rutas.TMP_DIR);
    var regPleno = processLines(pleno, lines);
    Files.writeString(Path.of("pr-title.txt"), pleno.prTitle());
    Files.writeString(Path.of("pr-branch.txt"), pleno.prBranchName());
    Files.writeString(Path.of("pr-content.txt"), pleno.prContent());
    SaveRegistroPlenoToCsv.save(regPleno);
  }
}
