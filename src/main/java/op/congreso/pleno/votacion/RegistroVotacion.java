package op.congreso.pleno.votacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;

public record RegistroVotacion(
  Pleno pleno,
  String titulo,
  int quorum,
  LocalDateTime fechaHora,
  String presidente,
  String asunto,
  Map<String, String> etiquetas,
  List<ResultadoCongresista<Votacion>> votaciones,
  Map<GrupoParlamentario, ResultadoVotacion> resultadosPorGrupo,
  ResultadoVotacion resultados
) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    Pleno pleno;
    String titulo;
    int quorum;
    LocalDateTime fechaHora;
    String presidente, asunto;
    Map<String, String> etiquetas = new HashMap<>();
    List<ResultadoCongresista<Votacion>> votaciones;
    Map<GrupoParlamentario, ResultadoVotacion> resultadosPorGrupo;
    ResultadoVotacion resultados;

    public Builder withQuorum(int quorum) {
      this.quorum = quorum;
      return this;
    }

    public Builder withPleno(Pleno pleno) {
      this.pleno = pleno;
      return this;
    }

    public Builder withFechaHora(LocalDateTime hora) {
      this.fechaHora = hora;
      return this;
    }

    public Builder withFechaHora(LocalDate fecha, String hora) {
      var horaTime = LocalTime.parse(hora, DateTimeFormatter.ofPattern("HH:mm"));
      this.fechaHora = fecha.atTime(horaTime);
      return this;
    }

    public Builder withPresidente(String presidente) {
      this.presidente = presidente.trim();
      return this;
    }

    public void addEtiqueta(String key, String value) {
      this.etiquetas.put(key, value);
    }

    public Builder withAsunto(String asunto) {
      this.asunto = asunto;
      return this;
    }

    public Builder withVotaciones(List<ResultadoCongresista<Votacion>> votaciones) {
      this.votaciones = votaciones;
      return this;
    }

    public Builder withResultados(ResultadoVotacion resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(Map<GrupoParlamentario, ResultadoVotacion> resultadosPorPartido) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    public RegistroVotacion build() {
      return new RegistroVotacion(
        pleno,
        titulo,
        quorum,
        fechaHora,
        presidente,
        asunto,
        etiquetas,
        votaciones,
        resultadosPorGrupo,
        resultados
      );
    }
  }
}
