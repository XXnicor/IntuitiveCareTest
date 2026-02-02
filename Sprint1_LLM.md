{
  "title": "Sprint de Emergência - 3 Dias (No Docker / MVP Mode)",
  "description": "Plano 'War Room' ajustado: Banco Local, Spring Boot para velocidade e foco total nos scripts SQL obrigatórios.",
  "current_status": "Step 2.2 (Data Enrichment)",
  "days": [
    {
      "day": 1,
      "name": "Dia 1: ETL Final & SQL (Obrigatórios)",
      "focus": "Finalizar manipulação de arquivos e criar a estrutura de Banco",
      "goal": "Arquivos CSV prontos e Scripts SQL escritos/testados.",
      "tasks": [
        "Finalizar 2.2 (Enricher): Implementar 'DataEnricher' usando HashMap em memória (Join O(1)).",
        "Finalizar 2.3 (Agregação): Usar Java Streams 'groupingBy' para gerar 'despesas_agregadas.csv'.",
        "Step 3.1 & 3.2 (SQL DDL): Escrever 'create_tables.sql' para criar tabelas no seu banco local (MySQL/Postgres).",
        "Step 3.3 (Import): Criar script ou código Java (JDBC Batch) para carregar os CSVs gerados para o banco.",
        "Step 3.4 (Analytics): Escrever 'queries_analiticas.sql' e testar no console do banco (Top 5, UF, Média)."
      ],
      "note": "Esqueça Docker. Rode o banco instalado na sua máquina. O objetivo é ter os arquivos .sql prontos para o ZIP."
    },
    {
      "day": 2,
      "name": "Dia 2: Backend Spring Boot (API)",
      "focus": "Conectar o Java ao Banco de Dados",
      "goal": "Endpoints JSON funcionando.",
      "tasks": [
        "Setup Spring Boot: Dependências Web, Data JPA, MySQL/Postgres Driver, Lombok.",
        "Entities: Criar classes mapeando as tabelas (ex: @Entity class Operadora).",
        "Repository: Criar interface OperadoraRepository extends JpaRepository.",
        "DICA DE OURO: Para as estatísticas, use @Query(value='...sua query sql do dia 1...', nativeQuery=true) no Repository.",
        "Controller: Implementar endpoints GET /operadoras (Pageable) e GET /estatisticas."
      ],
      "note": "Não perca tempo convertendo tudo para OO. Use Native Query para reaproveitar o SQL do Dia 1."
    },
    {
      "day": 3,
      "name": "Dia 3: Frontend Flash & README (Entrega)",
      "focus": "Visualização Mínima e Justificativas",
      "goal": "Interface rodando e ZIP fechado.",
      "tasks": [
        "Setup Vue.js: Criar projeto com Vite (npm create vite@latest).",
        "Tabela: Componente simples listando dados de GET /operadoras.",
        "Gráfico: Componente usando Chart.js para renderizar os dados de GET /estatisticas.",
        "README (CRÍTICO): Documentar como rodar localmente (sem Docker) e justificar os Trade-offs (ex: Join em memória, Native Queries).",
        "Entrega: Limpar pastas (target/node_modules), gerar ZIP e testar descompactação."
      ],
      "note": "Se o gráfico complicar, entregue a tabela bem feita. O README bem escrito salva a nota se o front estiver simples."
    }
  ]
}
