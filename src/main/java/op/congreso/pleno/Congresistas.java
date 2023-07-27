package op.congreso.pleno;

import static op.congreso.pleno.Constantes.DATA_PERIODO_ACTUAL;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.xdrop.fuzzywuzzy.FuzzySearch;

/** Utilidad para validar congresistas del actual periodo. */
public class Congresistas {

  public static Set<String> names = new HashSet<>();

  static {
    try {
      var lines = Files.readAllLines(DATA_PERIODO_ACTUAL.resolve("congresistas.csv"));
      for (int i = 1; i < lines.size(); i++) {
        var l = lines.get(i);
        names.add(l.replace("\"", ""));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> checkCongresistas(List<String> congresistas) {
    return congresistas.stream().filter(c -> !Congresistas.names.contains(c)).toList();
  }

  public static String findSimilar(String c) {
    var result = FuzzySearch.extractTop(c, names, 2);
    return result.get(0).getString();
  }

  public static void main(String[] args) {
    System.out.println(Congresistas.findSimilar("ECHAIZ DE NUNEZ IZAGA, GLADYS M."));
  }
}
