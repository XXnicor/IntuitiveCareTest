package com.intuitive.api.dto;

/**
 * DTO simplificado solicitado: contém apenas os campos expostos na API para
 * listagem e detalhes mínimos.
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperadoraDTO {

    // Registro na ANS (pode ser null se não existir no schema local)
    private String registroAns;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String modalidade;
    private String uf;
    private BigDecimal totalDespesas;

    // Construtor auxiliar para construções rápidas sem registroAns
    public OperadoraDTO(String cnpj, String razaoSocial, String uf, String modalidade) {
        this.registroAns = null;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.uf = uf;
        this.modalidade = modalidade;
    }

    // Construtor para respostas reduzidas (ex.: TOP5) — usa registroAns nulo
    public OperadoraDTO(String cnpj, String razaoSocial) {
        this.registroAns = null;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.uf = null;
        this.modalidade = null;
    }

    // Construtor para TOP5 com total de despesas
    public OperadoraDTO(String cnpj, String razaoSocial, String nomeFantasia, BigDecimal totalDespesas) {
        this.registroAns = null;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.totalDespesas = totalDespesas;
        this.uf = null;
        this.modalidade = null;
    }

}
