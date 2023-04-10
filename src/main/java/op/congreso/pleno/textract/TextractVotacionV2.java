package op.congreso.pleno.textract;

import static op.congreso.pleno.Constantes.FECHA_HORA_PATTERN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.util.GrupoParlamentarioUtil;
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextractVotacionV2 {

  public static Logger LOG = LoggerFactory.getLogger(TextractVotacionV2.class);

  //  public static final Pattern VOTACION_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try {
      var lines = Files.readAllLines(
        Path.of("./out/pdf/2021-2026/2021-2022/Segunda Legislatura Ordinaria/Asis_vot_OFICIAL_14-07-22/page_26.txt")
      );

      var registro = load(65, lines);

      System.out.println(" registry:\n" + registro);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RegistroVotacion load(int quorum, List<String> lines) throws IOException {
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
            .replace("CD -JPP", "CD-JPP")
            .replace("CD JPP", "CD-JPP")
            .replace("CD-JPF", "CD-JPP")
            .replace("ID-JPP", "CD-JPP")
            .trim()
        )
        .map(s -> {
          if (s.endsWith(" EP")) return s.replace("EP", "FP");
          if (s.equals("EP")) return "FP";
          return s;
        })
        .map(s -> {
          if (s.contains("-JPP") && !s.contains("CD-JPP")) return s.replace("-JPP", "CD-JPP"); else return s;
        })
        .map(s -> s.replace(" +++", ""))
        .map(s -> s.replace("+++ ", ""))
        .map(s -> s.replace("****", "SINRES"))
        .map(s -> s.replace("***", "SINRES"))
        .map(s -> s.replace("L0", "LO"))
        .toList();

    var registroBuilder = RegistroVotacion.newBuilder().withQuorum(quorum);
    var plenoBuilder = Pleno.newBuilder();
    var resultadosBuilder = ResultadoVotacion.newBuilder();
    var resultados = new ArrayList<ResultadoCongresista<Votacion>>();
    var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoVotacion>();

    int i = 0;
    LocalDateTime fechaHora = null;

    var current = ResultadoCongresista.<Votacion>newBuilder();

    while (i < lines.size()) {
      final var text = lines.get(i);
      try {
        if (i < 6) { // Process headers metadata
          switch (i) {
            //case 0 -> titulo = lines.get(i);
            case 1 -> plenoBuilder.withLegislatura(lines.get(i));
            case 2 -> registroBuilder.withPresidente(lines.get(i).substring("Presidente: ".length()));
            case 3 -> plenoBuilder.withTitulo(lines.get(i));
            //case 4 -> type = lines.get(i);
            case 5 -> {
              fechaHora = LocalDateTime.parse(lines.get(i).toLowerCase(), FECHA_HORA_PATTERN);
              registroBuilder.withFechaHora(fechaHora);
              plenoBuilder.withFecha(fechaHora.toLocalDate());
            }
          }
        } else if (lines.get(i).equals("Asunto:")) { // Process Asunto (up to 6 lines)
          StringBuilder asunto = new StringBuilder();
          i++;
          asunto.append(lines.get(i));
          while (!GrupoParlamentarioUtil.is(lines.get(i + 1))) {
            i++;
            var next = lines.get(i);
            asunto.append(" ").append(next);
          }
          registroBuilder.withAsunto(asunto.toString());
        } else if (resultados.size() < 130 && !text.startsWith("Resultados")) { // Process votacion per congresistas
          var b = new StringBuilder(text);
          current.processVotacionLine(b);
          if (current.isReady()) {
            var build = current.build();
            resultados
              .stream()
              .filter(r -> r.congresista().equals(build.congresista()))
              .findAny()
              .ifPresent(votacionResultadoCongresista -> {
                throw new IllegalStateException("Repeated congresista: " + build);
              });
            resultados.add(build);
            current = ResultadoCongresista.newBuilder();
            if (!b.isEmpty()) current.processVotacionLine(b);
          }
        } else { // Process resultados
          break;
        }
      } catch (Exception e) {
        LOG.error("Error at line {} line {}", i, lines.get(i), e);
        throw new RuntimeException(e);
      }
      i++;
    }

    var allGrupos = GrupoParlamentarioUtil.all();
    var grupos = resultados
      .stream()
      .map(ResultadoCongresista::grupoParlamentario)
      .distinct()
      .collect(Collectors.toMap(a -> a, allGrupos::get));
    return registroBuilder
      .withPleno(plenoBuilder.withGruposParlamentarios(grupos).build())
      .withVotaciones(grupos, resultados)
      .withResultadosPorPartido(resultadosGrupos)
      .withResultados(resultadosBuilder.build())
      .build();
  }
}
