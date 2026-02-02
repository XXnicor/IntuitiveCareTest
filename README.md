# 📊 Sistema de Análise de Despesas de Operadoras de Saúde

> **Desafio Técnico Intuitive Care** - Pipeline ETL completo e API REST para análise de dados de saúde suplementar da ANS.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.3-42b883.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
- [Processo de ETL e Qualidade de Dados](#-processo-de-etl-e-qualidade-de-dados)
- [API REST e Frontend](#-api-rest-e-frontend)
- [Trade-offs e Decisões de Design](#-trade-offs-e-decisões-de-design)
- [Como Executar](#-como-executar)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Visão de Futuro](#-visão-de-futuro)
- [Autor](#-autor)

---

## 🎯 Sobre o Projeto

Este projeto implementa uma **solução completa de ETL (Extract, Transform, Load)** para processar dados públicos de operadoras de saúde da ANS (Agência Nacional de Saúde Suplementar), combinando técnicas de **web scraping**, **processamento em lote otimizado** e uma **API REST moderna** para disponibilizar insights gerenciais.

### Funcionalidades Principais

✅ **Web Crawler** para download automatizado de arquivos CSV da ANS  
✅ **Pipeline ETL** com validação, limpeza e normalização de dados  
✅ **API REST** com paginação, busca e endpoints analíticos  
✅ **Dashboard Gerencial** em Vue.js com visualizações interativas  
✅ **Processamento Otimizado** para grandes volumes de dados (~300MB+)  

---

## 🏗️ Arquitetura e Tecnologias

### Stack Tecnológico

| Camada | Tecnologia | Versão | Justificativa |
|--------|-----------|---------|---------------|
| **Backend** | Java | 17 (LTS) | Performance, maturidade e compatibilidade corporativa |
| **Framework** | Spring Boot | 3.2.0 | Ecossistema robusto, injeção de dependências e produtividade |
| **Persistência** | Spring JDBC | 3.2.0 | **Controle fino sobre SQL e performance em operações em lote** |
| **Banco de Dados** | MySQL | 8.0+ | Compatibilidade, suporte a grandes volumes e funcionalidades analíticas |
| **Crawler** | Jsoup | 1.17.2 | Simplicidade para parsing HTML e extração de links |
| **Frontend** | Vue.js | 3.3 | Reatividade, leveza e curva de aprendizado reduzida |
| **Gráficos** | Chart.js | 4.4 | Biblioteca madura para visualizações interativas |
| **Build Tool** | Maven | 3.x | Gerenciamento de dependências e build reproduzível |

### 🔑 Por Que JDBC ao Invés de JPA/Hibernate?

**Decisão técnica fundamentada no contexto do desafio:**

#### Vantagens do Spring JDBC (JdbcTemplate):
1. **Performance em Batch Inserts**: JPA gera queries individuais por padrão. Com `JdbcTemplate.batchUpdate()`, conseguimos inserir **10.000+ registros** em uma única operação de rede, reduzindo drasticamente o tempo de carga inicial do banco.

2. **Controle Total sobre SQL**: O projeto lida com consultas analíticas complexas (agregações, JOINs) onde SQL nativo oferece maior expressividade e otimização manual.

3. **Menor Overhead de Memória**: Ao processar arquivos CSV com milhões de linhas, evitamos o custo do cache de primeiro nível do Hibernate e o lazy loading desnecessário.

4. **Simplicidade Arquitetural**: Para um pipeline ETL onde 80% das operações são inserções em lote e consultas de leitura, a complexidade do ORM não agrega valor proporcional.

```java
// Exemplo de Batch Insert Otimizado (DatabaseImportService.java)
jdbcTemplate.batchUpdate(
    "INSERT INTO operadora_despesas (registro_ans, cnpj, razao_social, ...) VALUES (?, ?, ?, ...)",
    new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Operadora op = operadoras.get(i);
            ps.setString(1, op.getRegistroANS());
            // ... 15+ campos
        }
        @Override
        public int getBatchSize() { return operadoras.size(); }
    }
);
```

---

## 🔄 Processo de ETL e Qualidade de Dados

### 1️⃣ Extract (Extração)

**Desafio Técnico**: Os arquivos CSV da ANS utilizam encoding **ISO-8859-1 (Latin1)** e podem conter caracteres especiais corrompidos.

```java
// AnsCrawlerService.java - Conversão de encoding segura
try (BufferedReader reader = new BufferedReader(
    new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
    // Processa linha por linha sem carregar todo arquivo em memória
}
```

**Implementação:**
- Web Scraping com Jsoup para extrair URLs de download da página da ANS
- Download incremental com validação de integridade (tamanho de arquivo)
- Extração automática de arquivos ZIP mantendo estrutura de diretórios

### 2️⃣ Transform (Transformação)

#### Processamento em Streaming
Para evitar `OutOfMemoryError` em arquivos grandes (>200MB), utilizamos **processamento linha a linha**:

```java
// CsvParserService.java - Streaming de dados
public Stream<Operadora> parseIncrementalComStream(File arquivo) {
    return Files.lines(arquivo.toPath(), StandardCharsets.ISO_8859_1)
        .skip(1) // Ignora cabeçalho
        .map(this::parseLinhaCSV)
        .filter(Objects::nonNull); // Remove linhas inválidas
}
```

#### Limpeza e Validação de Dados

**Filtros Aplicados:**
1. **Remoção de Ruídos Contábeis**: Linhas contendo termos como `"ATIVO"`, `"PASSIVO"`, `"(-) Acionistas"` são descartadas (registros de balanço, não operadoras).
2. **Validação de CNPJ**: Regex pattern `^\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}$` para garantir formato válido.
3. **Normalização de Valores Monetários**: Conversão de strings `"1.234.567,89"` para `BigDecimal` com tratamento de `null`.
4. **Padronização de Datas**: Conversão de `"4T2024"` para formato `yyyy-MM-dd` (último dia do trimestre).

```java
// DataEnricherService.java - Validação e enriquecimento
private boolean isValidOperadora(Operadora op) {
    return op.getCnpj() != null 
        && op.getCnpj().matches("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}")
        && !isRuidoContabil(op.getRazaoSocial());
}

private boolean isRuidoContabil(String texto) {
    String[] termos = {"ATIVO", "PASSIVO", "(-) Acionistas", "PATRIMÔNIO"};
    return Arrays.stream(termos).anyMatch(texto::contains);
}
```

### 3️⃣ Load (Carga)

**Estratégia de Inserção:**
- **Batch Size**: 5.000 registros por lote (ajustável via configuração)
- **Deduplicação**: Constraint `UNIQUE(registro_ans, data_referencia)` no banco
- **Transações Controladas**: Commit manual a cada batch para recuperação em caso de falha

---

## 🌐 API REST e Frontend

### Endpoints Principais

#### 1. Listar Operadoras com Paginação e Busca
```http
GET /api/operadoras?page=1&limit=20&q=unimed
```

**Response:**
```json
{
  "data": [
    {
      "cnpj": "11.111.111/0001-00",
      "razaoSocial": "UNIMED ABC",
      "registroAns": "123456",
      "modalidade": "Medicina de Grupo",
      "porte": "Grande"
    }
  ],
  "total": 1843,
  "page": 1,
  "limit": 20,
  "totalPages": 93
}
```

**Implementação:**
```java
// OperadoraRepository.java - Paginação manual com LIMIT/OFFSET
public List<OperadoraDTO> findAllPaginado(int page, int limit) {
    int offset = (page - 1) * limit;
    String sql = """
        SELECT DISTINCT cnpj, razao_social, registro_ans, modalidade, porte
        FROM operadora_despesas
        ORDER BY razao_social
        LIMIT ? OFFSET ?
    """;
    return jdbcTemplate.query(sql, new OperadoraRowMapper(), limit, offset);
}
```

#### 2. Detalhes da Operadora com Histórico
```http
GET /api/operadoras/{cnpj}/detalhes
```

**Response:**
```json
{
  "cnpj": "11.111.111/0001-00",
  "razaoSocial": "UNIMED ABC",
  "historico": [
    {
      "dataReferencia": "2024-12-31",
      "despesaTotal": 15678234.50,
      "despesaAssistencial": 12345678.90,
      "despesaAdministrativa": 3332555.60
    }
  ]
}
```

#### 3. Estatísticas Gerais (Dashboard)
```http
GET /api/estatisticas
```

Retorna agregações como:
- Total de operadoras ativas
- Soma de despesas por modalidade
- Top 10 operadoras por volume de despesas
- Evolução temporal (últimos 4 trimestres)

### Frontend - Dashboard Gerencial

**Tecnologias:**
- **Vue.js 3** (Composition API)
- **Vite** (build tool rápido)
- **Chart.js** (gráficos interativos)

**Funcionalidades:**
1. **Tabela de Operadoras**: Busca em tempo real, paginação, ordenação
2. **Detalhes da Operadora**: Modal com histórico de despesas e gráficos de evolução
3. **Dashboard Analítico**: Cards com KPIs e gráficos de barras/linhas

```vue
<!-- Estatisticas.vue - Exemplo de componente -->
<template>
  <div class="dashboard">
    <div class="cards">
      <div class="card">
        <h3>Total de Operadoras</h3>
        <p class="valor">{{ stats.totalOperadoras }}</p>
      </div>
      <div class="card">
        <h3>Despesa Total (Trimestre)</h3>
        <p class="valor">{{ formatarMoeda(stats.despesaTotal) }}</p>
      </div>
    </div>
    <canvas ref="chartCanvas"></canvas>
  </div>
</template>
```

---

## ⚖️ Trade-offs e Decisões de Design

### 1. Vue.js ao Invés de React/Angular

**Justificativa:**
- **Leveza**: Bundle final ~50KB (vs. React ~100KB)
- **Reatividade Nativa**: Sistema de reatividade simplificado sem necessidade de bibliotecas externas
- **Curva de Aprendizado**: Sintaxe HTML-like facilita manutenção por equipes mistas
- **Performance**: Virtual DOM otimizado para atualizações frequentes (busca em tempo real)

### 2. Lógica de Agregação no Banco de Dados

**Decisão**: Centralizar cálculos analíticos no MySQL com views e stored procedures.

**Prós:**
- ✅ Reduz transferência de dados (rede)
- ✅ Aproveita índices e otimizador do MySQL
- ✅ API mais simples (delegação de complexidade)

**Contras:**
- ❌ Menor portabilidade entre SGBDs
- ❌ Dificuldade em testes unitários de lógica SQL

**Exemplo:**
```sql
-- queries_analiticas.sql - Query otimizada para dashboard
SELECT 
    modalidade,
    COUNT(DISTINCT cnpj) as total_operadoras,
    SUM(eventos_indenizaveis_empenhado) as despesa_total
FROM operadora_despesas
WHERE data_referencia = (SELECT MAX(data_referencia) FROM operadora_despesas)
GROUP BY modalidade
ORDER BY despesa_total DESC;
```

### 3. H2 Database para Compatibilidade

**Uso**: Banco em memória para testes e demonstrações sem necessidade de instalação de MySQL.

**Configuração Dual:**
```properties
# application.properties - Perfis de ambiente
# Produção: MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/intuitive_care
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Desenvolvimento/Testes: H2 (compatível com dialeto MySQL)
# spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
# spring.h2.console.enabled=true
```

### 4. Ausência de Service Layer (MVP)

**Trade-off Consciente**: Controller acessa Repository diretamente.

**Justificativa para MVP:**
- Reduz boilerplate em operações CRUD simples
- Time-to-market mais rápido para validação de conceito

**Evolução Recomendada:**
```java
// Arquitetura futura
@Service
public class OperadoraService {
    private final OperadoraRepository repository;
    private final CacheService cache; // Redis
    
    public OperadoraDTO buscarComCache(String cnpj) {
        return cache.get(cnpj, () -> repository.findByCnpj(cnpj));
    }
}
```

---

## 🚀 Como Executar

### Pré-requisitos

- **Java 17+** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **Node.js 18+** ([Download](https://nodejs.org/))
- **MySQL 8.0+** ([Download](https://dev.mysql.com/downloads/mysql/))
- **Git** ([Download](https://git-scm.com/))

---

### 1️⃣ Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/desafio-intuitive-care.git
cd desafio-intuitive-care
```

---

### 2️⃣ Configurar Banco de Dados

#### Opção A: MySQL (Produção)

```bash
# Criar banco de dados
mysql -u root -p
```

```sql
CREATE DATABASE intuitive_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'intuitive_user'@'localhost' IDENTIFIED BY 'senha_segura';
GRANT ALL PRIVILEGES ON intuitive_care.* TO 'intuitive_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

```bash
# Executar scripts SQL
cd demo/src/main/resources/sql
mysql -u intuitive_user -p intuitive_care < schema.sql
mysql -u intuitive_user -p intuitive_care < data.sql  # (Opcional - dados de exemplo)
```

#### Opção B: H2 (Desenvolvimento/Testes)

Edite `demo/src/main/resources/application.properties`:

```properties
# Comente as linhas do MySQL e descomente:
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Acesse o console H2 em: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- User: `sa`
- Password: _(vazio)_

---

### 3️⃣ Executar Pipeline ETL (Download e Carga de Dados)

```bash
cd demo

# Compilar projeto
mvn clean compile

# Executar crawler e ETL
mvn exec:java -Dexec.mainClass="com.intuitive.crawler.Main"
```

**O que acontece:**
1. Download automático dos CSVs da ANS (~300MB)
2. Extração de arquivos ZIP
3. Parsing e limpeza de dados
4. Carga em lote no banco de dados (10.000+ registros)

**Tempo Estimado**: 3-5 minutos (dependendo da conexão de rede)

---

### 4️⃣ Iniciar Backend (API REST)

```bash
cd demo

# Iniciar Spring Boot
mvn spring-boot:run
```

API disponível em: `http://localhost:8080`

**Testar endpoints:**
```bash
# Listar operadoras
curl http://localhost:8080/api/operadoras

# Buscar operadora específica
curl http://localhost:8080/api/operadoras/11.111.111/0001-00

# Estatísticas gerais
curl http://localhost:8080/api/estatisticas
```

---

### 5️⃣ Iniciar Frontend (Dashboard)

```bash
cd frontend

# Instalar dependências
npm install

# Iniciar servidor de desenvolvimento
npm run dev
```

Acesse: `http://localhost:5173`

---

## 📁 Estrutura do Projeto

```
desafio-intuitive-care/
│
├── demo/                                    # Backend Java/Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/intuitive/
│   │   │   │   ├── api/                     # Camada REST
│   │   │   │   │   ├── ApiApplication.java # Entry point Spring Boot
│   │   │   │   │   ├── controller/         # Endpoints REST
│   │   │   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   │   ├── repository/         # Camada de dados (JDBC)
│   │   │   │   │   └── config/             # Configurações (CORS, UTF-8)
│   │   │   │   │
│   │   │   │   └── crawler/                # Pipeline ETL
│   │   │   │       ├── Main.java           # Orquestrador do ETL
│   │   │   │       ├── AnsCrawlerService.java      # Web scraping
│   │   │   │       ├── CsvParserService.java       # Parser CSV
│   │   │   │       ├── DataEnricherService.java    # Limpeza/Validação
│   │   │   │       ├── DatabaseImportService.java  # Carga em lote
│   │   │   │       └── FileManagerService.java     # Gestão de arquivos
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.properties   # Configuração Spring Boot
│   │   │       └── sql/
│   │   │           ├── schema.sql           # DDL (CREATE TABLE)
│   │   │           ├── data.sql             # Dados de exemplo (opcional)
│   │   │           └── queries_analiticas.sql # Queries otimizadas
│   │   │
│   │   └── test/                            # Testes unitários e integração
│   │       └── java/com/intuitive/crawler/
│   │           ├── CsvParserServiceTest.java
│   │           ├── DataEnricherServiceTest.java
│   │           └── H2InMemorySqlValidationTest.java
│   │
│   ├── pom.xml                              # Dependências Maven
│   └── downloads_ans/                       # Arquivos CSV baixados
│
├── frontend/                                # Frontend Vue.js
│   ├── src/
│   │   ├── components/
│   │   │   ├── OperadorasTable.vue         # Tabela de listagem
│   │   │   └── Estatisticas.vue            # Dashboard analítico
│   │   ├── services/
│   │   │   └── api.js                      # Cliente HTTP (fetch)
│   │   ├── App.vue                         # Componente raiz
│   │   └── main.js                         # Entry point
│   │
│   ├── index.html
│   ├── package.json                        # Dependências NPM
│   └── vite.config.js                      # Configuração Vite
│
├── Context.MD                               # Documentação do contexto
├── Sprint1_LLM.md                           # Documentação do desenvolvimento
└── README.md                                # Este arquivo
```

---

## 🔮 Visão de Futuro

### Melhorias Planejadas

#### 1. **Cache Distribuído com Redis**
```yaml
Objetivo: Reduzir latência de consultas frequentes (estatísticas, top operadoras)
Tecnologia: Redis 7.x com Spring Data Redis
Ganho Esperado: -70% no tempo de resposta de endpoints analíticos
Implementação:
  - Cache de estatísticas com TTL de 1 hora
  - Invalidação inteligente após carga de novos dados
```

#### 2. **Dockerização Completa**
```dockerfile
# docker-compose.yml proposto
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: intuitive_care
    volumes:
      - ./demo/src/main/resources/sql:/docker-entrypoint-initdb.d
      - mysql_data:/var/lib/mysql

  backend:
    build: ./demo
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/intuitive_care

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
```

**Benefícios:**
- Ambiente reproduzível em qualquer máquina
- Facilita CI/CD e deployment em Kubernetes
- Isola dependências (sem necessidade de instalar MySQL localmente)

#### 3. **Testes End-to-End com Cypress**
```javascript
// Exemplo de teste E2E proposto
describe('Busca de Operadora', () => {
  it('deve filtrar e exibir detalhes', () => {
    cy.visit('http://localhost:5173');
    cy.get('input[name="busca"]').type('UNIMED');
    cy.contains('UNIMED ABC').click();
    cy.get('.modal').should('contain', 'Histórico de Despesas');
  });
});
```

**Cobertura Planejada:**
- Fluxo completo de busca e visualização
- Navegação entre páginas
- Responsividade mobile

#### 4. **Observabilidade e Monitoramento**
- **Spring Boot Actuator**: Métricas de saúde da aplicação
- **Prometheus + Grafana**: Dashboards de performance
- **ELK Stack (Elasticsearch, Logstash, Kibana)**: Análise de logs centralizada

#### 5. **Segurança**
- **Spring Security**: Autenticação JWT para API
- **Rate Limiting**: Proteção contra abuso de endpoints
- **HTTPS**: Certificado SSL/TLS obrigatório em produção

#### 6. **Escalabilidade**
- **Arquitetura de Microserviços**: Separar ETL e API em serviços independentes
- **Message Queue (RabbitMQ/Kafka)**: Processamento assíncrono de arquivos grandes
- **Load Balancer**: Distribuição de carga entre múltiplas instâncias

---

## 👨‍💻 Autor

**Nicolas**  
🎓 Estudante de Engenharia de Software (Conclusão prevista: 2028)  
🎯 Foco: Desenvolvimento Backend e Ecossistema Java  

### Competências Demonstradas Neste Projeto:

✔️ **Arquitetura de Software**: Design modular com separação de responsabilidades (ETL, API, Frontend)  
✔️ **Performance**: Otimização de processamento em lote e consultas SQL analíticas  
✔️ **Engenharia de Dados**: Pipeline ETL completo com validação e qualidade de dados  
✔️ **Boas Práticas**: Clean Code, SOLID, tratamento de exceções robusto  
✔️ **DevOps**: Conhecimento de Docker, CI/CD e estratégias de deployment  
✔️ **Visão de Negócios**: Trade-offs técnicos justificados com foco em valor entregue  

---

### 📫 Contato

- **LinkedIn**: [linkedin.com/in/seu-perfil](https://linkedin.com/in/seu-perfil)
- **GitHub**: [github.com/seu-usuario](https://github.com/seu-usuario)
- **Email**: seu.email@exemplo.com

---

## 📄 Licença

Este projeto foi desenvolvido como parte de um desafio técnico e está disponível sob a licença MIT.

---

<div align="center">

**⭐ Se este projeto foi útil para você, considere deixar uma estrela no repositório!**

</div>
