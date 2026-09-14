# API TOTVS

API Spring Boot responsável por autenticação de usuários, upload de arquivos de reuniões, publicação de jobs no Azure Service Bus e persistência dos insights retornados pelo Worker.

## Sumário

- [Visão geral](#visão-geral)
- [Stack](#stack)
- [Arquitetura e fluxo](#arquitetura-e-fluxo)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Autenticação](#autenticação)
- [Status das requests](#status-das-requests)
- [Endpoints](#endpoints)
- [Modelo de dados](#modelo-de-dados)
- [Collection Postman](#collection-postman)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Visão geral

A API atua como orquestradora entre o frontend e o Worker de análise:

1. O usuário autentica e faz upload de um arquivo (CSV/JSON/JSONL).
2. A API grava o arquivo no Azure Blob Storage (Azurite em local), cria um registro com `request_id` e publica uma mensagem na fila `meet-process`.
3. O Worker processa o arquivo e devolve os insights via `POST /api/v1/worker/insights`.
4. O frontend consulta o status das requests e os insights persistidos.

O vínculo entre envio e retorno é o **`request_id`**.

---

## Stack

| Tecnologia                   | Uso                        |
| ---------------------------- | -------------------------- |
| Java 25                      | Runtime                    |
| Spring Boot 4.1.1            | Framework                  |
| Spring Security + JWT        | Autenticação               |
| Spring Data JPA / Hibernate  | Persistência               |
| PostgreSQL 16                | Banco de dados             |
| Azure Blob Storage (Azurite) | Armazenamento de arquivos  |
| Azure Service Bus (emulator) | Fila de jobs para o Worker |
| Lombok                       | Boilerplate                |
| Docker Compose               | Infra local                |

---

## Arquitetura e fluxo

```text
Frontend                API TOTVS                    Azure / Emulators              Worker
   |                        |                               |                         |
   |-- POST /api/auth ----->|                               |                         |
   |<----- JWT -------------|                               |                         |
   |                        |                               |                         |
   |-- POST /api/blobs ---->|                               |                         |
   |                        |-- upload blob --------------->| Azurite                 |
   |                        |-- save MeetRegister --------->| Postgres                |
   |                        |-- publish message ----------->| Service Bus             |
   |<-- requestId+status ---|                               |------------ job ------->|
   |                        |                               |                         |
   |                        |<-------- POST /api/v1/worker/insights -----------------|
   |                        |-- upsert insights ----------->| Postgres                |
   |                        |-- status = processado/falha                           |
   |                        |                               |                         |
   |-- GET /api/blobs ----->|                               |                         |
   |-- GET /api/insights -->|                               |                         |
```

---

## Pré-requisitos

- Java 25+
- Maven Wrapper (`./mvnw` já incluso)
- Docker e Docker Compose
- (Opcional) Postman

---

## Como rodar

Na pasta `totvs-api`:

```bash
./mvnw spring-boot:run
```

O Spring Boot sobe automaticamente os serviços do `compose.yaml` (Postgres, Azurite, MSSQL e Service Bus emulator). A API fica em:

```text
http://localhost:8080
```

Para apenas compilar:

```bash
./mvnw compile -DskipTests
```

> O Service Bus emulator pode demorar na primeira subida (depende do MSSQL). O timeout de readiness está em `240s`.

---

## Variáveis de ambiente

Valores padrão em `src/main/resources/application.properties` (adequados para local):

| Variável                              | Descrição                         | Default local                |
| ------------------------------------- | --------------------------------- | ---------------------------- |
| `JWT_SECRET`                          | Segredo HS256 do JWT (≥ 32 chars) | chave de desenvolvimento     |
| `AZURE_STORAGE_CONNECTION_STRING`     | Connection string do Blob         | Azurite `devstoreaccount1`   |
| `AZURE_STORAGE_CONTAINER_NAME`        | Container de arquivos             | `reunioes`                   |
| `AZURE_STORAGE_SAS_EXPIRY_HOURS`      | Validade da SAS URL               | `24`                         |
| `AZURE_SERVICE_BUS_CONNECTION_STRING` | Connection string da fila         | emulator em `localhost:5673` |
| `AZURE_SERVICE_BUS_QUEUE_NAME`        | Nome da fila                      | `meet-process`               |

Banco local:

| Item    | Valor                                         |
| ------- | --------------------------------------------- |
| URL     | `jdbc:postgresql://localhost:5432/mydatabase` |
| Usuário | `myuser`                                      |
| Senha   | `secret`                                      |
| DDL     | `spring.jpa.hibernate.ddl-auto=update`        |

Limites de upload / payload:

- Multipart: até **50 MB**
- Callback do Worker (JSON): até **~15 MB**

---

## Autenticação

- Rotas públicas: `/api/auth/**`, `/api/v1/worker/**`
- Rotas protegidas (JWT Bearer): `/api/blobs/**`, `/api/insights/**` e demais

Header nas rotas autenticadas:

```http
Authorization: Bearer <token>
```

O token é retornado em `POST /api/auth/register` e `POST /api/auth/login`.

---

## Status das requests

Cada upload gera um `MeetRegister` com status:

| Status       | Quando                                                         |
| ------------ | -------------------------------------------------------------- |
| `criado`     | Registro gravado após o upload                                 |
| `analisando` | Mensagem publicada com sucesso no Service Bus                  |
| `processado` | Worker respondeu com sucesso (`status: concluido`, etc.)       |
| `falha`      | Erro ao publicar na fila **ou** Worker retornou `falha`/`erro` |

O frontend pode acompanhar o andamento via `GET /api/blobs` (campo `status`).

---

## Endpoints

### Auth

#### `POST /api/auth/register`

Cria usuário e retorna JWT.

```json
{
  "name": "Usuario Teste",
  "email": "teste@totvs.com",
  "password": "senha123"
}
```

**Resposta `201`:**

```json
{
  "token": "<jwt>",
  "userId": "1",
  "username": "Usuario Teste",
  "email": "teste@totvs.com"
}
```

#### `POST /api/auth/login`

```json
{
  "email": "teste@totvs.com",
  "password": "senha123"
}
```

**Resposta `200`:** mesmo formato do register.

---

### Blobs (JWT)

#### `POST /api/blobs`

`multipart/form-data` com campo `file`.

Fluxo interno:

1. Gera `request_id` (`req-<uuid>`)
2. Faz upload no Blob Storage
3. Gera SAS URL de leitura
4. Persiste `MeetRegister` (`criado` → `analisando`)
5. Publica na fila:

```json
{
  "request_id": "req-...",
  "file_url": "https://...?...sas...",
  "file_name": "reunioes.jsonl"
}
```

**Resposta `201`:**

```json
{
  "id": 1,
  "requestId": "req-...",
  "fileName": "reunioes.jsonl",
  "blobUrl": "https://...",
  "status": "analisando",
  "errorMessage": null,
  "createdAt": "2026-09-13T22:00:00Z",
  "updatedAt": "2026-09-13T22:00:01Z"
}
```

#### `GET /api/blobs`

Lista os uploads do usuário autenticado (com `status`).

---

### Insights (JWT)

#### `GET /api/insights`

Lista os retornos do Worker vinculados ao usuário (resumo).

#### `GET /api/insights/{requestId}`

Detalhe completo dos insights daquele `request_id` (resumo + reuniões).

---

### Worker (público)

#### `POST /api/v1/worker/insights`

Callback usado pelo Worker para entregar o resultado consolidado. Idempotente por `request_id` (upsert das reuniões por `request_id + id_meeting`).

Headers recomendados (contrato do Worker):

```http
Content-Type: application/json
Accept: application/json
Idempotency-Key: <request_id>
X-Request-ID: <request_id>
```

Corpo (envelope):

```json
{
  "event_type": "meeting_insights.completed",
  "schema_version": "1.0",
  "request_id": "req-123",
  "status": "concluido",
  "sent_at_utc": "2026-09-13T14:00:00Z",
  "insights": {
    "schema_version": "1.1",
    "gerado_em_utc": "2026-09-13T13:59:50Z",
    "resumo": {},
    "reunioes": []
  }
}
```

Mapeamento do `status` do envelope:

| Worker envia                             | API marca `MeetRegister` |
| ---------------------------------------- | ------------------------ |
| `concluido`, `sucesso`, `completed`, ... | `processado`             |
| `falha`, `erro`, `failed`, ...           | `falha`                  |

**Resposta `200`:**

```json
{
  "request_id": "req-123",
  "status": "persistido",
  "reunioes_recebidas": 1
}
```

---

## Modelo de dados

### `meet_register`

Registro da request enviada ao Worker.

| Campo           | Descrição                                        |
| --------------- | ------------------------------------------------ |
| `request_id`    | Chave única de vínculo (envio ↔ retorno)         |
| `blob_url`      | SAS URL do arquivo                               |
| `file_name`     | Nome original                                    |
| `status`        | `criado` / `analisando` / `processado` / `falha` |
| `error_message` | Motivo em caso de falha                          |
| `user_id`       | Dono da request                                  |

### `worker_insight_result`

Envelope persistido do callback (1:1 com `meet_register`).

| Campo                                             | Descrição              |
| ------------------------------------------------- | ---------------------- |
| `request_id`                                      | Mesmo id da request    |
| `event_type` / `schema_version` / `worker_status` | Metadados do envelope  |
| `resumo_json`                                     | JSONB do resumo        |
| `reunioes_recebidas`                              | Quantidade de reuniões |

### `meeting_insight`

Cada reunião do payload (unique: `request_id + id_meeting`).

| Campo            | Descrição                                 |
| ---------------- | ----------------------------------------- |
| `id_meeting`     | Identificador da reunião                  |
| `status_analise` | Ex.: `concluida_llm_rag`                  |
| `payload_json`   | JSONB com o documento completo da reunião |

---

## Collection Postman

Arquivo:

```text
postman/TOTVS_API.postman_collection.json
```

Importe no Postman. Variáveis da collection:

| Variável                                  | Uso                                          |
| ----------------------------------------- | -------------------------------------------- |
| `baseUrl`                                 | `http://localhost:8080`                      |
| `token`                                   | Preenchido automaticamente no Register/Login |
| `requestId`                               | Preenchido no Upload / List blobs            |
| `userEmail` / `userPassword` / `userName` | Credenciais de teste                         |

Fluxo sugerido:

1. **Auth → Register** (ou Login)
2. **Blobs → Upload file**
3. **Worker → Receive insights (success)**
4. **Insights → List** / **Get by requestId**
5. **Blobs → List blobs** (conferir `status: processado`)

---

## Estrutura do projeto

```text
totvs-api/
├── compose.yaml
├── docker/servicebus/Config.json
├── postman/TOTVS_API.postman_collection.json
├── pom.xml
└── src/main/java/com/fiap/apitotvs/
    ├── controller/          # Auth, Blobs, Insights, Worker
    ├── service/             # Regras de negócio + interfaces
    ├── service/impl/        # Implementações
    ├── repository/          # Spring Data JPA
    ├── entity/              # User, MeetRegister, WorkerInsightResult, MeetingInsight
    ├── dto/request|response|message/
    ├── mapper/              # Conversão DTO ↔ Entity
    ├── enums/               # MeetRequestStatus
    ├── security/            # JWT + SecurityConfig
    ├── config/              # Azure clients, Jackson
    └── exception/           # Handler global
```
