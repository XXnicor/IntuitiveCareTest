package com.intuitive.crawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseImportServiceTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private DatabaseImportService service;

    @BeforeEach
    void setup() throws Exception {
        service = new DatabaseImportService(TEST_DB_URL, "sa", "");

        // Criar schema manualmente (sem createSchema para isolar testes)
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL, "sa", ""); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS operadoras (
                    cnpj VARCHAR(14) PRIMARY KEY,
                    razao_social VARCHAR(255),
                    nome_fantasia VARCHAR(255)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS despesas_agregadas (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    cnpj_operadora VARCHAR(14),
                    codigo_conta VARCHAR(20),
                    valor_total DECIMAL(15, 2)
                )
                """);
            // Garantir que cada teste comece com tabelas vazias
            stmt.execute("TRUNCATE TABLE operadoras");
            stmt.execute("TRUNCATE TABLE despesas_agregadas");
        }
    }

    @Test
    void shouldImportAggregatedRecords() throws Exception {
        List<AggregatorService.AggregatedRecord> records = List.of(
                new AggregatorService.AggregatedRecord("123", "Op A", "OpA", 1000.50),
                new AggregatorService.AggregatedRecord("456", "Op B", "OpB", 2000.00)
        );

        service.importAgregatedRecords(records);

        try (Connection conn = DriverManager.getConnection(TEST_DB_URL, "sa", ""); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM despesas_agregadas")) {

            rs.next();
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void shouldImportOperadoras() throws Exception {
        List<DataEnricherService.Operadora> operadoras = List.of(
                new DataEnricherService.Operadora("123", "Operadora A", "Op A"),
                new DataEnricherService.Operadora("456", "Operadora B", "Op B")
        );

        service.importOperadoras(operadoras);

        try (Connection conn = DriverManager.getConnection(TEST_DB_URL, "sa", ""); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operadoras")) {

            rs.next();
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void shouldHandleLargeBatches() throws Exception {
        // 2500 registros = 2 batches completos + 500 resto
        List<AggregatorService.AggregatedRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            records.add(new AggregatorService.AggregatedRecord(
                    "CNPJ" + i, "Op" + i, "Op" + i, 100.0 * i
            ));
        }

        service.importAgregatedRecords(records);

        try (Connection conn = DriverManager.getConnection(TEST_DB_URL, "sa", ""); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM despesas_agregadas")) {

            rs.next();
            assertEquals(2500, rs.getInt(1));
        }
    }
}
