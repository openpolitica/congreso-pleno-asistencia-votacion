package op.congreso.pleno.textract;

import static java.util.Locale.UK;
import static op.congreso.pleno.Constantes.FECHA_HORA_PATTERN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import op.congreso.pleno.Constantes;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;
import op.congreso.pleno.util.GrupoParlamentarioUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextractAsistenciaV2 {

  public static Logger LOG = LoggerFactory.getLogger(TextractAsistenciaV2.class);

  //  public static final Pattern ASISTENCIA_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try {
      var lines = Files.readAllLines(
        Path.of("./out/pdf/2021-2026/2021-2022/Segunda Legislatura Ordinaria/Asis_vot_OFICIAL_14-07-22/page_26.txt")
      );

      var registro = load(clean(lines));

      System.out.println(" registry:\n" + registro);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RegistroAsistencia load(List<String> lines) throws IOException {
    lines =
      lines
        .stream()
        .map(s ->
          s
            .replace("GLADYS M.", "GLADYS MARGOT")
            // Wrong GP
            .replace("AP PIS", "AP-PIS")
            .replace("AP -PIS", "AP-PIS")
            .replace("P-PIS", "AP-PIS")
            .replace("CD- JPP", "CD-JPP")
            .replace("D-JPP", "CD-JPP")
            .replace("CD -JPP", "CD-JPP")
            .replace("CD JPP", "CD-JPP")
            .replace("CD-JPF", "CD-JPP")
            .replace("CD-JPI", "CD-JPP")
            .replace("ID-JPP", "CD-JPP")
            .trim()
        )
        .map(s -> {
          if (s.endsWith(" EP")) return s.replace("EP", "FP");
          if (s.equals("EP")) return "FP";
          return s;
        })
        .map(s -> {
          if (s.contains("CD-J") && !s.contains("CD-JPP")) return s.replace("CD-J", "CD-JPP"); else return s;
        })
        .map(s -> {
          if (s.contains("-JPP") && !s.contains("CD-JPP")) return s.replace("-JPP", "CD-JPP"); else return s;
        })
        .map(s -> s.replace("L0", "LO"))
        .toList();

    var registroBuilder = RegistroAsistencia.newBuilder();
    var plenoBuilder = Pleno.newBuilder();
    var resultadosBuilder = ResultadoAsistencia.newBuilder();
    var asistencias = new ArrayList<ResultadoCongresista<Asistencia>>();
    var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoAsistencia>();

    int i = 0;
    int errors = 0;
    var titulo = "";
    var type = "";
    LocalDateTime fechaHora = null;
    boolean headersReady = false;
    var previous = "";

    var current = ResultadoCongresista.<Asistencia>newBuilder();

    while (i < lines.size()) {
      final var text = lines.get(i);
      try {
        if (i < 4) { // Process headers metadata
          switch (i) {
            case 0 -> titulo = text;
            case 1 -> plenoBuilder.withLegislatura(text);
            case 2 -> plenoBuilder.withTitulo(text);
            case 3 -> {
              if (text.equals(Constantes.ASISTENCIA)) {
                type = text;
                i++;
                fechaHora = LocalDateTime.parse(lines.get(i).toLowerCase(), FECHA_HORA_PATTERN);
                registroBuilder.withFechaHora(fechaHora);
                plenoBuilder.withFecha(fechaHora.toLocalDate());
              } else if (text.startsWith(Constantes.ASISTENCIA)) {
                type = Constantes.ASISTENCIA;
                var fechaText = text.substring(Constantes.ASISTENCIA.length() + 1);
                fechaHora = LocalDateTime.parse(fechaText.toLowerCase(UK), FECHA_HORA_PATTERN);
                registroBuilder.withFechaHora(fechaHora);
                plenoBuilder.withFecha(fechaHora.toLocalDate());
              }
            }
          }
        } else if (asistencias.size() < 130) { // Process asistencia per congresistas
          if (text.equals("Resultados de la ASISTENCIA")) {
            LOG.warn("Faltan congresistas");
          } else {
            var b = new StringBuilder(text);
            current.processAsistenciaLine(b);
            if (current.isReady()) {
              asistencias.add(current.build());
              current = ResultadoCongresista.newBuilder();
              if (!b.isEmpty()) current.processAsistenciaLine(b);
            }
          }
        } else { // Process resultados
          if (text.equals("Asistencia para Quórum") || text.equals("Asistencia para Quorum")) { // Finally get quorum
            i++;
            var s = lines.get(i);
            if (s.contains(" ")) s = s.substring(0, s.indexOf(" "));
            registroBuilder.withQuorum(Integer.parseInt(s));
          }
        }
      } catch (Exception e) {
        LOG.error("Error at line {} text: {}", i, text, e);
        throw new RuntimeException(e);
      }
      i++;
    }

    //    if (fechaHora == null) errors++;

    var allGrupos = GrupoParlamentarioUtil.all();

    var gp = asistencias.stream().map(ResultadoCongresista::grupoParlamentario).distinct().toList();
    var grupos = gp.stream().collect(Collectors.toMap(a -> a, allGrupos::get));

    return registroBuilder
      .withPleno(plenoBuilder.withGruposParlamentarios(grupos).build())
      .withAsistencias(grupos, asistencias)
      .withResultadosPorPartido(resultadosGrupos)
      .withResultados(resultadosBuilder.build())
      .build();
  }

  static List<String> clean(List<String> list) {
    return list
      .stream()
      .map(s ->
        s
          .replace("GLADYS M.", "GLADYS MARGOT")
          // Wrong GP
          .replace("AP PIS", "AP-PIS")
          .trim()
      )
      .flatMap(s -> {
        var ss = s.split(" ");
        if (ss.length > 1 && Arrays.stream(ss).anyMatch(Asistencia::is)) {
          var t = "";
          for (int i = 0; i < ss.length; i++) {
            if (Asistencia.is(ss[i])) {
              var as = ss[i];
              var after = "";
              while (i < ss.length) {
                after = after + " " + ss[i];
                i++;
              }
              return Stream.of(t, as, after);
            } else {
              t = t + " " + ss[i];
            }
          }
          return Stream.of(ss[0], s.substring(s.indexOf(" ") + 1));
        } else return Stream.of(s);
      })
      .flatMap(s -> {
        var ss = s.split(" ");
        if (ss.length > 1 && GrupoParlamentarioUtil.isSimilar(ss[0])) {
          return Stream.of(ss[0], s.substring(s.indexOf(" ") + 1));
        } else return Stream.of(s);
      })
      .flatMap(s -> {
        var ss = s.split(" ");
        if (ss.length > 1 && Asistencia.is(ss[0])) {
          return Stream.of(ss[0], s.substring(s.indexOf(" ") + 1));
        } else return Stream.of(s);
      })
      .flatMap(s -> {
        var ss = s.split(" ");
        if (ss.length > 1 && isInteger(ss[0])) {
          return Stream.of(ss[0], s.substring(s.indexOf(" ") + 1));
        } else return Stream.of(s);
      })
      .filter(s -> !s.isBlank())
      .toList();
  }

  private static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
