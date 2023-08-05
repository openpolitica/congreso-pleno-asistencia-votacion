package op.congreso.pleno;

import java.util.List;

/**
 * Resultado individual de una sesion del pleno del congreso
 *
 * @see op.congreso.pleno.asistencia.Asistencia
 * @see op.congreso.pleno.votacion.Votacion
 */
public interface Resultado {
  List<String> codigos();

  String descripcion();
}
