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

public class PlenoPdfToImages {

  static List<Path> generateImageFromPDF(Path pdfPath) throws IOException {
    try (var document = PDDocument.load(pdfPath.toFile())) {
      var pdfRenderer = new PDFRenderer(document);
      Path dir = Path.of(pdfPath.toString().replace(".pdf", ""));
      if (!Files.isDirectory(dir)) Files.createDirectories(dir);
      var pages = new ArrayList<Path>();
      for (int page = 0; page < document.getNumberOfPages(); ++page) {
        var bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

        var pagePath = dir.resolve("page_" + (page + 1) + ".png");
        pages.add(pagePath);

        ImageIOUtil.writeImage(bim, pagePath.toString(), 300);
      }
      return pages;
    }
  }

  public static void main(String[] args) throws IOException {
    final var path = Path.of("./out/Asis_vot_OFICIAL_07-07-22.pdf");
    generateImageFromPDF(path);
  }
}
