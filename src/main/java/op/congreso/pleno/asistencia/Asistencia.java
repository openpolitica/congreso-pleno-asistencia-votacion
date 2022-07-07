package op.congreso.pleno.asistencia;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.Resultado;

public enum Asistencia implements Resultado {
  PRESENTE("PRE", "PRESENTE"),
  AUSENTE("AUS", "AUSENTE"),
  LICENCIA_OFICIAL("LO", "LICENCIA OFICIAL"),
  LICENCIA_POR_ENFERMEDAD("LE", "LICENCIA POR ENFERMEDAD"),
  LICENCIA_PERSONAL("LP", "LICENCIA PERSONAL"),
  COMISION_ORDINARIA("COM", "COMISION ORDINARIA"),
  COMISION_EXT_INTERNACIONAL("CEI", "COMISION EXT. INTERNACIONAL"),
  JUNTA_DE_PORTAVOCES("JP", "JUNTA DE PORTAVOCES"),
  BANCADA("BAN", "BANCADA"),
  SUSPENDIDO("SUS", "SUSPENDIDO"),
  FALLECIDO("F", "FALLECIDO"),
  FALTO("FA", "FALTO");

  final String codigo;
  final String descripcion;

  Asistencia(String codigo, String description) {
    this.codigo = codigo;
    this.descripcion = description;
  }

  static final Map<String, Asistencia> map = Arrays
    .stream(Asistencia.values())
    .collect(Collectors.toMap(a -> a.codigo, a -> a));

  public static Asistencia of(String asistencia) {
    return map.get(asistencia);
  }

  @Override
  public String codigo() {
    return codigo;
  }

  @Override
  public String descripcion() {
    return descripcion;
  }
}
