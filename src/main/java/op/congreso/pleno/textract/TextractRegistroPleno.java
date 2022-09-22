package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import op.congreso.pleno.Constantes;
import op.congreso.pleno.RegistroPleno;
import op.congreso.pleno.RegistroPlenoDocument;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.textract.TextractClient;

public class TextractRegistroPleno {

  static final Logger LOG = LoggerFactory.getLogger(
    TextractRegistroPleno.class
  );

  public static Map<Path, List<String>> extractRegistroPleno(Path plenoPdf)
    throws IOException {
    LOG.info("Generate images from PDF...");
    var pages = PlenoPdfToImages.generateImageFromPDF(plenoPdf);
    LOG.info("Extract lines...");
    var list = new HashMap<Path, List<String>>();
    try (
      TextractClient textractClient = TextractClient
        .builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build()
    ) {
      var t2t = new TextractToText(textractClient);
      for (var page : pages) {
        var lines = t2t.imageLines2(page);
        list.put(page, lines);
        LOG.info("Lines extracted from {}", page);
      }
    }
    return list;
  }

  public static RegistroPlenoDocument plenoToRetry(Path base)
    throws IOException {
    return Files
      .list(base)
      .flatMap(listDir()) // pp
      .flatMap(listDir()) // pa
      .flatMap(listDir()) // leg
      .filter(p -> p.toString().endsWith(".json"))
      .findFirst()
      .map(p -> {
        try {
          return Files.readString(p);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .map(RegistroPlenoDocument::parseJson)
      .get();
  }

  public static Map<Path, List<String>> retryProcessRegistroPleno(Path base)
    throws IOException {
    return Files
      .list(base)
      .flatMap(listDir()) // pp
      .flatMap(listDir()) // pa
      .flatMap(listDir()) // leg
      .filter(Files::isDirectory)
      .flatMap(listDir())
      .filter(p1 -> p1.toString().endsWith(".txt"))
      .collect(
        Collectors.toMap(
          p1 -> p1,
          p1 -> {
            try {
              List<String> lines = Files.readAllLines(p1);
              //              if (
              //                lines.contains(Constantes.ASISTENCIA) ||
              //                lines.get(3).startsWith(Constantes.ASISTENCIA)
              //              ) {
              //                lines = TextractAsistencia.clean(lines);
              //              } else if (lines.contains(Constantes.VOTACION)) {
              //                lines = TextractVotacion.clean(lines);
              //              }
              return lines;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        )
      );
  }

  private static Function<Path, Stream<? extends Path>> listDir() {
    return p -> {
      try {
        return Files.list(p);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static Map<Path, List<String>> loadLines(Path path)
    throws IOException {
    try (var ls = Files.list(path)) {
      var map = new HashMap<Path, List<String>>();
      ls
        .filter(p -> p.toString().endsWith(".txt"))
        .sorted()
        .forEach(p -> {
          try {
            var lines = Files.readAllLines(p);
            if (
              lines.contains(Constantes.ASISTENCIA) ||
              lines.get(3).startsWith(Constantes.ASISTENCIA)
            ) {
              var l = TextractAsistencia.clean(lines);
              map.put(p, l);
            } else if (lines.contains(Constantes.VOTACION)) {
              var l = TextractVotacion.clean(lines);
              map.put(p, l);
            } else map.put(p, List.of());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      return map;
    }
  }

  public static RegistroPleno processLines(
    RegistroPlenoDocument document,
    Map<Path, List<String>> list
  ) {
    var builder = RegistroPleno.newBuilder(document);
    RegistroAsistencia latestAsistencia = null;
    var pageNumber = 1;
    var errors = 0;
    for (var key : list.keySet()) {
      LOG.info("Processing page: {}", key);
      try {
        var lines = list.get(key);
        if (
          lines.contains(Constantes.ASISTENCIA) ||
          lines.get(3).startsWith(Constantes.ASISTENCIA)
        ) {
          var asistencia = TextractAsistenciaV2.load(lines);
          builder.addAsistencia(asistencia);
          latestAsistencia = asistencia;
        } else if (lines.contains(Constantes.VOTACION)) {
          var quorum = -1;
          if (latestAsistencia != null) {
            // TODO potential error
            quorum = latestAsistencia.quorum();
          }
          var votacion = TextractVotacionV2.load(quorum, lines);
          builder.addVotacion(votacion);
        } else {
          // TODO potential error
          errors++;
        }
      } catch (Exception e) {
        LOG.error("Error processing page: {}", key, e);
        throw new RuntimeException(e);
      }
      pageNumber++;
    }
    if (latestAsistencia != null) builder.withGruposParlamentarios(
      latestAsistencia.pleno().gruposParlamentarios()
    );
    System.out.println(errors);
    return builder.build();
  }

  public static void main(String[] args) throws IOException {
    //            var lines = extractRegistroPleno(
    //                    Path.of("./out/Asis_vot_OFICIAL_13-07-2022.pdf"));
    //    var lines = loadLines(Path.of("./out/Asis_vot_OFICIAL_13-07-2022"));
    //    var pleno = processLines(
    //      new RegistroPlenoDocument(
    //        "2021-2026",
    //        "Período Anual de Sesiones 2021 - 2022",
    //        "Segunda Legislatura Ordinaria",
    //        "2022-07-13",
    //        "Asistencias y votaciones de la sesión del 13-07-2022",
    //        "https://www2.congreso.gob.pe/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/Apleno/F54E4633A78E76420525888A00730BD7/$FILE/Asis_vot_OFICIAL_13-07-2022.pdf",
    //        "",
    //        0,
    //        false
    //      ),
    //      lines
    //    );
    //    System.out.println(pleno);
    //    SaveRegistroPlenoToCsv.save(pleno);
    TextractRegistroPleno
      .retryProcessRegistroPleno(Path.of("out/pdf"))
      .forEach((path, strings) -> System.out.println(path + " -> " + strings));
  }
}
