package op.congreso.pleno.asistencia;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record RegistroAsistencia(
  Pleno pleno,
  int quorum,
  LocalDateTime fechaHora,
  List<ResultadoCongresista<Asistencia>> asistencias,
  Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorGrupo,
  ResultadoAsistencia resultados,
  List<String> log
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
        .map(a ->
          a.grupoParlamentario() +
          ",\"" +
          a.congresista() +
          "\"," +
          a.resultado().name()
        )
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
    //grupo_parlamentario,numero_legal,presentes,ausentes,licencias,otros
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
      "grupo_parlamentario,numero_legal,presentes,ausentes,licencias,suspendidos,otros\n" +
      resultadosPorGrupo
        .keySet()
        .stream()
        .sorted(Comparator.comparing(GrupoParlamentario::nombre))
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
        .collect(Collectors.joining("\n")) +
      "\n" +
      "TOTAL," +
      resultados.total() +
      "," +
      resultados.presentes() +
      "," +
      resultados.ausentes() +
      "," +
      resultados.licencias() +
      "," +
      resultados.suspendidos() +
      "," +
      resultados.otros()
    );
  }

  public String printLog() {
    return String.join("\n", log);
  }

  public static class Builder {

    static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    Pleno pleno;
    int quorum;
    LocalDateTime fechaHora;
    List<ResultadoCongresista<Asistencia>> asistencias;
    Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorGrupo;
    ResultadoAsistencia resultados;
    Map<String, String> grupos = new HashMap<>();
    List<String> log = new ArrayList<>();

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
      var horaTime = LocalTime.parse(
        hora,
        DateTimeFormatter.ofPattern("HH:mm")
      );
      this.fechaHora = fecha.atTime(horaTime);
      return this;
    }

    public Builder withAsistencias(
      Map<String, String> grupos,
      List<ResultadoCongresista<Asistencia>> asistencias
    ) {
      this.asistencias = asistencias;
      this.grupos = grupos;
      return this;
    }

    public ResultadoAsistencia calculateResultados() {
      var b = ResultadoAsistencia.newBuilder();
      for (var asistencia : asistencias) {
        b.increase(asistencia.resultado());
      }
      return b.build();
    }

    public Map<GrupoParlamentario, ResultadoAsistencia> calculateResultadosPorGrupoParlamentario(
      Map<String, String> grupos
    ) {
      var results = new HashMap<GrupoParlamentario, ResultadoAsistencia.Builder>();
      for (var asistencia : asistencias) {
        var grupoParlamentario = new GrupoParlamentario(
          asistencia.grupoParlamentario(),
          grupos.get(asistencia.grupoParlamentario())
        );
        results.computeIfPresent(
          grupoParlamentario,
          (gp, resultadoAsistencia) ->
            resultadoAsistencia.increase(asistencia.resultado())
        );
        results.computeIfAbsent(
          grupoParlamentario,
          gp ->
            ResultadoAsistencia.newBuilder().increase(asistencia.resultado())
        );
      }
      return results
        .keySet()
        .stream()
        .collect(Collectors.toMap(k -> k, k -> results.get(k).build()));
    }

    public Builder withResultados(ResultadoAsistencia resultados) {
      this.resultados = resultados;
      return this;
    }

    public Builder withResultadosPorPartido(
      Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorPartido
    ) {
      this.resultadosPorGrupo = resultadosPorPartido;
      return this;
    }

    void checkCongresistas() {
      var errores = Congresistas.checkCongresistas(
        asistencias.stream().map(ResultadoCongresista::congresista).toList()
      );
      if (!errores.isEmpty()) {
        var map = errores
          .stream()
          .collect(Collectors.toMap(c -> c, Congresistas::findSimilar));
        asistencias =
          asistencias
            .stream()
            .map(v -> {
              if (map.containsKey(v.congresista())) {
                return v.replaceCongresista(map.get(v.congresista()));
              }
              return v;
            })
            .toList();
        log =
          Congresistas.checkCongresistas(
            asistencias.stream().map(ResultadoCongresista::congresista).toList()
          );
      }
    }

    public RegistroAsistencia build() {
      var calcResultsPerGroup = calculateResultadosPorGrupoParlamentario(
        grupos
      );
      var calcResults = calculateResultados();
      if (!calcResultsPerGroup.equals(resultadosPorGrupo)) {
        LOG.warn(
          "Resultados por grupo calculados son diferentes de capturados Pleno: {}",
          fechaHora
        );
        LOG.warn(
          "Diff: \nOld: {} \nNew: {}",
          resultadosPorGrupo,
          calcResultsPerGroup
        );
        this.resultadosPorGrupo = calcResultsPerGroup;
      }
      if (!calcResults.equals(resultados)) {
        LOG.warn(
          "Resultados calculados son diferentes de capturados Pleno: {}",
          fechaHora
        );
        LOG.warn("Diff: \nOld: {} \nNew: {}", resultados, calcResults);
        this.resultados = calcResults;
      }
      checkResultsMatch(resultados, resultadosPorGrupo);
      checkCongresistas();
      return new RegistroAsistencia(
        pleno,
        quorum,
        fechaHora,
        asistencias,
        resultadosPorGrupo,
        resultados,
        log
      );
    }

    private void checkResultsMatch(
      ResultadoAsistencia resultados,
      Map<GrupoParlamentario, ResultadoAsistencia> resultadosPorGrupo
    ) {
      var presentes = 0;
      var ausentes = 0;
      var otros = 0;
      var suspendidos = 0;
      var licencias = 0;
      var total = 0;
      for (var grupo : resultadosPorGrupo.keySet()) {
        presentes = presentes + resultadosPorGrupo.get(grupo).presentes();
        ausentes = ausentes + resultadosPorGrupo.get(grupo).ausentes();
        suspendidos = suspendidos + resultadosPorGrupo.get(grupo).suspendidos();
        otros = otros + resultadosPorGrupo.get(grupo).otros();
        licencias = licencias + resultadosPorGrupo.get(grupo).licencias();
        total = total + resultadosPorGrupo.get(grupo).total();
      }
      if (
        presentes != resultados.presentes() ||
        ausentes != resultados.ausentes() ||
        otros != resultados.otros() ||
        suspendidos != resultados.suspendidos() ||
        licencias != resultados.licencias() ||
        total != resultados.total()
      ) {
        LOG.warn(
          "Resultados calculados son diferentes de resulados generales: {}",
          fechaHora
        );
      }
    }
  }
}
