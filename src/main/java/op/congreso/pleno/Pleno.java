package op.congreso.pleno;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

  static Builder newBuilder() {
    return new Builder();
  }

  static class Builder {

    String periodoParlamentario;
    String periodoAnual;
    String legislatura;
    String titulo;
    String url;
    LocalDate fecha;
    int quorum;

    void withLegislatura(String legislatura) {
      this.legislatura = legislatura;
    }

    void withTitulo(String titulo) {
      this.titulo = titulo;
    }

    public void withFecha(LocalDate fecha) {
      this.fecha = fecha;
    }

    public void withQuorum(int quorum) {
      this.quorum = quorum;
    }

    public Pleno build() {
      return new Pleno(
        "2021-2026",
        "2021-2022",
        legislatura,
        titulo,
        "",
        fecha,
        quorum
      );
    }
  }
}
