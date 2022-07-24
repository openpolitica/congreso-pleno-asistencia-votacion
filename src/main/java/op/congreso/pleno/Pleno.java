package op.congreso.pleno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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
    static Pattern periodo = Pattern.compile("\\d\\d\\d\\d-\\d\\d\\d\\d$");

    String periodoParlamentario;
    String periodoAnual;
    String legislatura;
    String titulo;
    String url;
    LocalDate fecha;
    int quorum;

    void withLegislatura(String legislatura) {
      this.legislatura = legislatura;
      var m = periodo.matcher(legislatura);
      if (m.find()) this.periodoAnual = m.group();
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
        periodoAnual,
        legislatura,
        titulo,
        "",
        fecha,
        quorum
      );
    }
  }
}
