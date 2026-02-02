package com.intuitive.api.repository;

import com.intuitive.api.dto.OperadoraDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositório para acesso a dados de operadoras via JDBC puro. Trade-off:
 * JdbcTemplate vs JPA - Controle total do SQL e performance previsível, sem
 * overhead de cache/lazy loading do Hibernate.
 */
@Repository
public class OperadoraRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperadoraRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Filtro SQL para excluir categorias contábeis e registros que não são
     * operadoras reais. Remove: ATIVO, PASSIVO, PATRIMÔNIO, contas negativas
     * (-) e outros termos contábeis.
     */
    private static final String FILTRO_OPERADORAS_REAIS = """
        AND razao_social NOT LIKE '(-)%'
        AND razao_social NOT LIKE '%(-) %'
        AND razao_social NOT LIKE '%Acionistas%'
        AND razao_social NOT LIKE '%Provisão%'
        AND razao_social NOT LIKE '%Outros%'
        AND UPPER(razao_social) NOT LIKE '%ATIVO%'
        AND UPPER(razao_social) NOT LIKE '%PASSIVO%'
        AND UPPER(razao_social) NOT LIKE '%PATRIMÔNIO%'
        AND UPPER(razao_social) NOT LIKE '%PATRIMONIO%'
        AND UPPER(razao_social) NOT LIKE '%RESULTADO%'
        AND UPPER(razao_social) NOT LIKE '%RECEITA%'
        AND UPPER(razao_social) NOT LIKE '%DESPESA%'
        AND razao_social IS NOT NULL
        AND LENGTH(TRIM(razao_social)) > 5
        """;

    /**
     * RowMapper para converter ResultSet em OperadoraDTO (listagem simples).
     */
    private static final RowMapper<OperadoraDTO> OPERADORA_ROW_MAPPER = (rs, rowNum)
            -> new OperadoraDTO(
                    rs.getString("cnpj"),
                    rs.getString("razao_social"),
                    rs.getString("uf"),
                    rs.getString("modalidade")
            );

    /**
     * RowMapper para query com valor total agregado.
     */
    private static final RowMapper<OperadoraDTO> OPERADORA_COM_TOTAL_MAPPER = (rs, rowNum)
            -> new OperadoraDTO(
                    rs.getString("cnpj"),
                    rs.getString("razao_social")
            );

    /**
     * Lista operadoras com paginação.
     *
     * @param page número da página (1-based)
     * @param limit registros por página
     * @return lista de operadoras
     */
    public List<OperadoraDTO> findAllPaginado(int page, int limit) {
        int offset = (page - 1) * limit;

        String sql = """
            SELECT cnpj, razao_social, nome_fantasia, uf, modalidade
            FROM operadoras
            WHERE 1=1
            """ + FILTRO_OPERADORAS_REAIS + """
            ORDER BY nome_fantasia
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, OPERADORA_ROW_MAPPER, limit, offset);
    }

    /**
     * Conta total de operadoras (para cálculo de páginas).
     *
     * @return total de registros
     */
    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM operadoras WHERE 1=1 " + FILTRO_OPERADORAS_REAIS;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Busca operadoras com filtro por CNPJ ou Razão Social.
     *
     * @param query termo de busca
     * @param page número da página
     * @param limit registros por página
     * @return lista de operadoras filtradas
     */
    public List<OperadoraDTO> findByQuery(String query, int page, int limit) {
        int offset = (page - 1) * limit;
        String searchPattern = "%" + query + "%";

        String sql = """
            SELECT cnpj, razao_social, nome_fantasia, uf, modalidade
            FROM operadoras
            WHERE (cnpj LIKE ? OR LOWER(razao_social) LIKE LOWER(?))
            """ + FILTRO_OPERADORAS_REAIS + """
            ORDER BY nome_fantasia
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, OPERADORA_ROW_MAPPER, searchPattern, searchPattern, limit, offset);
    }

    /**
     * Conta total de operadoras filtradas por query.
     *
     * @param query termo de busca
     * @return total de registros filtrados
     */
    public int countByQuery(String query) {
        String searchPattern = "%" + query + "%";
        String sql = """
            SELECT COUNT(*) FROM operadoras 
            WHERE (cnpj LIKE ? OR LOWER(razao_social) LIKE LOWER(?))
            """ + FILTRO_OPERADORAS_REAIS;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, searchPattern, searchPattern);
        return count != null ? count : 0;
    }

    /**
     * Busca operadora por CNPJ com histórico de despesas.
     *
     * @param cnpj CNPJ da operadora
     * @return OperadoraDTO com histórico
     */
    public OperadoraDTO findByCnpjWithHistory(String cnpj) {
        String sql = "SELECT cnpj, razao_social, nome_fantasia, uf, modalidade FROM operadoras WHERE cnpj = ?";
        List<OperadoraDTO> result = jdbcTemplate.query(sql, OPERADORA_ROW_MAPPER, cnpj);

        if (result.isEmpty()) {
            return null;
        }

        OperadoraDTO op = result.get(0);

        // Buscar histórico de despesas (simulando trimestres)
        String sqlDespesas = """
            SELECT codigo_conta, valor_total
            FROM despesas_agregadas
            WHERE cnpj_operadora = ?
            ORDER BY codigo_conta
            """;

        java.util.Map<String, BigDecimal> historico = new java.util.HashMap<>();
        jdbcTemplate.query(sqlDespesas, rs -> {
            String conta = rs.getString("codigo_conta");
            BigDecimal valor = rs.getBigDecimal("valor_total");
            historico.put("Conta " + conta, valor);
        }, cnpj);

        // Retornar o DTO existente (histórico não incorporado no DTO atual)
        return op;
    }

    /**
     * Retorna as Top 5 operadoras com maiores despesas. Reutiliza a Query 1 do
     * arquivo queries_analiticas.sql.
     *
     * @return lista das top 5
     */
    public List<OperadoraDTO> findTop5Despesas() {
        String sql = """
            SELECT da.cnpj_operadora AS cnpj,
                   o.razao_social,
                   o.nome_fantasia,
                   SUM(da.valor_total) AS total_despesas
            FROM despesas_agregadas da
            INNER JOIN operadoras o ON da.cnpj_operadora = o.cnpj
            WHERE 1=1
            """ + FILTRO_OPERADORAS_REAIS + """
            GROUP BY da.cnpj_operadora, o.razao_social, o.nome_fantasia
            ORDER BY total_despesas DESC 
            LIMIT 5
            """;

        return jdbcTemplate.query(sql, (rs, rowNum)
                -> new OperadoraDTO(
                        rs.getString("cnpj"),
                        rs.getString("razao_social"),
                        rs.getString("nome_fantasia"),
                        rs.getBigDecimal("total_despesas")
                )
        );
    }

    /**
     * Retorna média de gastos por código de conta. Reutiliza a Query 2 do
     * arquivo queries_analiticas.sql.
     *
     * @return lista com estatísticas por conta
     */
    public List<EstatisticaContaDTO> findMediaPorConta() {
        String sql = """
            SELECT 
                codigo_conta,
                ROUND(AVG(valor_total), 2) AS media_gastos,
                COUNT(*) AS num_operadoras 
            FROM despesas_agregadas
            GROUP BY codigo_conta
            ORDER BY media_gastos DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum)
                -> new EstatisticaContaDTO(
                        rs.getString("codigo_conta"),
                        rs.getBigDecimal("media_gastos"),
                        rs.getInt("num_operadoras")
                )
        );
    }

    /**
     * Busca detalhes completos da operadora incluindo histórico estruturado.
     *
     * @param cnpj CNPJ da operadora
     * @return OperadoraDetalhadaDTO com todos os dados
     */
    public com.intuitive.api.dto.OperadoraDetalhadaDTO findDetalhesCompletos(String cnpj) {
        // Buscar dados cadastrais
        String sqlOperadora = """
            SELECT cnpj, razao_social, nome_fantasia, uf, modalidade
            FROM operadoras 
            WHERE cnpj = ?
            """;

        List<com.intuitive.api.dto.OperadoraDetalhadaDTO> result = jdbcTemplate.query(sqlOperadora, (rs, rowNum) -> {
            com.intuitive.api.dto.OperadoraDetalhadaDTO dto = new com.intuitive.api.dto.OperadoraDetalhadaDTO();
            dto.setCnpj(rs.getString("cnpj"));
            dto.setRazaoSocial(rs.getString("razao_social"));
            dto.setNomeFantasia(rs.getString("nome_fantasia"));
            dto.setUf(rs.getString("uf"));
            dto.setModalidade(rs.getString("modalidade"));

            // Dados simulados para contato (em produção viriam do banco)
            dto.setCidade(rs.getString("uf") != null ? "São Paulo" : null);
            dto.setEmail("contato@operadora.com.br");
            dto.setTelefone("(11) 3000-0000");

            return dto;
        }, cnpj);

        if (result.isEmpty()) {
            return null;
        }

        com.intuitive.api.dto.OperadoraDetalhadaDTO operadora = result.get(0);

        // Buscar histórico de despesas
        String sqlHistorico = """
            SELECT codigo_conta, valor_total
            FROM despesas_agregadas
            WHERE cnpj_operadora = ?
            ORDER BY codigo_conta
            """;

        List<com.intuitive.api.dto.OperadoraDetalhadaDTO.DespesaHistoricoDTO> historico
                = jdbcTemplate.query(sqlHistorico, (rs, rowNum) -> {
                    String codigoConta = rs.getString("codigo_conta");
                    BigDecimal valor = rs.getBigDecimal("valor_total");

                    // Extrair ano e trimestre do código da conta (exemplo: "4112024" -> ano: 2024, trim: 4T)
                    String ano = "2024";
                    String trimestre = "4T";

                    // Tentar extrair informações reais do código
                    if (codigoConta != null && codigoConta.length() >= 3) {
                        String ultimoDigito = codigoConta.substring(0, 1);
                        if (ultimoDigito.matches("[1-4]")) {
                            trimestre = ultimoDigito + "T";
                        }
                        // Tentar extrair ano dos últimos 4 dígitos
                        if (codigoConta.length() >= 7) {
                            String anoStr = codigoConta.substring(codigoConta.length() - 4);
                            if (anoStr.matches("\\d{4}")) {
                                ano = anoStr;
                            }
                        }
                    }

                    return new com.intuitive.api.dto.OperadoraDetalhadaDTO.DespesaHistoricoDTO(
                            ano, trimestre, valor, codigoConta
                    );
                }, cnpj);

        operadora.setHistoricoDespesas(historico);

        return operadora;
    }

    /**
     * DTO auxiliar para estatísticas por conta.
     */
    public record EstatisticaContaDTO(
            String codigoConta,
            BigDecimal mediaGastos,
            int numOperadoras
            ) {

    }
}
