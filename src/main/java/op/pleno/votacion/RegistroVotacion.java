package op.pleno.votacion;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import op.pleno.Pleno;
import op.pleno.ResultadoCongresista;

public record RegistroVotacion(
    Pleno pleno,
    String sesion,
    String presidente,
    LocalTime hora,
    String asunto,
    List<ResultadoCongresista> votaciones,
    Map<String, ResultadoVotacion> porGrupo,
    ResultadoVotacion total
) {
  String id() {
    return pleno.fecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T" +
        hora.format(DateTimeFormatter.ofPattern("HH:mm")) + "-votacion";
  }
}
