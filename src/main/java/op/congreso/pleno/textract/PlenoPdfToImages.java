package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlenoPdfToImages {

  static final Logger LOG = LoggerFactory.getLogger(PlenoPdfToImages.class);

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

  public static void main(String[] args) throws IOException {
    final var path = Path.of("./target/out/Asis_vot_OFICIAL_12-07-22.pdf");
    generateImageFromPDF(path);
  }
}
