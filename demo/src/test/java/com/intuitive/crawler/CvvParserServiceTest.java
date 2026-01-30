package com.intuitive.crawler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CvvParserServiceTest {

    @Test
    void shouldFilterLinesByKeywords(@TempDir Path tempDir) throws Exception {
        // Arrange: criar CSV de teste com delimitador ';'
        Path csvFile = tempDir.resolve("test.csv");
        String csvContent = """
                Tipo;Descricao;Valor
                DESPESAS;Sinistros pagos;1000000
                RECEITA;Mensalidades;5000000
                DESPESAS;Despesas administrativas;200000
                ATIVO;Disponibilidades;300000
                """;
        Files.writeString(csvFile, csvContent);

        CsvParserService parser = new CsvParserService();
        Set<String> keywords = Set.of("DESPESAS");

        // Act
        List<Map<String, String>> result = parser.parseAndFilter(csvFile, keywords);

        // Assert
        assertEquals(2, result.size(), "Deve retornar 2 linhas com DESPESAS");
        
        Map<String, String> firstRow = result.get(0);
        assertTrue(firstRow.containsKey("TIPO"), "Headers devem estar em UPPERCASE");
        assertEquals("DESPESAS", firstRow.get("TIPO"));
        assertEquals("Sinistros pagos", firstRow.get("DESCRICAO"));
    }

    @Test
    void shouldHandleEmptyFile(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, "Header1;Header2\n");

        CsvParserService parser = new CsvParserService();
        List<Map<String, String>> result = parser.parseAndFilter(csvFile, Set.of("TESTE"));

        assertTrue(result.isEmpty(), "Arquivo vazio deve retornar lista vazia");
    }

    @Test
    void shouldThrowExceptionForInvalidFile(@TempDir Path tempDir) {
        Path invalidFile = tempDir.resolve("nao-existe.csv");
        CsvParserService parser = new CsvParserService();

        assertThrows(IllegalArgumentException.class, () -> 
            parser.parseAndFilter(invalidFile, Set.of("TESTE"))
        );
    }

    @Test
    void shouldBeCaseInsensitiveForKeywords(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("test.csv");
        String csvContent = """
                Tipo;Descricao
                despesas;Teste minúscula
                DESPESAS;Teste maiúscula
                Despesas;Teste mista
                """;
        Files.writeString(csvFile, csvContent);

        CsvParserService parser = new CsvParserService();
        List<Map<String, String>> result = parser.parseAndFilter(csvFile, Set.of("DESPESAS"));

        assertEquals(3, result.size(), "Deve encontrar todas as variações de case");
    }

}
