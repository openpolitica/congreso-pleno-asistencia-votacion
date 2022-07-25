package op.congreso.pleno.asistencia;

public record ResultadoAsistencia(
  int presentes,
  int ausentes,
  int licencias,
  int suspendidos,
  int otros,
  int total
) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public static ResultadoAsistencia create(
    int presentes,
    int ausentes,
    int licencias,
    int suspendidos,
    int otros
  ) {
    return new ResultadoAsistencia(
      presentes,
      ausentes,
      licencias,
      suspendidos,
      otros,
      presentes + ausentes + licencias + suspendidos + otros
    );
  }

  public static class Builder {

    int presentes = 0;
    int ausentes = 0;
    int licencias = 0;
    int suspendidos = 0;
    int otros = 0;

    public void with(Asistencia asistencia, int resultado) {
      switch (asistencia) {
        case PRESENTE -> this.presentes = resultado;
        case AUSENTE -> this.ausentes = resultado;
        case LICENCIA_OFICIAL,
          LICENCIA_PERSONAL,
          LICENCIA_POR_ENFERMEDAD -> this.licencias =
          this.licencias + resultado;
        case SUSPENDIDO -> this.suspendidos = resultado;
        default -> this.otros = resultado;
      }
    }

    public ResultadoAsistencia build() {
      return ResultadoAsistencia.create(
        presentes,
        ausentes,
        licencias,
        suspendidos,
        otros
      );
    }
  }
}
