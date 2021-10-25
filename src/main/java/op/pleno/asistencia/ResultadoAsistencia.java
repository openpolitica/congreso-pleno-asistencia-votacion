package op.pleno.asistencia;

public record ResultadoAsistencia(
        int presentes,
        int ausentes,
        int licencias,
        int otros,
        int total
) {
}
