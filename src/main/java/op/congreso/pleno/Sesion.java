package op.congreso.pleno;

import java.time.LocalDateTime;

/**
 * Sesion de pleno
 *
 * @see op.congreso.pleno.asistencia.AsistenciaSesion
 * @see op.congreso.pleno.votacion.VotacionSesion
 */
public record Sesion(Pleno pleno, int quorum, LocalDateTime fechaHora) {}
