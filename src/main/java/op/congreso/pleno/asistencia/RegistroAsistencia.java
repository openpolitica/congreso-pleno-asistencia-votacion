package op.congreso.pleno.asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;

public record RegistroAsistencia(
  Pleno pleno,
  int quorum,
  LocalDateTime fechaHora,
  List<ResultadoCongresista<Asistencia>> asistencias,
  Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorGrupo,
  ResultadoAsistencia resultados
) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public String printAsistenciasAsCsv() {
    // ignore numero column
    return (
      "grupo_parlamentario,congresista,asistencia\n" +
      asistencias
        .stream()
        .sorted(Comparator.comparing(ResultadoCongresista::congresista))
        .map(a -> a.grupoParlamentario() + ",\"" + a.congresista() + "\"," + a.resultado().name())
        .collect(Collectors.joining("\n"))
    );
  }

  public String printMetadatosAsCsv() {
    return (
      "metadato,valor\n" +
      "dia," +
      fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE) +
      "\n" +
      "hora," +
      fechaHora.format(DateTimeFormatter.ofPattern("HH:mm")) +
      "\n" +
      "quorum," +
      quorum
    );
  }

  public String printResultadosAsCsv() {
    //
    //asistencia,total
    //numero_legal,130
    //presentes,104
    //ausentes,24
    //licencias,2
    //otros,0
    return (
      "resultado,total\n" +
      "numero_legal," +
      resultados.total() +
      "\n" +
      "presentes," +
      resultados.presentes() +
      "\n" +
      "ausentes," +
      resultados.ausentes() +
      "\n" +
      "licencias," +
      resultados.licencias() +
      "\n" +
      "suspendidos," +
      resultados.suspendidos() +
      "\n" +
      "otros," +
      resultados.otros()
    );
  }

  public String printResultadosPorGrupoAsCsv() {
    //
    //por_partido,numero_legal,presentes,ausentes,licencias,otros
    //PL,37,33,3,1,0
    //FP,24,19,5,0,0
    //AP,15,13,2,0,0
    //APP,15,12,3,0,0
    //AP-PIS,10,8,2,0,0
    //RP,9,8,1,0,0
    //SP,6,3,3,0,0
    //PP,5,2,3,0,0
    //JP,5,4,0,1,0
    //NA,4,2,2,0,0
    return (
      "por_partido,numero_legal,presentes,ausentes,licencias,suspendidos,otros\n" +
      resultadosPorGrupo
        .keySet()
        .stream()
        .map(k ->
          k.nombre() +
          "," +
          resultadosPorGrupo.get(k).total() +
          "," +
          resultadosPorGrupo.get(k).presentes() +
          "," +
          resultadosPorGrupo.get(k).ausentes() +
          "," +
          resultadosPorGrupo.get(k).licencias() +
          "," +
          resultadosPorGrupo.get(k).suspendidos() +
          "," +
          resultadosPorGrupo.get(k).otros()
        )
        .collect(Collectors.joining("\n"))
    );
  }

  public static class Builder {

    Pleno pleno;
    int quorum;
    LocalDateTime fechaHora;
    List<ResultadoCongresista<Asistencia>> asistencias;
    Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorGrupo;
    ResultadoAsistencia resultados;

    public Builder withPleno(Pleno pleno) {
      this.pleno = pleno;
      return this;
    }

    public Builder withQuorum(int quorum) {
      this.quorum = quorum;
      return this;
    }

    public Builder withFechaHora(LocalDateTime fechaHora) {
      this.fechaHora = fechaHora;
      return this;
    }

    public Builder withFechaHora(LocalDate fecha, String hora) {
      var horaTime = LocalTime.parse(hora, DateTimeFormatter.ofPattern("HH:mm"));
      this.fechaHora = fecha.atTime(horaTime);
      return this;
    }

    public Builder withAsistencias(List<ResultadoCongresista<Asistencia>> asistencias) {
      this.asistencias = asistencias;
      return this;
    }

    public Builder withResultados(ResultadoAsistencia resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorPartido) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    public RegistroAsistencia build() {
      return new RegistroAsistencia(pleno, quorum, fechaHora, asistencias, resultadosPorGrupo, resultados);
    }
  }
}
