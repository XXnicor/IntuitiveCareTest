package com.intuitive.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/*
* Parser de CSV das demonstrações contábeis da ANS.
*
* Trade-off: Usa streaming (BufferedReader) para processar arquivos grandes.
* Alternativa rejeitada: Files.readAlllines () causa OOM em CSVs > 50MB.
*
* Descisão: Encoding ISO-8859-1 (padrão gov.br), mas configuravel.
*
* Parser Strategy:
* - Usa String.split(";") para simplicidade (CSV simples, sem aspas).
* - adequado para CSVs SIMPLES (sem valores com ';' entre aspas).
* - Limitação: Não suporta campos com escape (ex: "valor;com;vírgula").
* - Refatoração futura: Migrar para CSVParser (Apache Commons) para casos complexos.
 */
public class CsvParserService {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    /*
     * Le CSV e retorna apenas linhas que contenham keywords.
     *
     * @param csvFile Path do arquivo CSV
     * @param keywords Palavras-chave para filtrar (case-insensitive)
     * @return Lista de mapas (chave = header normalizado, valor = célula)
     * @throws IOException Se erro ao ler arquivo
     */
    public List<Map<String, String>> parseAndFilter(Path csvFile, Set<String> keywords) throws IOException {

        if (csvFile == null || !Files.exists(csvFile)) {
            throw new IllegalArgumentException("O arquivo CSV não existe: " + csvFile);
        }

        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("As palavras-chave devem ser fornecidas.");
        }

        List<Map<String, String>> filteredRows = new java.util.ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, DEFAULT_CHARSET)) {

            String headerLine = reader.readLine();

            if (headerLine == null) {
                return filteredRows; // Arquivo vazio
            }
            String[] headers = headerLine.split(";");

            String line;

            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(";");
                Map<String, String> rowMap = new java.util.HashMap<>();
                boolean containsKeyword = false;

                for (int i = 0; i < headers.length && i < cells.length; i++) {
                    String normalizedHeader = headers[i].trim().toUpperCase();
                    String cellValue = cells[i].trim();
                    rowMap.put(normalizedHeader, cellValue);
                    // Verifica se a célula contém alguma keyword
                    for (String keyword : keywords) {
                        // tratar keywords como regex (case-insensitive). Ex: ".*" casa tudo
                        try {
                            Pattern p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
                            if (p.matcher(cellValue).find()) {
                                containsKeyword = true;
                                break;
                            }
                        } catch (Exception e) {
                            // em caso de regex inválida, fallback para contains simples
                            if (cellValue.toLowerCase().contains(keyword.toLowerCase())) {
                                containsKeyword = true;
                                break;
                            }
                        }
                    }
                }
                if (containsKeyword) {
                    filteredRows.add(rowMap);
                }
            }
            return filteredRows;
        }
    }
}
