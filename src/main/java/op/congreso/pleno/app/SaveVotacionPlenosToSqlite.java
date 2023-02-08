package op.congreso.pleno.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import op.congreso.pleno.VotacionPlenos;
import op.congreso.pleno.votacion.RegistroVotacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveVotacionPlenosToSqlite implements Consumer<VotacionPlenos> {

  static final Logger LOG = LoggerFactory.getLogger(SaveVotacionPlenosToSqlite.class);
  static final ObjectMapper jsonMapper = new ObjectMapper();

  public static final String YYYY_MM_DD = "yyyy-MM-dd";
  public static final String HH_MM = "HH:mm";

  static List<TableLoad> tableLoadList = List.of(
    new VotacionCongresistaLoad(),
    new VotacionGrupoParlamentarioLoad(),
    new VotacionResultadoLoad()
  );

  @Override
  public void accept(VotacionPlenos votacionPlenos) {
    var jdbcUrl = "jdbc:sqlite:%s-asistencias-votaciones.db".formatted(votacionPlenos.periodo());
    try (var connection = DriverManager.getConnection(jdbcUrl)) {
      var statement = connection.createStatement();
      statement.executeUpdate("pragma journal_mode = WAL");
      statement.executeUpdate("pragma synchronous = off");
      statement.executeUpdate("pragma temp_store = memory");
      statement.executeUpdate("pragma mmap_size = 300000000");
      statement.executeUpdate("pragma page_size = 32768");

      for (var tableLoad : tableLoadList) {
        LOG.debug("Loading {}", tableLoad.tableName);
        statement.executeUpdate(tableLoad.createTableStatement());
        for (String s : tableLoad.createIndexesStatement()) {
          statement.executeUpdate(s);
        }
        LOG.debug("Table {} created", tableLoad.tableName);

        var ps = connection.prepareStatement(tableLoad.prepareStatement());
        LOG.debug("Statement for {} prepared", tableLoad.tableName);

        for (var m : votacionPlenos.registros()) {
          tableLoad.addBatch(ps, m);
        }

        LOG.debug("Batch for {} ready", tableLoad.tableName);
        ps.executeBatch();
        LOG.debug("Table {} updated", tableLoad.tableName);
      }
      statement.executeUpdate("pragma vacuum;");
      statement.executeUpdate("pragma optimize;");
    } catch (Exception throwables) {
      throwables.printStackTrace();
    }
  }

  abstract static class TableLoad {

    final String tableName;

    public TableLoad(String tableName) {
      this.tableName = tableName;
    }

    abstract String createTableStatement();

    abstract List<String> createIndexesStatement();

    String index(String field) {
      return "CREATE INDEX IF NOT EXISTS %s_%s ON %s(\"%s\");\n".formatted(tableName, field, tableName, field);
    }

    abstract String prepareStatement();

    abstract void addBatch(PreparedStatement ps, RegistroVotacion pl) throws Exception;
  }

  static class VotacionResultadoLoad extends TableLoad {

    public VotacionResultadoLoad() {
      super("votacion_resultado");
    }

    @Override
    String createTableStatement() {
      return """
          create table if not exists %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            
            asunto text not null,
            presidente text not null,
            etiquetas text not null,
            
            quorum integer not null,
            si integer not null,
            no integer not null,
            abstenciones integer not null,
            sin_responder integer not null,
            total integer not null
          )
          """.formatted(
          tableName
        );
    }

    @Override
    List<String> createIndexesStatement() {
      return List.of(index("periodo_parlamentario"), index("periodo_anual"), index("legislatura"));
    }

    @Override
    String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?, ?
          )
          """.formatted(
          tableName
        );
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroVotacion r) throws Exception {
      ps.setString(1, r.pleno().id());
      ps.setString(2, r.pleno().periodoParlamentario());
      ps.setString(3, r.pleno().periodoAnual());
      ps.setString(4, r.pleno().legislatura());
      ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
      ps.setString(6, r.fechaHora().format(DateTimeFormatter.ofPattern(HH_MM)));

      ps.setString(7, r.asunto());
      ps.setString(8, r.presidente());
      ps.setString(9, jsonMapper.writeValueAsString(r.etiquetas()));

      ps.setInt(10, r.quorum());
      ps.setInt(11, r.resultados().si());
      ps.setInt(12, r.resultados().no());
      ps.setInt(13, r.resultados().abstenciones());
      ps.setInt(14, r.resultados().sinResponder());
      ps.setInt(15, r.resultados().total());

      ps.addBatch();
    }
  }

  static class VotacionGrupoParlamentarioLoad extends TableLoad {

    public VotacionGrupoParlamentarioLoad() {
      super("votacion_grupo_parlamentario");
    }

    @Override
    String createTableStatement() {
      return """
          create table if not exists %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            asunto text not null,
            presidente text not null,
            etiquetas text not null,
            
            grupo_parlamentario text not null,
            grupo_parlamentario_descripcion text not null,
          
            si integer not null,
            no integer not null,
            abstenciones integer not null,
            sin_responder integer not null,
            total integer not null
          )
          """.formatted(
          tableName
        );
    }

    @Override
    List<String> createIndexesStatement() {
      return List.of(
        index("periodo_parlamentario"),
        index("periodo_anual"),
        index("legislatura"),
        index("grupo_parlamentario")
      );
    }

    @Override
    String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?, ?, ?
          )
          """.formatted(
          tableName
        );
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroVotacion r) throws Exception {
      for (var a : r.resultadosPorGrupo().entrySet()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.fechaHora().format(DateTimeFormatter.ofPattern(HH_MM)));
        ps.setString(7, r.asunto());
        ps.setString(8, r.presidente());
        ps.setString(9, jsonMapper.writeValueAsString(r.etiquetas()));

        ps.setString(10, a.getKey().nombre());
        if (a.getKey().descripcion() == null) {
          throw new IllegalStateException("null grupo descripcion");
        }
        ps.setString(11, a.getKey().descripcion());

        ps.setInt(12, a.getValue().si());
        ps.setInt(13, a.getValue().no());
        ps.setInt(14, a.getValue().abstenciones());
        ps.setInt(15, a.getValue().sinResponder());
        ps.setInt(16, a.getValue().total());

        ps.addBatch();
      }
    }
  }

  static class VotacionCongresistaLoad extends TableLoad {

    public VotacionCongresistaLoad() {
      super("votacion_congresista");
    }

    @Override
    String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?, ?
          )
          """.formatted(
          tableName
        );
    }

    @Override
    String createTableStatement() {
      return """
          create table if not exists %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            asunto text not null,
            presidente text not null,
            etiquetas text not null,
            
            congresista text not null,
            grupo_parlamentario text not null,
            grupo_parlamentario_descripcion text not null,
            resultado text not null,
            resultado_descripcion text not null
          )
          """.formatted(
          tableName
        );
    }

    @Override
    List<String> createIndexesStatement() {
      return List.of(
        index("periodo_parlamentario"),
        index("periodo_anual"),
        index("legislatura"),
        index("congresista"),
        index("grupo_parlamentario"),
        index("grupo_parlamentario_descripcion"),
        index("resultado"),
        index("resultado_descripcion")
      );
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroVotacion r) throws Exception {
      for (var v : r.votaciones()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.fechaHora().format(DateTimeFormatter.ofPattern(HH_MM)));
        ps.setString(7, r.asunto());
        ps.setString(8, r.presidente());
        ps.setString(9, jsonMapper.writeValueAsString(r.etiquetas()));

        ps.setString(10, v.congresista());
        ps.setString(11, v.grupoParlamentario());
        String gp = r.pleno().gruposParlamentarios().get(v.grupoParlamentario());
        if (gp == null) {
          throw new IllegalStateException("null grupo descripcion - name: " + v.grupoParlamentario() + " Pleno: "+ r.pleno().fecha());
        }
        ps.setString(12, gp);

        ps.setString(13, v.resultado().name());
        ps.setString(14, v.resultado().descripcion());

        ps.addBatch();
      }
    }
  }
}
