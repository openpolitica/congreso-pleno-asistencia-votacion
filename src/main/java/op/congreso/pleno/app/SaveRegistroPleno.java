package op.congreso.pleno.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import op.congreso.pleno.RegistroPleno;

public class SaveRegistroPleno {
    public static void save(RegistroPleno registroPleno) throws IOException {
        // create data dir
        var plenoDir = Path.of("data")
                .resolve(registroPleno.periodoParlamentario())
                .resolve(String.valueOf(registroPleno.fecha().getYear()))
                .resolve(registroPleno.fecha().format(DateTimeFormatter.ofPattern("MM")))
                .resolve(registroPleno.fecha().format(DateTimeFormatter.ISO_LOCAL_DATE));
        Files.createDirectories(plenoDir);
        // save pleno.csv
        Files.writeString(plenoDir.resolve("pleno.csv"), registroPleno.printPlenoAsCsv());
        // save grupos_parlamentarios.csv
        Files.writeString(plenoDir.resolve("grupo_parlamentario.csv"), registroPleno.printGruposParlametariosAsCsv());
        // write asistencias
        for (var a : registroPleno.asistencias().values()) {
            var asistenciaDir = plenoDir.resolve(a.fechaHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + "-asistencia");
            Files.createDirectories(asistenciaDir);
            Files.writeString(asistenciaDir.resolve("asistencias.csv"), a.printAsistenciasAsCsv());
        }
        // TODO write votaciones
    }
}
