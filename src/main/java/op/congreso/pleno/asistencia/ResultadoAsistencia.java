package op.congreso.pleno.asistencia;

public record ResultadoAsistencia(
  int presentes,
  int ausentes,
  int licencias,
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
    int otros
  ) {
    return new ResultadoAsistencia(
      presentes,
      ausentes,
      licencias,
      otros,
      presentes + ausentes + licencias + otros
    );
  }

  public static class Builder {

    int presentes = 0;
    int ausentes = 0;
    int licencias = 0;
    int otros = 0;

    public void with(Asistencia asistencia, int resultado) {
      switch (asistencia) {
        case PRESENTE -> this.presentes = resultado;
        case AUSENTE -> this.ausentes = resultado;
        case LICENCIA_OFICIAL,
          LICENCIA_PERSONAL,
          LICENCIA_POR_ENFERMEDAD -> this.licencias =
          this.licencias + resultado;
        default -> this.otros = resultado;
      }
    }

    public ResultadoAsistencia build() {
      return new ResultadoAsistencia(
        presentes,
        ausentes,
        licencias,
        otros,
        presentes + ausentes + licencias + otros
      );
    }
  }
}
