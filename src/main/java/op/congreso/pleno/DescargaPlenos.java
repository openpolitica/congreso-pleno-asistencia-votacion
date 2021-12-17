package op.congreso.pleno;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class DescargaPlenos {

    public static void main(String[] args) throws IOException {
        var root = RegistroPleno.collect("/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/new_asistenciavotacion", 5);

        var content = RegistroPleno.csvHeader();

        var plenosList = new LinkedList<RegistroPleno>();

        for (var entry : root.entrySet()) {
            System.out.println(entry.getKey());
            var year = RegistroPleno.collect(entry.getValue(), 4);
            for (var e2: year.entrySet()) {
                System.out.println(e2.getKey());
                var periodo = RegistroPleno.collect(e2.getValue(), 3);
                for (var e3 : periodo.entrySet()) {
                    System.out.println(e3.getKey());

                    var plenos = RegistroPleno.collectPleno(entry.getKey(), e2.getKey(), e3.getKey(), e3.getValue());
                    var csv = plenos.values().stream()
                            .map(RegistroPleno::csvEntry)
                            .collect(Collectors.joining());
                    content.append(csv);
                    plenosList.addAll(plenos.values());
                }
            }
        }

        var jsonMapper = new ObjectMapper();
        Files.writeString(Path.of("plenos.json"),  jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plenosList));
        Files.writeString(Path.of("plenos.csv"), content.toString());
    }
}
