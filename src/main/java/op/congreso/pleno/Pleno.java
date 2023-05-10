package op.congreso.pleno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

public record Pleno(
    Periodo periodo,
    String titulo,
    LocalDate fecha,
    String url,
    Map<String, String> gruposParlamentarios) {
  public String id() {
    return (periodo.periodoParlamentario()
        + ":"
        + fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
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

    public Builder withLegislatura(String legislatura) {
      this.legislatura = legislatura;
      var m = periodo.matcher(legislatura);
      if (m.find()) this.periodoAnual = m.group();
      return this;
    }

    public Builder withPeriodoAnual(String periodoAnual) {
      this.periodoAnual = periodoAnual;
      return this;
    }

    public Builder withTitulo(String titulo) {
      this.titulo = titulo;
      return this;
    }

    public Builder withFecha(LocalDate fecha) {
      this.fecha = fecha;
      return this;
    }

    public Builder withGruposParlamentarios(Map<String, String> grupos) {
      this.gruposParlamentarios = grupos;
      return this;
    }

    public Pleno build() {
      return new Pleno(
          new Periodo(periodoParlamentario, periodoAnual, legislatura),
          titulo,
          fecha,
          url,
          gruposParlamentarios);
    }

    public Builder withPeriodoParlamentario(String periodoParlamentario) {
      this.periodoParlamentario = periodoParlamentario;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }
  }
}
