package op.congreso.pleno;

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
      return grupoParlamentario == null && congresista == null && resultado == null;
    }

    public boolean isReady() {
      return grupoParlamentario != null && congresista != null && resultado != null;
    }

    public ResultadoCongresista<T> build() {
      return new ResultadoCongresista<>(grupoParlamentario, congresista, resultado);
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
      while(!text.toString().trim().isEmpty() && !this.isReady()) {
        int idx = text.indexOf(" ");
        if (idx == -1) {
          var t = text.toString();
          if (GrupoParlamentarioUtil.isSimilar(t)) {
            withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(t));
          } else if (Asistencia.is(t)
//                  || Votacion.is(t)
          ) {
            if (Asistencia.is(t)) {
              withResultado((T) Asistencia.of(t));
            }
//            if (Votacion.is(t)) {
//              withResultado((T) Votacion.of(t));
//            }
          } else {
            c.append(t);
          }
          text.delete(0, text.length() + 1);
        } else {
          var word = text.substring(0, idx);
          if (GrupoParlamentarioUtil.isSimilar(word)) {
            withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(word));
          } else if (Asistencia.is(word)) {
            withResultado((T) Asistencia.of(word));
//          } else if (Votacion.is(word)) {
//            withResultado((T) Votacion.of(word));
          } else {
            c.append(word).append(" ");
          }
          text.delete(0, word.length() + 1);
        }
      }
      if (!c.isEmpty()) {
        withCongresista(Congresistas.findSimilar(c.toString().trim()));
      }
    }

    @SuppressWarnings("unchecked")
    public void processVotacionLine(StringBuilder text) {
      var c = new StringBuilder();
      while(!text.toString().trim().isEmpty() && !this.isReady()) {
        int idx = text.indexOf(" ");
        if (idx == -1) {
          var t = text.toString();
          if (GrupoParlamentarioUtil.isSimilar(t)) {
            withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(t));
          } else if (Votacion.is(t)
//                  || Votacion.is(t)
          ) {
            if (Votacion.is(t)) {
              withResultado((T) Votacion.of(t));
            }
//            if (Votacion.is(t)) {
//              withResultado((T) Votacion.of(t));
//            }
          } else {
            c.append(t);
          }
          text.delete(0, text.length() + 1);
        } else {
          var word = text.substring(0, idx);
          if (GrupoParlamentarioUtil.isSimilar(word)) {
            withGrupoParlamentario(GrupoParlamentarioUtil.findSimilar(word));
          } else if (Votacion.is(word)) {
            withResultado((T) Votacion.of(word));
//          } else if (Votacion.is(word)) {
//            withResultado((T) Votacion.of(word));
          } else {
            c.append(word).append(" ");
          }
          text.delete(0, word.length() + 1);
        }
      }
      if (!c.isEmpty()) {
        withCongresista(Congresistas.findSimilar(c.toString().trim()));
      }
    }
  }

  public static void main(String[] args) {
    var b = new Builder<Asistencia>();
    b.processAsistenciaLine("CD-JPP ACUÑA PERALTA, SEGUNDO HÉCTOR");
    b.processAsistenciaLine("PRE");
    System.out.println(b.isReady());
  }
}
