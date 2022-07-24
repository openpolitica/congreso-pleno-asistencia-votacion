package op.congreso.pleno;

public record ResultadoCongresista<T>(
  String grupoParlamentario,
//  String grupoParlamentarioDescripcion,
  String congresista,
  T resultado
) {}
