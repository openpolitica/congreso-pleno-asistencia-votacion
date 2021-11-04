package op.pleno;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import op.pleno.asistencia.RegistroAsistencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportAsistenciaPlenos implements Consumer<AsistenciaPlenos> {
  static final Logger LOG = LoggerFactory.getLogger(ExportAsistenciaPlenos.class);

  public static final String YYYY_MM_DD = "yyyy-MM-dd";
  public static final String HH_MM = "HH:mm";

  static List<TableLoad> tableLoadList = List.of(
      new AsistenciaCongresistaLoad(),
      new AsistenciaGrupoParlamentarioLoad(),
      new AsistenciaResultadoLoad()
  );

  @Override
  public void accept(AsistenciaPlenos asistenciaPlenos) {
    var jdbcUrl = "jdbc:sqlite:%s-plenos-asistencia.db".formatted(asistenciaPlenos.periodo());
    try (var connection = DriverManager.getConnection(jdbcUrl)) {
      var statement = connection.createStatement();
      statement.executeUpdate("pragma journal_mode = WAL");
      statement.executeUpdate("pragma synchronous = off");
      statement.executeUpdate("pragma temp_store = memory");
      statement.executeUpdate("pragma mmap_size = 300000000");
      statement.executeUpdate("pragma page_size = 32768");

      for (var tableLoad : tableLoadList) {
        LOG.info("Loading {}", tableLoad.tableName);
        statement.executeUpdate(tableLoad.dropTableStatement());
        statement.executeUpdate(tableLoad.createTableStatement());
        for (String s : tableLoad.createIndexesStatement()) {
          statement.executeUpdate(s);
        }
        LOG.info("Table {} created", tableLoad.tableName);

        var ps = connection.prepareStatement(tableLoad.prepareStatement());
        LOG.info("Statement for {} prepared", tableLoad.tableName);

        for (var m : asistenciaPlenos.registros()) {
          tableLoad.addBatch(ps, m);
        }

        LOG.info("Batch for {} ready", tableLoad.tableName);
        ps.executeBatch();
        LOG.info("Table {} updated", tableLoad.tableName);
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

    String dropTableStatement() {
      return "drop table if exists %s".formatted(tableName);
    }

    abstract String createTableStatement();

    abstract List<String> createIndexesStatement();

    String index(String field) {
      return "CREATE INDEX %s_%s ON %s(\"%s\");\n"
          .formatted(tableName, field, tableName, field);
    }

    abstract String prepareStatement();

    abstract void addBatch(PreparedStatement ps, RegistroAsistencia pl)
        throws SQLException, IOException;
  }

  static class AsistenciaResultadoLoad extends TableLoad {

    public AsistenciaResultadoLoad() {
      super("asistencia_resultado");
    }

    @Override
    String createTableStatement() {
      return """
          create table %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            pleno_titulo text not null,
            
            quorum integer not null,
            presentes integer not null,
            ausentes integer not null,
            licencias integer not null,
            otros integer not null,
            total integer not null
          )
          """.formatted(tableName);
    }

    @Override
    List<String> createIndexesStatement() {
      return List.of(
          index("periodo_parlamentario"),
          index("periodo_anual"),
          index("legislatura")
      );
    }

    @Override
    String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroAsistencia r) throws SQLException {
      ps.setString(1, r.pleno().id());
      ps.setString(2, r.pleno().periodoParlamentario());
      ps.setString(3, r.pleno().periodoAnual());
      ps.setString(4, r.pleno().legislatura());
      ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
      ps.setString(6, r.hora().format(DateTimeFormatter.ofPattern(HH_MM)));
      ps.setString(7, r.pleno().titulo());

      ps.setInt(8, r.resultados().presentes());
      ps.setInt(9, r.resultados().presentes());
      ps.setInt(10, r.resultados().ausentes());
      ps.setInt(11, r.resultados().licencias());
      ps.setInt(12, r.resultados().otros());
      ps.setInt(13, r.resultados().total());

      ps.addBatch();
    }
  }

  static class AsistenciaGrupoParlamentarioLoad extends TableLoad {

    public AsistenciaGrupoParlamentarioLoad() {
      super("asistencia_grupo_parlamentario");
    }

    @Override
    String createTableStatement() {
      return """
          create table %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            pleno_titulo text not null,
            
            grupo_parlamentario text not null,
            presentes integer not null,
            ausentes integer not null,
            licencias integer not null,
            otros integer not null,
            total integer not null
          )
          """.formatted(tableName);
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
            ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroAsistencia r) throws SQLException {
      for (var a : r.resultadosPorGrupo().entrySet()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.hora().format(DateTimeFormatter.ofPattern(HH_MM)));
        ps.setString(7, r.pleno().titulo());

        ps.setString(8, a.getKey());
        ps.setInt(9, a.getValue().presentes());
        ps.setInt(10, a.getValue().ausentes());
        ps.setInt(11, a.getValue().licencias());
        ps.setInt(12, a.getValue().otros());
        ps.setInt(13, a.getValue().total());

        ps.addBatch();
      }
    }
  }

  static class AsistenciaCongresistaLoad extends TableLoad {

    public AsistenciaCongresistaLoad() {
      super("asistencia_congresista");
    }

    @Override
    String prepareStatement() {
      return """
          insert into %s values (
            ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?
          )
          """.formatted(tableName);
    }

    @Override
    String createTableStatement() {
      return """
          create table %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            pleno_titulo text not null,
            
            congresista text not null,
            grupo_parlamentario text not null,
            grupo_parlamentario_descripcion text not null,
            resultado text not null,
            resultado_descripcion text not null
          )
          """.formatted(tableName);
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
    void addBatch(PreparedStatement ps, RegistroAsistencia r)
        throws SQLException {
      for (var a : r.asistencias()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.hora().format(DateTimeFormatter.ofPattern(HH_MM)));
        ps.setString(7, r.pleno().titulo());

        ps.setString(8, a.congresista());
        ps.setString(9, a.grupoParlamentario());
        ps.setString(10, a.grupoParlamentarioDescripcion());
        ps.setString(11, a.resultado());
        ps.setString(12, a.resultadoDescripcion());

        ps.addBatch();
      }
    }
  }
}
