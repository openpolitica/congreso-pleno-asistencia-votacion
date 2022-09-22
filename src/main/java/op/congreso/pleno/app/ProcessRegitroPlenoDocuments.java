package op.congreso.pleno.app;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessRegitroPlenoDocuments {

  static final Logger LOG = LoggerFactory.getLogger(
    ProcessRegitroPlenoDocuments.class
  );
  public static final String CURRENT = "2021-2026";

  public static void main(String[] args) throws IOException {
    LOG.info("Load existing plenos");
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
        if (pleno.periodoParlamentario().equals(CURRENT)) existing.put(
          pleno.id(),
          pleno
        );
      }
    }

    boolean extractPleno = true; // opt-out to do this manually yet

    var updated = new HashSet<RegistroPlenoDocument>();
    for (var p : existing.values()) {
      var pleno = existing.getOrDefault(p.id(), p);
      if (
        pleno.fecha().equals("2022-07-12")
        //        !pleno.provisional() &&
        //        pleno.paginas() < 1 &&
        //        pleno.periodoParlamentario().equals(CURRENT)
      ) {
        if (extractPleno) {
          LOG.info("Extracting Pleno: {}", pleno.fecha());
          pleno = pleno.extract();
          Files.writeString(Path.of("pr-title.txt"), pleno.prTitle());
          Files.writeString(Path.of("pr-branch.txt"), pleno.prBranchName());
          Files.writeString(Path.of("pr-content.txt"), pleno.prContent());
          extractPleno = false;
        }
      }
      updated.add(pleno);
    }

    var registroPlenos = updated
      .stream()
      .sorted(Comparator.comparing(RegistroPlenoDocument::id).reversed())
      .toList();

    LOG.info("Writing CSV to file");
    var content = csvHeader();
    registroPlenos.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(plenosCsvPath, content.toString());
    LOG.info("Done");
  }
}
