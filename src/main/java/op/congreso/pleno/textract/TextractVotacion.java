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
import java.util.stream.Stream;
import op.congreso.pleno.GrupoParlamentario;
import op.congreso.pleno.Pleno;
import op.congreso.pleno.ResultadoCongresista;
import op.congreso.pleno.asistencia.Asistencia;
import op.congreso.pleno.votacion.RegistroVotacion;
import op.congreso.pleno.votacion.ResultadoVotacion;
import op.congreso.pleno.votacion.Votacion;

public class TextractVotacion {

    public static final Pattern VOTACION_GROUP = Pattern.compile("(\\w+)");

    public static void main(String[] args) throws IOException {
        try {
            var lines = Files.readAllLines(Path.of("./out/Asis_vot_OFICIAL_07-07-22/page_16.txt"));

            var registro = load(65, clean(lines));

            System.out.println(" registry:\n" + registro);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static RegistroVotacion load(int quorum, List<String> lines) {
        lines = lines.stream()
                .map(s -> s.replace(" +++", ""))
                .map(s -> s.replace("+++ ", ""))
                .toList();

        var registroBuilder = RegistroVotacion.newBuilder();
        var plenoBuilder = Pleno.newBuilder().withQuorum(quorum);
        var resultadosBuilder = ResultadoVotacion.newBuilder();
        var resultados = new ArrayList<ResultadoCongresista<Votacion>>();
        var grupos = new HashMap<String, String>();
        var resultadosGrupos = new HashMap<GrupoParlamentario, ResultadoVotacion>();

        int i = 0;
        var titulo = "";
        var type = "";
        LocalDateTime fechaHora = null;
        boolean headersReady = false;
        var previous = "";
        int errors = 0;

        while (i < lines.size()) {
            if (i < 6) { // Process headers metadata
                switch (i) {
                    case 0 -> titulo = lines.get(i);
                    case 1 -> plenoBuilder.withLegislatura(lines.get(i));
                    case 2 -> registroBuilder.withPresidente(
                            lines.get(i).substring("Presidente: ".length())
                    );
                    case 3 -> plenoBuilder.withTitulo(lines.get(i));
                    case 4 -> type = lines.get(i);
                    case 5 -> {
                        fechaHora = LocalDateTime.parse(lines.get(i), FECHA_HORA_PATTERN);
                        registroBuilder.withHora(fechaHora.toLocalTime());
                        plenoBuilder.withFecha(fechaHora.toLocalDate());
                    }
                }
            } else if (lines.get(i).equals("Asunto:")) { // Process Asunto (up to 6 lines)
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
            } else if (resultados.size() < 130) {// Process votacion per congresistas
                // Process GP + Congresista + Votacion
                if (VALID_GP.contains(previous)) { // GP found in previous round
                    var congresista = lines.get(i); // get congresista from current line
                    var maybeVotacion = congresista.substring(
                            congresista.lastIndexOf(" ") + 1
                    ); // check if maybe contains votacion at the end
                    if (maybeVotacion.equals("+++") || maybeVotacion.equals("-")) {
                        congresista =
                                congresista.substring(0, congresista.lastIndexOf(" ")); // get congresista
                        maybeVotacion = congresista.substring(
                                congresista.lastIndexOf(" ") + 1
                        );
                    }
                    if (Votacion.is(maybeVotacion)) { // if it does
                        congresista =
                                congresista.substring(0, congresista.lastIndexOf(" ")); // get congresista
                        // and add votacion
                        resultados.add(
                                new ResultadoCongresista<>(
                                        previous,
                                        congresista.substring(0, congresista.lastIndexOf(" ")),
                                        Votacion.of(maybeVotacion)
                                )
                        );
                        previous = "";
                    } else { // if congresista is ok
                        i++;
                        var votacionWords = lines.get(i).split("\\s+"); // go to next line and get vote
                        if (votacionWords.length > 1) { // if votacion has more data
                            var prev = ""; // get GP for next round
                            if (votacionWords.length == 2) {
                                if (
                                        !votacionWords[1].equals("+++") &&
                                                !votacionWords[1].equals("-") &&
                                                !votacionWords[1].equals("--") &&
                                                !votacionWords[1].equals("---")
                                ) {
                                    prev = votacionWords[1];
                                }
                            } else {
                                prev = votacionWords[2];
                            }
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            previous,
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                            previous = prev;
                        } else {
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            previous,
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                            previous = "";
                        }
                    }
                } else if (VALID_GP.contains(lines.get(i).trim())) { // GP found, process next line
                    var gp = lines.get(i).trim();
                    i++;
                    var congresista = lines.get(i); // get congresista
                    var maybeVotacion = congresista.substring(
                            congresista.lastIndexOf(" ") + 1
                    );
                    if (maybeVotacion.equals("+++") || maybeVotacion.equals("-")) {
                        congresista =
                                congresista.substring(0, congresista.lastIndexOf(" ")); // get congresista
                        maybeVotacion = congresista.substring(
                                congresista.lastIndexOf(" ") + 1
                        );
                    }
                    if (Votacion.is(maybeVotacion)) { // if votacion included
                        congresista =
                                congresista.substring(0, congresista.lastIndexOf(" ")); // extract congresista
                        // and add resultado
                        resultados.add(
                                new ResultadoCongresista<>(
                                        gp,
                                        congresista.substring(0, congresista.lastIndexOf(" ")),
                                        Votacion.of(maybeVotacion)
                                )
                        );
                    } else { // if not get votacion from next line
                        i++;
                        var votacionWords = lines.get(i).split("\\s+");
                        if (votacionWords.length > 1) { // check votacion contains GP
                            if (votacionWords.length == 2) {
                                if (
                                        !votacionWords[1].equals("+++") &&
                                                !votacionWords[1].equals("-") &&
                                                !votacionWords[1].equals("--") &&
                                                !votacionWords[1].equals("---")
                                ) {
                                    previous = votacionWords[1];
                                }
                            } else {
                                previous = votacionWords[2];
                            }
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            gp,
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                        } else {
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            lines.get(i),
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                        }
                    }
                } else if (!lines.get(i).equals("+++")) { // if line does not have additional characters
                    var gp = lines.get(i).contains(" ")
                            ? lines.get(i).substring(0, lines.get(i).indexOf(" "))
                            : lines.get(i);
                    var congresista = lines
                            .get(i)
                            .substring(lines.get(i).indexOf(" ") + 1);
                    var maybeVotacion = congresista.substring(
                            congresista.lastIndexOf(" ") + 1
                    );
                    if (Votacion.is(maybeVotacion)) {
                        congresista =
                                congresista.substring(0, congresista.lastIndexOf(" "));
                        resultados.add(
                                new ResultadoCongresista<>(
                                        gp,
                                        congresista,
                                        Votacion.of(maybeVotacion)
                                )
                        );
                    } else {
                        i++;
                        var votacionWords = lines.get(i).split("\\s+");
                        if (votacionWords.length > 1) {
                            if (votacionWords.length == 2) {
                                if (
                                        !votacionWords[1].equals("+++") &&
                                                !votacionWords[1].equals("-") &&
                                                !votacionWords[1].equals("--") &&
                                                !votacionWords[1].equals("---")
                                ) {
                                    previous = votacionWords[1];
                                }
                            } else {
                                previous = votacionWords[2];
                            }
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            gp,
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                        } else {
                            resultados.add(
                                    new ResultadoCongresista<>(
                                            gp,
                                            congresista,
                                            Votacion.of(votacionWords[0])
                                    )
                            );
                        }
                    }
                }
            } else { // Process resultados
                // Parse headers:
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
                    if (headersReady) { // once resultado headers are ready
                        var votacion = lines.get(i);
                        if (Votacion.is(votacion)) {
                            i++;
                            var votacionTotal = lines.get(i);
                            resultadosBuilder.with(
                                    Votacion.of(votacion),
                                    Integer.parseInt(votacionTotal)
                            );
                        } else if (
                                lines.get(i).contains("(") && lines.get(i).contains(")")
                        ) {
                            try {
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
                            } catch (Exception e) {
                                System.out.println("Error processing total: " + lines.get(i));
                                e.printStackTrace();
                            }
                        } else {
                            var gp = lines.get(i);
                            if (VALID_GP.contains(gp)) {
                                i++;
                                grupos.put(lines.get(i), gp);
                                try {
                                    i++;
                                    var s = lines.get(i);
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
                                } catch (Exception e) {
                                    System.out.println("Error processing group results: " + gp);
                                    e.printStackTrace();
                                }
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
            i++;
        }

        return registroBuilder
                .withPleno(plenoBuilder.withGruposParlamentarios(grupos).build())
                .withVotaciones(resultados)
                .withResultadosPorPartido(resultadosGrupos)
                .withResultados(resultadosBuilder.build())
                .build();
    }

    static List<String> clean(List<String> list) {
        return list
                .stream()
                .map(s ->
                        s.replace("+++ ", "")
                                .replace("+++", "")
                                .replace(" +++", "")
                                .replace("***", "")
                                .replace("NO---", "NO")
                                .replace("NO-", "NO")
                                .trim()
                )
                .flatMap(s -> {
                    var ss = s.split(" ");
                    if (ss.length > 1 && Votacion.is(ss[0])) {
                        return Stream.of(ss[0], s.substring(s.indexOf(" ") + 1));
                    } else return Stream.of(s);
                })
                .filter(s -> !s.isBlank())
                .toList();
    }
}
