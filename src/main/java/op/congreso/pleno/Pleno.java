package op.congreso.pleno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

public record Pleno(
  String periodoParlamentario,
  String periodoAnual,
  String legislatura,
  String titulo,
  LocalDate fecha,
  String url,
  Map<String, String> gruposParlamentarios
) {
  public String id() {
    return (periodoParlamentario + ":" + fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    static Pattern periodo = Pattern.compile("\\d\\d\\d\\d-\\d\\d\\d\\d$");

    String periodoParlamentario;
    String periodoAnual;
    String legislatura;
    String titulo;
    String url;
    LocalDate fecha;
    Map<String, String> gruposParlamentarios;

    public void withLegislatura(String legislatura) {
      this.legislatura = legislatura;
      var m = periodo.matcher(legislatura);
      if (m.find()) this.periodoAnual = m.group();
    }

    public void withTitulo(String titulo) {
      this.titulo = titulo;
    }

    public void withFecha(LocalDate fecha) {
      this.fecha = fecha;
    }

    public Builder withGruposParlamentarios(Map<String, String> grupos) {
      this.gruposParlamentarios = grupos;
      return this;
    }

    public Pleno build() {
      return new Pleno("2021-2026", periodoAnual, legislatura, titulo, fecha, url, gruposParlamentarios);
    }
  }
}
