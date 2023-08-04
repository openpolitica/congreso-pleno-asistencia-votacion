package op.congreso.pleno;

import java.nio.file.Path;

/** Datos del periodo parlamentario */
public record Periodo(String periodoParlamentario, String periodoAnual, String legislatura) {
  public Path path(Path base) {
    return base.resolve(periodoParlamentario).resolve(periodoAnual).resolve(legislatura);
  }
}
