package com.intuitive.crawler;

import org.hibernate.annotations.BatchSize;

/**
 * Serviço para importar dados dos CSVs para o banco de dados.
 * 
 * Trade-off: JDBC vs. ORM (Hibernate).
 * - Batch: 50x mais para >10k registros, sem overhead de EntityManger.
 * - Limitação: SQL manual,sem cache de segundo nivel
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
            ON DUPLICATE KEY UPDATE valor_total = VALUES(valor_total);
                """;
                        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);
                int count = 0;

                for (AggregatorService.AggregatedRecord record : records) {
                    pstmt.setString(1, record.cnpjOperadora);
                    pstmt.setString(2, record.codigoConta);
                    pstmt.setDouble(3, record.totalValor);
                    pstmt.addBatch();
                    count++;
                    
                    if (++count % BatchSize == 0) {
                        pstmt.executeBatch();
                    }
                pstmt.executeBatch(); // Executa o restante
                conn.commit();
            }
            catch (SQLException e) {
           if (conn != null) {
               conn.rollback();
           }
           throw e;
        }
        }
    
    }
}
