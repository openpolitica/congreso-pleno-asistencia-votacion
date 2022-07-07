package op.congreso.pleno.votacion;

public record ResultadoVotacion(
  int si,
  int no,
  int abstenciones,
  int ausentes,
  int licencias,
  int otros,
  int total
) {}
