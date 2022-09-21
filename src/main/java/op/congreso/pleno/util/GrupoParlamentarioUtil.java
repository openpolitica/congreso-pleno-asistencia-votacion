package op.congreso.pleno.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public class GrupoParlamentarioUtil {

  public static final List<String> VALID_GP = List.of(
    "FP",
    "PL",
    "AP",
    "APP",
    "BM",
    "AP-PIS",
    "RP",
    "PD",
    "SP",
    "CD-JP",
    "CD-JPP",
    "PB",
    "PD",
    "PP",
    "NA",
    "ID"
  );

  public static String findSimilar(String gp) {
    var result = FuzzySearch.extractOne(gp, VALID_GP);
    if (result.getScore() < 80) throw new IllegalArgumentException(
      "Not similar found, score: " + result.getScore()
    );
    return result.getString();
  }

  public static boolean isSimilar(String gp) {
    var result = FuzzySearch.extractOne(gp, VALID_GP);
    return result.getScore() > 80;
  }

  public static void main(String[] args) {
    System.out.println(findSimilar("P-PIS"));
  }

  public static Map<String, String> all() throws IOException {
    return Files.readAllLines(Path.of("data/2021-2026/grupos_parlamentarios.csv"))
            .stream()
            .map(a -> a.split(","))
            .filter(a -> a.length == 2)
            .collect(Collectors.toMap(a -> a[0], a -> a[1]));
  }
}
