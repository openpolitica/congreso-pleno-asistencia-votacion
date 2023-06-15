package op.congreso.pleno.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AddMissingNotas {
    public static void main(String[] args) throws IOException {
        Files.list(Path.of("data/2021-2026"))
            .filter(Files::isDirectory)
            .flatMap(AddMissingNotas::listPath)
            .flatMap(AddMissingNotas::listPath)
            .flatMap(AddMissingNotas::listPath)
            .filter(Files::isDirectory)
            .filter(path -> listPath(path).noneMatch(p -> p.endsWith("notas.csv")))
            .forEach(AddMissingNotas::writeNotas);
    }

    private static void writeNotas(Path path) {
        try {
            System.out.println("Creating notas file at " + path);
            Files.writeString(path.resolve("notas.csv"), "hora,nota\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Path> listPath(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
