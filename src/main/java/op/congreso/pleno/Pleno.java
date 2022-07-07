package op.congreso.pleno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Pleno(
  String periodoParlamentario,
  String periodoAnual,
  String legislatura,
  String titulo,
  String url,
  LocalDate fecha,
  int quorum
) {
  public String id() {
    return (
      fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "-" + titulo
    );
  }
}
