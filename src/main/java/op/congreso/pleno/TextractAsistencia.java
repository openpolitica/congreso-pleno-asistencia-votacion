package op.congreso.pleno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

public class TextractAsistencia {

  public static void main(String[] args) {
    //        EndpointConfiguration endpoint = new EndpointConfiguration(
    //                "https://textract.us-east-1.amazonaws.com", "us-east-1");
    try (
      TextractClient textractClient = TextractClient
        .builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()
    ) {
      // final var path = Path.of("/home/jeqo/Downloads/congreso/Asis_vot_OFICIAL_16-06-221024_1.jpg");
      final var path = Path.of(
        "/home/jeqo/Downloads/congreso-2/02-08-2011_page-0001.jpg"
      );
      byte[] bytes = Files.readAllBytes(path);
      final var document = Document
        .builder()
        .bytes(SdkBytes.fromByteArray(bytes))
        .build();
      final var request = DetectDocumentTextRequest
        .builder()
        .document(document)
        .build();
      final var response = textractClient.detectDocumentText(request);

      int i = 0;
      final var blocks = response.blocks();
      while (i < blocks.size()) {
        final var block = blocks.get(i);
        if (block.blockType().equals(BlockType.LINE)) {
          final var text = block.text();
          //                switch (text) {
          //
          //                }
          System.out.println(text);
        }
        i++;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
