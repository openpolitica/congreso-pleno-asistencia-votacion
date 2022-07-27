package op.congreso.pleno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.votacion.RegistroVotacion;

public record RegistroPleno(
        String periodoParlamentario,
        String periodoAnual,
        String legislatura,
        LocalDate fecha,
        String titulo,

        List<RegistroAsistencia> asistencias,
        List<RegistroVotacion> votaciones,
        int potentialErrors,
        List<String> comments
) {
    public static Builder newBuilder(RegistroPlenoDocument document) {
        return new Builder(document);
    }

    public static class Builder {
        final String periodoParlamentario;
        final String periodoAnual;
        final String legislatura;
        final LocalDate fecha;
        final String titulo;
        List<RegistroAsistencia> asistencias = new ArrayList<>();
        List<RegistroVotacion> votaciones = new ArrayList<>();
        int potentialErrors = 0;
        List<String> comments = new ArrayList<>();

        public Builder(RegistroPlenoDocument document) {
            this.periodoParlamentario = document.periodoParlamentario();
            this.periodoAnual = document.periodoAnual();
            this.legislatura = document.legislatura();
            this.titulo = document.titulo();
            this.fecha = LocalDate.parse(document.fecha(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        public Builder(String periodoParlamentario, String periodoAnual, String legislatura, LocalDate fecha, String titulo) {
            this.periodoParlamentario = periodoParlamentario;
            this.periodoAnual = periodoAnual;
            this.legislatura = legislatura;
            this.fecha = fecha;
            this.titulo = titulo;
        }

        public void addAsistencia(RegistroAsistencia asistencia) {
            asistencias.add(asistencia);
        }

        public void addVotacion(RegistroVotacion votacion) {
            votaciones.add(votacion);
        }

        public RegistroPleno build() {
            return new RegistroPleno(periodoParlamentario, periodoAnual, legislatura, fecha, titulo, asistencias, votaciones, potentialErrors, comments);
        }
    }
}
