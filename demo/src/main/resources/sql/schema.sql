
-- Tabela de Operadoras (dados cadastrais)
CREATE TABLE IF NOT EXISTS operadoras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    nome_fantasia VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cnpj (cnpj)  -- Justificativa: Join frequente por CNPJ
);
-- Tabela de Despesas Agregadas (resultado do AggregatorService)
CREATE TABLE IF NOT EXISTS despesas_agregadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnpj_operadora VARCHAR(14) NOT NULL,
    codigo_conta VARCHAR(20) NOT NULL,
    valor_total DECIMAL(15, 2) NOT NULL,  -- Justificativa: DECIMAL evita erros de arredondamento
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cnpj_operadora) REFERENCES operadoras(cnpj),
    INDEX idx_cnpj_conta (cnpj_operadora, codigo_conta),
    INDEX idx_valor (valor_total)  -- Para queries de Top N
);