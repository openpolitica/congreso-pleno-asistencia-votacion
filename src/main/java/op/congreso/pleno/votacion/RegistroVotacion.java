package op.congreso.pleno.votacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;

public record RegistroVotacion(
  Pleno pleno,
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

  public String printMetadatosAsCsv() {
    //
    //asunto,"MOCIÓN 1486, QUE PROPONE LA CENSURA AL MINISTRO DE EDUCACIÓN, SEÑOR CARLOS ALFONSO GALLARDO GÓMEZ."
    //presidente,"ALVA PRIETO, MARÍA DEL CARMEN"
    return (
      "metadato,valor\n" +
      "dia," +
      fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE) +
      "\n" +
      "hora," +
      fechaHora.format(DateTimeFormatter.ofPattern("HH:mm")) +
      "\n" +
      "asunto,\"" +
      asunto.replace("\"", "'") +
      "\"\n" +
      "presidente,\"" +
      presidente +
      "\"\n" +
      "quorum," +
      quorum
    );
  }

  public String printVotacionesAsCsv() {
    // ignore numero column
    return (
      "grupo_parlamentario,congresista,asistencia\n" +
      votaciones
        .stream()
        .sorted(Comparator.comparing(ResultadoCongresista::congresista))
        .map(a -> a.grupoParlamentario() + ",\"" + a.congresista() + "\"," + a.resultado().name())
        .collect(Collectors.joining("\n"))
    );
  }

  public String printResultadosPorGrupoAsCsv() {
    return (
      "por_partido,numero_legal,si,no,abstenciones,sin_responder\n" +
      resultadosPorGrupo
        .keySet()
        .stream()
        .map(k ->
          k +
          "," +
          resultadosPorGrupo.get(k).total() +
          "," +
          resultadosPorGrupo.get(k).si() +
          "," +
          resultadosPorGrupo.get(k).no() +
          "," +
          resultadosPorGrupo.get(k).abstenciones() +
          "," +
          resultadosPorGrupo.get(k).sinResponder() +
          "\n"
        )
        .collect(Collectors.joining("\n"))
    );
  }

  public String printResultadosAsCsv() {
    return (
      "resultado,total\n" +
      "numero_legal," +
      resultados.total() +
      "\n" +
      "si," +
      resultados.si() +
      "\n" +
      "no," +
      resultados.no() +
      "\n" +
      "abstenciones," +
      resultados.abstenciones() +
      "\n" +
      "sin_responder," +
      resultados.sinResponder()
    )//            "ausentes," + resultados.ausentes() + "\n" + // + "\n" +
    //            "sin_responder," + resultados.sinResponder() + "\n" +
    //            "otros," + resultados.otros()
    ;
  }

  public static class Builder {

    Pleno pleno;
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
