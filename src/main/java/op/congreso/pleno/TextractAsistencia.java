package op.congreso.pleno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import op.congreso.pleno.asistencia.ResultadoAsistencia;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

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
    "PB",
    "PD",
    "PP",
    "NA"
  );
  public static final String FECHA_PATTERN =
    "'Fecha: 'd/MM/yyyy' Hora: 'kk:mm a";

  public static final Pattern ASISTENCIA_GROUP = Pattern.compile("(\\w+)");

  public static void main(String[] args) throws IOException {
    try (
      TextractClient textractClient = TextractClient
        .builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()
    ) {
      final var path = Path.of("./out/pdf-1.png");

      byte[] bytes = Files.readAllBytes(path);
      final var document = Document
        .builder()
        .bytes(SdkBytes.fromByteArray(bytes))
        .build();
      final var request = DetectDocumentTextRequest
        .builder()
        .document(document)
        .build();
      final var response = textractClient.detectDocumentText(request);

      var plenoBuilder = Pleno.newBuilder();
      var resultadosBuilder = ResultadoAsistencia.newBuilder();
      var resultados = new ArrayList<ResultadoCongresista<Asistencia>>();
      var grupos = new HashMap<String, String>();
      var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoAsistencia>();

      final var blocks = response.blocks();

      int i = 0;
      int errors = 0;
      String type = null;
      LocalDateTime fechaHora = null;
      boolean headersReady = false;

      while (i < blocks.size()) {
        final var block = blocks.get(i);
        if (block.blockType().equals(BlockType.LINE)) {
          final var text = block.text();
          if (i < 6) {
            switch (i) {
              case 1 -> {
                if (!text.equals("CONGRESO DE LA REPÚBLICA DEL PERÚ")) errors++;
              }
              case 2 -> plenoBuilder.withLegislatura(text);
              case 3 -> plenoBuilder.withTitulo(text);
              case 4 -> type = text;
              case 5 -> {
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
              var congresista = blocks.get(i).text();
              if (
                Asistencia.is(
                  congresista.substring(congresista.lastIndexOf(" ") + 1)
                )
              ) {
                var asistencia = congresista.substring(
                  congresista.lastIndexOf("")
                );
                resultados.add(
                  new ResultadoCongresista<>(
                    text.trim(),
                    "",
                    congresista.substring(0, congresista.lastIndexOf(" ")),
                    Asistencia.of(asistencia)
                  )
                );
              } else {
                i++;
                var asistencia = blocks.get(i).text();
                resultados.add(
                  new ResultadoCongresista<>(
                    text.trim(),
                    "",
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
              if (blocks.get(i).text().equals("Grupo Parlamentario")) {
                i++;
                var headers = blocks.get(i).text();
                if (headers.equals("Presente Ausente Licencias Susp. Otros")) {
                  headersReady = true;
                } else if (headers.equals("Presente Ausente Licencias Susp.")) {
                  i++;
                  if (blocks.get(i).text().equals("Otros")) headersReady = true;
                }
              }
            } else {
              if (headersReady) {
                //TODO Process results
                if (Asistencia.isDescripcion(text)) {
                  i++;
                  var asistencia = blocks.get(i).text();
                  var matcher = ASISTENCIA_GROUP.matcher(asistencia);
                  if (matcher.find()) {
                    var asis = matcher.group();
                    i++;
                    var result = blocks.get(i).text();
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
                      var result = blocks.get(i).text();
                      resultadosBuilder.with(
                        Asistencia.of(asis),
                        Integer.parseInt(result)
                      );
                    }
                  }
                } else {
                  if (VALID_GP.contains(text)) {
                    i++;
                    grupos.put(text, blocks.get(i).text());
                    i++;
                    var presentes = Integer.parseInt(blocks.get(i).text());
                    i++;
                    var ausentes = Integer.parseInt(blocks.get(i).text());
                    i++;
                    var licencias = Integer.parseInt(blocks.get(i).text());
                    i++;
                    var suspendidos = Integer.parseInt(blocks.get(i).text());
                    i++;
                    var otros = Integer.parseInt(blocks.get(i).text());
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
                    if (
                      VALID_GP.contains(text.substring(0, text.indexOf(" ")))
                    ) {
                      String grupo = text.substring(0, text.indexOf(" "));
                      grupos.put(grupo, text.substring(text.indexOf(" ") + 1));
                      i++;
                      var presentes = Integer.parseInt(blocks.get(i).text());
                      i++;
                      var ausentes = Integer.parseInt(blocks.get(i).text());
                      i++;
                      var licencias = Integer.parseInt(blocks.get(i).text());
                      i++;
                      var suspendidos = Integer.parseInt(blocks.get(i).text());
                      i++;
                      var otros = Integer.parseInt(blocks.get(i).text());
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
                      plenoBuilder.withQuorum(
                        Integer.parseInt(blocks.get(i).text())
                      );
                    }
                  }
                }
              }
            }
          }
        }
        i++;
      }

      if (fechaHora == null) errors++;

      var registro = new RegistroAsistencia(
        plenoBuilder.build(),
        fechaHora != null ? fechaHora.toLocalTime() : null,
        resultados,
        resultadosGrupos,
        resultadosBuilder.build()
      );

      System.out.println(
        type +
        "=>" +
        errors +
        " headers = " +
        headersReady +
        " registry:\n" +
        registro
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
