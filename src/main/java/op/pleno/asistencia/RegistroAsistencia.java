package op.pleno.asistencia;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import op.pleno.Pleno;
import op.pleno.ResultadoCongresista;

public record RegistroAsistencia(
    Pleno pleno,
    LocalTime hora,
    List<ResultadoCongresista> asistencias,
    Map<String, ResultadoAsistencia> resultadosPorGrupo,
    ResultadoAsistencia resultados
) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    Pleno pleno;
    LocalTime hora;
    List<ResultadoCongresista> asistencias;
    Map<String, ResultadoAsistencia> resultadosPorGrupo;
    ResultadoAsistencia resultados;

    public Builder withPleno(Pleno pleno) {
      this.pleno = pleno;
      return this;
    }

    public Builder withHora(String hora) {
      this.hora = LocalTime.parse(hora, DateTimeFormatter.ofPattern("HH:mm"));
      return this;
    }

    public Builder withAsistencias(List<ResultadoCongresista> asistencias) {
      this.asistencias = asistencias;
      return this;
    }

    public Builder withResultados(ResultadoAsistencia resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(Map<String, ResultadoAsistencia> resultadosPorPartido) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    public RegistroAsistencia build() {
      return new RegistroAsistencia(pleno, hora, asistencias, resultadosPorGrupo, resultados);
    }
  }

  String id() {
    return pleno.fecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T" +
        hora.format(DateTimeFormatter.ofPattern("HH:mm")) + "-asistencia";
  }
}
