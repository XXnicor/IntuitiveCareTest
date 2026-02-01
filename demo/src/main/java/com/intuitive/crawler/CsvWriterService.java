package com.intuitive.crawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Escreve CSVs em UTF-8 (formato limpo para BD/API).
 * 
 * Trade-off: UTF-8 é padrão moderno, mas ocupa mais bytes para acentos.
 * Decisão: Priorizar compatibilidade com bancos modernos e APIs REST.
 */

public class CsvWriterService {

    /**
     * Salva registros enriquecidos em CSV UTF-8.
     * 
     * @param records Lista de registros enriquecidos
     * @param outputPath Path do arquivo de saída
     * @throws IOException Se erro ao escrever arquivo
     */
    public void writeEnrichedRecords(
        List<DataEnricherService.EnrichedRecord> records, 
        Path outputPath
    ) throws IOException {
        
        // TODO: 1. Criar StringBuilder com header
        StringBuilder csv = new StringBuilder();
        csv.append("CNPJ;RAZAO_SOCIAL;NOME_FANTASIA;CODIGO_CONTA;VALOR;DATA\n");
        
        // TODO: 2. Iterar records e adicionar linhas
        for (DataEnricherService.EnrichedRecord record : records) {
            csv.append(record.cnpj).append(";")
               .append(record.razaoSocial).append(";")
               .append(record.nomeFantasia).append(";")
               .append(record.codigoConta).append(";")
               .append(record.valor).append(";")
               .append(record.data).append("\n");
        }
        
        // TODO: 3. Escrever em UTF-8
        Files.writeString(outputPath, csv.toString(), StandardCharsets.UTF_8);
    }

}
