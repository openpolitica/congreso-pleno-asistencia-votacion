package op.congreso.pleno.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class GrupoParlamentarioUtil {

  public static final Set<String> VALID_GP;

  static {
    try {
      VALID_GP = all().keySet();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String findSimilar(String gp) {
    var result = FuzzySearch.extractOne(gp, VALID_GP);
    if (
      similar(gp, result)
    ) return result.getString(); else throw new IllegalArgumentException(
      "Not similar found, score: " + result.getScore()
    );
  }

  public static boolean isSimilar(String gp) {
    var result = FuzzySearch.extractOne(gp, VALID_GP);
    return similar(gp, result);
  }

  private static boolean similar(String gp, ExtractedResult result) {
    return (
      result.getScore() >= 50 &&
      Math.abs(result.getString().length() - gp.length()) < 4
    );
  }

  public static void main(String[] args) {
    System.out.println(isSimilar("EP"));
    System.out.println(findSimilar("PIS"));
  }

  public static Map<String, String> all() throws IOException {
    return Files
      .readAllLines(Path.of("data/2021-2026/grupos_parlamentarios.csv"))
      .stream()
      .dropWhile(s -> s.startsWith("grupo_parlamentario"))
      .map(a -> a.split(","))
      .filter(a -> a.length == 2)
      .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }
}
