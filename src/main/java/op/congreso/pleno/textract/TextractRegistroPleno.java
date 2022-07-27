package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import op.congreso.pleno.Constantes;
import op.congreso.pleno.RegistroPleno;
import op.congreso.pleno.RegistroPlenoDocument;
import op.congreso.pleno.asistencia.RegistroAsistencia;

public class TextractRegistroPleno {

    static ArrayList<List<String>> extractRegistroPleno(Path plenoPdf) throws IOException {
        List<Path> pages = PlenoPdfToImages.generateImageFromPDF(plenoPdf);
        var list = new ArrayList<List<String>>();
        for (var page : pages) {
            var lines = TextractToText.imageLines(page);
            list.add(lines);
        }
        return list;
    }

    public static List<List<String>> loadLines(Path path) throws IOException {
        try (var ls = Files.list(path)) {
            return ls
                    .filter(p -> p.toString().endsWith(".txt"))
                    .peek(System.out::println)
                    .sorted()
                    .map(p -> {
                        try {
                            var lines = Files.readAllLines(p);
                            if (lines.contains(Constantes.ASISTENCIA) || lines.get(3).startsWith(Constantes.ASISTENCIA)) {
                                return TextractAsistencia.clean(lines);
                            } else if (lines.contains(Constantes.VOTACION)) {
                                return TextractVotacion.clean(lines);
                            } else return List.<String>of();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }
    }

    public static RegistroPleno processLines(RegistroPlenoDocument document, List<List<String>> list) {
        var builder = RegistroPleno.newBuilder(document);
        RegistroAsistencia latestAsistencia = null;
        var pageNumber = 1;
        var errors = 0;
        for (var lines : list) {
            System.out.println("Processing page: " + pageNumber);
            try {
                if (lines.contains(Constantes.ASISTENCIA) || lines.get(3).startsWith(Constantes.ASISTENCIA)) {
                    var asistencia = TextractAsistencia.load(lines);
                    builder.addAsistencia(asistencia);
                    latestAsistencia = asistencia;
                } else if (lines.contains(Constantes.VOTACION)) {
                    var quorum = -1;
                    if (latestAsistencia != null) {
                        // TODO potential error
                        quorum = latestAsistencia.pleno().quorum();
                    }
                    var votacion = TextractVotacion.load(quorum, lines);
                    builder.addVotacion(votacion);
                } else {
                    // TODO potential error
                    errors++;
                }
            } catch (Exception e) {
                System.out.println("Error processing page: " + pageNumber);
                e.printStackTrace();
            }
            pageNumber++;
        }
        System.out.println(errors);
        return builder.build();
    }

    public static void main(String[] args) throws IOException {
//        var lines = extractRegistroPleno(
//                Path.of("./out/Asis_vot_OFICIAL_07-07-22.pdf"));
        var lines = loadLines(Path.of("./out/Asis_vot_OFICIAL_07-07-22"));
        var pleno = processLines(
                new RegistroPlenoDocument(
                        "2021-2026",
                        "Período Anual de Sesiones 2021 - 2022",
                        "Segunda Legislatura Ordinaria",
                        "2022-07-07",
                        "Asistencias y votaciones de la sesión del 07-07-2022",
                        "",
                        "",
                        0
                )
                , lines);
        System.out.println(pleno);
    }
}
