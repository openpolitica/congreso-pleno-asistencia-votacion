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
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.Document;

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
        try //(
//                TextractClient textractClient = TextractClient
//                        .builder()
//                        .region(Region.US_EAST_1)
//                        .credentialsProvider(ProfileCredentialsProvider.create())
//                        .build()
        // )
        {
//            final var path = Path.of("./out/pdf-9.png");
//
//            byte[] bytes = Files.readAllBytes(path);
//            final var document = Document
//                    .builder()
//                    .bytes(SdkBytes.fromByteArray(bytes))
//                    .build();
//            final var request = DetectDocumentTextRequest
//                    .builder()
//                    .document(document)
//                    .build();
//            final var response = textractClient.detectDocumentText(request);
//
            var registroBuilder = RegistroVotacion.newBuilder();
            var plenoBuilder = Pleno.newBuilder();
            var resultadosBuilder = ResultadoVotacion.newBuilder();
            var resultados = new ArrayList<ResultadoCongresista<Votacion>>();
            var grupos = new HashMap<String, String>();
            var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoVotacion>();
//
//            final var blocks = response.blocks();
            var blocks = Files.readAllLines(Path.of("output-votacion.txt"));

            int i = 0;
            int errors = 0;
            String type = null;
            LocalDateTime fechaHora = null;
            boolean headersReady = false;
            var previous = "";

            while (i < blocks.size()) {
                final var block = blocks.get(i);
//                if (block.blockType().equals(BlockType.LINE)) {
                final var text = block;
                if (i < 6) {
                    switch (i) {
                        case 0 -> {
                            if (!text.equals("CONGRESO DE LA REPÚBLICA DEL PERÚ")) errors++;
                        }
                        case 1 -> plenoBuilder.withLegislatura(text);
                        case 2 -> registroBuilder.withPresidente(text.substring("Presidente: ".length()));
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
                } else if (text.equals("Asunto:")) {
                    var asunto = "";
                    i++;
                    asunto += blocks.get(i);
                    i++;
                    var next = blocks.get(i);
                    if (VALID_GP.contains(next.split("\\+s")[0])) { // segunda linea
                        i--;
                    } else {
                        asunto += " " + next;

                        i++;
                        next = blocks.get(i);
                        if (VALID_GP.contains(next.split("\\+s")[0])) { // tercera linea
                            i--;
                        } else {
                            asunto += " " + next;

                            i++;
                            next = blocks.get(i);
                            if (VALID_GP.contains(next.split("\\+s")[0])) { // cuarta linea
                                i--;
                            } else {
                                asunto += " " + next;

                                i++;
                                next = blocks.get(i);
                                if (VALID_GP.contains(next.split("\\+s")[0])) { // quinta linea
                                    i--;
                                } else {
                                    asunto += " " + next;

                                    i++;
                                    next = blocks.get(i);
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
                        var congresista = text;
                        String potVot = congresista.substring(congresista.lastIndexOf(" ") + 1);
                        if (Votacion.is(potVot)) {
                            congresista = congresista.substring(0, congresista.lastIndexOf(" "));
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            previous,
                                            "",
                                            congresista.substring(0, congresista.lastIndexOf(" ")),
                                            Votacion.of(potVot)
                                    )
                            );
                            previous = "";
                        } else {
                            i++;
                            var vot = blocks.get(i);
                            var votw = vot.split("\\s+");
                            if (votw.length > 1) {
                                var prev = "";
                                if (votw.length == 2) {
                                    if (!votw[1].equals("+++") && !votw[1].equals("-") && !votw[1].equals("--") && !votw[1].equals("---")) {
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
                                                "",
                                                congresista,
                                                Votacion.of(votw[0])
                                        )
                                );
                                previous = prev;
                            } else {
                                resultados.add(
                                        new ResultadoCongresista<>(
                                                previous,
                                                "",
                                                congresista,
                                                Votacion.of(votw[0])
                                        )
                                );
                                previous = "";
                            }
                        }
                    } else if (VALID_GP.contains(text.trim())) { // Add GP and process next line
                        i++;
                        var congresista = blocks.get(i);
                        var potVot = congresista.substring(congresista.lastIndexOf(" ") + 1);
                        if (Votacion.is(potVot)) {
                            congresista = congresista.substring(0, congresista.lastIndexOf(" "));
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            text,
                                            "",
                                            congresista.substring(0, congresista.lastIndexOf(" ")),
                                            Votacion.of(potVot)
                                    )
                            );
                        } else {
                            i++;
                            var vot = blocks.get(i);
                            var votw = vot.split("\\s+");
                            if (votw.length > 1) {
                                if (votw.length == 2) {
                                    if (!votw[1].equals("+++") && !votw[1].equals("-") && !votw[1].equals("--") && !votw[1].equals("---")) {
                                        previous = votw[1];
                                    }
                                } else {
//                                        if (!votw[1].equals("+++") && !votw[1].equals("-") && !votw[1].equals("--") && !votw[1].equals("---")) {
                                    previous = votw[2];
//                                        }
                                }
                                resultados.add(
                                        new ResultadoCongresista<>(
                                                text,
                                                "",
                                                congresista,
                                                Votacion.of(votw[0])
                                        )
                                );
                            } else {
                                resultados.add(
                                        new ResultadoCongresista<>(
                                                text,
                                                "",
                                                congresista,
                                                Votacion.of(votw[0])
                                        )
                                );
                            }
                        }
                    } else if (!text.equals("+++")) {
                        var gp = text.contains(" ") ? text.substring(0, text.indexOf(" ")) : text;
                        var congresista = text.substring(text.indexOf(" ") + 1);
                        String potVot = congresista.substring(congresista.lastIndexOf(" ") + 1);
                        if (Votacion.is(potVot)) {
                            congresista = congresista.substring(0, congresista.lastIndexOf(" "));
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            gp,
                                            "",
                                            congresista.substring(0, congresista.lastIndexOf(" ")),
                                            Votacion.of(potVot)
                                    )
                            );
                        } else {
                            i++;
                            var vot = blocks.get(i);
                            var votw = vot.split("\\s+");
                            if (votw.length > 1) {
                                if (votw.length == 2) {
                                    if (!votw[1].equals("+++") && !votw[1].equals("-") && !votw[1].equals("--") && !votw[1].equals("---")) {
                                        previous = votw[1];
                                    }
                                } else {
//                                        if (!votw[2].equals("+++") && !votw[2].equals("-") && !votw[2].equals("--") && !votw[2].equals("---")) {
                                    previous = votw[2];
//                                        }
                                }
                                resultados.add(
                                        new ResultadoCongresista<>(
                                                text,
                                                "",
                                                congresista,
                                                Votacion.of(votw[0])
                                        )
                                );
                            } else {
                                resultados.add(
                                        new ResultadoCongresista<>(
                                                gp,
                                                "",
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
                    if (text.equals("Resultados de VOTACIÓN")) {
                        i++;
                        if (blocks.get(i).equals("Grupo Parlamentario")) {
                            i++;
                            var headers = blocks.get(i);
                            if (headers.equals("Si+++")) {
                                i++;
                                if (blocks.get(i).equals("No")) {
                                    i++;
                                    if (blocks.get(i).equals("Abst.")) {
                                        i++;
                                        if (blocks.get(i).equals("Sin Resp.")) {
                                            headersReady = true;
                                        }
                                    }
                                }
                            } else if (headers.equals("Si+++ No Abst.")) {
                                i++;
                                if (blocks.get(i).equals("Sin Resp.")) headersReady = true;
                            }
                        }
                    } else {
                        if (headersReady) {
                            //TODO Process results
                            if (Votacion.is(text)) {
                                i++;
                                var asistencia = blocks.get(i);
                                resultadosBuilder.with(
                                        Votacion.of(text),
                                        Integer.parseInt(asistencia)
                                );
                            } else if (text.contains("(") && text.contains(")")) {
//                                if (
//                                        Votacion.is(
//                                                text.substring(0, text.lastIndexOf("(") - 1)
//                                        )
//                                ) {
                                    var matcher = VOTACION_GROUP.matcher(
                                            text.substring(text.lastIndexOf("("))
                                    );
                                    if (matcher.find()) {
                                        var asis = matcher.group();
                                        i++;
                                        var result = blocks.get(i);
                                        resultadosBuilder.with(
                                                Votacion.of(asis),
                                                Integer.parseInt(result)
                                        );
                                    }
//                                }
                            } else {
                                if (VALID_GP.contains(text)) {
                                    i++;
                                    grupos.put(text, blocks.get(i));
                                    i++;
                                    String s = blocks.get(i);
                                    try {
                                        Integer.parseInt(s);
                                    } catch (NumberFormatException e) {
                                        i++;
                                    }
                                    var si = Integer.parseInt(blocks.get(i));
                                    i++;
                                    var no = Integer.parseInt(blocks.get(i));
                                    i++;
                                    var abst = Integer.parseInt(blocks.get(i));
                                    i++;
                                    var sinResp = Integer.parseInt(blocks.get(i));
                                    resultadosGrupos.put(
                                            new GrupoParlamentario(text, grupos.get(text)),
                                            ResultadoVotacion.create(
                                                    si, no, abst, sinResp
                                            )
                                    );
                                } else if (!text.isBlank() && text.contains(" ")) {
                                    if (
                                            VALID_GP.contains(text.substring(0, text.indexOf(" ")))
                                    ) {
                                        String grupo = text.substring(0, text.indexOf(" "));
                                        grupos.put(grupo, text.substring(text.indexOf(" ") + 1));
                                        i++;
                                        var si = Integer.parseInt(blocks.get(i));
                                        i++;
                                        var no = Integer.parseInt(blocks.get(i));
                                        i++;
                                        var abst = Integer.parseInt(blocks.get(i));
                                        i++;
                                        var sinResp = Integer.parseInt(blocks.get(i));
                                        resultadosGrupos.put(
                                                new GrupoParlamentario(grupo, grupos.get(grupo)),
                                                ResultadoVotacion.create(
                                                        si, no, abst, sinResp
                                                )
                                        );
                                    } else if (text.equals("Asistencia para Quórum")) {
                                        i++;
                                        plenoBuilder.withQuorum(
                                                Integer.parseInt(blocks.get(i))
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
                //}
                i++;
            }

            if (fechaHora == null) errors++;
            else registroBuilder.withHora(fechaHora.toLocalTime());

            var registro =
                    registroBuilder.withPleno(plenoBuilder.build())
                            .withVotaciones(resultados)
                            .withResultadosPorPartido(resultadosGrupos)
                            .withResultados(resultadosBuilder.build()).build();

//            var registro = new RegistroAsistencia(
//                    plenoBuilder.build(),
//                    fechaHora != null ? fechaHora.toLocalTime() : null,
//                    resultados,
//                    resultadosGrupos,
//                    resultadosBuilder.build()
//            );

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
