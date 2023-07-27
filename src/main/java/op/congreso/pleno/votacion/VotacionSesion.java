package op.congreso.pleno.votacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.Congresistas;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Votacion durante sesion de pleno */
public record VotacionSesion(
    Sesion sesion,
    String presidente,
    String asunto,
    Map<String, String> etiquetas,
    List<ResultadoCongresista<Votacion>> votaciones,
    Map<GrupoParlamentario, VotacionAgregada> resultadosPorGrupo,
    VotacionAgregada resultados,
    List<String> log) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public String printMetadatosAsCsv() {
    return ("metadato,valor\n"
        + "dia,"
        + sesion.fechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE)
        + "\n"
        + "hora,"
        + sesion.fechaHora().format(DateTimeFormatter.ofPattern("HH:mm"))
        + "\n"
        + "asunto,\""
        + asunto.replace("\"", "'")
        + "\"\n"
        + "presidente,\""
        + presidente
        + "\"\n"
        + "quorum,"
        + sesion.quorum());
  }

  public String printVotacionesAsCsv() {
    // ignore numero column
    return ("grupo_parlamentario,congresista,votacion\n"
        + votaciones.stream()
            .sorted(Comparator.comparing(ResultadoCongresista::congresista))
            .map(
                v ->
                    v.grupoParlamentario() + ",\"" + v.congresista() + "\"," + v.resultado().name())
            .collect(Collectors.joining("\n")));
  }

  public String printResultadosPorGrupoAsCsv() {
    return ("grupo_parlamentario,numero_legal,si,no,abstenciones,sin_responder,ausentes,licencias,otros\n"
        + resultadosPorGrupo.keySet().stream()
            .sorted(Comparator.comparing(GrupoParlamentario::nombre))
            .map(
                k ->
                    k.nombre()
                        + ","
                        + resultadosPorGrupo.get(k).total()
                        + ","
                        + resultadosPorGrupo.get(k).si()
                        + ","
                        + resultadosPorGrupo.get(k).no()
                        + ","
                        + resultadosPorGrupo.get(k).abstenciones()
                        + ","
                        + resultadosPorGrupo.get(k).sinResponder()
                        + ","
                        + resultadosPorGrupo.get(k).ausentes()
                        + ","
                        + resultadosPorGrupo.get(k).licencias()
                        + ","
                        + resultadosPorGrupo.get(k).otros())
            .collect(Collectors.joining("\n"))
        + "\n"
        + "TOTAL,"
        + resultados.total()
        + ","
        + resultados.si()
        + ","
        + resultados.no()
        + ","
        + resultados.abstenciones()
        + ","
        + resultados.sinResponder()
        + ","
        + resultados.ausentes()
        + ","
        + resultados.licencias()
        + ","
        + resultados.otros());
  }

  public String printResultadosAsCsv() {
    return ("resultado,total\n"
        + "numero_legal,"
        + resultados.total()
        + "\n"
        + "si,"
        + resultados.si()
        + "\n"
        + "no,"
        + resultados.no()
        + "\n"
        + "abstenciones,"
        + resultados.abstenciones()
        + "\n"
        + "sin_responder,"
        + resultados.sinResponder()
        + "\n"
        + "ausentes,"
        + resultados.ausentes()
        + "\n"
        + "licencias,"
        + resultados.licencias()
        + "\n"
        + "otros,"
        + resultados.otros());
  }

  public String printEtiquetasAsCsv() {
    return ("etiqueta,valor\n"
        + etiquetas.keySet().stream()
            .map(k -> k + ",\"" + etiquetas.get(k) + "\"")
            .collect(Collectors.joining("\n")));
  }

  public String printLog() {
    return String.join("\n", log);
  }

  public static class Builder {

    static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    Pleno pleno;
    int quorum;
    LocalDateTime fechaHora;
    String presidente, asunto;
    Map<String, String> etiquetas = new HashMap<>();
    List<ResultadoCongresista<Votacion>> votaciones;
    Map<GrupoParlamentario, VotacionAgregada> resultadosPorGrupo;
    VotacionAgregada resultados;
    Map<String, String> grupos = new HashMap<>();
    List<String> log = new ArrayList<>();

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

    public Builder withVotaciones(
        Map<String, String> grupos, List<ResultadoCongresista<Votacion>> votaciones) {
      this.grupos = grupos;
      this.votaciones = votaciones;
      return this;
    }

    public Builder withResultados(VotacionAgregada resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(
        Map<GrupoParlamentario, VotacionAgregada> resultadosPorPartido) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    public VotacionAgregada calculateResultados() {
      var b = VotacionAgregada.newBuilder();
      for (var votacion : votaciones) {
        b.increase(votacion.resultado());
      }
      return b.build();
    }

    public Map<GrupoParlamentario, VotacionAgregada> calculateResultadosPorGrupoParlamentario(
        Map<String, String> grupos) {
      var results = new HashMap<GrupoParlamentario, VotacionAgregada.Builder>();
      for (var votacion : votaciones) {
        var grupoParlamentario =
            new GrupoParlamentario(
                votacion.grupoParlamentario(), grupos.get(votacion.grupoParlamentario()));
        results.computeIfPresent(
            grupoParlamentario, (gp, resultado) -> resultado.increase(votacion.resultado()));
        results.computeIfAbsent(
            grupoParlamentario, gp -> VotacionAgregada.newBuilder().increase(votacion.resultado()));
      }
      return results.keySet().stream()
          .collect(Collectors.toMap(k -> k, k -> results.get(k).build()));
    }

    void checkCongresistas() {
      var errores =
          Congresistas.checkCongresistas(
              votaciones.stream().map(ResultadoCongresista::congresista).toList());
      if (!errores.isEmpty()) {
        var map = errores.stream().collect(Collectors.toMap(c -> c, Congresistas::findSimilar));
        votaciones =
            votaciones.stream()
                .map(
                    v -> {
                      if (map.containsKey(v.congresista())) {
                        return v.replaceCongresista(map.get(v.congresista()));
                      }
                      return v;
                    })
                .toList();
        log =
            Congresistas.checkCongresistas(
                votaciones.stream().map(ResultadoCongresista::congresista).toList());
      }
    }

    public VotacionSesion build() {
      var calcResultsPerGroup = calculateResultadosPorGrupoParlamentario(grupos);
      var calcResults = calculateResultados();
      if (!calcResultsPerGroup.equals(resultadosPorGrupo)) {
        LOG.warn(
            "Resultados por grupo calculados son diferentes de capturados Pleno: {}", fechaHora);
        LOG.warn("Diff: \nOld: {} \nNew: {}", resultadosPorGrupo, calcResultsPerGroup);
        this.resultadosPorGrupo = calcResultsPerGroup;
      }
      if (!calcResults.equals(resultados)) {
        LOG.warn("Resultados calculados son diferentes de capturados Pleno: {}", fechaHora);
        LOG.warn("Diff: \nOld: {} \nNew: {}", resultados, calcResults);
        this.resultados = calcResults;
      }
      checkResultsMatch(resultados, resultadosPorGrupo);
      checkCongresistas();
      return new VotacionSesion(
          new Sesion(pleno, quorum, fechaHora),
          presidente,
          asunto,
          etiquetas,
          votaciones,
          resultadosPorGrupo,
          resultados,
          log);
    }

    private void checkResultsMatch(
        VotacionAgregada resultados, Map<GrupoParlamentario, VotacionAgregada> resultadosPorGrupo) {
      var si = 0;
      var no = 0;
      var abstenciones = 0;
      var sinResponder = 0;
      var ausentes = 0;
      var otros = 0;
      var licencias = 0;
      var total = 0;
      for (var grupo : resultadosPorGrupo.keySet()) {
        si = si + resultadosPorGrupo.get(grupo).si();
        no = no + resultadosPorGrupo.get(grupo).no();
        abstenciones = abstenciones + resultadosPorGrupo.get(grupo).abstenciones();
        sinResponder = sinResponder + resultadosPorGrupo.get(grupo).sinResponder();
        ausentes = ausentes + resultadosPorGrupo.get(grupo).ausentes();
        otros = otros + resultadosPorGrupo.get(grupo).otros();
        licencias = licencias + resultadosPorGrupo.get(grupo).licencias();
        total = total + resultadosPorGrupo.get(grupo).total();
      }
      if (si != resultados.si()
          || no != resultados.no()
          || ausentes != resultados.ausentes()
          || otros != resultados.otros()
          || sinResponder != resultados.sinResponder()
          || licencias != resultados.licencias()
          || total != resultados.total()) {
        LOG.warn("Resultados calculados son diferentes de resulados generales: {}", fechaHora);
      }
    }
  }
}
