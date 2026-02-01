# Frontend — Intuitive Care (MVP)

Esse diretório contém um frontend mínimo em Vue 3 + Vite para consumir a API do backend.

Pré-requisitos
- Node.js 18+ e npm
- Backend Spring Boot rodando em `http://localhost:8081` (padrão do projeto demo)

Instalação e execução

```bash
cd frontend
npm install
npm run dev
```

O Vite rodará por padrão em `http://localhost:5173`. A API do backend está configurada na porta `8081` no arquivo `demo/src/main/resources/application.properties`.

Notas rápidas
- Endpoints usados: `GET /api/operadoras`, `GET /api/estatisticas/top5`, `GET /api/estatisticas/media-conta`.
- CORS já está habilitado no backend (`@CrossOrigin(origins = "*")`).
- Trade-off: frontend mínimo para MVP; usa fetch nativo e Chart.js sem wrapper Vue para simplicidade.
