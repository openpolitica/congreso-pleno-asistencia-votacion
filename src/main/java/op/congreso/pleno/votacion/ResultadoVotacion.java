package op.congreso.pleno.votacion;

public record ResultadoVotacion(
  int si,
  int no,
  int abstenciones,
  int sinResponder,
  int total
) {
    public static ResultadoVotacion create(int si, int no, int abstenciones, int sinResp) {
        return new ResultadoVotacion(
                si, no, abstenciones,
                sinResp,
                si + no + abstenciones + sinResp
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        int si, no, abstenciones = 0;
        int ausentes = 0;
        int licencias = 0;
        int otros = 0;
        int sinResp = 0;

        public void with(Votacion votacion, int resultado) {
            switch (votacion) {
                case SI -> this.si = resultado;
                case NO -> this.no = resultado;
                case ABSTENCION -> this.abstenciones = resultado;
                case SIN_RESPONDER -> this.sinResp = resultado;
                case AUSENTE -> this.ausentes = resultado;
                case LICENCIA_OFICIAL,
                        LICENCIA_PERSONAL,
                        LICENCIA_POR_ENFERMEDAD -> this.licencias =
                        this.licencias + resultado;
                default -> this.otros = resultado;
            }
        }

        public ResultadoVotacion build() {
            return new ResultadoVotacion(
                    si, no, abstenciones,
                    sinResp,
                    si + no + abstenciones + sinResp
            );
        }
    }
}
