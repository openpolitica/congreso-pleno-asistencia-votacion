package op.congreso.pleno;

public record ResultadoCongresista<T>(String grupoParlamentario, String congresista, T resultado) {
    public ResultadoCongresista<T> replaceCongresista(String s) {
        return new ResultadoCongresista<T>(grupoParlamentario, s, resultado);
    }
}
