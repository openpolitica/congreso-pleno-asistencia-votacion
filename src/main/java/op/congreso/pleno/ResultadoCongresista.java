package op.congreso.pleno;

import java.util.List;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.votacion.Votacion;

/**
 * Resultado de Congresista (e.g. Votacion o Asistencia). Cada Sesion tiene una lista de resultados
 * por congresista.
 */
public record ResultadoCongresista<T>(String grupoParlamentario, String congresista, T resultado) {
  public static <T> Builder<T> newBuilder() {
    return new Builder<>();
  }

  public ResultadoCongresista<T> replaceCongresista(String s) {
    return new ResultadoCongresista<>(grupoParlamentario, s, resultado);
  }

  public static class Builder<T> {

    String grupoParlamentario;
    String congresista;
    T resultado;

    void withGrupoParlamentario(String gp) {
      this.grupoParlamentario = gp;
    }

    void withCongresista(String congresista) {
      this.congresista = congresista;
    }

    void withResultado(T resultado) {
      this.resultado = resultado;
    }

    public boolean isReady() {
      return (grupoParlamentario != null && congresista != null && resultado != null);
    }

    public ResultadoCongresista<T> build() {
      return new ResultadoCongresista<>(grupoParlamentario, congresista, resultado);
    }

    public void processAsistenciaLine(String s) {
      processAsistenciaLine(new StringBuilder(s));
    }

    /**
     * Text either contain: GP, or GP+Congresista, or GP+Congresista+Result, or Congresista, or
     * Congresista+Result, or Congresista+Result+GP, or Result, or Result+GP
     */
    @SuppressWarnings("unchecked")
    public void processAsistenciaLine(StringBuilder text) {
      var c = new StringBuilder();
      while (!text.toString().trim().isEmpty() && !this.isReady()) {
        int idx = text.indexOf(" ");
        var word = text.substring(0, idx > 0 ? idx : text.length()).trim();
        if (grupoParlamentario == null) {
          if (GrupoParlamentario.isSimilar(word)) {
            withGrupoParlamentario(GrupoParlamentario.findSimilar(word));
          } else
            throw new IllegalArgumentException(
                "Grupo no encontrado! Grupo [" + word + "] en texto: [" + text + "]");
        } else {
          if (Asistencia.is(word)) {
            if (!c.isEmpty()) {
              withCongresista(Congresistas.findSimilar(c.toString().trim()));
            }
            withResultado((T) Asistencia.of(word));
          } else {
            c.append(word).append(" ");
          }
        }
        text.delete(0, word.length() + 1);
      }
      if (!c.isEmpty()) {
        withCongresista(Congresistas.findSimilar(c.toString().trim()));
      }
    }

    public void processVotacionLine(String t) {
      processVotacionLine(new StringBuilder(t));
    }

    @SuppressWarnings("unchecked")
    public void processVotacionLine(StringBuilder text) {
      var c = new StringBuilder();
      while (!text.toString().trim().isEmpty() && !this.isReady()) {
        int idx = text.indexOf(" ");
        var word = text.substring(0, idx > 0 ? idx : text.length());
        if (!List.of("+++", "---").contains(word)) {
          if (grupoParlamentario == null) {
            if (GrupoParlamentario.isSimilar(word)) {
              withGrupoParlamentario(GrupoParlamentario.findSimilar(word));
            } else throw new IllegalArgumentException("No GP! " + word);
          } else {
            if (Votacion.is(word)) {
              if (!c.isEmpty()) {
                withCongresista(Congresistas.findSimilar(c.toString().trim()));
              }
              withResultado((T) Votacion.of(word));
            } else {
              c.append(word).append(" ");
            }
          }
        }
        text.delete(0, word.length() + 1);
      }
      if (!c.isEmpty()) {
        withCongresista(Congresistas.findSimilar(c.toString().trim()));
      }
    }
  }
}
