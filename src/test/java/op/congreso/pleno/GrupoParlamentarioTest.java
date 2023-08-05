package op.congreso.pleno;

import static op.congreso.pleno.GrupoParlamentario.findSimilar;
import static op.congreso.pleno.GrupoParlamentario.isSimilar;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GrupoParlamentarioTest {

  @Test
  void testSimilar() {
    assertThat(isSimilar("EP")).isTrue();
    assertThat(findSimilar("PIS")).isEqualTo("AP-PIS");
  }
}
