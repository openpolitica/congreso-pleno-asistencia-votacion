package op.congreso.pleno.app;

import static op.congreso.pleno.DocumentoPleno.csvHeader;
import static op.congreso.pleno.Rutas.DATA_PERIODO_ACTUAL;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import op.congreso.pleno.DocumentoPleno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Procesa la lista de documentos de pleno. Si el registro no ha sido procesado (e.g. paginas = 0)
 * entonces se procede a procesar el PDF.
 *
 * <p>Si procesos falla, texto extraido esta contenido en {@code Constantes.TMP_DIR}. Datos pueden
 * ser ajustados para reintentar proceso.
 *
 * @see RetryProcessDocumentoPleno
 */
public class ProcessDocumentoPleno {

  static final Logger LOG = LoggerFactory.getLogger(ProcessDocumentoPleno.class);
  public static final String CURRENT = "2021-2026";

  public static void main(String[] args) throws IOException {
    LOG.info("Load existing plenos");
    var mapper = new CsvMapper();
    var plenosCsvPath = DATA_PERIODO_ACTUAL.resolve("plenos.csv");

    var existing = new HashMap<String, DocumentoPleno>();

    try (final var it =
        mapper
            .readerFor(Map.class)
            .with(CsvSchema.emptySchema().withHeader())
            .<Map<String, String>>readValues(plenosCsvPath.toFile())) {
      while (it.hasNext()) {
        var v = it.next();
        var pleno = DocumentoPleno.parse(v);
        if (pleno.periodo().periodoParlamentario().equals(CURRENT)) existing.put(pleno.id(), pleno);
      }
    }

    boolean extractPleno = true; // opt-out to do this manually yet

    var updated = new HashSet<DocumentoPleno>();
    for (var p : existing.values()) {
      var pleno = existing.getOrDefault(p.id(), p);
      if (!pleno.provisional()
          && pleno.paginas() < 1
          && pleno.periodo().periodoParlamentario().equals(CURRENT)) {
        if (extractPleno) {
          LOG.info("Extracting Pleno: {}", pleno.fecha());
          Files.writeString(Path.of("pr-title.txt"), pleno.prTitle());
          Files.writeString(Path.of("pr-branch.txt"), pleno.prBranchName());
          Files.writeString(Path.of("pr-content.txt"), pleno.prContent());
          pleno = pleno.extract();
          extractPleno = false;
        }
      }
      updated.add(pleno);
    }

    var registroPlenos =
        updated.stream().sorted(Comparator.comparing(DocumentoPleno::id).reversed()).toList();

    LOG.info("Writing CSV to file");
    var content = csvHeader();
    registroPlenos.forEach(pleno -> content.append(pleno.csvEntry()));
    Files.writeString(plenosCsvPath, content.toString());
    LOG.info("Done");
  }
}
