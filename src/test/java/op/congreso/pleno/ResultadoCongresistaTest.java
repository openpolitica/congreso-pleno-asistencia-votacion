package op.congreso.pleno;

import static org.assertj.core.api.Assertions.assertThat;

import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.votacion.Votacion;
import org.junit.jupiter.api.Test;

class ResultadoCongresistaTest {

  @Test
  void asistenciaShouldBeReadyAndCompleteName() {
    var b = new ResultadoCongresista.Builder<Asistencia>();
    b.processAsistenciaLine("RP");
    b.processAsistenciaLine("JÁUREGUI MARTÍNEZ DE AGUAYO, MARIA PRE");

    assertThat(b.isReady()).isTrue();
    var r = b.build();
    assertThat(r.congresista())
        .isEqualTo("JÁUREGUI MARTÍNEZ DE AGUAYO, MARÍA DE LOS MILAGROS JACKELINE");
    assertThat(r.resultado()).isEqualTo(Asistencia.PRESENTE);
    assertThat(r.grupoParlamentario()).isEqualTo("RP");
  }

  @Test
  void votacionShouldBeReady_no() {
    var b = new ResultadoCongresista.Builder<Votacion>();
    b.processVotacionLine("FP");
    b.processVotacionLine("BARBARÁN REYES, ROSANGELLA ANDREA");
    assertThat(b.isReady()).isFalse();
    b.processVotacionLine(new StringBuilder("NO"));
    assertThat(b.isReady()).isTrue();
    var r = b.build();
    assertThat(r.congresista()).isEqualTo("BARBARÁN REYES, ROSANGELLA ANDREA");
    assertThat(r.resultado()).isEqualTo(Votacion.NO);
    assertThat(r.grupoParlamentario()).isEqualTo("FP");
  }

  @Test
  void votacionShouldBeReady_aus() {
    var b = new ResultadoCongresista.Builder<Votacion>();
    b.processVotacionLine("RP");
    b.processVotacionLine("JÁUREGUI MARTÍNEZ DE AGUAYO, MARIA aus");
    assertThat(b.isReady()).isTrue();
    var r = b.build();
    assertThat(r.congresista())
        .isEqualTo("JÁUREGUI MARTÍNEZ DE AGUAYO, MARÍA DE LOS MILAGROS JACKELINE");
    assertThat(r.resultado()).isEqualTo(Votacion.AUSENTE);
    assertThat(r.grupoParlamentario()).isEqualTo("RP");
  }
}
