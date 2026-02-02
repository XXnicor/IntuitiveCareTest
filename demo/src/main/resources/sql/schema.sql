
-- Tabela de Operadoras (dados cadastrais)
CREATE TABLE IF NOT EXISTS operadoras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    razao_social VARCHAR(255),
    nome_fantasia VARCHAR(255),
    uf VARCHAR(2),
    modalidade VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Tabela de Despesas Agregadas (resultado do AggregatorService)
CREATE TABLE IF NOT EXISTS despesas_agregadas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnpj_operadora VARCHAR(14) NOT NULL,
    codigo_conta VARCHAR(20) NOT NULL,
    valor_total DECIMAL(15, 2) NOT NULL,  -- Justificativa: DECIMAL evita erros de arredondamento
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cnpj_operadora) REFERENCES operadoras(cnpj)
);

-- Índices (criados separadamente para compatibilidade com H2)
-- Índices para MySQL (idempotente: cria apenas se não existir)

-- idx_cnpj on operadoras(cnpj)
SET @cnt := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'operadoras' AND index_name = 'idx_cnpj');
SET @sql := IF(@cnt = 0, 'CREATE INDEX idx_cnpj ON operadoras(cnpj)', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_cnpj_conta on despesas_agregadas(cnpj_operadora, codigo_conta)
SET @cnt := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'despesas_agregadas' AND index_name = 'idx_cnpj_conta');
SET @sql := IF(@cnt = 0, 'CREATE INDEX idx_cnpj_conta ON despesas_agregadas(cnpj_operadora, codigo_conta)', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_valor on despesas_agregadas(valor_total)
SET @cnt := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'despesas_agregadas' AND index_name = 'idx_valor');
SET @sql := IF(@cnt = 0, 'CREATE INDEX idx_valor ON despesas_agregadas(valor_total)', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;