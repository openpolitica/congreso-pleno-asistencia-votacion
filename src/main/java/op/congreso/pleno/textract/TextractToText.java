package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import op.congreso.pleno.Constantes;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

public class TextractToText {

    static List<String> imageLines(Path path) {
        try (
                TextractClient textractClient = TextractClient
                        .builder()
                        .region(Region.US_EAST_1)
                        .credentialsProvider(ProfileCredentialsProvider.create())
                        .build()
        ) {

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
            List<String> lines = new ArrayList<>();
            while (i < blocks.size()) {
                final var block = blocks.get(i);
                if (block.blockType().equals(BlockType.LINE)) {
                    final var text = block.text();
                    lines.add(text);
                }
                i++;
            }
            if (lines.contains(Constantes.ASISTENCIA) || lines.get(3).startsWith(Constantes.ASISTENCIA)) {
                lines = TextractAsistencia.clean(lines);
            } else if (lines.contains(Constantes.VOTACION)) {
                lines = TextractVotacion.clean(lines);
            }
            Files.writeString(
                    Path.of(path.toString().replace(".png", ".txt")),
                    String.join("\n", lines));
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        final var path = Path.of("./out/pdf-9.png");
        var lines = imageLines(path);
        Files.writeString(
                Path.of(path.toString().replace(".png", ".txt")),
                String.join("\n", lines),
                StandardOpenOption.CREATE_NEW
        );
    }
}
