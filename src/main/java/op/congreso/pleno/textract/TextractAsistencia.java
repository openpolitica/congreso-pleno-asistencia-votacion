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
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;

public class TextractAsistencia {

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

  public static final Pattern ASISTENCIA_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try {

      final var blocks = Files.readAllLines(Path.of("./output-asistencia.txt"));

      var registro = load(blocks);

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

  static RegistroAsistencia load(List<String> lines) {
    var plenoBuilder = Pleno.newBuilder();
    var resultadosBuilder = ResultadoAsistencia.newBuilder();
    var resultados = new ArrayList<ResultadoCongresista<Asistencia>>();
    var grupos = new HashMap<String, String>();
    var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoAsistencia>();

    int i = 0;
    int errors = 0;
    String type = null;
    LocalDateTime fechaHora = null;
    boolean headersReady = false;

    while (i < lines.size()) {
      //        if (block.blockType().equals(BlockType.LINE)) {
      final var text = lines.get(i);
      if (i < 5) {
        switch (i) {
          case 0 -> {
            if (!text.equals("CONGRESO DE LA REPÚBLICA DEL PERÚ")) errors++;
          }
          case 1 -> plenoBuilder.withLegislatura(text);
          case 2 -> plenoBuilder.withTitulo(text);
          case 3 -> type = text;
          case 4 -> {
            fechaHora =
                    LocalDateTime.parse(
                            text,
                            DateTimeFormatter.ofPattern(FECHA_PATTERN)
                    );
            plenoBuilder.withFecha(fechaHora.toLocalDate());
          }
        }
      } else if (resultados.size() < 130) {
        if (VALID_GP.contains(text.trim())) { // Add GP and process next line
          i++;
          var congresista = lines.get(i);
          if (
                  Asistencia.is(
                          congresista.substring(congresista.lastIndexOf(" ") + 1)
                  )
          ) {
            var asistencia = congresista.substring(
                    congresista.lastIndexOf(" ")
            );
            resultados.add(
                    new ResultadoCongresista<>(
                            text.trim(),
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Asistencia.of(asistencia)
                    )
            );
          } else {
            i++;
            var asistencia = lines.get(i);
            resultados.add(
                    new ResultadoCongresista<>(
                            text.trim(),
                            congresista,
                            Asistencia.of(asistencia)
                    )
            );
          }
        } else {
          var gp = text.substring(0, text.indexOf(" "));
          var congresista = text.substring(text.indexOf(" ") + 1);
          if (
                  Asistencia.is(
                          congresista.substring(congresista.lastIndexOf(" ") + 1)
                  )
          ) {
            var asistencia = congresista.substring(
                    congresista.lastIndexOf(" ")
            );
            resultados.add(
                    new ResultadoCongresista<>(
                            gp,
                            congresista.substring(0, congresista.lastIndexOf(" ")),
                            Asistencia.of(asistencia)
                    )
            );
          } else {
            i++;
            var asistencia = lines.get(i);
            resultados.add(
                    new ResultadoCongresista<>(
                            gp,
                            congresista,
                            Asistencia.of(asistencia)
                    )
            );
          }
        }
      } else {
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
        } else {
          if (headersReady) {
            if (Asistencia.isDescripcion(text)) {
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
            } else if (text.contains("(") && text.contains(")")) {
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
            } else {
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
                                  otros
                          )
                  );
                } else if (text.equals("Asistencia para Quórum")) {
                  i++;
                  plenoBuilder.withQuorum(Integer.parseInt(lines.get(i)));
                }
              }
            }
          }
        }
        //          }
      }
      i++;
    }

    if (fechaHora == null) errors++;

    return new RegistroAsistencia(
            plenoBuilder.build(),
            fechaHora != null ? fechaHora.toLocalTime() : null,
            resultados,
            resultadosGrupos,
            resultadosBuilder.build()
    );
  }
}
