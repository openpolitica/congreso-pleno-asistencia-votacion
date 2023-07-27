package op.congreso.pleno.asistencia;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import op.congreso.pleno.Resultado;

/**
 * Posibles resultados de asistencia de un Congresista durante una Sesion.
 *
 * @see AsistenciaAgregada para ver los resultados agregados
 * @see AsistenciaSesion para ver la sesion de asistencia
 */
public enum Asistencia implements Resultado {
  PRESENTE("PRE", "PRESENTES"),
  AUSENTE("AUS", "AUSENTES"),
  LICENCIA_OFICIAL("LO", "CON LICENCIA OFICIAL"),
  LICENCIA_POR_ENFERMEDAD("LE", "LICENCIA POR ENFERMEDAD"),
  LICENCIA_PERSONAL("LP", "LICENCIA PERSONAL"),
  COMISION_ORDINARIA("COM", "COMISIÓN ORDINARIA"),
  COMISION_EXT_INTERNACIONAL("CEI", "COM. EXT. INTERNACIONAL"),
  JUNTA_DE_PORTAVOCES("JP", "JUNTA DE PORTAVOCES"),
  BANCADA("BAN", "BANCADA"),
  SUSPENDIDO("SUS", "SUSPENDIDOS"),
  FALLECIDO("F", "FALLECIDOS"),
  FALTO("FA", "FALTO");

  /** Siglas utilizadas en los documentos de sesion */
  final String codigo;
  /** Descripcion del tipo de asistencia */
  final String descripcion;

  Asistencia(String codigo, String description) {
    this.codigo = codigo;
    this.descripcion = description;
  }

  static final Map<String, Asistencia> all =
      Arrays.stream(Asistencia.values()).collect(Collectors.toMap(a -> a.codigo, a -> a));

  public static Asistencia of(String asistencia) {
    var a1 = all.get(asistencia.toUpperCase().trim());
    if (a1 != null) return a1;
    else return Asistencia.valueOf(asistencia);
  }

  public static boolean is(String text) {
    return all.containsKey(text.toUpperCase().trim());
  }

  public static boolean isDescripcion(String text) {
    return all.values().stream()
        .map(Asistencia::descripcion)
        .anyMatch(s -> s.equals(text.toUpperCase()));
  }

  @Override
  public List<String> codigos() {
    return List.of(codigo);
  }

  @Override
  public String descripcion() {
    return descripcion;
  }
}
