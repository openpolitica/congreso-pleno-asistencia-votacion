package op.pleno;

import java.util.Set;
import op.pleno.asistencia.RegistroAsistencia;

public record AsistenciaPlenos(
    String periodo,
    Set<RegistroAsistencia> registros
) {
}
