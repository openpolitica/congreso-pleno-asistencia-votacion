package op.congreso.pleno.app;

import static op.congreso.pleno.RegistroPleno.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import op.congreso.pleno.RegistroPleno;

public class CargaRegitroPlenos {

  public static void main(String[] args) throws IOException {
    var root = collect(
      "/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/new_asistenciavotacion",
      5
    );

    var plenos = new HashSet<RegistroPleno>();

    for (var periodos : root.entrySet()) {
      System.out.println(periodos.getKey());
      var year = collect(periodos.getValue(), 4);
      for (var anual : year.entrySet()) {
        System.out.println(anual.getKey());
        var periodo = collect(anual.getValue(), 3);
        for (var legislatura : periodo.entrySet()) {
          System.out.println(legislatura.getKey());

          var p = collectPleno(
            periodos.getKey(),
            anual.getKey(),
            legislatura.getKey(),
            legislatura.getValue()
          );
          plenos.addAll(p.values());
        }
      }
    }

    var jsonMapper = new ObjectMapper();

    var bytes = Files.readAllBytes(Path.of("plenos.json"));
    var existing = jsonMapper
      .readValue(bytes, new TypeReference<List<RegistroPleno>>() {})
      .stream()
      .collect(Collectors.toMap(RegistroPleno::id, p -> p));

    var updated = plenos
      .stream()
      .map(p -> existing.getOrDefault(p.id(), p))
      .map(p -> {
        if (p.paginas() < 1) {
          System.out.printf("Descargando %s%n", p);
          p.download();
          return p.withPaginas();
        } else {
          return p;
        }
      })
      .collect(Collectors.toSet());
    var registroPlenos = updated
      .stream()
      .sorted(Comparator.comparing(RegistroPleno::id).reversed())
      .toList();
    //json
    Files.writeString(
      Path.of("plenos.json"),
      jsonMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(registroPlenos)
    );
    //csv
    StringBuilder content = RegistroPleno.csvHeader();
    registroPlenos.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(Path.of("plenos.csv"), content.toString());
  }
}
