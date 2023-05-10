package op.congreso.pleno;

public record Periodo(String periodoParlamentario, String periodoAnual, String legislatura) {
  public String id() {
    return periodoParlamentario;
  }
}
