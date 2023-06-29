package op.congreso.pleno.app;

import io.vavr.collection.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    Files.readAllLines(Path.of("notas-restantes.csv")).stream()
        .skip(1)
        .filter(s -> !s.isBlank())
        .map(
            s ->
                Map.entry(
                    Path.of("./data/2021-2026", s.substring(0, s.indexOf(","))),
                    s.substring(s.indexOf(",") + 1)))
        .map(
            kv ->
                Map.entry(
                    kv._1.resolve(kv._2.substring(0, kv._2.indexOf(","))),
                    kv._2.substring(kv._2.indexOf(",") + 1)))
        .map(
            kv ->
                Map.entry(
                    kv._1.resolve(kv._2.substring(0, kv._2.indexOf(","))),
                    kv._2.substring(kv._2.indexOf(",") + 1)))
        .map(
            kv ->
                Map.entry(
                    kv._1.resolve(kv._2.substring(0, kv._2.indexOf(","))),
                    kv._2.substring(kv._2.indexOf(",") + 1)))
        .map(kv -> Map.entry(kv._1.resolve("notas.csv"), kv._2))
        .forEach(
            kv -> {
              try {
                if (Files.exists(kv._1) && Files.readAllLines(kv._1).size() == 1) {
                  Files.writeString(kv._1, kv._2, StandardOpenOption.APPEND);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
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
