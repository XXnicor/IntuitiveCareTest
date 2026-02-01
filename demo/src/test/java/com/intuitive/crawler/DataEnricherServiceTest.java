package com.intuitive.crawler;

import com.intuitive.crawler.DataEnricherService.EnrichedRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataEnricherServiceTest {

    @Test
    void shouldEnrichRecordsWithCadastroData(@TempDir Path tempDir) throws Exception {
        // Arrange: Criar CSV em ISO-8859-1 (simulando arquivo da ANS)
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        String csvContent
                = "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12.345.678/0001-99;Operadora Saúde Ltda;Saúde Total\n"
                + // ú sem problemas
                "98.765.432/0001-11;Planos Médicos SA;Bem Estar\n";

        // TODO: Escrever explicitamente em ISO-8859-1
        Files.writeString(cadastroFile, csvContent, StandardCharsets.ISO_8859_1);

        // Arrange: Registros financeiros (já estão em memória como UTF-16)
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

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert
        assertEquals(2, enriched.size());

        EnrichedRecord first = enriched.get(0);
        assertEquals("12345678000199", first.cnpj);
        assertEquals("Operadora Saúde Ltda", first.razaoSocial);  // ú deve funcionar
        assertEquals("Saúde Total", first.nomeFantasia);
    }

    @Test
    void shouldHandleSpecialCharactersInIso88591(@TempDir Path tempDir) throws Exception {
        // TODO: Testar caracteres críticos do português (ç, ã, õ, é, á, etc)
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        String csvContent
                = "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12345678000199;Operações Médicas Ção LTDA;Atenção Saúde\n";  // ç, ã, ç

        Files.writeString(cadastroFile, csvContent, StandardCharsets.ISO_8859_1);

        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert: Caracteres especiais devem ser lidos corretamente
        assertEquals(1, enriched.size());
        assertTrue(enriched.get(0).razaoSocial.toLowerCase().contains("ção"),
                "Deve ler ç corretamente");
        assertTrue(enriched.get(0).nomeFantasia.toLowerCase().contains("ção"),
                "Deve ler ã corretamente");
    }

    @Test
    void shouldHandleOrphanRecords(@TempDir Path tempDir) throws Exception {
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12345678000199;Operadora Alpha;Alpha Saúde\n",
                StandardCharsets.ISO_8859_1 // ← Importante!
        );

        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31"),
                Map.of("CNPJ", "99999999999999", "CODIGO_CONTA", "3.1.2", "VALOR", "2000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert
        assertEquals(1, enriched.size());
    }

    @Test
    void shouldNormalizeCnpjWithSpecialCharacters(@TempDir Path tempDir) throws Exception {
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12.345.678/0001-99;Operadora Teste;Teste Saúde\n",
                StandardCharsets.ISO_8859_1
        );

        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert
        assertEquals(1, enriched.size());
        assertEquals("Operadora Teste", enriched.get(0).razaoSocial);
    }

    @Test
    void shouldReturnEmptyListWhenCadastroIsEmpty(@TempDir Path tempDir) throws Exception {
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n",
                StandardCharsets.ISO_8859_1
        );

        List<Map<String, String>> financialRecords = List.of(
                Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert
        assertTrue(enriched.isEmpty());
    }

    @Test
    void shouldHandleRecordsWithRegAnsInsteadOfCnpj(@TempDir Path tempDir) throws Exception {
        Path cadastroFile = tempDir.resolve("cadastro.csv");
        Files.writeString(cadastroFile,
                "REG_ANS;RAZAO_SOCIAL;NOME_FANTASIA\n"
                + "12345678000199;Operadora Teste;Teste Saúde\n",
                StandardCharsets.ISO_8859_1
        );

        List<Map<String, String>> financialRecords = List.of(
                Map.of("REG_ANS", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
        );

        CsvParserService parser = new CsvParserService();
        DataEnricherService enricher = new DataEnricherService(parser);

        // Act
        List<EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, cadastroFile);

        // Assert
        assertEquals(1, enriched.size());
        assertEquals("Operadora Teste", enriched.get(0).razaoSocial);
    }
}
