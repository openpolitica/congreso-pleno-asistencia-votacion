package op.congreso.pleno.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import op.congreso.pleno.RegistroPleno;

public class SaveRegistroPlenoToCsv {

    public static void save(RegistroPleno registroPleno) throws IOException {
        // create data dir
        var plenoDir =
            Path.of("data")
                .resolve(registroPleno.periodo().periodoParlamentario())
                .resolve(String.valueOf(registroPleno.fecha().getYear()))
                .resolve(registroPleno.fecha().format(DateTimeFormatter.ofPattern("MM")))
                .resolve(registroPleno.fecha().format(DateTimeFormatter.ofPattern("dd")));
        Files.createDirectories(plenoDir);
        // save pleno.csv
        Files.writeString(plenoDir.resolve("pleno.csv"), registroPleno.printPlenoAsCsv());
        // save grupos_parlamentarios.csv
        Files.writeString(
            plenoDir.resolve("grupo_parlamentario.csv"), registroPleno.printGruposParlametariosAsCsv());
        // write asistencias
        for (var a : registroPleno.asistencias().values()) {
            var asistenciaDir =
                plenoDir.resolve(
                    a.fechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm"))
                        + "-asistencia");
            Files.createDirectories(asistenciaDir);
            Files.writeString(asistenciaDir.resolve("metadatos.csv"), a.printMetadatosAsCsv());
            Files.writeString(asistenciaDir.resolve("asistencias.csv"), a.printAsistenciasAsCsv());
            Files.writeString(
                asistenciaDir.resolve("resultados_grupo.csv"), a.printResultadosPorGrupoAsCsv());
            Files.writeString(asistenciaDir.resolve("resultados.csv"), a.printResultadosAsCsv());
            Files.writeString(asistenciaDir.resolve("log.txt"), a.printLog());
            Files.writeString(asistenciaDir.resolve("notas.csv"), "hora,nota\n");

        }
        // write votaciones
        for (var v : registroPleno.votaciones().values()) {
            var votacionDir =
                plenoDir.resolve(
                    v.fechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm"))
                        + "-votacion");
            Files.createDirectories(votacionDir);
            Files.writeString(votacionDir.resolve("metadatos.csv"), v.printMetadatosAsCsv());
            Files.writeString(votacionDir.resolve("votaciones.csv"), v.printVotacionesAsCsv());
            Files.writeString(
                votacionDir.resolve("resultados_grupo.csv"), v.printResultadosPorGrupoAsCsv());
            Files.writeString(votacionDir.resolve("resultados.csv"), v.printResultadosAsCsv());
            Files.writeString(votacionDir.resolve("etiquetas.csv"), v.printEtiquetasAsCsv());
            Files.writeString(votacionDir.resolve("log.txt"), v.printLog());
            Files.writeString(votacionDir.resolve("notas.csv"), "hora,nota\n");
        }
    }
}
