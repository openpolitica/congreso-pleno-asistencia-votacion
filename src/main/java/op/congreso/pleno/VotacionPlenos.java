package op.congreso.pleno;

import java.util.Set;
import op.congreso.pleno.votacion.RegistroVotacion;

public record VotacionPlenos(String periodo, Set<RegistroVotacion> registros) {}
