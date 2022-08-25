package op.congreso.pleno.app;

import static op.congreso.pleno.RegistroPlenoDocument.collect;
import static op.congreso.pleno.RegistroPlenoDocument.collectPleno;
import static op.congreso.pleno.RegistroPlenoDocument.csvHeader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import op.congreso.pleno.RegistroPlenoDocument;

public class LoadRegitroPlenoDocuments {

  public static void main(String[] args) throws IOException {
    var root = collect(
      "/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/new_asistenciavotacion",
      5
    );

    var plenos = new HashSet<RegistroPlenoDocument>();

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

    //    var mapper = new ObjectMapper();
    var mapper = new CsvMapper();

    var plenosCsvPath = Path.of("plenos.csv");

    var existing = new HashMap<String, RegistroPlenoDocument>();

    try (
      final var it = mapper
        .readerFor(Map.class)
        .with(CsvSchema.emptySchema().withHeader())
        .<Map<String, String>>readValues(plenosCsvPath.toFile())
    ) {
      while (it.hasNext()) {
        var v = it.next();
        var pleno = RegistroPlenoDocument.parse(v);
        existing.put(pleno.id(), pleno);
      }
    }

    boolean extractPleno = true;
    var current = "2021-2026";

    var updated = new HashSet<RegistroPlenoDocument>();
    for (var p : plenos) {
      var pleno = existing.getOrDefault(p.id(), p);
      if (p.paginas() < 1 && pleno.periodoParlamentario().equals(current)) {
        if (extractPleno) {
          pleno = p.extract();
          Files.writeString(Path.of("pr-title.txt"), pleno.prTitle());
          Files.writeString(Path.of("pr-branch.txt"), pleno.prTitle());
          Files.writeString(Path.of("pr-content.txt"), pleno.prContent());
          extractPleno = false;
        }
      }
      updated.add(pleno);
    }

    //    var updated = plenos
    //      .stream()
    //      .map(p -> existing.getOrDefault(p.id(), p))
    //      .map(p -> {
    //        if (p.paginas() < 1) {
    //          System.out.printf("Descargando %s%n", p);
    //          p.download();
    //          return p.withPaginas();
    //        } else {
    //          return p;
    //        }
    //      })
    //      .collect(Collectors.toSet());
    var registroPlenos = updated
      .stream()
      .sorted(Comparator.comparing(RegistroPlenoDocument::id).reversed())
      .toList();
    //json
    //    Files.writeString(plenosJsonPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(registroPlenos));
    //csv
    var content = csvHeader();
    registroPlenos.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(plenosCsvPath, content.toString());
  }
}
