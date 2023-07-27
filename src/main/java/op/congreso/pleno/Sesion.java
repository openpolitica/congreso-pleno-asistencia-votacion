package op.congreso.pleno;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Sesion(Pleno pleno, int quorum, LocalDateTime fechaHora) {
  public String printMetadatosAsCsv() {
    return ("metadato,valor\n"
        + "dia,"
        + fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE)
        + "\n"
        + "hora,"
        + fechaHora.format(DateTimeFormatter.ofPattern("HH:mm"))
        + "\n"
        + "quorum,"
        + quorum);
  }
}
