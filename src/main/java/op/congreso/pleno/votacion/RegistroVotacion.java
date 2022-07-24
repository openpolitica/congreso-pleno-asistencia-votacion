package op.congreso.pleno.votacion;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;

public record RegistroVotacion(
  Pleno pleno,
  LocalTime hora,
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
    LocalTime hora;
    String presidente, asunto;
    Map<String, String> etiquetas = new HashMap<>();
    List<ResultadoCongresista<Votacion>> votaciones;
    Map<GrupoParlamentario, ResultadoVotacion> resultadosPorGrupo;
    ResultadoVotacion resultados;

    public Builder withPleno(Pleno pleno) {
      this.pleno = pleno;
      return this;
    }

    public Builder withHora(LocalTime hora) {
      this.hora = hora;
      return this;
    }

    public Builder withHora(String hora) {
      this.hora = LocalTime.parse(hora, DateTimeFormatter.ofPattern("HH:mm"));
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

    public Builder withVotaciones(
      List<ResultadoCongresista<Votacion>> votaciones
    ) {
      this.votaciones = votaciones;
      return this;
    }

    public Builder withResultados(ResultadoVotacion resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(
      Map<GrupoParlamentario, ResultadoVotacion> resultadosPorPartido
    ) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    public RegistroVotacion build() {
      return new RegistroVotacion(
        pleno,
        hora,
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
