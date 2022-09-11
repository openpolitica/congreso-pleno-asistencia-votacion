package op.congreso.pleno.app;

import static op.congreso.pleno.textract.TextractRegistroPleno.processLines;

import java.io.IOException;
import java.nio.file.Path;
import op.congreso.pleno.textract.TextractRegistroPleno;

public class RetryProcessRegistroPleno {

  public static void main(String[] args) throws IOException {
    Path of = Path.of("target/pdf");
    var pleno = TextractRegistroPleno.plenoToRetry(of);
    var lines = TextractRegistroPleno.retryProcessRegistroPleno(of);
    var regPleno = processLines(pleno, lines);
    SaveRegistroPlenoToCsv.save(regPleno);
  }
}
