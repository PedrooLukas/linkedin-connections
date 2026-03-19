# Improved Linkedin

Projeto full stack com frontend em Next.js e backend em Spring Boot.

## Como rodar localmente com Docker

### Requisitos

- Docker Desktop instalado
- Docker Compose disponivel

### Subir o projeto

Na raiz do repositorio, rode:

```bash
docker compose up --build
```

Se quiser rodar em background:

```bash
docker compose up -d --build
```

### URLs locais

- Frontend: `http://localhost:3006`
- Backend: `http://localhost:9091`
- API: `http://localhost:9091/api`

### Parar os containers

```bash
docker compose down
```

### Rebuildar apos alteracoes

Se voce alterou o codigo e quer refletir isso nos containers:

```bash
docker compose up --build
```

Se mudou so o frontend:

```bash
docker compose build frontend
docker compose up -d frontend
```

Se mudou so o backend:

```bash
docker compose build backend
docker compose up -d backend
```

### Observacoes

- Os dados processados pelo backend ficam persistidos no volume Docker usado pelo SQLite.
- O arquivo ZIP enviado pelo frontend nao e armazenado permanentemente; apenas os dados importados sao salvos no banco.
- Se o build do frontend falhar no Docker, rode sem cache para diagnostico:

```bash
docker compose build frontend --no-cache
```
