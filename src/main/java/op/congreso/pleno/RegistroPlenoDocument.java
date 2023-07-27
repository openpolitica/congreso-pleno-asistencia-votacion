package op.congreso.pleno;

import static op.congreso.pleno.textract.TextractRegistroPleno.extractRegistroPleno;
import static op.congreso.pleno.textract.TextractRegistroPleno.processLines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import op.congreso.pleno.db.SaveRegistroPlenoToCsv;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record RegistroPlenoDocument(
    Periodo periodo,
    String fecha,
    String titulo,
    String url,
    String filename,
    int paginas,
    boolean provisional) {
  static final Logger LOG = LoggerFactory.getLogger(RegistroPlenoDocument.class);
  static Pattern periodoPattern = Pattern.compile("\\d\\d\\d\\d ?- ?\\d\\d\\d\\d$");

  public static StringBuilder csvHeader() {
    return new StringBuilder(
        "periodo_parlamentario,periodo_anual,legislatura,"
            + "fecha,titulo,url,filename,paginas,provisional\n");
  }

  public static String parsePeriodo(String text) {
    String val = text;
    var m1 = periodoPattern.matcher(text);
    if (m1.find()) val = m1.group().replace(" ", "");
    return val;
  }

  public static RegistroPlenoDocument parse(Map<String, String> v) {
    return new RegistroPlenoDocument(
        new Periodo(
            parsePeriodo(v.get("periodo_parlamentario")),
            parsePeriodo(v.get("periodo_anual")),
            v.get("legislatura")),
        v.get("fecha"),
        v.get("titulo"),
        v.get("url"),
        v.get("filename"),
        Integer.parseInt(v.get("paginas")),
        Optional.ofNullable(v.get("provisional")).map(Boolean::parseBoolean).orElse(Boolean.FALSE));
  }

  public static RegistroPlenoDocument parseJson(String s) {
    try {
      return new ObjectMapper().readValue(s, RegistroPlenoDocument.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public String csvEntry() {
    return "%s,%s,%s,%s,\"%s\",%s,%s,%s,%s%n"
        .formatted(
            periodo.periodoParlamentario(),
            periodo.periodoAnual(),
            periodo.legislatura(),
            fecha,
            titulo,
            url,
            filename,
            paginas,
            provisional);
  }

  String directory() {
    return ("out/pdf/"
        + periodo.periodoParlamentario()
        + "/"
        + periodo.periodoAnual()
        + "/"
        + periodo.legislatura());
  }

  String path() {
    return directory() + "/" + filename;
  }

  public RegistroPlenoDocument withPaginas() {
    return new RegistroPlenoDocument(
        periodo, fecha, titulo, url, filename, countPages(), provisional);
  }

  public String id() {
    return "%s:%s".formatted(fecha, titulo);
  }

  int countPages() {
    try (PDDocument doc = PDDocument.load(Path.of(path()).toFile())) {
      return doc.getNumberOfPages();
    } catch (Exception | NoClassDefFoundError e) {
      LOG.error("ERROR with path: {}", path(), e);
      return -1;
    }
  }

  public void download() {
    try {
      var dir = Path.of(directory());
      if (!Files.isDirectory(dir)) Files.createDirectories(dir);
      var readableByteChannel = Channels.newChannel(new URL(url()).openStream());
      var path = path();
      try (var fileOutputStream = new FileOutputStream(path)) {
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      }
      LOG.info("PDF downloaded: {}", path);
    } catch (IOException e) {
      LOG.error("Error downloading {}", path(), e);
    }
  }

  public static final String BASE_URL = "https://www2.congreso.gob.pe";

  public static Map<String, String> collect(String url, int colspan) throws IOException {
    var root = new LinkedHashMap<String, String>();
    {
      var jsoup = Jsoup.connect(BASE_URL + url);
      var doc = jsoup.get();
      var main = doc.body().select("table[cellpadding=2]").first();

      assert main != null;
      for (var td : main.select("td[colspan=%s]".formatted(colspan))) {
        var table = td.child(0).selectFirst("table");
        assert table != null;
        var tds = table.select("td");
        if (tds.size() == 2) {
          var a = tds.get(0).selectFirst("a");
          assert a != null;
          var href = a.attr("href");
          var periodo = tds.get(1).text();
          root.put(periodo, href);
        }
      }
      return root;
    }
  }

  private static final Pattern p = Pattern.compile("javascript:openWindow\\('(.+)'\\)");

  public static Map<String, RegistroPlenoDocument> collectPleno(
      String pp, String pa, String l, String url) throws IOException {
    var root = new LinkedHashMap<String, RegistroPlenoDocument>();
    {
      var jsoup = Jsoup.connect(BASE_URL + url);
      var doc = jsoup.get();
      var main = doc.body().select("table[cellpadding=2]").first();
      assert main != null;
      var trs = main.select("tr[valign=top]");
      for (var tr : trs) {
        if (tr.children().size() == 6) {
          var fonts = tr.select("font[size=4]");
          var fecha = fonts.get(0).text();
          var date =
              LocalDate.parse(fecha, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                  .format(DateTimeFormatter.ISO_LOCAL_DATE);
          var second = fonts.get(2).children().first();
          assert second != null;
          var titulo = second.text();
          var href = fonts.get(2).select("a").attr("href");
          var matcher = p.matcher(href);
          if (matcher.find()) {
            var u = matcher.group(1);
            var fullUrl = BASE_URL + "/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/" + u;
            root.put(
                titulo,
                new RegistroPlenoDocument(
                    new Periodo(parsePeriodo(pp), parsePeriodo(pa), l),
                    date,
                    titulo,
                    fullUrl,
                    fullUrl.split("/")[9],
                    0,
                    titulo.contains("PROVISIONAL")));
          }
        }
      }
      return root;
    }
  }

  public RegistroPlenoDocument extract() throws IOException {
    download();
    Files.writeString(Path.of(path() + ".json"), new ObjectMapper().writeValueAsString(this));
    var lines = extractRegistroPleno(Path.of(path()));
    var regPleno = processLines(this, lines);
    SaveRegistroPlenoToCsv.save(regPleno);
    return this.withPaginas();
  }

  public String prBranchName() {
    return "pleno-" + fecha();
  }

  public String prTitle() {
    return "Pleno: " + id();
  }

  public String prContent() {
    return ("Periodo parlamentario: "
        + periodo.periodoParlamentario()
        + " | "
        + "Periodo anual: "
        + periodo.periodoAnual()
        + " | "
        + "Titulo: "
        + titulo
        + " | "
        + "URL: <"
        + url
        + "> | "
        + "Paginas: "
        + paginas);
  }
}
