package op.congreso.pleno.textract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;

public class TextractVotacion {

  public static final List<String> VALID_GP = List.of(
    "FP",
    "PL",
    "AP",
    "APP",
    "BM",
    "AP-PIS",
    "RP",
    "PD",
    "SP",
    "CD-JP",
    "CD-JPP",
    "PB",
    "PD",
    "PP",
    "NA"
  );
  public static final String FECHA_PATTERN =
    "'Fecha: 'd/MM/yyyy' Hora: 'hh:mm a";

  public static final Pattern VOTACION_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try {
      var lines = Files.readAllLines(Path.of("output-votacion.txt"));

      var registro = load(lines);

      System.out.println(
//        type +
//        "=>" +
//        errors +
//        " headers = " +
//        headersReady +
        " registry:\n" +
        registro
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RegistroVotacion load(List<String> lines) {
    var registroBuilder = RegistroVotacion.newBuilder();
    var plenoBuilder = Pleno.newBuilder();
    var resultadosBuilder = ResultadoVotacion.newBuilder();
    var resultados = new ArrayList<ResultadoCongresista<Votacion>>();
    var grupos = new HashMap<String, String>();
    var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoVotacion>();

    int i = 0;
    String type = null;
    LocalDateTime fechaHora = null;
    boolean headersReady = false;
    var previous = "";
    int errors = 0;

    while (i < lines.size()) {
      //                if (block.blockType().equals(BlockType.LINE)) {
      if (i < 6) {
        switch (i) {
          case 0 -> {
            if (
                    !lines.get(i).equals("CONGRESO DE LA REPÚBLICA DEL PERÚ")
            ) errors++;
          }
          case 1 -> plenoBuilder.withLegislatura(lines.get(i));
          case 2 -> registroBuilder.withPresidente(
                  lines.get(i).substring("Presidente: ".length())
          );
          case 3 -> plenoBuilder.withTitulo(lines.get(i));
          case 4 -> type = lines.get(i);
          case 5 -> {
            fechaHora =
                    LocalDateTime.parse(
                            lines.get(i),
                            DateTimeFormatter.ofPattern(FECHA_PATTERN)
                    );
            plenoBuilder.withFecha(fechaHora.toLocalDate());
          }
        }
      } else if (lines.get(i).equals("Asunto:")) {
        var asunto = "";
        i++;
        asunto += lines.get(i);
        i++;
        var next = lines.get(i);
        if (VALID_GP.contains(next.split("\\+s")[0])) { // segunda linea
          i--;
        } else {
          asunto += " " + next;

          i++;
          next = lines.get(i);
          if (VALID_GP.contains(next.split("\\+s")[0])) { // tercera linea
            i--;
          } else {
            asunto += " " + next;

            i++;
            next = lines.get(i);
            if (VALID_GP.contains(next.split("\\+s")[0])) { // cuarta linea
              i--;
            } else {
              asunto += " " + next;

              i++;
              next = lines.get(i);
              if (VALID_GP.contains(next.split("\\+s")[0])) { // quinta linea
                i--;
              } else {
                asunto += " " + next;

                i++;
                next = lines.get(i);
                if (VALID_GP.contains(next.split("\\+s")[0])) { // sexta
                  i--;
                } else {
                  asunto += " " + next;
                }
              }
            }
          }
        }

        registroBuilder.withAsunto(asunto);
      } else if (resultados.size() < 130) {
        if (VALID_GP.contains(previous)) {
          var congresista = lines.get(i);
          String potVot = congresista.substring(
                  congresista.lastIndexOf(" ") + 1
          );
          if (Votacion.is(potVot)) {
            congresista =
                    congresista.substring(0, congresista.lastIndexOf(" "));
            resultados.add(
                    new ResultadoCongresista<>(
                            previous,
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Votacion.of(potVot)
                    )
            );
            previous = "";
          } else {
            i++;
            var vot = lines.get(i);
            var votw = vot.split("\\s+");
            if (votw.length > 1) {
              var prev = "";
              if (votw.length == 2) {
                if (
                        !votw[1].equals("+++") &&
                                !votw[1].equals("-") &&
                                !votw[1].equals("--") &&
                                !votw[1].equals("---")
                ) {
                  prev = votw[1];
                }
              } else {
                //                                        if (!votw[2].equals("+++") && !votw[2].equals("-") && !votw[2].equals("--") && !votw[2].equals("---")) {
                prev = votw[2];
                //                                        }
              }
              resultados.add(
                      new ResultadoCongresista<>(
                              previous,
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
              previous = prev;
            } else {
              resultados.add(
                      new ResultadoCongresista<>(
                              previous,
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
              previous = "";
            }
          }
        } else if (VALID_GP.contains(lines.get(i).trim())) { // Add GP and process next line
          i++;
          var congresista = lines.get(i);
          var potVot = congresista.substring(
                  congresista.lastIndexOf(" ") + 1
          );
          if (Votacion.is(potVot)) {
            congresista =
                    congresista.substring(0, congresista.lastIndexOf(" "));
            resultados.add(
                    new ResultadoCongresista<>(
                            lines.get(i),
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Votacion.of(potVot)
                    )
            );
          } else {
            i++;
            var vot = lines.get(i);
            var votw = vot.split("\\s+");
            if (votw.length > 1) {
              if (votw.length == 2) {
                if (
                        !votw[1].equals("+++") &&
                                !votw[1].equals("-") &&
                                !votw[1].equals("--") &&
                                !votw[1].equals("---")
                ) {
                  previous = votw[1];
                }
              } else {
                //                                        if (!votw[1].equals("+++") && !votw[1].equals("-") && !votw[1].equals("--") && !votw[1].equals("---")) {
                previous = votw[2];
                //                                        }
              }
              resultados.add(
                      new ResultadoCongresista<>(
                              lines.get(i),
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
            } else {
              resultados.add(
                      new ResultadoCongresista<>(
                              lines.get(i),
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
            }
          }
        } else if (!lines.get(i).equals("+++")) {
          var gp = lines.get(i).contains(" ")
                  ? lines.get(i).substring(0, lines.get(i).indexOf(" "))
                  : lines.get(i);
          var congresista = lines
                  .get(i)
                  .substring(lines.get(i).indexOf(" ") + 1);
          String potVot = congresista.substring(
                  congresista.lastIndexOf(" ") + 1
          );
          if (Votacion.is(potVot)) {
            congresista =
                    congresista.substring(0, congresista.lastIndexOf(" "));
            resultados.add(
                    new ResultadoCongresista<>(
                            gp,
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Votacion.of(potVot)
                    )
            );
          } else {
            i++;
            var vot = lines.get(i);
            var votw = vot.split("\\s+");
            if (votw.length > 1) {
              if (votw.length == 2) {
                if (
                        !votw[1].equals("+++") &&
                                !votw[1].equals("-") &&
                                !votw[1].equals("--") &&
                                !votw[1].equals("---")
                ) {
                  previous = votw[1];
                }
              } else {
                //                                        if (!votw[2].equals("+++") && !votw[2].equals("-") && !votw[2].equals("--") && !votw[2].equals("---")) {
                previous = votw[2];
                //                                        }
              }
              resultados.add(
                      new ResultadoCongresista<>(
                              lines.get(i),
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
            } else {
              resultados.add(
                      new ResultadoCongresista<>(
                              gp,
                              congresista,
                              Votacion.of(votw[0])
                      )
              );
            }
          }
        }
      } else {
        // Resultados de la ASISTENCIA
        // Grupo Parlamentario
        // Presente Ausente Licencias Susp.
        // Otros
        if (lines.get(i).equals("Resultados de VOTACIÓN")) {
          i++;
          if (lines.get(i).equals("Grupo Parlamentario")) {
            i++;
            var headers = lines.get(i);
            if (headers.equals("Si+++")) {
              i++;
              if (lines.get(i).equals("No")) {
                i++;
                if (lines.get(i).equals("Abst.")) {
                  i++;
                  if (lines.get(i).equals("Sin Resp.")) {
                    headersReady = true;
                  }
                }
              }
            } else if (headers.equals("Si+++ No Abst.")) {
              i++;
              if (lines.get(i).equals("Sin Resp.")) headersReady = true;
            }
          }
        } else {
          if (headersReady) {
            //TODO Process results
            if (Votacion.is(lines.get(i))) {
              i++;
              var asistencia = lines.get(i);
              resultadosBuilder.with(
                      Votacion.of(lines.get(i)),
                      Integer.parseInt(asistencia)
              );
            } else if (
                    lines.get(i).contains("(") && lines.get(i).contains(")")
            ) {
              //                                if (
              //                                        Votacion.is(
              //                                                text.substring(0, text.lastIndexOf("(") - 1)
              //                                        )
              //                                ) {
              var matcher = VOTACION_GROUP.matcher(
                      lines.get(i).substring(lines.get(i).lastIndexOf("("))
              );
              if (matcher.find()) {
                var asis = matcher.group();
                i++;
                var result = lines.get(i);
                resultadosBuilder.with(
                        Votacion.of(asis),
                        Integer.parseInt(result)
                );
              }
              //                                }
            } else {
              if (VALID_GP.contains(lines.get(i))) {
                i++;
                grupos.put(lines.get(i), lines.get(i));
                i++;
                String s = lines.get(i);
                try {
                  Integer.parseInt(s);
                } catch (NumberFormatException e) {
                  i++;
                }
                var si = Integer.parseInt(lines.get(i));
                i++;
                var no = Integer.parseInt(lines.get(i));
                i++;
                var abst = Integer.parseInt(lines.get(i));
                i++;
                var sinResp = Integer.parseInt(lines.get(i));
                resultadosGrupos.put(
                        new GrupoParlamentario(
                                lines.get(i),
                                grupos.get(lines.get(i))
                        ),
                        ResultadoVotacion.create(si, no, abst, sinResp)
                );
              } else if (
                      !lines.get(i).isBlank() && lines.get(i).contains(" ")
              ) {
                if (
                        VALID_GP.contains(
                                lines.get(i).substring(0, lines.get(i).indexOf(" "))
                        )
                ) {
                  String grupo = lines
                          .get(i)
                          .substring(0, lines.get(i).indexOf(" "));
                  grupos.put(
                          grupo,
                          lines.get(i).substring(lines.get(i).indexOf(" ") + 1)
                  );
                  i++;
                  var si = Integer.parseInt(lines.get(i));
                  i++;
                  var no = Integer.parseInt(lines.get(i));
                  i++;
                  var abst = Integer.parseInt(lines.get(i));
                  i++;
                  var sinResp = Integer.parseInt(lines.get(i));
                  resultadosGrupos.put(
                          new GrupoParlamentario(grupo, grupos.get(grupo)),
                          ResultadoVotacion.create(si, no, abst, sinResp)
                  );
                } else if (lines.get(i).equals("Asistencia para Quórum")) {
                  i++;
                  plenoBuilder.withQuorum(Integer.parseInt(lines.get(i)));
                }
              }
            }
          }
        }
      }
      //}
      i++;
    }

    if (fechaHora == null) errors++; else registroBuilder.withHora(
            fechaHora.toLocalTime()
    );

    return registroBuilder
            .withPleno(plenoBuilder.withGruposParlamentarios(grupos).build())
            .withVotaciones(resultados)
            .withResultadosPorPartido(resultadosGrupos)
            .withResultados(resultadosBuilder.build())
            .build();
  }
}
