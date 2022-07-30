package op.congreso.pleno;

public record ResultadoCongresista<T>(
  String grupoParlamentario,
  String congresista,
  T resultado
) {}
