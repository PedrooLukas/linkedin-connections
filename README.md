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


```bash
docker compose build frontend --no-cache
```
