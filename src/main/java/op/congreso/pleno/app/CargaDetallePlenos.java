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
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.VotacionPlenos;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;

public class CargaDetallePlenos {

    final ObjectMapper mapper = new CsvMapper();

    public static void main(String[] args) throws IOException {
        new CargaDetallePlenos().load();
    }

    void load() throws IOException {
        try (final var ls = Files.list(Path.of("data"))) {
            var periodos = ls.toList();
            for (var periodo : periodos) {
                var paths = paths(periodo) // periodo anual
                        .flatMap(this::paths) // mes
                        .flatMap(this::paths) // fecha
                        .flatMap(this::paths) // asistencia/votacion
                        .collect(Collectors.toSet());
                var asistencias = paths
                        .stream()
                        .filter(s -> s.toString().endsWith("-asistencia"))
                        .map(this::loadAsistencia)
                        .collect(Collectors.toSet());
                var asistenciaPlenos = new AsistenciaPlenos(
                        periodo.getFileName().toString(),
                        asistencias
                );
                new CargaAsistenciaPlenos().accept(asistenciaPlenos);
                var votaciones = paths
                        .stream()
                        .filter(s -> s.toString().endsWith("-votacion"))
                        .map(this::loadVotacion)
                        .collect(Collectors.toSet());
                var votacionPlenos = new VotacionPlenos(
                        periodo.getFileName().toString(),
                        votaciones
                );
                new CargaVotacionPlenos().accept(votacionPlenos);
            }
        }
    }

    private Stream<Path> paths(Path p) {
        try {
            return Files.list(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RegistroVotacion loadVotacion(Path path) {
        try (final var ls = Files.list(path)) {
            assert ls.count() == 9;

            var gruposParlamentarios = loadGruposParlamentarios(
                    path.resolve("datos_grupo_parlamentario.csv")
            );

            var builder = loadVotacionMetadatos(path.resolve("metadatos.csv"),
                    gruposParlamentarios);
            loadVotacionEtiquetas(path.resolve("etiquetas.csv"))
                    .forEach(builder::addEtiqueta);
            var votaciones = loadVotacionLista(
                    path.resolve("votaciones.csv")
            );
            var resultados = loadVotacionResultado(path.resolve("resultados.csv"));
            var resultadosPorGrupo = loadVotacionResultadoPorPartido(
                    gruposParlamentarios,
                    path.resolve("resultados_partido.csv"),
                    gruposParlamentarios
            );

            return builder
                    .withVotaciones(votaciones)
                    .withResultados(resultados)
                    .withResultadosPorPartido(resultadosPorGrupo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error en " + path, e);
        }
    }

    private RegistroAsistencia loadAsistencia(Path path) {
        try (final var ls = Files.list(path)) {
            assert ls.count() == 8;

            var gruposParlamentarios = loadGruposParlamentarios(
                    path.resolve("datos_grupo_parlamentario.csv")
            );

            var builder = loadAsistenciaMetadatos(path.resolve("metadatos.csv"),
                    gruposParlamentarios);
            var asistencias = loadAsistenciaLista(
                    path.resolve("asistencias.csv")
            );
            var resultados = loadAsistenciaResultado(path.resolve("resultados.csv"));
            var resultadosPorGrupo = loadAsistenciaResultadoPorPartido(
                    gruposParlamentarios,
                    path.resolve("resultados_partido.csv"),
                    gruposParlamentarios
            );

            return builder
                    .withAsistencias(asistencias)
                    .withResultados(resultados)
                    .withResultadosPorPartido(resultadosPorGrupo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error at %s".formatted(path), e);
        }
    }

    private Map<String, String> loadGruposParlamentarios(Path path)
            throws IOException {
        try (
                final var it = mapper
                        .readerFor(Map.class)
                        .with(CsvSchema.emptySchema().withHeader())
                        .<Map<String, String>>readValues(path.toFile())
        ) {
            var data = new LinkedHashMap<String, String>();
            while (it.hasNext()) {
                var m = it.next();
                data.put(
                        m.get("grupo_parlamentario").trim(),
                        m.get("descripcion").trim()
                );
            }
            return data;
        }
    }

    private RegistroVotacion.Builder loadVotacionMetadatos(Path path,
                                                           Map<String, String> gruposParlamentarios

    )
            throws IOException {
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
            return RegistroVotacion
                    .newBuilder()
                    .withPleno(
                            new Pleno(
                                    f.get("periodo_parlamentario").trim(),
                                    f.get("periodo_anual").trim(),
                                    f.get("legislatura").trim(),
                                    f.get("sesion").trim(),
                                    f.get("url_pdf").trim(),
                                    LocalDate.parse(
                                            f.get("dia"),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                    ),
                                    Integer.parseInt(f.get("quorum")),
                                    gruposParlamentarios
                            )
                    )
                    .withHora(f.get("hora"))
                    .withAsunto(f.get("asunto").trim())
                    .withPresidente(f.get("presidente").trim());
        }
    }

    private Map<String, String> loadVotacionEtiquetas(Path path)
            throws IOException {
        try (
                final var it = mapper
                        .readerFor(Map.class)
                        .with(CsvSchema.emptySchema().withHeader())
                        .<Map<String, String>>readValues(path.toFile())
        ) {
            var f = new HashMap<String, String>();
            while (it.hasNext()) {
                var v = it.next();
                if (!v.get("valor").isBlank()) f.put(v.get("etiqueta"), v.get("valor"));
            }
            return f;
        }
    }

    private RegistroAsistencia.Builder loadAsistenciaMetadatos(Path path,
                                                               Map<String, String> gruposParlamentarios
    )
            throws IOException {
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
            return RegistroAsistencia
                    .newBuilder()
                    .withPleno(
                            new Pleno(
                                    f.get("periodo_parlamentario").trim(),
                                    f.get("periodo_anual").trim(),
                                    f.get("legislatura").trim(),
                                    f.get("sesion").trim(),
                                    f.get("url_pdf").trim(),
                                    LocalDate.parse(
                                            f.get("dia"),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                    ),
                                    Integer.parseInt(f.get("quorum")),
                                    gruposParlamentarios
                            )
                    )
                    .withHora(f.get("hora"));
        }
    }

    private List<ResultadoCongresista<Asistencia>> loadAsistenciaLista(
            Path path
    ) throws IOException {
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
                        new ResultadoCongresista<>(
                                grupo_parlamentario,
//            gruposParlamentarios.get(grupo_parlamentario),
                                m.get("congresista").trim(),
                                Asistencia.of(asistencia)
                        )
                );
            }
            return data;
        }
    }

