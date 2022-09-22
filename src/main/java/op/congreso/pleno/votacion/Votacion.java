package op.congreso.pleno.votacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import op.congreso.pleno.Resultado;

public enum Votacion implements Resultado {
  SI(List.of("SI+++", "SI"), "SI"),
  NO(List.of("NO-", "NO"), "NO"),
  ABSTENCION(List.of("ABST.", "ABST"), "ABSTENCION"),
  AUSENTE(List.of("AUS"), "AUSENTE"),
  LICENCIA_OFICIAL(List.of("LO"), "LICENCIA OFICIAL"),
  LICENCIA_POR_ENFERMEDAD(List.of("LE"), "LICENCIA POR ENFERMEDAD"),
  LICENCIA_PERSONAL(List.of("LP"), "LICENCIA PERSONAL"),
  SIN_RESPONDER(List.of("SINRES", "SinRes", "***"), "SIN RESPONDER"),
  COMISION_ORDINARIA(List.of("COM"), "COMISION ORDINARIA"),
  COMISION_EXT_INTERNACIONAL(List.of("CEI"), "COMISION EXT. INTERNACIONAL"),
  JUNTA_DE_PORTAVOCES(List.of("JP"), "JUNTA DE PORTAVOCES"),
  BANCADA(List.of("BAN"), "BANCADA"),
  SUSPENDIDO(List.of("SUS"), "SUSPENDIDO"),
  FALLECIDO(List.of("F"), "FALLECIDO"),
  FALTO(List.of("FA"), "FALTO");

  final List<String> codigos;
  final String descripcion;

  Votacion(List<String> codigos, String descripcion) {
    this.codigos = codigos;
    this.descripcion = descripcion;
  }

  static final Map<String, Votacion> map = new HashMap<>();

  static {
    for (var v : Votacion.values()) {
      for (var k : v.codigos()) {
        map.put(k.toUpperCase(), v);
      }
    }
  }

  public static Votacion of(String votacion) {
    Votacion v1 = map.get(votacion.toUpperCase());
    if (v1 == null) {
      return Votacion.valueOf(votacion);
    }
    return v1;
  }

  public static boolean is(String text) {
    return map.containsKey(text.toUpperCase());
  }

  @Override
  public List<String> codigos() {
    return codigos;
  }

  @Override
  public String descripcion() {
    return descripcion;
  }
}
