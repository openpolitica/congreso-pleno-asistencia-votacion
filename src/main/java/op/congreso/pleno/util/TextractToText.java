package op.congreso.pleno.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

public class TextractToText {

  public static void main(String[] args) {
    try (
      TextractClient textractClient = TextractClient
        .builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()
    ) {
      final var path = Path.of("./out/pdf-1.png");

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

      final var blocks = response.blocks();
      int i = 0;
      StringBuilder content = new StringBuilder();
      while (i < blocks.size()) {
        final var block = blocks.get(i);
        if (block.blockType().equals(BlockType.LINE)) {
          final var text = block.text();
          content.append(text).append("\n");
        }
        i++;
      }
      Files.writeString(
        Path.of("output.txt"),
        content.toString(),
        StandardOpenOption.CREATE_NEW
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
