package op.congreso.pleno;

import java.util.List;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.util.GrupoParlamentarioUtil;
import op.congreso.pleno.votacion.Votacion;

public record ResultadoCongresista<T>(
  String grupoParlamentario,
  String congresista,
  T resultado
) {
  public static <T> Builder<T> newBuilder() {
    return new Builder<>();
  }

  public ResultadoCongresista<T> replaceCongresista(String s) {
    return new ResultadoCongresista<T>(grupoParlamentario, s, resultado);
  }

  public static class Builder<T> {

    String grupoParlamentario;
    String congresista;
    T resultado;

    Builder<T> withGrupoParlamentario(String gp) {
      this.grupoParlamentario = gp;
      return this;
    }

    Builder<T> withCongresista(String congresista) {
      this.congresista = congresista;
      return this;
    }

    Builder<T> withResultado(T resultado) {
      this.resultado = resultado;
      return this;
    }

    boolean isEmpty() {
      return (
        grupoParlamentario == null && congresista == null && resultado == null
      );
    }

    public boolean isReady() {
      return (
        grupoParlamentario != null && congresista != null && resultado != null
      );
    }

    public ResultadoCongresista<T> build() {
      return new ResultadoCongresista<>(
        grupoParlamentario,
        congresista,
        resultado
      );
    }

    public void processAsistenciaLine(String s) {
      processAsistenciaLine(new StringBuilder(s));
    }

    /**
     * Text either contain:
     * GP or
     * GP+Congresista or
     * GP+Congresista+Result or
     * Congresista or
     * Congresista+Result or
     * Congresista+Result+GP or
     * Result or
     * Result+GP
     */
    @SuppressWarnings("unchecked")
    public void processAsistenciaLine(StringBuilder text) {
      var c = new StringBuilder();
      while (!text.toString().trim().isEmpty() && !this.isReady()) {
        int idx = text.indexOf(" ");
        var word = text.substring(0, idx > 0 ? idx : text.length()).trim();
        if (grupoParlamentario == null) {
          if (GrupoParlamentarioUtil.isSimilar(word)) {
            withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(word));
          } else throw new IllegalArgumentException(
            "No GP! " + word + " at " + text
          );
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
            if (GrupoParlamentarioUtil.isSimilar(word)) {
              withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(word));
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

  public static void main(String[] args) {
    var b = new Builder<Asistencia>();
    b.processAsistenciaLine("RP");
    b.processAsistenciaLine("JÁUREGUI MARTÍNEZ DE AGUAYO, MARIA PRE");
    System.out.println(b.isReady());
    System.out.println(b.build());

    var b1 = new Builder<Votacion>();
    b1.processVotacionLine("APP");
    b1.processVotacionLine("ACUÑA PERALTA, MARIA GRIMANEZA");
    StringBuilder sinRes_sp = new StringBuilder("SinRes SP");
    b1.processVotacionLine(sinRes_sp);
    System.out.println(b1.isReady());
    System.out.println(b1.build());
    b1 = new Builder<>();
    b1.processVotacionLine(sinRes_sp);
    b1.processVotacionLine("ELERA GARCÍA, WILMAR ALBERTO");
    b1.processVotacionLine("SI BM");
    System.out.println(b1.isReady());
    System.out.println(b1.build());
    b1.processVotacionLine("PAREDES CASTRO, FRANCIS JHASMINA");
    b1.processVotacionLine("SI");
    b1.processVotacionLine("APP");
    b1.processVotacionLine("ACUÑA PERALTA, SEGUNDO HÉCTOR");
  }
}
