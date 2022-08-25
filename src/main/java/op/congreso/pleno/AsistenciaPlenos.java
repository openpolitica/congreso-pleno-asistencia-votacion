package op.congreso.pleno;

import java.util.Set;
import op.congreso.pleno.asistencia.RegistroAsistencia;

public record AsistenciaPlenos(
  String periodo,
  Set<RegistroAsistencia> registros
) {}
