package com.intuitive.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.intuitive.crawler.DataEnricherService.EnrichedRecord;

public class Main {

     public static void main(String[] args) throws IOException {
    CsvParserService parser = new CsvParserService();
    DataEnricherService enricher = new DataEnricherService(parser);
    
    // Criar CSV fake de cadastro
    Path tempCadastro = Files.createTempFile("cadastro", ".csv");
    Files.writeString(tempCadastro, 
        "CNPJ;RAZAO_SOCIAL;NOME_FANTASIA\n" +
        "12345678000199;Operadora Teste;Teste Saúde\n"
    );
    
    // Criar registros financeiros fake
    List<Map<String, String>> financial = List.of(
        Map.of("CNPJ", "12345678000199", "CODIGO_CONTA", "3.1.1", "VALOR", "1000", "DATA", "2024-12-31")
    );
    
    // Enriquecer
    List<EnrichedRecord> result = enricher.enrichRecords(financial, tempCadastro);
    
    System.out.println("Resultado: " + result.size() + " registros");
    if (!result.isEmpty()) {
        EnrichedRecord first = result.get(0);
        System.out.println("Razão Social: " + first.razaoSocial);
        System.out.println("Nome Fantasia: " + first.nomeFantasia);
    }
  }
}
