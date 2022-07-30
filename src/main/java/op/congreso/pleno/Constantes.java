package op.congreso.pleno;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class Constantes {

  public static final List<String> VALID_GP = List.of(
    "FP",
    "PL",
    "AP",
    "APP",
    "BM",
    "AP-PIS",
    "RP",
    "PD",
    "SP",
    "CD-JP",
    "CD-JPP",
    "PB",
    "PD",
    "PP",
    "NA"
  );
  public static final DateTimeFormatter FECHA_HORA_PATTERN = DateTimeFormatter.ofPattern(
    "'Fecha: 'd/MM/yyyy' Hora: 'hh:mm a"
  );

  public static final String ASISTENCIA = "ASISTENCIA:";
  public static final String VOTACION = "VOTACIÓN:";
}
