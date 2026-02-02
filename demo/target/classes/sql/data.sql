-- Dados mock para desenvolvimento (H2 in-memory)

-- Operadoras
INSERT INTO operadoras (cnpj, razao_social, nome_fantasia, uf, modalidade) VALUES
('00000000000001', 'Operadora A LTDA', 'Operadora A', 'SP', 'Medicina de Grupo'),
('00000000000002', 'Operadora B SA', 'Operadora B', 'RJ', 'Cooperativa Médica'),
('00000000000003', 'Operadora C ME', 'Operadora C', 'MG', 'Autogestão'),
('00000000000004', 'Operadora D EIRELI', 'Operadora D', 'SP', 'Seguradora'),
('00000000000005', 'Operadora E SA', 'Operadora E', 'RS', 'Filantropia');

-- Despesas agregadas (vários registros por operadora)
INSERT INTO despesas_agregadas (cnpj_operadora, codigo_conta, valor_total) VALUES
('00000000000001', '311', 125000.50),
('00000000000001', '312', 45000.00),
('00000000000002', '311', 98000.00),
('00000000000002', '313', 15000.00),
('00000000000003', '312', 76000.00),
('00000000000004', '311', 250000.00),
('00000000000005', '314', 12000.00),
('00000000000003', '314', 5000.00),
('00000000000002', '312', 30000.00),
('00000000000004', '312', 40000.00);

-- Nota: valores suficientes para as queries Top5 e média por conta
