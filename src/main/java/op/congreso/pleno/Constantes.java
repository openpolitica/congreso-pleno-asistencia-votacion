package op.congreso.pleno;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Constantes {

  public static final Path DATA = Path.of("./data");
  public static final String PERIODO_ACTUAL = "2021-2026";
  public static final Path DATA_PERIODO_ACTUAL = DATA.resolve(PERIODO_ACTUAL);
  public static final DateTimeFormatter FECHA_HORA_PATTERN =
      DateTimeFormatter.ofPattern("'fecha: 'd/MM/yyyy' hora: 'hh:mm' 'a", Locale.UK);

  public static final String ASISTENCIA = "ASISTENCIA:";
  public static final String VOTACION = "VOTACIÓN:";

  public static void main(String[] args) {
    String text = "FECHA: 7/09/2022 HORA: 04:19 pm";
    System.out.println(text.charAt(29));
    System.out.println(LocalDateTime.parse(text.toLowerCase(Locale.UK), FECHA_HORA_PATTERN));
  }
}
