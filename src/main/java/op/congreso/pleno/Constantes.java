package op.congreso.pleno;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Constantes {

  public static final DateTimeFormatter FECHA_HORA_PATTERN = DateTimeFormatter.ofPattern(
    "'FECHA: 'd/MM/yyyy' HORA: 'hh:mm a"
  );

  public static final String ASISTENCIA = "ASISTENCIA:";
  public static final String VOTACION = "VOTACIÓN:";

  public static void main(String[] args) {
    System.out.println(LocalDateTime.parse("Fecha: 26/05/2022 Hora: 11:14 am".toUpperCase(), FECHA_HORA_PATTERN));
  }
}
