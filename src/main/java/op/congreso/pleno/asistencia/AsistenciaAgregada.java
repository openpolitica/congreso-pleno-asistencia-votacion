package op.congreso.pleno.asistencia;

/** Resultados de asistencia agregados por Sesion */
public record AsistenciaAgregada(
    int presentes, int ausentes, int licencias, int suspendidos, int otros, int total) {
  public static Builder newBuilder() {
    return new Builder();
  }

  public static AsistenciaAgregada create(
      int presentes, int ausentes, int licencias, int suspendidos, int otros) {
    return new AsistenciaAgregada(
        presentes,
        ausentes,
        licencias,
        suspendidos,
        otros,
        presentes + ausentes + licencias + suspendidos + otros);
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
        case LICENCIA_OFICIAL, LICENCIA_PERSONAL, LICENCIA_POR_ENFERMEDAD -> this.licencias =
            this.licencias + resultado;
        case SUSPENDIDO -> this.suspendidos = resultado;
        default -> this.otros = resultado;
      }
    }

    public AsistenciaAgregada build() {
      return AsistenciaAgregada.create(presentes, ausentes, licencias, suspendidos, otros);
    }

    public Builder increase(Asistencia asistencia) {
      switch (asistencia) {
        case PRESENTE -> this.presentes = this.presentes + 1;
        case AUSENTE -> this.ausentes = this.ausentes + 1;
        case LICENCIA_OFICIAL, LICENCIA_PERSONAL, LICENCIA_POR_ENFERMEDAD -> this.licencias =
            this.licencias + 1;
        case SUSPENDIDO -> this.suspendidos = this.suspendidos + 1;
        default -> this.otros = this.otros + 1;
      }
      return this;
    }
  }
}
