package com.intuitive.crawler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.mapping.Index;

/**
 * Enriquece dados financeiros com informações de operações cadastrais de
 * operadoras.
 *
 * trade-off: HashMap in-memory (O(1) lookup) vs. Join SQL. - Para 2000
 * operadoras: HashMap usa 100MB RAM mas é mais rapido - limitação: Não escala
 * para >100k operadoras (considerar SQL/indice)
 */
public class DataEnricherService {

    private final CsvParserService csvParser;

    public DataEnricherService(CsvParserService csvParser) {
        this.csvParser = csvParser;
    }

    /*
    * Enriquece os registros financeiros com dados cadastrais.
    *
    * @param financialRecords registros do CSV de demonstrações contabeis.
    * @param cadastroPath Path do CSV de cadastro de operadoras
    * @return Lista de registros enriquecidos.
    *@throws IOException se houver erro na leitura do CSV.
     */
    public List<EnrichedRecord> enrichRecords(
            List<Map<String, String>> financialRecords, Path cadastroPath
    ) throws IOException {

        List<Map<String, String>> cadastroRecords = csvParser.parseAndFilter(cadastroPath, Set.of(".*"));

        Map<String, Operadora> operadoraIndex = buildOperadoraIndex(cadastroRecords);

        System.out.println("Operadora index size: " + operadoraIndex.size());

        List<EnrichedRecord> enrichedRecords = financialRecords.stream()
                .map(finRecord -> {

                    String cnpjBruto = finRecord.getOrDefault("CNPJ",
                            finRecord.getOrDefault("REG_ANS",
                                    finRecord.getOrDefault("REGISTRO_ANS", "")));

                    String cnpj = normalizeCnpj(cnpjBruto);
                    Operadora operadora = operadoraIndex.get(cnpj);

                    if (operadora == null) {
                        return null;
                    }

                    String codigoConta = finRecord.getOrDefault("CD_CONTA_CONTABIL",
                            finRecord.getOrDefault("CODIGO_CONTA", ""));
                    String valor = finRecord.getOrDefault("VL_SALDO_FINAL",
                            finRecord.getOrDefault("VALOR", ""));
                    String data = finRecord.getOrDefault("DATA", "");

                    return new EnrichedRecord(
                            operadora.cnpj,
                            operadora.razaoSocial,
                            operadora.nomeFantasia,
                            codigoConta,
                            valor,
                            data
                    );

                })
                .filter(enrichedRecord -> enrichedRecord != null)
                .toList();

        int orphans = financialRecords.size() - enrichedRecords.size();
        System.out.println("DEBUG: " + enrichedRecords.size() + " registros enriquecidos, "
                + orphans + " órfãos ignorados");

        return enrichedRecords;
    }

    /*
     *Constroi indice de operadoras por CNPJ/Registro ANS.
     *
     *  @param cadastroRecords Registros do CSV de cadastro
     * @return Map<String,operadora> (chave= CNPJ normalizado)
     */
    private Map<String, Operadora> buildOperadoraIndex(
            List<Map<String, String>> cadastroRecords
    ) {
        return cadastroRecords.stream()
                .filter(record
                        -> record.containsKey("CNPJ")
                || record.containsKey("REG_ANS")
                || record.containsKey("NOME_FANTASIA")
                || record.containsKey("RAZAO_SOCIAL"))
                .map(record -> {
                    String cnpjBruto = record.getOrDefault("CNPJ",
                            record.getOrDefault("REG_ANS",
                                    record.getOrDefault("REGISTRO_ANS", "")));

                    String cnpj = normalizeCnpj(cnpjBruto);
                    String razaoSocial = record.getOrDefault("RAZAO_SOCIAL", "");
                    String nomeFantasia = record.getOrDefault("NOME_FANTASIA", "");

                    return new Operadora(cnpj, razaoSocial, nomeFantasia);

                })
                .filter(operadora -> !operadora.cnpj.isEmpty())
                .collect(Collectors.toMap(
                        operadora -> operadora.cnpj,
                        operadora -> operadora,
                        (operadora1, operadora2) -> operadora1
                ));
    }

    /* Normaliza CNPJ para o formato padrão (apenas digitos, upercase).
      *
      * @param cnpj CNPJ bruto.
      * @return CNPJ normalizado.
     */
    private String normalizeCnpj(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "").toUpperCase();
    }

    /**
     * Representa dados cadastrais de uma operadora.
     */
    public static class Operadora {

        public final String cnpj;
        public final String razaoSocial;
        public final String nomeFantasia;

        public Operadora(
                String cnpj, String razaoSocial, String nomeFantasia
        ) {
            this.cnpj = cnpj;
            this.razaoSocial = razaoSocial;
            this.nomeFantasia = nomeFantasia;
        }
    }

    /**
     * Representa um registro financeiro enriquecido com dados cadastrais.
     */
    public static class EnrichedRecord {

        public final String cnpj;
        public final String razaoSocial;
        public final String nomeFantasia;
        public final String codigoConta;
        public final String valor;
        public final String data;

        public EnrichedRecord(
                String cnpj,
                String razaoSocial,
                String nomeFantasia,
                String codigoConta,
                String valor,
                String data
        ) {
            this.cnpj = cnpj;
            this.razaoSocial = razaoSocial;
            this.nomeFantasia = nomeFantasia;
            this.codigoConta = codigoConta;
            this.valor = valor;
            this.data = data;
        }
    }

}