    private List<ResultadoCongresista<Votacion>> loadVotacionLista(
            Path path
    ) throws IOException {
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
                var votacion = m.get("votacion").trim();
                var vot = Votacion.of(votacion);
                data.add(
                        new ResultadoCongresista<>(
                                grupo_parlamentario,
//            gruposParlamentarios.get(grupo_parlamentario),
                                m.get("congresista").trim(),
                                vot
                        )
                );
            }
            return data;
        }
    }

    private Map<GrupoParlamentario, ResultadoAsistencia> loadAsistenciaResultadoPorPartido(
            Map<String, String> grupos,
            Path path,
            Map<String, String> gruposParlamentarios
    ) throws IOException {
        try (
                final var it = mapper
                        .readerFor(Map.class)
                        .with(CsvSchema.emptySchema().withHeader())
                        .<Map<String, String>>readValues(path.toFile())
        ) {
            // por_partido,numero_legal,presentes,ausentes,licencias,otros
            var data = new HashMap<GrupoParlamentario, ResultadoAsistencia>();
            while (it.hasNext()) {
                var v = it.next();
                var partido = v.get("por_partido");
                if (!partido.isBlank() && !partido.equals("TOTAL")) {
                    assert grupos.containsKey(partido);
                    data.put(
                            new GrupoParlamentario(partido, gruposParlamentarios.get(partido)),
                            new ResultadoAsistencia(
                                    Integer.parseInt(v.get("presentes")),
                                    Integer.parseInt(v.get("ausentes")),
                                    Integer.parseInt(v.get("licencias")),
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
            Map<String, String> grupos,
            Path path,
            Map<String, String> gruposParlamentarios
    ) throws IOException {
        try (
                final var it = mapper
                        .readerFor(Map.class)
                        .with(CsvSchema.emptySchema().withHeader())
                        .<Map<String, String>>readValues(path.toFile())
        ) {
            // por_partido,numero_legal,presentes,ausentes,licencias,otros
            var data = new HashMap<GrupoParlamentario, ResultadoVotacion>();
            while (it.hasNext()) {
                var v = it.next();
                var partido = v.get("por_partido");
                if (!partido.isBlank() && !partido.equals("TOTAL")) {
                    assert grupos.containsKey(partido);
                    data.put(
                            new GrupoParlamentario(partido, gruposParlamentarios.get(partido)),
                            new ResultadoVotacion(
                                    Integer.parseInt(v.get("si")),
                                    Integer.parseInt(v.get("no")),
                                    Integer.parseInt(v.get("abstenciones")),
                                    //FIXME
                                    //              Integer.parseInt(v.get("ausentes")),
                                    //              Integer.parseInt(v.get("licencias")),
                                    Integer.parseInt(v.get("otros")),
                                    Integer.parseInt(v.get("numero_legal"))
                            )
                    );
                }
            }
            return data;
        }
    }

    private ResultadoAsistencia loadAsistenciaResultado(Path path)
            throws IOException {
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
                t.put(v.get("asistencia"), Integer.parseInt(v.get("total")));
            }
            return new ResultadoAsistencia(
                    t.get("presentes"),
                    t.get("ausentes"),
                    t.get("licencias"),
                    t.get("otros"),
                    t.get("numero_legal")
            );
        }
    }

    private ResultadoVotacion loadVotacionResultado(Path path)
            throws IOException {
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
                t.put(v.get("asistencia"), Integer.parseInt(v.get("total")));
            }
            return new ResultadoVotacion(
                    t.get("si"),
                    t.get("no"),
                    t.get("abstenciones"),
                    //FIXME
                    //        t.get("ausentes"),
                    //        t.get("licencias"),
                    t.get("otros"),
                    t.get("numero_legal")
            );
        }
    }
}
