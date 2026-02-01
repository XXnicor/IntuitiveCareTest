package com.intuitive.crawler;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Serviço para importar dados dos CSVs para o banco de dados.
 *
 * Trade-off: JDBC vs. ORM (Hibernate). - Batch: 50x mais para >10k registros,
 * sem overhead de EntityManger. - Limitação: SQL manual,sem cache de segundo
 * nivel
 */
public class DatabaseImportService {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseImportService(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Importa registros agregados para o banco.
     *
     * @param records lista de AggregatedRecord
     * @throws SQLException em erros de conexão/insert
     */
    public void importAgregatedRecords(List<AggregatorService.AggregatedRecord> records) throws SQLException {

        String sql = """
            INSERT INTO despesas_agregadas (cnpj_operadora, codigo_conta, valor_total) 
            VALUES (?, ?, ?)
                """;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int batchSize = 1000;
                int count = 0;

                for (AggregatorService.AggregatedRecord record : records) {
                    pstmt.setString(1, record.cnpjOperadora);
                    pstmt.setString(2, record.codigoConta);
                    pstmt.setDouble(3, record.totalValor);
                    pstmt.addBatch();

                    if (++count % batchSize == 0) {
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }

                // Executa o restante
                pstmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

      /**
     * Cria as tabelas no banco (executa schema.sql).
     * 
     * @param schemaPath caminho para o arquivo schema.sql
     * @throws SQLException, IOException
     */
    public void createSchema(String schemaPath) throws SQLException, java.io.IOException {
        
        String schemaSql = Files.readString(java.nio.file.Path.of(schemaPath));
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            java.sql.Statement stmt = conn.createStatement();

            String[] statements = schemaSql.split(";");

            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                    System.out.println("DDL executado: ");
                }
        }
    }
}

/**
     * Importa operadoras do CSV de cadastro.
     * 
     * @param operadoras lista de Operadora (do DataEnricherService)
     * @throws SQLException
     */
    public void importOperadoras(List<DataEnricherService.Operadora> operadoras) throws SQLException {
        String sql = """
            INSERT INTO operadoras (cnpj, nome_operadora) 
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE 
            cnpj = VALUES(cnpj),
            razao_social = VALUES(razao_social),
            nome_fantasia = VALUES(nome_fantasia)
                """;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int batchSize = 1000;
                int count = 0;

                for (DataEnricherService.Operadora operadora : operadoras) {
                    pstmt.setString(1, operadora.cnpj);
                    pstmt.setString(2, operadora.razaoSocial);
                    pstmt.addBatch();

                    if (++count % batchSize == 0) {
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }

                // Executa o restante
                pstmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}