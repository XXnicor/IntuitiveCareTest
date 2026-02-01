package com.intuitive.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para representar dados de operadoras.
 * Trade-off: POJO simples para controle total e performance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperadoraDTO {
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private BigDecimal total_despesas;  // Total de despesas agregadas
    
    // Construtor para query de listagem simples (sem valor total)
    public OperadoraDTO(String cnpj, String razaoSocial, String nomeFantasia) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.total_despesas = null;
    }
}