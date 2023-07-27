package op.congreso.pleno.app;

import static op.congreso.pleno.Constantes.PERIODO_ACTUAL;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadRegistroPlenoDocuments {

  static final Logger LOG = LoggerFactory.getLogger(LoadRegistroPlenoDocuments.class);

  public static void main(String[] args) throws IOException {
    LOG.info("Load existing plenos");
    var mapper = new CsvMapper();
    var plenosCsvPath = Path.of("plenos.csv");

    var existing = new HashMap<String, RegistroPlenoDocument>();

    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(plenosCsvPath.toFile())) {
      while (it.hasNext()) {
        var v = it.next();
        var pleno = RegistroPlenoDocument.parse(v);
        if (pleno.periodo().periodoParlamentario().equals(PERIODO_ACTUAL))
          existing.put(pleno.id(), pleno);
      }
    }
    LOG.info("Starting to collect plenos");
    var root = collect("/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/new_asistenciavotacion", 5);

    var plenos = new HashSet<RegistroPlenoDocument>();

    for (var periodos : root.entrySet()) {
      LOG.debug("Periodo: {}", periodos.getKey());
      var year = collect(periodos.getValue(), 4);
      for (var anual : year.entrySet()) {
        LOG.debug("Periodo Anual: {}", anual.getKey());
        var periodo = collect(anual.getValue(), 3);
        for (var legislatura : periodo.entrySet()) {
          LOG.debug("Legislatura: {}", legislatura.getKey());

          var p =
              collectPleno(
                  periodos.getKey(), anual.getKey(), legislatura.getKey(), legislatura.getValue());
          p.values().stream()
              .filter(p1 -> p1.periodo().periodoParlamentario().equals("2021-2026"))
              .forEach(plenos::add);
        }
      }
    }
    LOG.info("Plenos collected: {}", plenos.size());

    var updated = new HashSet<RegistroPlenoDocument>();
    for (var p : plenos) {
      var pleno = existing.getOrDefault(p.id(), p);
      updated.add(pleno);
    }

    var registroPlenos =
        updated.stream()
            .sorted(Comparator.comparing(RegistroPlenoDocument::id).reversed())
            .toList();

    LOG.info("Writing CSV to file");
    var content = csvHeader();
    registroPlenos.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(plenosCsvPath, content.toString());
    LOG.info("Done");
  }
}
