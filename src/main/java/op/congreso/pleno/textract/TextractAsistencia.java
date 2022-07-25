package op.congreso.pleno.textract;

import static op.congreso.pleno.Constantes.FECHA_HORA_PATTERN;
import static op.congreso.pleno.Constantes.VALID_GP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;

public class TextractAsistencia {

  public static final Pattern ASISTENCIA_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try {

      final var blocks = Files.readAllLines(Path.of("./output-asistencia.txt"));

      var registro = load(blocks);

      System.out.println(
        " registry:\n" +
        registro
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RegistroAsistencia load(List<String> lines) {
    var registroBuilder = RegistroAsistencia.newBuilder();
    var plenoBuilder = Pleno.newBuilder();
    var resultadosBuilder = ResultadoAsistencia.newBuilder();
    var asistencias = new ArrayList<ResultadoCongresista<Asistencia>>();
    var grupos = new HashMap<String, String>();
    var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoAsistencia>();

    int i = 0;
    int errors = 0;
    var titulo = "";
    var type = "";
    LocalDateTime fechaHora = null;
    boolean headersReady = false;

    while (i < lines.size()) {
      final var text = lines.get(i);
      if (i < 5) { // Process headers metadata
        switch (i) {
          case 0 -> titulo = text;
          case 1 -> plenoBuilder.withLegislatura(text);
          case 2 -> plenoBuilder.withTitulo(text);
          case 3 -> type = text;
          case 4 -> {
            fechaHora =
                    LocalDateTime.parse(
                            text,
                            FECHA_HORA_PATTERN
                    );
            registroBuilder.withHora(fechaHora.toLocalTime());
            plenoBuilder.withFecha(fechaHora.toLocalDate());
          }
        }
      } else if (asistencias.size() < 130) { // Process asistencia per congresistas
        // Process GP + Congresista + Asistencia
        if (VALID_GP.contains(text.trim())) { // GP found, process next line
          i++;
          var congresista = lines.get(i);
          // Check Congresista text does not contain Asistencia
          if (
                  Asistencia.is(
                          congresista.substring(congresista.lastIndexOf(" ") + 1)
                  )
          ) { // if it contains Asistencia
            var asistencia = congresista.substring(
                    congresista.lastIndexOf(" ")
            ); // get asistencia
            // and build resultado
            asistencias.add(
                    new ResultadoCongresista<>(
                            text.trim(),
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Asistencia.of(asistencia)
                    )
            );
          } else { // if it does not contain asistencia
            i++;
            var asistencia = lines.get(i); // get asistencia from next line
            // and build resultado
            asistencias.add(
                    new ResultadoCongresista<>(
                            text.trim(),
                            congresista,
                            Asistencia.of(asistencia)
                    )
            );
          }
        } else { // else GP is in the same line as congresista
          var gp = text.substring(0, text.indexOf(" ")); // get GP from first work
          // Check Congresista text does not contain Asistencia
          var congresista = text.substring(text.indexOf(" ") + 1);
          if (
                  Asistencia.is(
                          congresista.substring(congresista.lastIndexOf(" ") + 1)
                  )
          ) { // if it contains asistencia
            var asistencia = congresista.substring(
                    congresista.lastIndexOf(" ")
            ); // get asistencia
            // and build resultado
            asistencias.add(
                    new ResultadoCongresista<>(
                            gp,
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Asistencia.of(asistencia)
                    )
            );
          } else { // if it does not contain asistencia
            i++;
            var asistencia = lines.get(i); // get asistencia from next line
            // and build resultado
            asistencias.add(
                    new ResultadoCongresista<>(
                            gp,
                            congresista,
                            Asistencia.of(asistencia)
                    )
            );
          }
        }
      } else { // Process resultados
        // First: get resultado headers:
        // Resultados de la ASISTENCIA
        // Grupo Parlamentario
        // Presente Ausente Licencias Susp.
        // Otros
        if (text.equals("Resultados de la ASISTENCIA")) {
          i++;
          if (lines.get(i).equals("Grupo Parlamentario")) {
            i++;
            var headers = lines.get(i);
            if (headers.equals("Presente Ausente Licencias Susp. Otros")) {
              headersReady = true;
            } else if (headers.equals("Presente Ausente Licencias Susp.")) {
              i++;
              if (lines.get(i).equals("Otros")) headersReady = true;
            }
          }
        } else { // Then once headers ready:
          if (headersReady) {
            if (Asistencia.isDescripcion(text)) { // Get Resultado per Asistencia type
              i++;
              var asistencia = lines.get(i);
              var matcher = ASISTENCIA_GROUP.matcher(asistencia);
              if (matcher.find()) {
                var asis = matcher.group();
                i++;
                var result = lines.get(i);
                resultadosBuilder.with(
                        Asistencia.of(asis),
                        Integer.parseInt(result)
                );
              }
            } else if (text.contains("(") && text.contains(")")) { // Or get Asistencia from within ()
              if (
                      Asistencia.isDescripcion(
                              text.substring(0, text.lastIndexOf("(") - 1)
                      )
              ) {
                var matcher = ASISTENCIA_GROUP.matcher(
                        text.substring(text.lastIndexOf("("))
                );
                if (matcher.find()) {
                  var asis = matcher.group();
                  i++;
                  var result = lines.get(i);
                  resultadosBuilder.with(
                          Asistencia.of(asis),
                          Integer.parseInt(result)
                  );
                }
              }
            } else { // Or get resulados per GP
              if (VALID_GP.contains(text)) {
                i++;
                grupos.put(text, lines.get(i));
                i++;
                var presentes = Integer.parseInt(lines.get(i));
                i++;
                var ausentes = Integer.parseInt(lines.get(i));
                i++;
                var licencias = Integer.parseInt(lines.get(i));
                i++;
                var suspendidos = Integer.parseInt(lines.get(i));
                i++;
                var otros = Integer.parseInt(lines.get(i));
                resultadosGrupos.put(
                        new GrupoParlamentario(text, grupos.get(text)),
                        ResultadoAsistencia.create(
                                presentes,
                                ausentes,
                                licencias,
                                suspendidos,
                                otros
                        )
                );
              } else if (!text.isBlank() && text.contains(" ")) {
                if (VALID_GP.contains(text.substring(0, text.indexOf(" ")))) {
                  String grupo = text.substring(0, text.indexOf(" "));
                  grupos.put(grupo, text.substring(text.indexOf(" ") + 1));
                  i++;
                  var presentes = Integer.parseInt(lines.get(i));
                  i++;
                  var ausentes = Integer.parseInt(lines.get(i));
                  i++;
                  var licencias = Integer.parseInt(lines.get(i));
                  i++;
                  var suspendidos = Integer.parseInt(lines.get(i));
                  i++;
                  var otros = Integer.parseInt(lines.get(i));
                  resultadosGrupos.put(
                          new GrupoParlamentario(grupo, grupos.get(grupo)),
                          ResultadoAsistencia.create(
                                  presentes,
                                  ausentes,
                                  licencias,
                                  suspendidos,
                                  otros
                          )
                  );
                } else if (text.equals("Asistencia para Quórum")) { // Finally get quorum
                  i++;
                  plenoBuilder.withQuorum(Integer.parseInt(lines.get(i)));
                }
              }
            }
          }
        }
      }
      i++;
    }

    if (fechaHora == null) errors++;

    return registroBuilder.withPleno(plenoBuilder.build())
            .withAsistencias(asistencias)
            .withResultadosPorPartido(resultadosGrupos)
            .withResultados(resultadosBuilder.build())
            .build();
  }
}
