package op.congreso.pleno.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import op.congreso.pleno.RegistroPlenoDocument;

public class ConteoPaginas {

  public static void main(String[] args) throws IOException {
    var jsonMapper = new ObjectMapper();

    var plenosPath = Path.of("plenos.json");
    var bytes = Files.readAllBytes(plenosPath);
    var plenos = jsonMapper.readValue(
      bytes,
      new TypeReference<List<RegistroPlenoDocument>>() {}
    );

    var plenosWithPaginas = plenos
      .stream()
      .filter(pleno -> pleno.paginas() < 1)
      .map(RegistroPlenoDocument::withPaginas)
      .collect(Collectors.toList());
    Files.writeString(
      plenosPath,
      jsonMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(plenosWithPaginas)
    );

    var content = RegistroPlenoDocument.csvHeader();
    plenosWithPaginas.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(Path.of("plenos.csv"), content.toString());
  }
}
