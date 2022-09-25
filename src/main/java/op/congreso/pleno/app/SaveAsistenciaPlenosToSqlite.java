package op.congreso.pleno.app;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import op.congreso.pleno.AsistenciaPlenos;
import op.congreso.pleno.asistencia.RegistroAsistencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteException;

public class SaveAsistenciaPlenosToSqlite implements Consumer<AsistenciaPlenos> {

  static final Logger LOG = LoggerFactory.getLogger(SaveAsistenciaPlenosToSqlite.class);

  public static final String YYYY_MM_DD = "yyyy-MM-dd";
  public static final String HH_MM = "HH:mm";

  static List<TableLoad> tableLoadList = List.of(
    new AsistenciaCongresistaLoad(),
    new AsistenciaGrupoParlamentarioLoad(),
    new AsistenciaResultadoLoad()
  );

  @Override
  public void accept(AsistenciaPlenos asistenciaPlenos) {
    var jdbcUrl = "jdbc:sqlite:%s-asistencias-votaciones.db".formatted(asistenciaPlenos.periodo());
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

        for (var m : asistenciaPlenos.registros()) {
          tableLoad.addBatch(ps, m);
        }

        LOG.debug("Batch for {} ready", tableLoad.tableName);
        ps.executeBatch();
        LOG.debug("Table {} updated", tableLoad.tableName);
      }
      statement.executeUpdate("pragma vacuum;");
      statement.executeUpdate("pragma optimize;");
    } catch (Exception e) {
      if (e instanceof SQLiteException) {
        e.printStackTrace();
      }
      e.printStackTrace();
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

    abstract void addBatch(PreparedStatement ps, RegistroAsistencia pl) throws SQLException, IOException;
  }

  static class AsistenciaResultadoLoad extends TableLoad {

    public AsistenciaResultadoLoad() {
      super("asistencia_resultado");
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
            
            quorum integer not null,
            presentes integer not null,
            ausentes integer not null,
            licencias integer not null,
            suspendidos integer not null,
            otros integer not null,
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
            ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?
          )
          """.formatted(
          tableName
        );
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroAsistencia r) throws SQLException {
      ps.setString(1, r.pleno().id());
      ps.setString(2, r.pleno().periodoParlamentario());
      ps.setString(3, r.pleno().periodoAnual());
      ps.setString(4, r.pleno().legislatura());
      ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
      ps.setString(6, r.fechaHora().toLocalTime().format(DateTimeFormatter.ofPattern(HH_MM)));

      ps.setInt(7, r.quorum());
      ps.setInt(8, r.resultados().presentes());
      ps.setInt(9, r.resultados().ausentes());
      ps.setInt(10, r.resultados().licencias());
      ps.setInt(11, r.resultados().suspendidos());
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
          create table if not exists %s (
            pleno_id text not null,
            periodo_parlamentario text not null,
            periodo_anual text not null,
            legislatura text not null,
            fecha text not null,
            hora text not null,
            
            grupo_parlamentario text not null,
            grupo_parlamentario_descripcion text not null,
            presentes integer not null,
            ausentes integer not null,
            licencias integer not null,
            otros integer not null,
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
            ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?
          )
          """.formatted(
          tableName
        );
    }

    @Override
    void addBatch(PreparedStatement ps, RegistroAsistencia r) throws SQLException {
      for (var a : r.resultadosPorGrupo().entrySet()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.fechaHora().toLocalTime().format(DateTimeFormatter.ofPattern(HH_MM)));

        ps.setString(7, a.getKey().nombre());
        ps.setString(8, a.getKey().descripcion());

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
            ?, ?, ?, ?, ?
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
    void addBatch(PreparedStatement ps, RegistroAsistencia r) throws SQLException {
      for (var a : r.asistencias()) {
        ps.setString(1, r.pleno().id());
        ps.setString(2, r.pleno().periodoParlamentario());
        ps.setString(3, r.pleno().periodoAnual());
        ps.setString(4, r.pleno().legislatura());
        ps.setString(5, r.pleno().fecha().format(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        ps.setString(6, r.fechaHora().toLocalTime().format(DateTimeFormatter.ofPattern(HH_MM)));

        ps.setString(7, a.congresista());
        ps.setString(8, a.grupoParlamentario());
        if (r.pleno().gruposParlamentarios().get(a.grupoParlamentario()) == null) {
          throw new IllegalArgumentException("a.grupoParlamentarioDescripcion == null");
        }
        ps.setString(9, r.pleno().gruposParlamentarios().get(a.grupoParlamentario()));
        if (a.resultado() == null) throw new RuntimeException(
          "Error with " + a + " at " + r.pleno() + " @ " + r.fechaHora()
        );
        ps.setString(10, a.resultado().name());
        ps.setString(11, a.resultado().descripcion());

        ps.addBatch();
      }
    }
  }
}
