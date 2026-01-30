package com.intuitive.crawler;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapeia headers variaveis dos CSVs da ANS para um schema unificado.
 * 
 * Trade-off:Usa Map stático (eager loading) vs. Regex dinamico (lazy loading).
 * - Vantagem: Performance O(1) e controle explicito de aliases.
 * - Desvantagem: Requer manutenção manual ao encontrar novos headers.
 */

public class CsvSchemaMapper {

    private final Map<String, String> headerAliases;

    public CsvSchemaMapper() {
        this.headerAliases = new HashMap<>();
        this.headerAliases.put("CD_CONTA_CONTABIL", "CODIGO_CONTA");
        this.headerAliases.put("CONTA_CONTABIL", "CODIGO_CONTA");
        this.headerAliases.put("REG_ANS", "REGISTRO_ANS");
        this.headerAliases.put("REGISTRO", "REGISTRO_ANS");
        this.headerAliases.put("DATA_DOCUMENTO", "DATA");
    }

    /**
     * Normaliza um header para o schema padrão.
     * 
     * @param originalHeader Header do CSV original
     * @return Header normalizado
     * @throws IllegalArgumentException se header não mapeado
     */
    public String normalizeHeader(String originalHeader) {
       if(originalHeader == null || originalHeader.isBlank()) {
           throw new IllegalArgumentException("Header não pode ser nulo");
       }
       String normalized = originalHeader.trim().toUpperCase();

         if (!headerAliases.containsKey(normalized)) {
             throw new IllegalArgumentException("Header não mapeado: " + originalHeader);
         }

         return headerAliases.get(normalized);
    }

    /**
     * Retorna o schema padrão (headers esperados no consolidado).
     * 
     * @return Array com headers na ordem correta
     */

    public String[] getStandardSchema() {
        return new String[] {
            "CODIGO_CONTA",
            "REGISTRO_ANS",
            "DATA"
        };
}
}
