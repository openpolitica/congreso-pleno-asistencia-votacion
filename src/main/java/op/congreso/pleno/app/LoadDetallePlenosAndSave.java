package op.congreso.pleno.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import op.congreso.pleno.AsistenciaPlenos;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.RegistroPleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.VotacionPlenos;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadDetallePlenosAndSave {

  static final Logger LOG = LoggerFactory.getLogger(LoadDetallePlenosAndSave.class);
  final ObjectMapper mapper = new CsvMapper();

  public static void main(String[] args) throws IOException {
    new LoadDetallePlenosAndSave().load();
  }

  void load() throws IOException {
    var data = Path.of("data");
    LOG.info("Starting to load data from: {}", data.toAbsolutePath());
    try (final var ls = Files.list(data)) {
      var periodos = ls.toList();
      for (var periodo : periodos) {
        var plenos = paths(periodo) // periodo anual
          .flatMap(this::paths) // mes
          .flatMap(this::paths) // fecha/pleno
          .toList();
        for (var pleno : plenos) {
          var grupos = loadGruposParlamentarios(pleno.resolve("grupo_parlamentario.csv"));
          var registroPlenoBuilder = loadRegistroPleno(pleno.resolve("pleno.csv")).withGruposParlamentarios(grupos);

          try (var list = Files.list(pleno)) {
            var lsPleno = list.toList();
            var asistencias = lsPleno
              .stream()
              .filter(s -> s.toString().endsWith("-asistencia"))
              .map(p -> loadAsistencia(p, registroPlenoBuilder.pleno(), grupos))
              .peek(registroPlenoBuilder::addAsistencia)
              .collect(Collectors.toSet());
            var asistenciaPlenos = new AsistenciaPlenos(periodo.getFileName().toString(), asistencias);
            new SaveAsistenciaPlenosToSqlite().accept(asistenciaPlenos);
            var votaciones = lsPleno
              .stream()
              .filter(s -> s.toString().endsWith("-votacion"))
              .map(p -> loadVotacion(p, registroPlenoBuilder.pleno(), grupos))
              .peek(registroPlenoBuilder::addVotacion)
              .collect(Collectors.toSet());
            var votacionPlenos = new VotacionPlenos(periodo.getFileName().toString(), votaciones);
            new SaveVotacionPlenosToSqlite().accept(votacionPlenos);
          }

          var registroPleno = registroPlenoBuilder.withGruposParlamentarios(grupos).build();
          SaveRegistroPlenoToCsv.save(registroPleno);
          // TODO save registro plenos to DB
        }
      }
    }
  }

  private RegistroPleno.Builder loadRegistroPleno(Path path) {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        f.put(v.get("metadato"), v.get("valor"));
      }
      LocalDate fecha = LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return RegistroPleno.newBuilder(
        f.get("periodo_parlamentario").trim(),
        f.get("periodo_anual").trim(),
        f.get("legislatura").trim(),
        fecha,
        f.get("sesion").trim(),
        f.get("url_pdf").trim()
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Stream<Path> paths(Path p) {
    try {
      if (Files.isDirectory(p)) return Files.list(p); else return Stream.of();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private RegistroVotacion loadVotacion(Path path, Pleno pleno, Map<String, String> grupos) {
    try {
      var builder = loadVotacionMetadatos(path.resolve("metadatos.csv"), pleno);
      loadVotacionEtiquetas(path.resolve("etiquetas.csv")).forEach(builder::addEtiqueta);
      var votaciones = loadVotacionLista(path.resolve("votaciones.csv"));
      var resultados = loadVotacionResultado(path.resolve("resultados.csv"));
      var resultadosPorGrupo = loadVotacionResultadoPorPartido(path.resolve("resultados_grupo.csv"), grupos);

      return builder
        .withVotaciones(grupos, votaciones)
        .withResultados(resultados)
        .withResultadosPorPartido(resultadosPorGrupo)
        .build();
    } catch (Exception e) {
      throw new RuntimeException("Error en " + path, e);
    }
  }

  private RegistroAsistencia loadAsistencia(Path path, Pleno pleno, Map<String, String> grupos) {
    try {
      var builder = loadAsistenciaMetadatos(path.resolve("metadatos.csv"), pleno);
      var asistencias = loadAsistenciaLista(path.resolve("asistencias.csv"));
      var resultados = loadAsistenciaResultado(path.resolve("resultados.csv"));
      var resultadosPorGrupo = loadAsistenciaResultadoPorPartido(path.resolve("resultados_grupo.csv"), grupos);

      return builder
        .withResultados(resultados)
        .withResultadosPorPartido(resultadosPorGrupo)
        .withAsistencias(grupos, asistencias)
        .build();
    } catch (Exception e) {
      throw new RuntimeException("Error at %s".formatted(path), e);
    }
  }

  private Map<String, String> loadGruposParlamentarios(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      var data = new LinkedHashMap<String, String>();
      while (it.hasNext()) {
        var m = it.next();
        data.put(m.get("grupo_parlamentario").trim(), m.get("descripcion").trim());
      }
      return data;
    }
  }

  private RegistroVotacion.Builder loadVotacionMetadatos(Path path, Pleno pleno) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        f.put(v.get("metadato"), v.get("valor"));
      }
      LocalDate fecha = LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return RegistroVotacion
        .newBuilder()
        .withPleno(pleno)
        .withQuorum(Integer.parseInt(f.get("quorum")))
        .withFechaHora(fecha, f.get("hora"))
        .withAsunto(f.get("asunto").trim())
        .withPresidente(f.get("presidente").trim());
    }
  }

  private Map<String, String> loadVotacionEtiquetas(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        if (!v.get("valor").isBlank()) {
          f.put(v.get("etiqueta"), v.get("valor"));
        }
      }
      return f;
    }
  }

  private RegistroAsistencia.Builder loadAsistenciaMetadatos(Path path, Pleno pleno) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        f.put(v.get("metadato"), v.get("valor"));
      }
      LocalDate fecha = LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return RegistroAsistencia
        .newBuilder()
        .withPleno(pleno)
        .withQuorum(Integer.parseInt(f.get("quorum")))
        .withFechaHora(fecha, f.get("hora"));
    }
  }

  private List<ResultadoCongresista<Asistencia>> loadAsistenciaLista(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // numero,grupo_parlamentario,congresista,asistencia
      var data = new ArrayList<ResultadoCongresista<Asistencia>>();
      while (it.hasNext()) {
        var m = it.next();
        var grupo_parlamentario = m.get("grupo_parlamentario").trim();
        var asistencia = m.get("asistencia").trim().toUpperCase();
        data.add(
          new ResultadoCongresista<>(grupo_parlamentario, m.get("congresista").trim(), Asistencia.of(asistencia))
        );
      }
      return data;
    }
  }

  private List<ResultadoCongresista<Votacion>> loadVotacionLista(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // numero,grupo_parlamentario,congresista,asistencia
      var data = new ArrayList<ResultadoCongresista<Votacion>>();
      while (it.hasNext()) {
        var m = it.next();
        var grupo_parlamentario = m.get("grupo_parlamentario").trim();
        var votacion = Votacion.of(m.get("votacion").trim());
        data.add(new ResultadoCongresista<>(grupo_parlamentario, m.get("congresista").trim(), votacion));
      }
      return data;
    }
  }

  private Map<GrupoParlamentario, ResultadoAsistencia> loadAsistenciaResultadoPorPartido(
    Path path,
    Map<String, String> grupos
  ) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // grupo_parlamentario,numero_legal,presentes,ausentes,licencias,otros
      var data = new HashMap<GrupoParlamentario, ResultadoAsistencia>();
      while (it.hasNext()) {
        var v = it.next();
        var partido = v.get("grupo_parlamentario");
        if (!partido.isBlank() && !partido.equals("TOTAL")) {
          assert grupos.containsKey(partido);
          data.put(
            new GrupoParlamentario(partido, grupos.get(partido)),
            new ResultadoAsistencia(
              Integer.parseInt(v.get("presentes")),
              Integer.parseInt(v.get("ausentes")),
              Integer.parseInt(v.get("licencias")),
              Integer.parseInt(v.getOrDefault("suspendidos", "0")),
              Integer.parseInt(v.get("otros")),
              Integer.parseInt(v.get("numero_legal"))
            )
          );
        }
      }
      return data;
    }
  }

  private Map<GrupoParlamentario, ResultadoVotacion> loadVotacionResultadoPorPartido(
    Path path,
    Map<String, String> gruposParlamentarios
  ) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // grupo_parlamentario,numero_legal,presentes,ausentes,licencias,otros
      var data = new HashMap<GrupoParlamentario, ResultadoVotacion>();
      while (it.hasNext()) {
        var v = it.next();
        var partido = v.get("grupo_parlamentario");
        if (!partido.isBlank() && !partido.equals("TOTAL")) {
          if (gruposParlamentarios.containsKey(partido)) {
            data.put(
              new GrupoParlamentario(partido, gruposParlamentarios.get(partido)),
              new ResultadoVotacion(
                Integer.parseInt(v.get("si")),
                Integer.parseInt(v.get("no")),
                Integer.parseInt(v.get("abstenciones")),
                Integer.parseInt(v.get("sin_responder")),
                Integer.parseInt(v.getOrDefault("ausentes", "0")),
                Integer.parseInt(v.getOrDefault("licencias", "0")),
                Integer.parseInt(v.getOrDefault("otros", "0")),
                Integer.parseInt(v.get("numero_legal"))
              )
            );
          }
        }
      }
      return data;
    }
  }

  private ResultadoAsistencia loadAsistenciaResultado(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // asistencia,total
      var t = new HashMap<String, Integer>();
      while (it.hasNext()) {
        var v = it.next();
        t.put(v.get("resultado"), Integer.parseInt(v.get("total")));
      }
      return new ResultadoAsistencia(
        t.get("presentes"),
        t.get("ausentes"),
        t.get("licencias"),
        t.getOrDefault("suspendidos", 0),
        t.get("otros"),
        t.get("numero_legal")
      );
    }
  }

  private ResultadoVotacion loadVotacionResultado(Path path) throws IOException {
    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(path.toFile())
    ) {
      // asistencia,total
      var t = new HashMap<String, Integer>();
      while (it.hasNext()) {
        var v = it.next();
        t.put(v.get("resultado"), Integer.parseInt(v.get("total")));
      }
      return new ResultadoVotacion(
        t.get("si"),
        t.get("no"),
        t.get("abstenciones"),
        t.get("sin_responder"),
        t.getOrDefault("ausentes", 0),
        t.getOrDefault("licencias", 0),
        t.getOrDefault("otros", 0),
        t.get("numero_legal")
      );
    }
  }
}
