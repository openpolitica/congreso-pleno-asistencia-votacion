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
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.RegistroPleno;
import op.congreso.pleno.ResultadoCongresista;
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
        var cambios = new HashMap<LocalDate, List<Map<String, String>>>();
        try (final var iter =
            mapper
                .readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .<Map<String, String>>readValues(periodo.resolve("cambios.csv").toFile())) {
          while (iter.hasNext()) {
            final var map = iter.next();
            final var fecha =
                LocalDate.parse(map.get("pleno"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            cambios.computeIfPresent(
                fecha,
                (localDate, maps) -> {
                  maps.add(map);
                  return maps;
                });
            cambios.computeIfAbsent(
                fecha,
                localDate -> {
                  final var maps = new ArrayList<Map<String, String>>();
                  maps.add(map);
                  return maps;
                });
          }
        }

        var plenos =
            paths(periodo) // periodo anual
                .flatMap(this::paths) // mes
                .flatMap(this::paths) // fecha/pleno
                .toList();
        for (var pleno : plenos) {
          var grupos = loadGruposParlamentarios(pleno.resolve("grupo_parlamentario.csv"));
          var registroPlenoBuilder =
              loadRegistroPleno(pleno.resolve("pleno.csv")).withGruposParlamentarios(grupos);

          try (var list = Files.list(pleno)) {
            var lsPleno = list.toList();
            var asistencias =
                lsPleno.stream()
                    .filter(s -> s.toString().endsWith("-asistencia"))
                    .map(p -> loadAsistencia(p, registroPlenoBuilder.pleno(), grupos))
                    .peek(registroPlenoBuilder::addAsistencia)
                    .collect(Collectors.toSet());
            new SaveAsistenciaPlenosToSqlite().accept(asistencias);
            var votaciones =
                lsPleno.stream()
                    .filter(s -> s.toString().endsWith("-votacion"))
                    .map(p -> loadVotacion(p, registroPlenoBuilder.pleno(), grupos))
                    .peek(registroPlenoBuilder::addVotacion)
                    .collect(Collectors.toSet());
            new SaveVotacionPlenosToSqlite().accept(votaciones);
          }

          var registroPleno = registroPlenoBuilder.withGruposParlamentarios(grupos).build();

          var primeraAsistencia =
              registroPleno.asistencias().entrySet().stream()
                  .filter(p -> p.getValue().asistencias().size() == 130)
                  .findFirst()
                  .get()
                  .getValue()
                  .asistencias()
                  .stream()
                  .collect(
                      Collectors.toMap(
                          ResultadoCongresista::congresista,
                          ResultadoCongresista::grupoParlamentario));

          registroPleno
              .asistencias()
              .forEach(
                  (fechaHora, registroVotacion) -> {
                    final List<ResultadoCongresista<Asistencia>> asistencias =
                        registroVotacion.asistencias();
                    final int asistenciaTotal = asistencias.size();
                    if (asistenciaTotal != primeraAsistencia.size()) {
                      var congresistas =
                          asistencias.stream().map(ResultadoCongresista::congresista).toList();
                      LOG.error("[A] {}: Votacion incompleta {}", fechaHora, asistenciaTotal);
                      for (var c : primeraAsistencia.keySet()) {
                        if (!congresistas.contains(c)) {
                          LOG.error("[A] {}: Congresista faltante: {}", fechaHora, c);
                        }
                      }
                    }

                    asistencias.forEach(
                        asistencia -> {
                          final String grupo = primeraAsistencia.get(asistencia.congresista());
                          if (grupo == null) {
                            LOG.warn(
                                "[A] {}: Votacion de Congresista {} Grupo {} no coincide con Asistencia",
                                fechaHora,
                                asistencia.congresista(),
                                asistencia.grupoParlamentario());
                          } else if (!grupo.equals(asistencia.grupoParlamentario())) {
                            var found = false;
                            if (cambios.containsKey(registroPleno.fecha())) {
                              for (final var map : cambios.get(registroPleno.fecha())) {
                                if (map.get("congresista").equals(asistencia.congresista())
                                    && map.get("grupo_nuevo")
                                        .equals(asistencia.grupoParlamentario())) {
                                  found = true;
                                }
                              }
                            }
                            if (!found) {
                              LOG.error(
                                  "[A] {}: Congresista {} en grupo {}, debe ser {}",
                                  fechaHora,
                                  asistencia.congresista(),
                                  asistencia.grupoParlamentario(),
                                  grupo);
                            }
                          }
                        });
                  });
          registroPleno
              .votaciones()
              .forEach(
                  (fechaHora, registroVotacion) -> {
                    final List<ResultadoCongresista<Votacion>> votaciones =
                        registroVotacion.votaciones();
                    final int votacionesTotal = votaciones.size();
                    if (votacionesTotal != primeraAsistencia.size()) {
                      var congresistas =
                          votaciones.stream().map(ResultadoCongresista::congresista).toList();
                      LOG.error("[V] {}: Votacion incompleta {}", fechaHora, votacionesTotal);
                      for (var c : primeraAsistencia.keySet()) {
                        if (!congresistas.contains(c)) {
                          LOG.error("[V] {}: Congresista faltante: {}", fechaHora, c);
                        }
                      }
                    }

                    votaciones.forEach(
                        votacion -> {
                          final String grupo = primeraAsistencia.get(votacion.congresista());
                          if (grupo == null) {
                            LOG.warn(
                                "[V] {}: Votacion de Congresista {} Grupo {} no coincide con Asistencia",
                                fechaHora,
                                votacion.congresista(),
                                votacion.grupoParlamentario());
                          } else if (!grupo.equals(votacion.grupoParlamentario())) {
                            var found = false;
                            if (cambios.containsKey(registroPleno.fecha())) {
                              for (final var map : cambios.get(registroPleno.fecha())) {
                                if (map.get("congresista").equals(votacion.congresista())
                                    && map.get("grupo_nuevo")
                                        .equals(votacion.grupoParlamentario())) {
                                  found = true;
                                }
                              }
                            }
                            if (!found) {
                              LOG.error(
                                  "[V] {}: Congresista {} en grupo {}, debe ser {}",
                                  fechaHora,
                                  votacion.congresista(),
                                  votacion.grupoParlamentario(),
                                  grupo);
                            }
                          }
                        });
                  });
          SaveRegistroPlenoToCsv.save(registroPleno);
          // TODO save registro plenos to DB
        }
      }
    }
  }

  private RegistroPleno.Builder loadRegistroPleno(Path path) {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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
          f.get("url_pdf").trim());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Stream<Path> paths(Path p) {
    try {
      if (Files.isDirectory(p)) {
        return Files.list(p);
      } else {
        return Stream.of();
      }
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
      var resultadosPorGrupo =
          loadVotacionResultadoPorPartido(path.resolve("resultados_grupo.csv"), grupos);

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
      var resultadosPorGrupo =
          loadAsistenciaResultadoPorPartido(path.resolve("resultados_grupo.csv"), grupos);

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
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
      var data = new LinkedHashMap<String, String>();
      while (it.hasNext()) {
        var m = it.next();
        data.put(m.get("grupo_parlamentario").trim(), m.get("descripcion").trim());
      }
      return data;
    }
  }

  private RegistroVotacion.Builder loadVotacionMetadatos(Path path, Pleno pleno)
      throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        f.put(v.get("metadato"), v.get("valor"));
      }
      LocalDate fecha = LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return RegistroVotacion.newBuilder()
          .withPleno(pleno)
          .withQuorum(Integer.parseInt(f.get("quorum")))
          .withFechaHora(fecha, f.get("hora"))
          .withAsunto(f.get("asunto").trim())
          .withPresidente(f.get("presidente").trim());
    }
  }

  private Map<String, String> loadVotacionEtiquetas(Path path) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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

  private RegistroAsistencia.Builder loadAsistenciaMetadatos(Path path, Pleno pleno)
      throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
      var f = new HashMap<String, String>();
      while (it.hasNext()) {
        var v = it.next();
        f.put(v.get("metadato"), v.get("valor"));
      }
      LocalDate fecha = LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return RegistroAsistencia.newBuilder()
          .withPleno(pleno)
          .withQuorum(Integer.parseInt(f.get("quorum")))
          .withFechaHora(fecha, f.get("hora"));
    }
  }

  private List<ResultadoCongresista<Asistencia>> loadAsistenciaLista(Path path) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
      // numero,grupo_parlamentario,congresista,asistencia
      var data = new ArrayList<ResultadoCongresista<Asistencia>>();
      while (it.hasNext()) {
        var m = it.next();
        var grupo_parlamentario = m.get("grupo_parlamentario").trim();
        var asistencia = m.get("asistencia").trim().toUpperCase();
        data.add(
            new ResultadoCongresista<>(
                grupo_parlamentario, m.get("congresista").trim(), Asistencia.of(asistencia)));
      }
      return data;
    }
  }

  private List<ResultadoCongresista<Votacion>> loadVotacionLista(Path path) {
    Map<String, String> map = new HashMap<>();
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
      // numero,grupo_parlamentario,congresista,asistencia
      var data = new ArrayList<ResultadoCongresista<Votacion>>();
      while (it.hasNext()) {
        map = it.next();
        var grupo_parlamentario = map.get("grupo_parlamentario").trim();
        var votacion = Votacion.of(map.get("votacion").trim());
        data.add(
            new ResultadoCongresista<>(
                grupo_parlamentario, map.get("congresista").trim(), votacion));
      }
      return data;
    } catch (Exception e) {
      throw new RuntimeException("Error with path: " + path.toString() + " at " + map, e);
    }
  }

  private Map<GrupoParlamentario, ResultadoAsistencia> loadAsistenciaResultadoPorPartido(
      Path path, Map<String, String> grupos) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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
                  Integer.parseInt(v.get("numero_legal"))));
        }
      }
      return data;
    }
  }

  private Map<GrupoParlamentario, ResultadoVotacion> loadVotacionResultadoPorPartido(
      Path path, Map<String, String> gruposParlamentarios) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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
                    Integer.parseInt(v.get("numero_legal"))));
          }
        }
      }
      return data;
    }
  }

  private ResultadoAsistencia loadAsistenciaResultado(Path path) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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
          t.get("numero_legal"));
    }
  }

  private ResultadoVotacion loadVotacionResultado(Path path) throws IOException {
    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(path.toFile())) {
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
          t.get("numero_legal"));
    }
  }
}
