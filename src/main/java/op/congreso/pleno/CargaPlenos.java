package op.congreso.pleno;

import static op.congreso.pleno.MetadataPleno.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CargaPlenos {
    public static void main(String[] args) throws IOException {
        var root = collect("/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/new_asistenciavotacion", 5);

        var plenosList = new LinkedList<MetadataPleno>();

        for (var periodos : root.entrySet()) {
            System.out.println(periodos.getKey());
            var year = collect(periodos.getValue(), 4);
            for (var anual : year.entrySet()) {
                System.out.println(anual.getKey());
                var periodo = collect(anual.getValue(), 3);
                for (var legislatura : periodo.entrySet()) {
                    System.out.println(legislatura.getKey());

                    var plenos = collectPleno(
                        periodos.getKey(), anual.getKey(),
                        legislatura.getKey(), legislatura.getValue()
                    );
                    plenosList.addAll(plenos.values());
                }
            }
        }

        var jsonMapper = new ObjectMapper();

        var bytes = Files.readAllBytes(Path.of("plenos.json"));
        var existing = jsonMapper.readValue(bytes, new TypeReference<List<MetadataPleno>>() {
            })
            .stream()
            .collect(Collectors.toMap(MetadataPleno::id, p -> p));

        var updated = plenosList.stream().map(p -> existing.getOrDefault(p.id(), p))
            .map(p -> {
                if (p.paginas() < 1) {
                    System.out.printf("Descargando %s%n", p);
                    p.download();
                    return p.withPaginas();
                } else {
                    return p;
                }
            })
            .sorted(Comparator.comparing(MetadataPleno::id).reversed())
            .collect(Collectors.toList());
        Files.writeString(Path.of("plenos.json"), jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updated));
        StringBuilder content = MetadataPleno.csvHeader();
        updated.forEach(pleno -> content.append(pleno.csvEntry()));
        Files.writeString(Path.of("plenos.csv"), content.toString());
    }
}
