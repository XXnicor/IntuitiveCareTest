package com.intuitive.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    // Configuração do banco MySQL (mesma do application.properties)
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/registrosans?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Amateresu123.";
    private static final String DOWNLOAD_DIR = "downloads_ans";
    // URL alternativa - usando URL direta conhecida da ANS (demonstrações contábeis)
    private static final String ANS_ZIP_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/2024/4T2024.zip";

    public static void main(String[] args) {
        System.out.println("=== Crawler ANS - Download e Importação de Dados ===\n");

        try {
            // Testa conexão com o banco
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("✓ Conexão com MySQL estabelecida com sucesso!\n");
            }

            // Cria diretório de downloads
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            Files.createDirectories(downloadPath);
            System.out.println("✓ Diretório de downloads criado: " + downloadPath.toAbsolutePath() + "\n");

            // Inicializa serviços
            AnsCrawlerService crawler = new AnsCrawlerService();
            FileManagerService fileManager = new FileManagerService();
            CsvParserService parser = new CsvParserService();
            DataEnricherService enricher = new DataEnricherService(parser);
            DatabaseImportService importer = new DatabaseImportService(JDBC_URL, DB_USER, DB_PASSWORD);
            AggregatorService aggregator = new AggregatorService();

            // Passo 1: Buscar e baixar dados financeiros (trimestres recentes)
            System.out.println("═══ PASSO 1: Buscando trimestres mais recentes da ANS ═══");

            String zipUrl;
            try {
                List<String> recentZips = crawler.findRecentTrimesters(1);
                if (!recentZips.isEmpty()) {
                    zipUrl = recentZips.get(0);
                    System.out.println("✓ Encontrado arquivo mais recente via crawler");
                } else {
                    System.out.println("⚠ Crawler não encontrou arquivos, usando URL direta: " + ANS_ZIP_URL);
                    zipUrl = ANS_ZIP_URL;
                }
            } catch (Exception e) {
                System.out.println("⚠ Erro ao fazer crawling: " + e.getMessage());
                System.out.println("  Usando URL direta: " + ANS_ZIP_URL);
                zipUrl = ANS_ZIP_URL;
            }

            // Passo 2: Download do arquivo financeiro
            System.out.println("\n═══ PASSO 2: Baixando dados financeiros ═══");
            String zipFileName = zipUrl.substring(zipUrl.lastIndexOf('/') + 1);
            Path zipPath = downloadPath.resolve(zipFileName);

            System.out.println("Baixando: " + zipFileName + " ...");
            fileManager.downloadFile(zipUrl, zipPath);
            System.out.println("✓ Download concluído: " + zipPath.toAbsolutePath());

            // Passo 3: Descompactar arquivo
            System.out.println("\n═══ PASSO 3: Descompactando arquivo ═══");
            Path extractPath = downloadPath.resolve("extracted");
            fileManager.unzipFile(zipPath, extractPath);
            System.out.println("✓ Arquivo descompactado em: " + extractPath.toAbsolutePath());

            // Passo 4: Encontrar CSV dentro do ZIP extraído
            System.out.println("\n═══ PASSO 4: Procurando arquivos CSV ═══");
            List<Path> csvFiles = Files.walk(extractPath)
                    .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                    .toList();

            if (csvFiles.isEmpty()) {
                System.err.println("✗ Nenhum arquivo CSV encontrado no ZIP");
                System.exit(1);
            }

            Path financialCsv = csvFiles.get(0);
            System.out.println("✓ CSV encontrado: " + financialCsv.getFileName());

            // Passo 5: Baixar arquivo de cadastro de operadoras
            System.out.println("\n═══ PASSO 5: Baixando cadastro de operadoras ═══");
            System.out.println("NOTA: Usando lista de operadoras do próprio arquivo financeiro");
            System.out.println("(O cadastro completo requer análise adicional da estrutura da ANS)");

            // Passo 6: Parsear dados financeiros
            System.out.println("\n═══ PASSO 6: Parseando dados financeiros ═══");
            List<Map<String, String>> financialRecords = parser.parseAndFilter(
                    financialCsv,
                    Set.of(".*") // Pega todos os registros
            );
            System.out.println("✓ " + financialRecords.size() + " registros parseados");

            if (financialRecords.isEmpty()) {
                System.out.println("⚠ Nenhum registro encontrado. Verifique o formato do CSV.");
                System.exit(0);
            }

            // Debug: Mostrar colunas disponíveis
            if (!financialRecords.isEmpty()) {
                System.out.println("\nColunas disponíveis no CSV:");
                financialRecords.get(0).keySet().forEach(k -> System.out.println("  - " + k));
            }

            // Passo 7: Criar índice de operadoras do próprio arquivo
            System.out.println("\n═══ PASSO 7: Extraindo informações de operadoras ═══");
            List<DataEnricherService.Operadora> operadoras = financialRecords.stream()
                    .map(record -> {
                        // O CSV da ANS usa REG_ANS como identificador da operadora
                        String regAns = record.getOrDefault("REG_ANS", "").trim();
                        String descricao = record.getOrDefault("DESCRICAO", "").trim();

                        // Usar REG_ANS como identificador (CNPJ no contexto do DataEnricherService)
                        return new DataEnricherService.Operadora(regAns, descricao, descricao);
                    })
                    .filter(op -> op.cnpj != null && !op.cnpj.isEmpty())
                    .collect(java.util.stream.Collectors.toMap(
                            op -> op.cnpj, // Chave: REG_ANS único
                            op -> op, // Valor: objeto Operadora
                            (op1, op2) -> op1 // Se houver duplicata, mantém o primeiro
                    ))
                    .values()
                    .stream()
                    .toList();

            System.out.println("✓ " + operadoras.size() + " operadoras únicas identificadas");

            // Passo 8: Importar operadoras
            System.out.println("\n═══ PASSO 8: Importando operadoras no banco ═══");
            importer.importOperadoras(operadoras);

            // Passo 9: Enriquecer e agregar dados
            System.out.println("\n═══ PASSO 9: Agregando despesas por operadora ═══");
            // Criar CSV temporário de cadastro para o enricher
            Path tempCadastro = Files.createTempFile("cadastro", ".csv");
            StringBuilder cadastroCsv = new StringBuilder("REG_ANS;RAZAO_SOCIAL;NOME_FANTASIA\n");
            operadoras.forEach(op -> cadastroCsv.append(op.cnpj).append(";")
                    .append(op.razaoSocial != null ? op.razaoSocial.replace(";", " ") : "").append(";")
                    .append(op.nomeFantasia != null ? op.nomeFantasia.replace(";", " ") : "").append("\n"));
            Files.writeString(tempCadastro, cadastroCsv.toString());

            List<DataEnricherService.EnrichedRecord> enriched = enricher.enrichRecords(financialRecords, tempCadastro);
            System.out.println("✓ " + enriched.size() + " registros enriquecidos");

            List<AggregatorService.AggregatedRecord> aggregated = aggregator.aggregateByOperadoraAndConta(enriched);
            System.out.println("✓ " + aggregated.size() + " registros agregados");

            // Passo 10: Importar dados agregados
            System.out.println("\n═══ PASSO 10: Importando despesas agregadas no banco ═══");
            importer.importAgregatedRecords(aggregated);

            // Limpar arquivo temporário
            Files.deleteIfExists(tempCadastro);

            System.out.println("\n" + "═".repeat(60));
            System.out.println("✓✓✓ IMPORTAÇÃO CONCLUÍDA COM SUCESSO! ✓✓✓");
            System.out.println("═".repeat(60));
            System.out.println("\nEstatísticas:");
            System.out.println("  • Operadoras únicas: " + operadoras.size());
            System.out.println("  • Registros financeiros: " + financialRecords.size());
            System.out.println("  • Registros agregados: " + aggregated.size());
            System.out.println("\nAPI disponível em: http://localhost:8081/api/operadoras");
            System.out.println("\nArquivos baixados em: " + downloadPath.toAbsolutePath());

        } catch (SQLException e) {
            System.err.println("\n✗ Erro ao conectar com o MySQL:");
            System.err.println("  " + e.getMessage());
            System.err.println("\nVerifique se:");
            System.err.println("  1. O MySQL está rodando");
            System.err.println("  2. O banco 'registrosans' existe");
            System.err.println("  3. As credenciais estão corretas");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("\n✗ Erro ao baixar ou processar arquivos:");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n✗ Erro durante a execução:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
