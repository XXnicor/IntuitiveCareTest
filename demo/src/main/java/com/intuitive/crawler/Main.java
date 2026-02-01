package com.intuitive.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.h2.tools.Server;

import com.intuitive.crawler.DataEnricherService.EnrichedRecord;

public class Main {

    public static void main(String[] args) throws IOException, SQLException {
        // Inicia o console web e o servidor TCP do H2
        Server webServer = Server.createWebServer("-webPort", "8082", "-webAllowOthers").start();
        Server tcpServer = Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers").start();
        System.out.println("H2 console disponível em: http://localhost:8082");
        System.out.println("H2 TCP server disponível em: tcp://localhost:9092");

        // Cria o banco e uma tabela de teste via JDBC (conecta ao servidor TCP iniciado)
        String jdbcUrl = "jdbc:h2:tcp://localhost/~/testdb"; // cria C:/Users/<user>/testdb
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", ""); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS teste(id INT PRIMARY KEY, nome VARCHAR(255))");
            System.out.println("Banco 'testdb' e tabela 'teste' garantidos (criados se não existiam).");
        } catch (SQLException e) {
            System.err.println("Falha ao criar banco/tabela: " + e.getMessage());
            throw e;
        }

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Criar CSV fake de cadastro
        Path tempCadastro = Files.createTempFile("cadastro", ".csv");
        Files.writeString(tempCadastro,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n" + "12345678000199;Operadora Teste;Teste Saúde\n");

        // Criar registros financeiros fake
        List<Map<String, String>> financial = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA",
                        "2024-12-31"));

        // Enriquecer
        List<EnrichedRecord> result = enricher.enrichRecords(financial, tempCadastro);

        System.out.println("Resultado: " + result.size() + " registros");
        if (!result.isEmpty()) {
            EnrichedRecord first = result.get(0);
            System.out.println("Razão Social: " + first.razaoSocial);
            System.out.println("Nome Fantasia: " + first.nomeFantasia);
        }

        System.out.println("Pressione ENTER para encerrar e parar o H2...");
        System.in.read();

        webServer.stop();
        tcpServer.stop();
    }
}
