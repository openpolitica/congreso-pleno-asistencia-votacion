package op.pleno;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import op.pleno.asistencia.RegistroAsistencia;
import op.pleno.asistencia.ResultadoAsistencia;
import op.pleno.votacion.RegistroVotacion;

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

public class LoadPlenos {
    ObjectMapper mapper = new CsvMapper();

    void load() throws IOException {
        var paths =
                Files.list(Path.of("data")) // periodo parlamentario
                        .flatMap(this::paths) // periodo anual
                        .flatMap(this::paths) // legislatura
                        .flatMap(this::paths) // fecha
                        .flatMap(this::paths) // asistencia/votacion
                        .peek(path -> System.out.println(path.getFileName().toString()))
                        .collect(Collectors.toSet());
        var asistencias =
                paths.stream().filter(s -> s.toString().endsWith("-asistencia"))
                        .peek(path -> System.out.println(path.getFileName().toString()))
                        .map(this::loadAsistencia)
                        .peek(System.out::println)
                        .collect(Collectors.toSet());
        var votaciones =
                paths.stream().filter(s -> s.toString().endsWith("-votacion"))
                        .peek(path -> System.out.println(path.getFileName().toString()))
                        .collect(Collectors.toSet());
    }

    private Stream<Path> paths(Path p) {
        try {
            return Files.list(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RegistroAsistencia loadAsistencia(Path path) {
        try {
            System.out.println(path);
            var version = Files.readString(path.resolve("version.csv"));
            System.out.printf("version: %s%n", version);
            assert Files.list(path).count() == 8;

            var gruposParlamentarios = loadGruposParlamentarios(path.resolve("datos_grupo_parlamentario.csv"));
            var tiposAsistencia = loadTipoAsistencia(path.resolve("datos_tipo_asistencia.csv"));

            var builder = loadAsistenciaMetadatos(path.resolve("metadatos.csv"));
            var asistencias = loadAsistenciaLista(gruposParlamentarios, tiposAsistencia, path.resolve("asistencias.csv"));
            asistencias.forEach(System.out::println);
            var resultados = loadAsistenciaResultado(path.resolve("resultados.csv"));
            System.out.println(resultados);
            var resultadosPorGrupo = loadAsistenciaResultadoPorPartido(
                    gruposParlamentarios,
                    path.resolve("resultados_partido.csv"));

            return builder.withAsistencias(asistencias)
                    .withResultados(resultados)
                    .withResultadosPorPartido(resultadosPorGrupo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> loadGruposParlamentarios(Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
        var data = new LinkedHashMap<String, String>();
        while (it.hasNext()) {
            var m = it.next();
            data.put(m.get("grupo_parlamentario"), m.get("descripcion"));
        }
        return data;
    }

    private Map<String, String> loadTipoAsistencia(Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
        var data = new LinkedHashMap<String, String>();
        while (it.hasNext()) {
            var m = it.next();
            data.put(m.get("asistencia"), m.get("descripcion"));
        }
        return data;
    }

    private RegistroAsistencia.Builder loadAsistenciaMetadatos(Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
        var f = new HashMap<String, String>();
        while (it.hasNext()) {
            var v = it.next();
            f.put(v.get("metadato"), v.get("valor"));
        }
        return RegistroAsistencia.newBuilder()
                .withPleno(new Pleno(
                        f.get("periodo_parlamentario"),
                        f.get("periodo_anual"),
                        f.get("legislatura"),
                        f.get("sesion"),
                        f.get("url_pdf"),
                        LocalDate.parse(f.get("dia"), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        Integer.parseInt(f.get("quorum"))
                ))
                .withHora(f.get("hora"));
    }

    private List<ResultadoCongresista> loadAsistenciaLista(Map<String, String> gruposParlamentarios, Map<String, String> tiposAsistencia, Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
        // numero,grupo_parlamentario,congresista,asistencia
        var data = new ArrayList<ResultadoCongresista>();
        while (it.hasNext()) {
            var m = it.next();
            var grupo_parlamentario = m.get("grupo_parlamentario");
            var asistencia = m.get("asistencia");
            data.add(new ResultadoCongresista(
                    Integer.parseInt(m.get("numero")),
                    grupo_parlamentario,
                    gruposParlamentarios.get(grupo_parlamentario),
                    m.get("congresista"),
                    asistencia,
                    tiposAsistencia.get(asistencia)
            ));
        }
        return data;
    }

    private Map<String, ResultadoAsistencia> loadAsistenciaResultadoPorPartido(Map<String, String> grupos, Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
        //por_partido,numero_legal,presentes,ausentes,licencias,otros
        var data = new HashMap<String, ResultadoAsistencia>();
        while (it.hasNext()) {
            var v = it.next();
            var partido = v.get("por_partido");
            if (!partido.isBlank() && !partido.equals("TOTAL")) {
                assert grupos.containsKey(partido);
                data.put(partido, new ResultadoAsistencia(
                        Integer.parseInt(v.get("presentes")),
                        Integer.parseInt(v.get("ausentes")),
                        Integer.parseInt(v.get("licencias")),
                        Integer.parseInt(v.get("otros")),
                        Integer.parseInt(v.get("numero_legal"))
                ));
            }
        }
        System.out.println(data);
        return data;
    }

    private ResultadoAsistencia loadAsistenciaResultado(Path path) throws IOException {
        MappingIterator<Map<String, String>> it = mapper.readerFor(Map.class)
                .with(CsvSchema.emptySchema().withHeader())
                .readValues(path.toFile());
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

    private RegistroVotacion loadVotacion(Path path) {
        throw new UnsupportedOperationException("yet");
    }

    public static void main(String[] args) throws IOException {
        new LoadPlenos().load();
    }
}
