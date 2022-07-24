package op.congreso.pleno.votacion;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.Resultado;

public enum Votacion implements Resultado {
  SI("SI+++", "SI"),
  NO("NO-", "NO"),
  ABSTENCION("ABST.", "ABSTENCION"),
  AUSENTE("AUS", "AUSENTE"),
  LICENCIA_OFICIAL("LO", "LICENCIA OFICIAL"),
  LICENCIA_POR_ENFERMEDAD("LE", "LICENCIA POR ENFERMEDAD"),
  LICENCIA_PERSONAL("LP", "LICENCIA PERSONAL"),
  SIN_RESPONDER("SINRES", "SIN RESPONDER"),
  COMISION_ORDINARIA("COM", "COMISION ORDINARIA"),
  COMISION_EXT_INTERNACIONAL("CEI", "COMISION EXT. INTERNACIONAL"),
  JUNTA_DE_PORTAVOCES("JP", "JUNTA DE PORTAVOCES"),
  BANCADA("BAN", "BANCADA"),
  SUSPENDIDO("SUS", "SUSPENDIDO"),
  FALLECIDO("F", "FALLECIDO"),
  FALTO("FA", "FALTO");

  final String codigo;
  final String descripcion;

  Votacion(String codigo, String descripcion) {
    this.codigo = codigo;
    this.descripcion = descripcion;
  }

  static final Map<String, Votacion> map = Arrays
    .stream(Votacion.values())
    .collect(Collectors.toMap(a -> a.codigo, a -> a));

  public static Votacion of(String asistencia) {
    return map.get(asistencia.toUpperCase());
  }

  public static boolean is(String text) {
    return map.containsKey(text.toUpperCase());
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
