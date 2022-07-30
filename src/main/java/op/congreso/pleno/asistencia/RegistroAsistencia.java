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
    return "grupo_parlamentario,congresista,asistencia\n" +
            asistencias.stream()
                    .sorted(Comparator.comparing(ResultadoCongresista::congresista))
                    .map(a -> a.grupoParlamentario() + ",\"" + a.congresista() + "\"," + a.resultado().name())
                    .collect(Collectors.joining("\n"));
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
