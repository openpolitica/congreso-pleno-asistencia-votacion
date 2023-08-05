package op.congreso.pleno.votacion;

/** Resultados de votacion agregados por Sesion */
public record VotacionAgregada(
    int si,
    int no,
    int abstenciones,
    int sinResponder,
    int ausentes,
    int licencias,
    int otros,
    int total) {
  public static VotacionAgregada create(
      int si, int no, int abstenciones, int sinResp, int ausentes, int licencias, int otros) {
    return new VotacionAgregada(
        si,
        no,
        abstenciones,
        sinResp,
        ausentes,
        licencias,
        otros,
        si + no + abstenciones + ausentes + sinResp + otros);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    int si, no, abstenciones = 0;
    int ausentes = 0;
    int licencias = 0;
    int otros = 0;
    int sinRespuesta = 0;

    public void with(Votacion votacion, int resultado) {
      switch (votacion) {
        case SI -> this.si = resultado;
        case NO -> this.no = resultado;
        case ABSTENCION -> this.abstenciones = resultado;
        case SIN_RESPONDER -> this.sinRespuesta = resultado;
        case AUSENTE -> this.ausentes = resultado;
        case LICENCIA_OFICIAL, LICENCIA_PERSONAL, LICENCIA_POR_ENFERMEDAD -> this.licencias =
            this.licencias + resultado;
        default -> this.otros = resultado;
      }
    }

    public VotacionAgregada build() {
      return new VotacionAgregada(
          si,
          no,
          abstenciones,
          sinRespuesta,
          ausentes,
          licencias,
          otros,
          si + no + abstenciones + sinRespuesta + ausentes + licencias + otros);
    }

    public Builder increase(Votacion votacion) {
      switch (votacion) {
        case SI -> this.si = this.si + 1;
        case NO -> this.no = this.no + 1;
        case ABSTENCION -> this.abstenciones = this.abstenciones + 1;
        case SIN_RESPONDER -> this.sinRespuesta = this.sinRespuesta + 1;
        case AUSENTE -> this.ausentes = this.ausentes + 1;
        case LICENCIA_OFICIAL, LICENCIA_PERSONAL, LICENCIA_POR_ENFERMEDAD -> this.licencias =
            this.licencias + 1;
        default -> this.otros = this.otros + 1;
      }
      return this;
    }
  }
}
