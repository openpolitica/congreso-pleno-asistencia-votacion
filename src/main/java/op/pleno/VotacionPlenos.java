package op.pleno;

import java.util.Set;
import op.pleno.asistencia.RegistroAsistencia;
import op.pleno.votacion.RegistroVotacion;

public record VotacionPlenos(
    String periodo,
    Set<RegistroVotacion> registros
) {
}
