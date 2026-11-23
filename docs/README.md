# Task Manager

Monorepo com a base inicial de um sistema de gerenciamento de tarefas, contendo backend em Spring Boot, frontend em React e infraestrutura local com Docker Compose.

Foi adotada a abordagem de monorepo neste momento porque o projeto está sendo conduzido como uma POC/teste, com backend, frontend e infraestrutura evoluindo em conjunto e com necessidade de setup local simples em uma única base.

## Estrutura

```text
taskmanager/
├── backend/   # Spring Boot + Maven
├── frontend/  # React + Vite
├── infra/     # Docker Compose e variáveis de ambiente locais
└── .github/   # Workflows de CI
```

## Stack

- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Banco de dados: PostgreSQL 16
- Mensageria: Kafka + Zookeeper
- Orquestração local: Docker Compose

## Arquitetura atual

### Backend

- API REST de tasks com CRUD completo em `/api/tasks`
- Camadas orientadas a casos de uso (controller dependente de portas)
- Liquibase em XML com separacao de changelogs `ddl/` e `dml/`
- Publicacao e consumo de eventos de task via Kafka

### Frontend

- Estrutura modular por `core`, `shared`, `layout` e `features`
- Feature `tasks` implementada com CRUD por componentes (sem grid generico)
- Componentes de UI reutilizaveis para formulario, modal, botoes e tipografia
- Hook generico de carregamento CRUD para evitar repeticao de fluxo assíncrono

## Contrato de erro

O backend responde erros com `ProblemDetail` enriquecido. Para validacoes de formulario, a resposta inclui `fieldErrors` por campo para o frontend destacar inputs invalidos.

Exemplo de erro de validacao (HTTP 400):

```json
{
	"detail": "Existem campos invalidos no formulario.",
	"status": 400,
	"title": "Validation error",
	"code": "VALIDATION_ERROR",
	"userSafe": true,
	"fieldErrors": {
		"title": "must not be blank",
		"description": "must not be blank"
	}
}
```

No frontend:

- normalizacao de erro fica centralizada no HTTP abstrato
- exibicao de erro por campo fica nos componentes de formulario
- erros de rede/timeout/backend indisponivel recebem fallback centralizado

## Desenvolvimento com GitHub Copilot

Este projeto está sendo desenvolvido com apoio do GitHub Copilot durante a estruturação do monorepo, configuração de ambiente local, organização de infraestrutura e evolução do código.

## Pré-requisitos

- Docker
- Docker Compose

Observação: o ambiente atual foi preparado para rodar localmente via containers, sem depender de Java ou Node instalados na máquina host.

## Configuração local

O Docker Compose lê variáveis a partir de `infra/.env`.

Use o arquivo de exemplo como base:

```bash
cp infra/.env.example infra/.env
```

Valores configuráveis atualmente:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `FRONTEND_PORT`
- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`

## Como subir o ambiente

Na raiz do projeto:

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

## Endpoints locais

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Healthcheck: `http://localhost:8080/actuator/health`

### Endpoints de tasks

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

PostgreSQL, Kafka e Zookeeper ficam acessíveis apenas na rede interna do Docker Compose por padrão.

As portas expostas acima podem ser alteradas em `infra/.env`.

## Comandos úteis

Subir o ambiente:

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

Ver status dos containers:

```bash
docker compose -f infra/docker-compose.yml ps
```

Ver logs:

```bash
docker compose -f infra/docker-compose.yml logs -f
```

Parar o ambiente:

```bash
docker compose -f infra/docker-compose.yml down
```

## Smoke E2E (API)

Comandos executados para validacao ponta a ponta:

```bash
# health
curl -sS http://localhost:8080/actuator/health

# create
curl -sS -X POST http://localhost:8080/api/tasks \
	-H 'Content-Type: application/json' \
	-d '{"title":"E2E task","description":"created by smoke test"}'

# list
curl -sS http://localhost:8080/api/tasks

# update
curl -sS -X PUT http://localhost:8080/api/tasks/{id} \
	-H 'Content-Type: application/json' \
	-d '{"title":"E2E task updated","description":"updated by smoke test","status":"IN_PROGRESS"}'

# validation error
curl -sS -X POST http://localhost:8080/api/tasks \
	-H 'Content-Type: application/json' \
	-d '{"title":"","description":""}'

# delete
curl -sS -X DELETE http://localhost:8080/api/tasks/{id}
```

Resultado observado na ultima validacao:

- Health: `200`
- Create: `201`
- List: `200`
- Update: `200`
- Validation error: `400` com `fieldErrors`
- Delete: `204`

## CI

O repositório possui workflows separados para backend e frontend:

- `.github/workflows/backend-ci.yml`
- `.github/workflows/frontend-ci.yml`

## Estado atual

- Backend com CRUD de tasks, validacao estruturada e eventos via Kafka
- Frontend TypeScript com CRUD por componentes e tratamento de erro centralizado
- Infra local preparada com PostgreSQL, Kafka e Zookeeper
- Configuracao centralizada por variaveis de ambiente em `infra/.env`