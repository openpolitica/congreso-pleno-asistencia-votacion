package op.congreso.pleno;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Rutas {

  public static final Path DATA = Path.of("./data");
  public static final String PERIODO_ACTUAL = "2021-2026";
  public static final Path DATA_PERIODO_ACTUAL = DATA.resolve(PERIODO_ACTUAL);
  public static final Path TMP_DIR = Path.of("out/pdf");

  public static final DateTimeFormatter FECHA_HORA_PATTERN =
      DateTimeFormatter.ofPattern("'fecha: 'd/MM/yyyy' hora: 'hh:mm' 'a", Locale.UK);
}
