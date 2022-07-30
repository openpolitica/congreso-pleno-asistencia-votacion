package op.congreso.pleno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.votacion.RegistroVotacion;

public record RegistroPleno(
  String periodoParlamentario,
  String periodoAnual,
  String legislatura,
  LocalDate fecha,
  String titulo,
  String url,
  Map<String, String> gruposParlamentarios,
  List<LocalDateTime> sesiones,
  Map<LocalDateTime, RegistroAsistencia> asistencias,
  Map<LocalDateTime, RegistroVotacion> votaciones,
  int potentialErrors,
  List<String> comments
) {
  public static Builder newBuilder(RegistroPlenoDocument document) {
    return new Builder(document);
  }

  public static Builder newBuilder(
    String periodoParlamentario,
    String periodoAnual,
    String legislatura,
    LocalDate fecha,
    String titulo,
    String url
  ) {
    return new Builder(periodoParlamentario, periodoAnual, legislatura, fecha, titulo, url);
  }

  public String printPlenoAsCsv() {
    return (
      "metadato,valor" +
      "\n" +
      "periodo_parlamentario," +
      periodoParlamentario +
      "\n" +
      "periodo_anual," +
      periodoAnual +
      "\n" +
      "legislatura," +
      legislatura +
      "\n" +
      "sesion," +
      titulo +
      "\n" +
      "url_pdf," +
      url +
      "\n" +
      "dia," +
      fecha.format(DateTimeFormatter.ISO_LOCAL_DATE) +
      "\n"
    );
  }

  public String printGruposParlametariosAsCsv() {
    return (
      "grupo_parlamentario,descripcion" +
      "\n" +
      gruposParlamentarios
        .keySet()
        .stream()
        .map(k -> k + "," + gruposParlamentarios.get(k))
        .collect(Collectors.joining("\n"))
    );
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
    Map<LocalDateTime, RegistroAsistencia> asistencias = new HashMap<>();
    Map<LocalDateTime, RegistroVotacion> votaciones = new HashMap<>();
    int potentialErrors = 0;
    List<String> comments = new ArrayList<>();

    public Builder(RegistroPlenoDocument document) {
      this.periodoParlamentario = document.periodoParlamentario();
      this.periodoAnual = document.periodoAnual();
      this.legislatura = document.legislatura();
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
      String url
    ) {
      this.periodoParlamentario = periodoParlamentario;
      this.periodoAnual = periodoAnual;
      this.legislatura = legislatura;
      this.fecha = fecha;
      this.titulo = titulo;
      this.url = url;
    }

    public void addAsistencia(RegistroAsistencia asistencia) {
      sesiones.add(asistencia.fechaHora());
      asistencias.put(asistencia.fechaHora(), asistencia);
    }

    public void addVotacion(RegistroVotacion votacion) {
      sesiones.add(votacion.fechaHora());
      votaciones.put(votacion.fechaHora(), votacion);
    }

    public RegistroPleno build() {
      return new RegistroPleno(
        periodoParlamentario,
        periodoAnual,
        legislatura,
        fecha,
        titulo,
        url,
        gruposParlamentarios,
        sesiones,
        asistencias,
        votaciones,
        potentialErrors,
        comments
      );
    }

    public Builder withGruposParlamentarios(Map<String, String> gruposParlamentarios) {
      this.gruposParlamentarios = gruposParlamentarios;
      return this;
    }

    public Pleno pleno() {
      return Pleno
        .newBuilder()
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
