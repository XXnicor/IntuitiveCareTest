package com.intuitive.crawler;

import com.intuitive.crawler.DataEnricherService.EnrichedRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEnricherServiceTest {

    @Test
    void shouldEnrichRecordsWithCadastroData(@TempDir Path tempDir) throws Exception {
        // Arrange: Criar CSV de cadastro com 2 operadoras
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12.345.678/0001-99;Operadora Alpha LTDA;Alpha Saúde\n"
                + "98.765.432/0001-11;Operadora Beta SA;Beta Planos\n",
                 StandardCharsets.ISO_8859_1);

        // Arrange: Criar registros financeiros (CNPJs normalizados)
        List<Map<String, String>> financialRecords = List.of(
                Map.of(
                        "CNPJ", "12345678000199",
                        "CODIGO_CONTA", "3.1.1.00",
                        "VALOR", "10000.50",
                        "DATA", "2024-12-31"
                ),
                Map.of(
                        "CNPJ", "98765432000111",
                        "CODIGO_CONTA", "3.1.2.00",
                        "VALOR", "20000.75",
                        "DATA", "2024-12-31"
                )
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act: Enriquecer os registros
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Verificar quantidade e conteúdo
        assertEquals(2, enriched.size(), "Deve enriquecer 2 registros");

        // TODO: Verificar primeiro registro (Alpha)
        EnrichedRecord first = enriched.get(0);
        assertEquals("12345678000199", first.cnpj);
        assertEquals("Operadora Alpha LTDA", first.razaoSocial);
        assertEquals("Alpha Saúde", first.nomeFantasia);
        assertEquals("3.1.1.00", first.codigoConta);
        assertEquals("10000.50", first.valor);
        assertEquals("2024-12-31", first.data);

        // TODO: Verificar segundo registro (Beta)
        EnrichedRecord second = enriched.get(1);
        assertEquals("98765432000111", second.cnpj);
        assertEquals("Operadora Beta SA", second.razaoSocial);
        assertEquals("Beta Planos", second.nomeFantasia);
    }

    @Test
    void shouldHandleOrphanRecords(@TempDir Path tempDir) throws Exception {
        // TODO: Arrange - CSV cadastro com apenas 1 operadora
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12345678000199;Operadora Alpha;Alpha Saúde\n",
                 StandardCharsets.ISO_8859_1);

        // TODO: Arrange - 2 registros financeiros (1 válido, 1 órfão)
        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31"),
                Map.of("CNPJ", "99999999999999", "CODIGO_CONTA", "3.1.2", "VALOR", "2000", "DATA", "2024-12-31") // Órfão!
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Apenas 1 registro deve ser enriquecido (o órfão é ignorado)
        assertEquals(1, enriched.size(), "Deve ignorar registros órfãos");
        assertEquals("12345678000199", enriched.get(0).cnpj);
    }

    @Test
    void shouldNormalizeCnpjWithSpecialCharacters(@TempDir Path tempDir) throws Exception {
        // TODO: Arrange - Cadastro com CNPJ formatado
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12.345.678/0001-99;Operadora Teste;Teste Saúde\n" // ← Com formatação!
                ,
                 StandardCharsets.ISO_8859_1);

        // TODO: Arrange - Registro financeiro com CNPJ SEM formatação
        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Deve fazer match mesmo com formatações diferentes
        assertEquals(1, enriched.size(), "Deve normalizar CNPJs diferentes");
        assertEquals("Operadora Teste", enriched.get(0).razaoSocial);
    }

    @Test
    void shouldReturnEmptyListWhenCadastroIsEmpty(@TempDir Path tempDir) throws Exception {
        // TODO: Arrange - CSV cadastro vazio (apenas header)
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile, "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n", StandardCharsets.ISO_8859_1);

        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Nenhum registro deve ser enriquecido
        assertTrue(enriched.isEmpty(), "Deve retornar lista vazia quando cadastro está vazio");
    }

    @Test
    void shouldHandleRecordsWithRegAnsInsteadOfCnpj(@TempDir Path tempDir) throws Exception {
        // TODO: Arrange - CSV usando REG_ANS ao invés de CNPJ
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "REG_ANS;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + // ← Usando REG_ANS!
                "12345678000199;Operadora Teste;Teste Saúde\n",
                 StandardCharsets.ISO_8859_1);

        List<Map<String, String>> financialRecords = List.of(
                Map.of("REG_ANS", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Deve funcionar com REG_ANS também
        assertEquals(1, enriched.size(), "Deve aceitar REG_ANS como alternativa ao CNPJ");
        assertEquals("Operadora Teste", enriched.get(0).razaoSocial);
    }
}
