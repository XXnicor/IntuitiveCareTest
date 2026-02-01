package com.intuitive.crawler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.intuitive.crawler.AggregatorService.AggregatedRecord;
import com.intuitive.crawler.DataEnricherService.EnrichedRecord;

public class AggregatorServiceTest {

    @Test
    void shouldAggregateRecordsByOperadoraAndConta() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();
        
        List<EnrichedRecord> input = List.of(
            new EnrichedRecord("12345678", "Operadora A", "Op A", "3111", "1.000,00", "2024-01"),
            new EnrichedRecord("12345678", "Operadora A", "Op A", "3111", "500,50", "2024-02"),
            new EnrichedRecord("12345678", "Operadora A", "Op A", "3112", "200,00", "2024-01"),
            new EnrichedRecord("87654321", "Operadora B", "Op B", "3111", "300,00", "2024-01")
        );

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(input);

        // Assert
        assertEquals(3, result.size(), "Devem existir 3 grupos distintos");

        // Verificar grupo (12345678, 3111) = 1000 + 500.50 = 1500.50
        AggregatedRecord grupo1 = result.stream()
            .filter(r -> r.cnpjOperadora.equals("12345678") && r.codigoConta.equals("3111"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Grupo (12345678, 3111) não encontrado"));
        
        assertEquals(1500.50, grupo1.totalValor, 0.01, "Soma do grupo deve ser 1500.50");
        assertEquals("Operadora A", grupo1.razaoSocial, "Deve preservar razão social");

        // Verificar grupo (12345678, 3112)
        AggregatedRecord grupo2 = result.stream()
            .filter(r -> r.cnpjOperadora.equals("12345678") && r.codigoConta.equals("3112"))
            .findFirst()
            .orElseThrow();
        
        assertEquals(200.00, grupo2.totalValor, 0.01);
    }

    @Test
    void shouldIgnoreInvalidNumericValues() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();
        
        List<EnrichedRecord> input = List.of(
            new EnrichedRecord("12345678", "Op A", "Op A", "3111", "INVALIDO", "2024-01"),
            new EnrichedRecord("12345678", "Op A", "Op A", "3111", "", "2024-02"),
            new EnrichedRecord("12345678", "Op A", "Op A", "3111", "100,00", "2024-03")
        );

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(input);

        // Assert
        assertEquals(1, result.size(), "Deve ter apenas 1 grupo");
        assertEquals(100.00, result.get(0).totalValor, 0.01, 
            "Valores inválidos devem ser tratados como 0.0");
    }

    @Test
    void shouldHandleBrazilianNumberFormat() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();
        
        List<EnrichedRecord> input = List.of(
            new EnrichedRecord("123", "Op X", "X", "3111", "1.234.567,89", "2024-01"),
            new EnrichedRecord("123", "Op X", "X", "3111", "100,11", "2024-02")
        );

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(input);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1234668.00, result.get(0).totalValor, 0.01, 
            "Deve somar corretamente valores com formato brasileiro (1.234.567,89 + 100,11)");
    }

    @Test
    void shouldGroupByBothCnpjAndConta() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();
        
        List<EnrichedRecord> input = List.of(
            new EnrichedRecord("111", "Op 1", "1", "3111", "100,00", "2024-01"),
            new EnrichedRecord("111", "Op 1", "1", "3112", "200,00", "2024-01"), // Conta diferente
            new EnrichedRecord("222", "Op 2", "2", "3111", "300,00", "2024-01")  // CNPJ diferente
        );

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(input);

        // Assert
        assertEquals(3, result.size(), 
            "Mesmo CNPJ com contas diferentes e CNPJs diferentes devem gerar grupos separados");
        
        assertTrue(result.stream().anyMatch(r -> 
            r.cnpjOperadora.equals("111") && r.codigoConta.equals("3111")));
        assertTrue(result.stream().anyMatch(r -> 
            r.cnpjOperadora.equals("111") && r.codigoConta.equals("3112")));
        assertTrue(result.stream().anyMatch(r -> 
            r.cnpjOperadora.equals("222") && r.codigoConta.equals("3111")));
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(List.of());

        // Assert
        assertTrue(result.isEmpty(), "Input vazio deve retornar lista vazia");
    }

    @Test
    void shouldSortResultsByCnpj() {
        // Arrange
        AggregatorService aggregator = new AggregatorService();
        
        List<EnrichedRecord> input = List.of(
            new EnrichedRecord("999", "Op Z", "Z", "3111", "100,00", "2024-01"),
            new EnrichedRecord("111", "Op A", "A", "3111", "100,00", "2024-01"),
            new EnrichedRecord("555", "Op M", "M", "3111", "100,00", "2024-01")
        );

        // Act
        List<AggregatedRecord> result = aggregator.aggregateByOperadoraAndConta(input);

        // Assert
        assertEquals("111", result.get(0).cnpjOperadora, "Primeiro deve ser o menor CNPJ");
        assertEquals("555", result.get(1).cnpjOperadora);
        assertEquals("999", result.get(2).cnpjOperadora, "Último deve ser o maior CNPJ");
    }
}