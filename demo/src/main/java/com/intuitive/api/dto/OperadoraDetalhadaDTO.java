package com.intuitive.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO detalhado para o endpoint de detalhes da operadora. Inclui informações
 * cadastrais completas e histórico de despesas.
 */
public class OperadoraDetalhadaDTO {

    // Dados cadastrais
    private String registroAns;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String modalidade;
    private String uf;
    private String cidade;
    private String email;
    private String telefone;

    // Histórico de despesas
    private List<DespesaHistoricoDTO> historicoDespesas;

    public OperadoraDetalhadaDTO() {
        this.historicoDespesas = new ArrayList<>();
    }

    // Getters e Setters
    public String getRegistroAns() {
        return registroAns;
    }

    public void setRegistroAns(String registroAns) {
        this.registroAns = registroAns;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getModalidade() {
        return modalidade;
    }

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public List<DespesaHistoricoDTO> getHistoricoDespesas() {
        return historicoDespesas;
    }

    public void setHistoricoDespesas(List<DespesaHistoricoDTO> historicoDespesas) {
        this.historicoDespesas = historicoDespesas;
    }

    /**
     * DTO interno para histórico de despesas
     */
    public static class DespesaHistoricoDTO {

        private String ano;
        private String trimestre;
        private BigDecimal valor;
        private String codigoConta;

        public DespesaHistoricoDTO() {
        }

        public DespesaHistoricoDTO(String ano, String trimestre, BigDecimal valor, String codigoConta) {
            this.ano = ano;
            this.trimestre = trimestre;
            this.valor = valor;
            this.codigoConta = codigoConta;
        }

        // Getters e Setters
        public String getAno() {
            return ano;
        }

        public void setAno(String ano) {
            this.ano = ano;
        }

        public String getTrimestre() {
            return trimestre;
        }

        public void setTrimestre(String trimestre) {
            this.trimestre = trimestre;
        }

        public BigDecimal getValor() {
            return valor;
        }

        public void setValor(BigDecimal valor) {
            this.valor = valor;
        }

        public String getCodigoConta() {
            return codigoConta;
        }

        public void setCodigoConta(String codigoConta) {
            this.codigoConta = codigoConta;
        }
    }
}
