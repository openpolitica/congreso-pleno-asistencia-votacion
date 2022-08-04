package op.congreso.pleno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Congresistas {
    public static Set<String> names = new HashSet<>();
    static {
        try {
            var lines = Files.readAllLines(Path.of("data/2021-2026/congresistas.csv"));
            for (int i = 1; i < lines.size(); i++) {
                var l = lines.get(i);
                names.add(l.replace("\"",""));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
