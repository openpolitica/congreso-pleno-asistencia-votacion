package op.congreso.pleno;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ConteoPaginas {
    public static void main(String[] args) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        var bytes = Files.readAllBytes(Path.of("plenos.json"));
        var plenos = jsonMapper.readValue(bytes, new TypeReference<List<RegistroPleno>>() {
        });

        var plenosWithPaginas =
                plenos.stream()
                        .filter(pleno -> pleno.paginas() < 1)
                        .map(RegistroPleno::withPaginas)
                        .collect(Collectors.toList());
        Files.writeString(Path.of("plenos.json"),
                jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plenosWithPaginas));
        StringBuilder content = RegistroPleno.csvHeader();
        plenosWithPaginas.forEach(pleno -> content.append(pleno.csvEntry()));
        Files.writeString(Path.of("plenos.csv"), content.toString());
    }

}
