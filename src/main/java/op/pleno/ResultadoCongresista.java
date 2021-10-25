package op.pleno;

public record ResultadoCongresista(
        int numero,
        String grupoParlamentario,
        String grupoParlamentarioDescripcion,
        String congresista,
        String resultado,
        String resultadoDescripcion
) {
}
