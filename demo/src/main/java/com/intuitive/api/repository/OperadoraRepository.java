package com.intuitive.api.repository;

import com.intuitive.api.dto.OperadoraDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Repositório para acesso a dados de operadoras via JDBC puro.
 * Trade-off: JdbcTemplate vs JPA - Controle total do SQL e performance previsível,
 * sem overhead de cache/lazy loading do Hibernate.
 */
@Repository
public class OperadoraRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperadoraRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper para converter ResultSet em OperadoraDTO (listagem simples).
     */
    private static final RowMapper<OperadoraDTO> OPERADORA_ROW_MAPPER = (rs, rowNum) -> 
        new OperadoraDTO(
            rs.getString("cnpj"),
            rs.getString("razao_social"),
            rs.getString("nome_fantasia")
        );

    /**
     * RowMapper para query com valor total agregado.
     */
    private static final RowMapper<OperadoraDTO> OPERADORA_COM_TOTAL_MAPPER = (rs, rowNum) -> 
        new OperadoraDTO(
            rs.getString("cnpj"),
            rs.getString("razao_social"),
            rs.getString("nome_fantasia")
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
            SELECT cnpj, razao_social, nome_fantasia
            FROM operadoras
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
        String sql = "SELECT COUNT(*) FROM operadoras";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Retorna as Top 5 operadoras com maiores despesas.
     * Reutiliza a Query 1 do arquivo queries_analiticas.sql.
     * 
     * @return lista das top 5
     */
    public List<OperadoraDTO> findTop5Despesas() {
        String sql = """
            SELECT da.cnpj_operadora AS cnpj,
                   o.nome_fantasia,
                   SUM(da.valor_total) AS total_despesas
            FROM despesas_agregadas da
            INNER JOIN operadoras o ON da.cnpj_operadora = o.cnpj
            GROUP BY da.cnpj_operadora, o.nome_fantasia 
            ORDER BY total_despesas DESC 
            LIMIT 5
            """;
        
        return jdbcTemplate.query(sql, OPERADORA_COM_TOTAL_MAPPER);
    }

    /**
     * Retorna média de gastos por código de conta.
     * Reutiliza a Query 2 do arquivo queries_analiticas.sql.
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
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            new EstatisticaContaDTO(
                rs.getString("codigo_conta"),
                rs.getBigDecimal("media_gastos"),
                rs.getInt("num_operadoras")
            )
        );
    }

    /**
     * DTO auxiliar para estatísticas por conta.
     */
    public record EstatisticaContaDTO(
        String codigoConta,
        BigDecimal mediaGastos,
        int numOperadoras
    ) {}
}