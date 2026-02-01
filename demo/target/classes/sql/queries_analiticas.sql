
-- Query 1: Top 5 Operadoras com Maiores Despesas Totais
-- Objetivo: Identificar as operadoras que mais gastam (soma de todas as contas)
-- Trade-off: GROUP BY + ORDER BY é O(n log n), mas com índice em valor_total fica aceitável até ~1M registros

SELECT cnpj_operadora,
       nome_fantasia,
       SUM(valor_total) AS total_despesas
FROM despesas_agregadas
INNER JOIN operadoras ON cnpj_operadora = operadoras.cnpj
GROUP BY cnpj_operadora, nome_fantasia 
ORDER BY total_despesas DESC 
LIMIT 5;


-- Query 2: Média de Gastos por Código de Conta
-- Objetivo: Descobrir quais tipos de conta (ex: '311', '312') têm maiores médias
-- Justificativa: Permite identificar categorias de despesa mais pesadas

SELECT 
    codigo_conta,
    ROUND(AVG(valor_total), 2) AS media_gastos,
    COUNT (*) AS num_operadoras 
FROM despesas_agregadas
GROUP BY codigo_conta
ORDER BY media_gastos DESC;

-- Query 3 : Variação de Gastos Entre Contas da Mesma Operadora
-- Objetivo: Identificar operadoras com grande disparidade entre contas (risco de inconsistência)
-- Justificativa: MAX - MIN pode indicar outliers ou erros de digitação

SELECT 
    o.nome_fantasia,
    MAX(valor_total) - MIN(valor_total) AS variacao
FROM despesas_agregadas da
INNER JOIN operadoras o ON da.cnpj_operadora = o.cnpj
GROUP BY o.cnpj, o.nome_fantasia 
HAVING (MAX(valor_total) - MIN(valor_total)) > 10000 
ORDER BY variacao DESC
LIMIT 10;