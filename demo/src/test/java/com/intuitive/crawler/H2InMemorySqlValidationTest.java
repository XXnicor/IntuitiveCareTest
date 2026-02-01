package com.intuitive.crawler;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

class H2InMemorySqlValidationTest {

    @Test
    void validateSqlScripts() throws Exception {
        String jdbc = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(jdbc, "sa", "")) {
            // Carrega schema.sql como texto para pré-processar índices inline
            try (InputStream schemaIs = getClass().getResourceAsStream("/sql/schema.sql")) {
                if (schemaIs == null) {
                    throw new IllegalStateException("schema.sql não encontrado no classpath");
                }
                String schema = new String(schemaIs.readAllBytes(), StandardCharsets.UTF_8);
                // Remover linhas INDEX dentro de CREATE TABLE para compatibilidade com H2
                schema = schema.replaceAll("(?m)^\\s*INDEX\\b.*$", "");
                // Remover comentários de linha
                schema = schema.replaceAll("(?m)^--.*$", "");
                // Remover vírgula final antes de fechamento de parênteses
                schema = schema.replaceAll(",\\s*\\)", ")");

                RunScript.execute(conn, new StringReader(schema));
            }

            // Lê e executa cada statement das queries para validar sintaxe/executabilidade
            try (InputStream queriesIs = getClass().getResourceAsStream("/sql/queries_analiticas.sql")) {
                if (queriesIs == null) {
                    throw new IllegalStateException("queries_analiticas.sql não encontrado no classpath");
                }
                String sql = new String(queriesIs.readAllBytes(), StandardCharsets.UTF_8);
                String[] parts = sql.split(";");
                for (String p : parts) {
                    // remove comentários de linha e trim
                    String stmt = p.replaceAll("(?m)^--.*", "").trim();
                    if (stmt.isEmpty()) {
                        continue;
                    }
                    try (Statement s = conn.createStatement()) {
                        if (stmt.trim().toUpperCase().startsWith("SELECT")) {
                            try (ResultSet rs = s.executeQuery(stmt)) {
                                // iterar levemente para garantir execução
                                if (rs.next()) {
                                    /* ok */
                                }
                            }
                        } else {
                            s.execute(stmt);
                        }
                    }
                }
            }
        }
    }
}
