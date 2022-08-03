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
import op.congreso.pleno.asistencia.RegistroAsistencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      "grupo_parlamentario,congresista,votacion\n" +
      votaciones
        .stream()
        .sorted(Comparator.comparing(ResultadoCongresista::congresista))
        .map(v -> v.grupoParlamentario() + ",\"" + v.congresista() + "\"," + v.resultado().name())
        .collect(Collectors.joining("\n"))
    );
  }

  public String printResultadosPorGrupoAsCsv() {
    return (
      "por_partido,numero_legal,si,no,abstenciones,sin_responder,ausentes,licencias,otros\n" +
      resultadosPorGrupo
        .keySet()
        .stream()
        .sorted(Comparator.comparing(GrupoParlamentario::nombre))
        .map(k ->
          k.nombre() +
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
          "," +
          resultadosPorGrupo.get(k).ausentes() +
          "," +
          resultadosPorGrupo.get(k).licencias() +
          "," +
          resultadosPorGrupo.get(k).otros()
        )
        .collect(Collectors.joining("\n")) + "\n" +
          "TOTAL," + resultados.total() + "," + resultados.si() + "," + resultados.no() + "," + resultados.abstenciones() + "," +
          resultados.sinResponder() + "," + resultados.ausentes() + "," + resultados.licencias() + "," + resultados.otros()
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
      resultados.sinResponder() +
      "\n" +
      "ausentes," +
      resultados.ausentes() +
      "\n" +
      "licencias," +
      resultados.licencias() +
      "\n" +
      "otros," +
      resultados.otros()
    ); //            "otros," + resultados.otros() //            "sin_responder," + resultados.sinResponder() + "\n" + //            "ausentes," + resultados.ausentes() + "\n" + // + "\n" +
  }

  public String printEtiquetasAsCsv() {
    return (
      "etiqueta,valor\n" +
      etiquetas.keySet().stream().map(k -> k + ",\"" + etiquetas.get(k) + "\"").collect(Collectors.joining("\n"))
    );
  }

  public static class Builder {

    static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    Pleno pleno;
    int quorum;
    LocalDateTime fechaHora;
    String presidente, asunto;
    Map<String, String> etiquetas = new HashMap<>();
    List<ResultadoCongresista<Votacion>> votaciones;
    Map<GrupoParlamentario, ResultadoVotacion> resultadosPorGrupo;
    ResultadoVotacion resultados;
    Map<String, String> grupos = new HashMap<>();

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

    public Builder withVotaciones(Map<String, String> grupos, List<ResultadoCongresista<Votacion>> votaciones) {
      this.grupos = grupos;
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

    public ResultadoVotacion calculateResultados() {
      var b = ResultadoVotacion.newBuilder();
      for (var votacion : votaciones) {
        b.increase(votacion.resultado());
      }
      return b.build();
    }

    public Map<GrupoParlamentario, ResultadoVotacion> calculateResultadosPorGrupoParlamentario(
      Map<String, String> grupos
    ) {
      var results = new HashMap<GrupoParlamentario, ResultadoVotacion.Builder>();
      for (var votacion : votaciones) {
        var grupoParlamentario = new GrupoParlamentario(
          votacion.grupoParlamentario(),
          grupos.get(votacion.grupoParlamentario())
        );
        results.computeIfPresent(grupoParlamentario, (gp, resultado) -> resultado.increase(votacion.resultado()));
        results.computeIfAbsent(
          grupoParlamentario,
          gp -> ResultadoVotacion.newBuilder().increase(votacion.resultado())
        );
      }
      return results.keySet().stream().collect(Collectors.toMap(k -> k, k -> results.get(k).build()));
    }

    public RegistroVotacion build() {
      var calcResultsPerGroup = calculateResultadosPorGrupoParlamentario(grupos);
      var calcResults = calculateResultados();
      if (!calcResultsPerGroup.equals(resultadosPorGrupo)) {
        LOG.warn("Resultados por grupo calculados son diferentes de capturados Pleno: {}", fechaHora);
        LOG.warn("Diff: \nOld: {} \nNew: {}", resultadosPorGrupo, calcResultsPerGroup);
        this.resultadosPorGrupo = calcResultsPerGroup;
      }
      if (!calcResults.equals(resultados)) {
        LOG.warn("Resultados calculados son diferentes de capturados Pleno: {}", fechaHora);
        LOG.warn("Diff: \nOld: {} \nNew: {}", resultados, calcResults);
        this.resultados = calcResults;
      }
      // TODO check sum per group equal to results
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
