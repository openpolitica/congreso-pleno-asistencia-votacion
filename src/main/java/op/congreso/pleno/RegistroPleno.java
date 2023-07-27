package op.congreso.pleno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import op.congreso.pleno.asistencia.AsistenciaSesion;
import op.congreso.pleno.votacion.VotacionSesion;

// TODO add provisional flag and test new PDFs
public record RegistroPleno(
    Periodo periodo,
    LocalDate fecha,
    String titulo,
    String url,
    Map<String, String> gruposParlamentarios,
    List<LocalDateTime> sesiones,
    Map<LocalDateTime, AsistenciaSesion> asistencias,
    Map<LocalDateTime, VotacionSesion> votaciones,
    int potentialErrors,
    List<String> comments) {
  public static Builder newBuilder(RegistroPlenoDocument document) {
    return new Builder(document);
  }

  public static Builder newBuilder(
      String periodoParlamentario,
      String periodoAnual,
      String legislatura,
      LocalDate fecha,
      String titulo,
      String url) {
    return new Builder(periodoParlamentario, periodoAnual, legislatura, fecha, titulo, url);
  }

  public String printPlenoAsCsv() {
    return ("metadato,valor"
        + "\n"
        + "periodo_parlamentario,"
        + periodo.periodoParlamentario()
        + "\n"
        + "periodo_anual,"
        + periodo.periodoAnual()
        + "\n"
        + "legislatura,"
        + periodo.legislatura()
        + "\n"
        + "sesion,"
        + titulo
        + "\n"
        + "url_pdf,"
        + url
        + "\n"
        + "dia,"
        + fecha.format(DateTimeFormatter.ISO_LOCAL_DATE));
  }

  public String printGruposParlametariosAsCsv() {
    return ("grupo_parlamentario,descripcion"
        + "\n"
        + gruposParlamentarios.keySet().stream()
            .sorted()
            .map(k -> k + "," + gruposParlamentarios.get(k))
            .collect(Collectors.joining("\n")));
  }

  public static class Builder {

    final String periodoParlamentario;
    final String periodoAnual;
    final String legislatura;
    final LocalDate fecha;
    final String titulo;
    final String url;
    Map<String, String> gruposParlamentarios = new HashMap<>();
    List<LocalDateTime> sesiones = new ArrayList<>();
    Map<LocalDateTime, AsistenciaSesion> asistencias = new LinkedHashMap<>();
    Map<LocalDateTime, VotacionSesion> votaciones = new LinkedHashMap<>();
    int potentialErrors = 0;
    List<String> comments = new ArrayList<>();

    public Builder(RegistroPlenoDocument document) {
      this.periodoParlamentario = document.periodo().periodoParlamentario();
      this.periodoAnual = document.periodo().periodoAnual();
      this.legislatura = document.periodo().legislatura();
      this.titulo = document.titulo();
      this.url = document.url();
      this.fecha = LocalDate.parse(document.fecha(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public Builder(
        String periodoParlamentario,
        String periodoAnual,
        String legislatura,
        LocalDate fecha,
        String titulo,
        String url) {
      this.periodoParlamentario = periodoParlamentario;
      this.periodoAnual = periodoAnual;
      this.legislatura = legislatura;
      this.fecha = fecha;
      this.titulo = titulo;
      this.url = url;
    }

    public void addAsistencia(AsistenciaSesion asistencia) {
      sesiones.add(asistencia.sesion().fechaHora());
      asistencias.put(asistencia.sesion().fechaHora(), asistencia);
    }

    public void addVotacion(VotacionSesion votacion) {
      sesiones.add(votacion.sesion().fechaHora());
      votaciones.put(votacion.sesion().fechaHora(), votacion);
    }

    public RegistroPleno build() {
      return new RegistroPleno(
          new Periodo(periodoParlamentario, periodoAnual, legislatura),
          fecha,
          titulo,
          url,
          gruposParlamentarios,
          sesiones,
          asistencias,
          votaciones,
          potentialErrors,
          comments);
    }

    public Builder withGruposParlamentarios(Map<String, String> gruposParlamentarios) {
      this.gruposParlamentarios = gruposParlamentarios;
      return this;
    }

    public Pleno pleno() {
      return Pleno.newBuilder()
          .withPeriodoParlamentario(periodoParlamentario)
          .withPeriodoAnual(periodoAnual)
          .withLegislatura(legislatura)
          .withTitulo(titulo)
          .withFecha(fecha)
          .withUrl(url)
          .withGruposParlamentarios(gruposParlamentarios)
          .build();
    }
  }
}
