package op.congreso.pleno;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;

record MetadataPleno(
        String periodoParlamentario, String periodoAnual, String legislatura,
        String fecha, String titulo, String url, String filename, int paginas) {
    static StringBuilder csvHeader() {
        return new StringBuilder("periodo_parlamentario,periodo_anual,legislatura," +
                "fecha,titulo,url,filename,paginas\n");
    }

    String csvEntry() {
        return "%s,%s,%s,%s,\"%s\",%s,%s,%s%n".formatted(
                periodoParlamentario, periodoAnual, legislatura,
                fecha, titulo, url, filename, paginas
        );
    }
     String directory() {
        return "target/pdf/" + periodoParlamentario + "/" + periodoAnual + "/" + legislatura;
    }

    String path() {
        return directory() + "/" + filename;
    }

    MetadataPleno withPaginas() {
        return new MetadataPleno(periodoParlamentario, periodoAnual, legislatura,
                fecha, titulo, url, filename,
                countPages());
    }

    String id() {
        return "%s:%s:%s:%s:%s".formatted(periodoParlamentario, periodoAnual, legislatura, fecha, titulo);
    }

    int countPages() {
        try (PDDocument doc = PDDocument.load(Path.of(path()).toFile())){
            return doc.getNumberOfPages();
        } catch (Exception | NoClassDefFoundError e) {
            System.out.println("ERROR with path: " + path());
            e.printStackTrace();
            return -1;
        }
    }

    void download() {
        try {
            var dir = Path.of(directory());
            if (!Files.isDirectory(dir)) Files.createDirectories(dir);
            System.out.println(dir);
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url()).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(path());
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
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

    public static Map<String, MetadataPleno> collectPleno(String pp, String pa, String l, String url) throws IOException {
        var root = new LinkedHashMap<String, MetadataPleno>();
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
                    var date = LocalDate.parse(fecha, DateTimeFormatter.ofPattern("MM/dd/yyyy")).format(DateTimeFormatter.ISO_LOCAL_DATE);
                    var second = fonts.get(2).children().first();
                    assert second != null;
                    var titulo = second.text();
                    var href = fonts.get(2).select("a").attr("href");
                    var matcher = p.matcher(href);
                    if (matcher.find()) {
                        var u = matcher.group(1);
                        var fullUrl = BASE_URL + "/Sicr/RelatAgenda/PlenoComiPerm20112016.nsf/" + u;
                        root.put(titulo, new MetadataPleno(
                                pp, pa, l,
                                date, titulo, fullUrl,
                                fullUrl.split("/")[9],
                                0));
                    }
                }
            }
            return root;
        }
    }

}
