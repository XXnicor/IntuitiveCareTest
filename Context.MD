@Context
PROJETO: Desafio Técnico Intuitive Care (Java + Spring Boot)
OBJETIVO: Criar um pipeline de dados (ETL) e uma API para análise de despesas de operadoras de saúde.
STACK: 
- Java 17+
- Build: Maven
- Libs: Jsoup (Crawler), Apache Commons CSV, Apache POI (Excel), Spring Boot 3.x (API).
- Banco: MySQL 8.0 ou PostgreSQL (via Docker).
- Frontend: Vue.js (Separado).

REGRAS CRÍTICAS (DoD):
1. DOCUMENTAÇÃO DE TRADE-OFFS: Toda decisão técnica complexa deve ser justificada (comentários ou README).
2. PERFORMANCE: Preferir processamento via Streams/Incremental ao invés de carregar tudo em memória.
3. ORGANIZAÇÃO: Clean Code, SOLID e tratamento de exceções robusto.