package op.congreso.pleno;

import static op.congreso.pleno.Rutas.DATA_PERIODO_ACTUAL;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

/** Grupos parlamentarios del actual periodo */
public record GrupoParlamentario(String nombre, String descripcion) {
  public static final Set<String> VALID_GP;

  static {
    try {
      VALID_GP = all().keySet();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String findSimilar(String gp) {
    if (VALID_GP.contains(gp)) {
      return gp;
    } else {
      var result = FuzzySearch.extractOne(gp, VALID_GP);
      if (similar(gp, result)) return result.getString();
      else throw new IllegalArgumentException("Not similar found, score: " + result.getScore());
    }
  }

  public static boolean isSimilar(String gp) {
    if (VALID_GP.contains(gp)) return true;
    var result = FuzzySearch.extractOne(gp, VALID_GP);
    return similar(gp, result);
  }

  private static boolean similar(String gp, ExtractedResult result) {
    return (result.getScore() >= 50 && Math.abs(result.getString().length() - gp.length()) < 4);
  }

  public static Map<String, String> all() throws IOException {
    return Files.readAllLines(DATA_PERIODO_ACTUAL.resolve("grupos_parlamentarios.csv")).stream()
        .dropWhile(s -> s.startsWith("grupo_parlamentario"))
        .map(a -> a.split(","))
        .filter(a -> a.length == 2)
        .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }

  public static boolean is(String gp) {
    return VALID_GP.contains(gp);
  }
}
