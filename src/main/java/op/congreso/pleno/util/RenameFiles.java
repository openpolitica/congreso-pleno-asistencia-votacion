package op.congreso.pleno.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class RenameFiles {

  public static void main(String[] args) throws IOException {
    Files
      .list(Path.of("data"))
      .flatMap(RenameFiles::list) // period
      .flatMap(RenameFiles::list) // years
      .flatMap(RenameFiles::list) // months
      .flatMap(RenameFiles::list) // pleno
      .flatMap(RenameFiles::list) // asis/vot
      .filter(path ->
        path.getFileName().toString().equals("resultados_grupo.csv")
      )
      .forEach(path -> {
        try {
          Files.move(path, path.getParent().resolve("resultados_grupo.csv"));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
  }

  private static Stream<Path> list(Path path) {
    try {
      if (Files.isDirectory(path)) return Files.list(
        path
      ); else return Stream.of();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
