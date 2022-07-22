package op.congreso.pleno.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

public class PdfToImage {

  private static void generateImageFromPDF(File pdf, String extension)
    throws IOException {
    PDDocument document = PDDocument.load(pdf);
    PDFRenderer pdfRenderer = new PDFRenderer(document);
    for (int page = 0; page < document.getNumberOfPages(); ++page) {
      BufferedImage bim = pdfRenderer.renderImageWithDPI(
        page,
        300,
        ImageType.RGB
      );
      ImageIOUtil.writeImage(
        bim,
        String.format("out/pdf-%d.%s", page + 1, extension),
        300
      );
    }
    document.close();
  }

  public static void main(String[] args) throws IOException {
    final var path = Path.of("./out/Asis_vot_OFICIAL_07-07-22.pdf");
    generateImageFromPDF(path.toFile(), "png");
  }
}
