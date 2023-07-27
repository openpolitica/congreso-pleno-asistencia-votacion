package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import op.congreso.pleno.Constantes;
import op.congreso.pleno.RegistroPleno;
import op.congreso.pleno.RegistroPlenoDocument;
import op.congreso.pleno.asistencia.AsistenciaSesion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

public class TextractRegistroPleno {

  final TextractClient textractClient;
  static final Logger LOG = LoggerFactory.getLogger(TextractRegistroPleno.class);

  public TextractRegistroPleno(TextractClient textractClient) {
    this.textractClient = textractClient;
  }

  List<String> lines(Path path) {
    try {
      var to = Path.of(path.toString().replace(".png", ".txt"));

      if (!Files.exists(to)) {
        byte[] bytes = Files.readAllBytes(path);
        final var document = Document.builder().bytes(SdkBytes.fromByteArray(bytes)).build();
        final var request = DetectDocumentTextRequest.builder().document(document).build();
        final var response = textractClient.detectDocumentText(request);

        final var blocks = response.blocks();
        int i = 0;
        List<String> lines = new ArrayList<>();
        while (i < blocks.size()) {
          final var block = blocks.get(i);
          if (block.blockType().equals(BlockType.LINE)) {
            final var text = block.text();
            lines.add(text);
          }
          i++;
        }
        Files.writeString(to, String.join("\n", lines));
        return lines;
      } else {
        return Files.readAllLines(to);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<Path, List<String>> extractRegistroPleno(Path plenoPdf) throws IOException {
    LOG.info("Generate images from PDF...");
    var pages = generateImageFromPDF(plenoPdf);
    LOG.info("Extract lines...");
    var list = new HashMap<Path, List<String>>();
    try (TextractClient textractClient =
        TextractClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()) {
      var t2t = new TextractRegistroPleno(textractClient);
      for (var page : pages) {
        var lines = t2t.lines(page);
        list.put(page, lines);
        LOG.info("Lines extracted from {}", page);
      }
    }
    return list;
  }

  static List<Path> generateImageFromPDF(Path pdfPath) throws IOException {
    try (var document = PDDocument.load(pdfPath.toFile())) {
      var pdfRenderer = new PDFRenderer(document);
      var dir = Path.of(pdfPath.toString().replace(".pdf", ""));
      if (!Files.isDirectory(dir)) Files.createDirectories(dir);
      var pages = new ArrayList<Path>();
      int numberOfPages = document.getNumberOfPages();
      LOG.info("PDF {} with pages: {}", pdfPath, numberOfPages);
      for (int page = 0; page < numberOfPages; ++page) {
        var pagePath = dir.resolve("page_%02d.png".formatted(page + 1));
        pages.add(pagePath);

        if (!Files.exists(pagePath)) {
          var bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
          ImageIOUtil.writeImage(bim, pagePath.toString(), 300);
        }
      }
      return pages;
    }
  }

  public static RegistroPlenoDocument plenoToRetry(Path base) throws IOException {
    try (final var paths = Files.list(base)) {
      return paths
          .flatMap(listDir()) // pp
          .flatMap(listDir()) // pa
          .flatMap(listDir()) // leg
          .filter(p -> p.toString().endsWith(".json"))
          .findFirst()
          .map(
              p -> {
                try {
                  return Files.readString(p);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              })
          .map(RegistroPlenoDocument::parseJson)
          .orElse(null);
    }
  }

  public static Map<Path, List<String>> retryProcessRegistroPleno(Path base) throws IOException {
    try (final var paths = Files.list(base)) {
      return paths
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
                      return Files.readAllLines(p1);
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  }));
    }
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

  public static RegistroPleno processLines(
      RegistroPlenoDocument document, Map<Path, List<String>> list) {
    var builder = RegistroPleno.newBuilder(document);
    AsistenciaSesion latestAsistencia = null;
    var errors = 0;
    var paths = list.keySet().stream().sorted().toList();
    for (var key : paths) {
      LOG.info("Processing page: {}", key);
      try {
        var lines = list.get(key);
        if (lines.contains(Constantes.ASISTENCIA)
            || lines.get(3).startsWith(Constantes.ASISTENCIA)) {
          var asistencia = TextractAsistencia.load(lines);
          builder.addAsistencia(asistencia);
          latestAsistencia = asistencia;
        } else if (lines.contains(Constantes.VOTACION)) {
          var quorum = -1;
          if (latestAsistencia != null) {
            // TODO potential error
            quorum = latestAsistencia.sesion().quorum();
          }
          var votacion = TextractVotacion.load(quorum, lines);
          builder.addVotacion(votacion);
        } else {
          // TODO potential error
          errors++;
        }
      } catch (Exception e) {
        LOG.error("Error processing page: {}", key, e);
        throw new RuntimeException(e);
      }
    }
    if (latestAsistencia != null)
      builder.withGruposParlamentarios(latestAsistencia.sesion().pleno().gruposParlamentarios());
    System.out.println(errors);
    return builder.build();
  }

  public static void main(String[] args) throws IOException {
    TextractRegistroPleno.retryProcessRegistroPleno(Path.of("out/pdf"))
        .forEach((path, strings) -> System.out.println(path + " -> " + strings));
  }
}
