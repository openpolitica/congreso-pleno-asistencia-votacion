package op.congreso.pleno;

import static op.congreso.pleno.Rutas.FECHA_HORA_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class RutasTest {

  @Test
  void testFecha() {
    String text = "FECHA: 7/09/2022 HORA: 04:19 pm";
    assertThat(text.charAt(29)).isEqualTo('p');
    assertThat(LocalDateTime.parse(text.toLowerCase(Locale.UK), FECHA_HORA_PATTERN))
        .isEqualTo("2022-09-07T16:19");
  }
}
