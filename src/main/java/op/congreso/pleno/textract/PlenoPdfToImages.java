package op.congreso.pleno.textract;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

public class PlenoPdfToImages {

  private static void generateImageFromPDF(Path pdfPath)
    throws IOException {
    PDDocument document = PDDocument.load(pdfPath.toFile());
    PDFRenderer pdfRenderer = new PDFRenderer(document);
    for (int page = 0; page < document.getNumberOfPages(); ++page) {
      BufferedImage bim = pdfRenderer.renderImageWithDPI(
        page,
        300,
        ImageType.RGB
      );
      ImageIOUtil.writeImage(
        bim,
        String.format("./target/out/pdf-%d.%s", page + 1, "png"),
        300
      );
    }
    document.close();
  }

  public static void main(String[] args) throws IOException {
    final var path = Path.of("./out/Asis_vot_OFICIAL_07-07-22.pdf");
    generateImageFromPDF(path);
  }
}
