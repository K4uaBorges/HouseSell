# Relatório Técnico do Projeto

### Developers

* 52790 Kauã Borges
* 51951 Gabriel Mano
* 50548 Gonçalo Amigo

## 1) Visão Geral

### 1.1 Propósito do Sistema

Backend Kotlin para plataforma de arrendamento de curta duração (tipo Airbnb), implementando:

- Gestão de utilizadores com autenticação por token UUID
- Hierarquia geográfica multinível (País → Região → Distrito → Município → Localidade)
- Anúncios de casas com metadados (área, preço, localização)
- Sistema de reservas com deteção de conflitos de datas
- Predição de preços por regressão linear (módulo auxiliar)

### 1.2 Stack Tecnológica

| Camada         | Tecnologia            | Versão          |
|----------------|-----------------------|-----------------|
| Linguagem      | Kotlin                | 2.3.0           |
| Runtime        | JVM                   | 21+             |
| Framework HTTP | http4k                | 6.1.0.1         |
| Serialização   | kotlinx-serialization | 1.8.0           |
| Datas          | kotlinx-datetime      | 0.6.2           |
| Servidor       | Undertow              | (via http4k)    |
| Base de Dados  | PostgreSQL            | 42.7.4 (driver) |
| Testes         | JUnit 5               | 5.10.2          |
| Testcontainers | PostgreSQL            | 1.20.4          |
| Linting        | ktlint                | 12.2.0          |

---

## 2) Estado atual do código (observação factual)

- O código foi analisado na branch atual tal como está no repositório.
- O comando `./gradlew compileKotlin` executa com sucesso.

```Retornando Starting a Gradle Daemon, 1 incompatible and 3 stopped Daemons could not be reused, use --status for details
BUILD SUCCESSFUL in 16s
1 actionable task: 1 executed
```
Sobre o código `./gradlew compileKoltin` resolve dependências
e valida configuração antes de compilar,
mas não arranca o servidor nem corre testes (test é outra task).

## 3) Arquitetura e fluxo da API (prioritário)

### 3.1 Fluxo principal

1. `main.app.App.kt` inicia servidor HTTP e escolhe modo de dados (memória ou BD).
2. `main.api.http_server.HousesRouter` arranca Undertow na porta configurada.
3. `main.api.http_server.HousesWebApi` recebe pedidos, valida input HTTP (path/query/body), transforma para DTO e chama serviços.
4. `main.api.http_server.HousesServices` faz orquestração de casos de uso, autenticação/autorização e mapeamento domínio ↔ DTO.
5. Serviços de domínio (`UsersService`, `HouseService`, `BookingService`, `LocationService`) aplicam regras de negócio.
6. Repositórios (`mem` ou `jdbc`) persistem e consultam dados.

![img.png](img.png)
#### Figura 1: Fluxo da API
(Na figura 1 apresenta )

### 3.2 Camadas e responsabilidades

- **HTTP/API**: routing, parsing JSON, validação de parâmetros HTTP, codes de resposta, erro uniforme.
- **Application services (`HousesServices`)**: coordenação entre serviços de domínio e API DTOs.
- **Domain services**: validações de negócio e invariantes.
- **Data layer**: armazenamento em memória e PostgreSQL.

### 3.3 Autenticação e autorização

- Token esperado no header `Authorization: Bearer <uuid>`.
- Parsing e validação em `main/api/utils/Auth.kt`.
- Operações que exigem token:
  - `POST /users`
  - `PUT /users/{uid}`
  - `DELETE /users/{uid}`
  - `POST /locations`
  - `PUT /locations/{lid}`
  - `DELETE /locations/{lid}`
  - `GET /houses/mine`
  - `POST /houses`
  - `PUT /houses/{hid}`
  - `DELETE /houses/{hid}`
  - `POST /bookings`
  - `GET /bookings`
  - `GET /bookings/{bid}`
  - `PUT /bookings/{bid}`
  - `DELETE /bookings/{bid}`
  
- Regras de Negócio:
  - Só dono do anúncio pode alterar/apagar casa.
  - Só dono da casa ou utilizador que reservou pode aceder/alterar/apagar booking.
  - `GET /bookings` exige que o token seja dono da casa pedida.
  - `POST /users` exige token válido de um utilizador já existente.

### 3.4 Paginação

- Implementada em `main/api/utils/Paging.kt`.
- Query params: `skip` e `limit`.
- Defaults: `skip=0`, `limit=20`.
- Limites: `skip >= 0`, `1 <= limit <= 100`.
- Aplicada aos endpoints listáveis que chamam `.page(...)`.

### 3.5 Tratamento de erros HTTP

Em `HousesWebApi.safe(...)`:

- `UnauthorizedException` -> `401 Unauthorized`
- `NoUserExist`, `NoHouseExist`, `NoLocationExist`, `NoBookingExist` -> `404 Not Found`
- `SerializationException`, `DomainErrorException`, `IllegalArgumentException` -> `400 Bad Request`
- `ServerErrorException` e restantes exceções -> `500 Internal Server Error`

Formato de erro:

```json
{
  "status": 400,
  "error": "mensagem"
}
```

## 4) API HTTP detalhada

Base path: `/`

Content-Type esperado para body JSON: `application/json`.

### 4.1 Users

| Método | Endpoint       | Auth | Request                                                                       | Response                                      |
|--------|----------------|------|-------------------------------------------------------------------------------|-----------------------------------------------|
| POST   | `/users`       | Sim  | `CreateUserRequest{name,email}`                                               | `201 CreateUserResponse{id,name,email,token}` |
| GET    | `/users`       | Não  | query opcional `skip`,`limit`                                                 | `200 ListUsersResponse{users[]}`              |
| GET    | `/users/{uid}` | Não  | path `uid`                                                                    | `200 GetUserResponse{id,name,email}`          |
| PUT    | `/users/{uid}` | Sim  | `UpdateUserRequest{name,email}`                                               | `200 GetUserResponse`                         |
| DELETE | `/users/{uid}` | Sim  | body opcional `DeleteUserRequest{id}` (se existir, tem de coincidir com path) | `200 DeleteUserResponse{id,deleted}`          |

### 4.2 Locations

| Método | Endpoint                          | Auth | Request                                                                           | Response                                                    |
|--------|-----------------------------------|------|-----------------------------------------------------------------------------------|-------------------------------------------------------------|
| POST   | `/locations`                      | Sim  | `CreateLocationRequest{name,type,parentId?}`                                      | `201 CreateLocationResponse{id,name,type,parentId}`         |
| GET    | `/locations`                      | Não  | sem body                                                                          | `200 ListLocationsResponse{locations[]}`                    |
| GET    | `/locations/{lid}`                | Não  | path `lid`                                                                        | `200 GetLocationResponse{id,name,type,parentId,fullPath[]}` |
| PUT    | `/locations/{lid}`                | Sim  | `UpdateLocationRequest{name,type,parentId?}`                                      | `200 GetLocationResponse`                                   |
| DELETE | `/locations/{lid}`                | Sim  | body opcional `DeleteLocationRequest{id}` (se existir, tem de coincidir com path) | `200 DeleteLocationResponse{id,deleted}`                    |
| GET    | `/locations/{lid}/childrenAll`    | Não  | path `lid`                                                                        | `200 List<LocationSummary>`                                 |
| GET    | `/locations/{lid}/childrenDirect` | Não  | path `lid`                                                                        | `200 List<LocationSummary>`                                 |
| GET    | `/locations/{lid}/path`           | Não  | path `lid`                                                                        | `200 List<LocationPathEntry>`                               |

Regras de hierarquia (serviço de domínio):

- `COUNTRY` não pode ter parent.
- Tipos filhos válidos por nível.
- Não pode criar ciclo.
- Não pode apagar localização com filhos.

### 4.3 Houses

| Método | Endpoint            | Auth | Request                                                                        | Response                                    |
|--------|---------------------|------|--------------------------------------------------------------------------------|---------------------------------------------|
| GET    | `/houses`           | Não  | query opcional `skip`,`limit`                                                  | `200 ListHousesResponse{houses[]}`          |
| GET    | `/houses/mine`      | Sim  | header Bearer + query opcional `skip`,`limit`                                  | `200 ListHousesResponse{houses[]}`          |
| POST   | `/houses`           | Sim  | `CreateHouseRequest{title,lid,areaSqMt,pricePerNight,description}`             | `201 CreateHouseResponse{...}`              |
| GET    | `/houses/available` | Não  | query obrigatória `startDate`,`endDate`; opcional `skip`,`limit`               | `200 ListAvailableHousesResponse{houses[]}` |
| GET    | `/houses/{hid}`     | Não  | path `hid`                                                                     | `200 GetHouseResponse{...}`                 |
| PUT    | `/houses/{hid}`     | Sim  | `UpdateHouseRequest{...}`                                                      | `200 GetHouseResponse`                      |
| DELETE | `/houses/{hid}`     | Sim  | body opcional `DeleteHouseRequest{id}` (se existir, tem de coincidir com path) | `200 DeleteHouseResponse{id,deleted}`       |

Observação de comportamento atual:

- `GET /houses` devolve casas disponíveis no intervalo `[hoje, amanhã)` (não lista "todas" as casas no estado atual do serviço).

### 4.4 Bookings

| Método | Endpoint          | Auth | Request                                                                          | Response                                |
|--------|-------------------|------|----------------------------------------------------------------------------------|-----------------------------------------|
| POST   | `/bookings`       | Sim  | `CreateBookingRequest{hid,startDate,endDate}`                                    | `201 CreateBookingResponse{...}`        |
| GET    | `/bookings`       | Sim  | query obrigatória `hid`,`dateStart`,`dateEnd`; opcional `skip`,`limit`           | `200 ListBookingsResponse{bookings[]}`  |
| GET    | `/bookings/{bid}` | Sim  | path `bid`                                                                       | `200 GetBookingResponse{...}`           |
| PUT    | `/bookings/{bid}` | Sim  | `UpdateBookingRequest{hid,startDate,endDate}`                                    | `200 GetBookingResponse`                |
| DELETE | `/bookings/{bid}` | Sim  | body opcional `DeleteBookingRequest{id}` (se existir, tem de coincidir com path) | `200 DeleteBookingResponse{id,deleted}` |

Regras de booking:

- Formato de data obrigatório `YYYY-MM-DD`.
- `endDate` tem de ser maior que `startDate`.
- Não pode haver overlap para a mesma casa.

## 5) Contratos DTO (API)

### 5.1 Request DTOs

- `CreateUserRequest`, `UpdateUserRequest`, `DeleteUserRequest`
- `CreateLocationRequest`, `UpdateLocationRequest`, `DeleteLocationRequest`
- `CreateHouseRequest`, `UpdateHouseRequest`, `DeleteHouseRequest`
- `CreateBookingRequest`, `UpdateBookingRequest`, `DeleteBookingRequest`

### 5.2 Response DTOs

- User: `CreateUserResponse`, `GetUserResponse`, `DeleteUserResponse`, `ListUsersResponse`
- Location: `CreateLocationResponse`, `GetLocationResponse`, `DeleteLocationResponse`, `ListLocationsResponse`, `LocationSummary`, `LocationPathEntry`
- House: `CreateHouseResponse`, `GetHouseResponse`, `DeleteHouseResponse`, `ListHousesResponse`
- Booking: `CreateBookingResponse`, `GetBookingResponse`, `DeleteBookingResponse`, `ListBookingsResponse`, `ListAvailableHousesResponse`, `AvailableHouseResponse`
- Erro: `ApiError`

## 6) Estrutura de pastas e ficheiros

### 6.1 Raiz do projeto

| Pasta/Ficheiro                               | Função                                                                                        |
|----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `build.gradle.kts`                           | Build Gradle, dependências (http4k, Kotlin serialization, PostgreSQL), tasks Docker e testes. |
| `docker-compose.yml`                         | Container PostgreSQL local (`houses-db`, porta host `5433`).                                  |
| `.env`                                       | Variáveis de BD (`JDBC_DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASS`, etc.).                |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/*` | Wrapper Gradle para build/test/run.                                                           |
| `Phase1.md`                                  | Requisitos funcionais e não funcionais da fase do projeto.                                    |
| `README.md`                                  | Este relatório.                                                                               |
| `src/main/`                                  | Código principal da aplicação.                                                                |
| `src/test/`                                  | Testes unitários e de integração + SQL de suporte.                                            |

### 6.2 `src/main/kotlin/main` (ficheiro a ficheiro)

#### App/config

| Ficheiro                       | Função                                                                                                 |
|--------------------------------|--------------------------------------------------------------------------------------------------------|
| `main/app/App.kt`              | Entry point; lê `PORT` e `JDBC_DATABASE_URL`, cria serviços e arranca servidor.                        |
| `main/app/config/EnvLoader.kt` | Loader opcional de `.env`, helpers para settings e `PGSimpleDataSource` (usado no módulo de predição). |

#### API HTTP

| Ficheiro                                 | Função                                                                                    |
|------------------------------------------|-------------------------------------------------------------------------------------------|
| `main/api/http_server/housesRouter.kt`   | Encapsula arranque do servidor Undertow.                                                  |
| `main/api/http_server/housesWebApi.kt`   | Define todas as rotas, parsing request, serialização response e mapeamento de erros HTTP. |
| `main/api/http_server/housesServices.kt` | Casos de uso da API; valida auth/autorização e chama serviços de domínio.                 |
| `main/api/http_server/housesDataMem.kt`  | Wiring de dependências; escolhe `InMemory` vs `JDBC` conforme `JDBC_DATABASE_URL`.        |
| `main/api/utils/Auth.kt`                 | Parsing/validação de Bearer token UUID.                                                   |
| `main/api/utils/Paging.kt`               | Modelo de paginação + extensão para paginar listas.                                       |
| `main/api/errors/ApiErrors.kt`           | DTO de erro HTTP (`status`, `error`).                                                     |

#### DTOs

| Ficheiro                       | Função                                                           |
|--------------------------------|------------------------------------------------------------------|
| `main/api/dto/UserDtos.kt`     | Contratos de request/response para users.                        |
| `main/api/dto/LocationDtos.kt` | Contratos de request/response para locations.                    |
| `main/api/dto/HouseDtos.kt`    | Contratos de request/response para houses.                       |
| `main/api/dto/BookingDtos.kt`  | Contratos de request/response para bookings e casas disponíveis. |

#### Domínio

| Ficheiro                                              | Função                                                                                    |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `main/domain_model/user/User.kt`                      | Entidade `User` e value objects `Name`, `Email`.                                          |
| `main/domain_model/user/UsersService.kt`              | Regras de users (criação, unicidade email, update/delete, listagem).                      |
| `main/domain_model/location/Location.kt`              | Entidade `Location`, `LocationType`, regras de tipo/hierarquia base.                      |
| `main/domain_model/location/LocationService.kt`       | Regras de localização (hierarquia, ciclo, path, children, update/delete).                 |
| `main/domain_model/house/House.kt`                    | Entidade `House` e value object `Title`.                                                  |
| `main/domain_model/house/HouseService.kt`             | Regras de houses (validação área/preço/descrição, CRUD).                                  |
| `main/domain_model/booking/Booking.kt`                | Entidade `Booking` e value object `Date`.                                                 |
| `main/domain_model/booking/BookingService.kt`         | Regras de booking (datas, overlap, disponibilidade, CRUD).                                |
| `main/domain_model/prediction/linearPreviewHouses.kt` | Módulo opcional de regressão linear para previsão de preço com dados de repo/BD/fallback. |

#### Data layer

| Ficheiro                                           | Função                                                              |
|----------------------------------------------------|---------------------------------------------------------------------|
| `main/data/interfaces/Repository.kt`               | Interface genérica de repositório.                                  |
| `main/data/interfaces/UsersRepository.kt`          | Contrato de persistência de users.                                  |
| `main/data/interfaces/HouseRepository.kt`          | Contrato de persistência de houses.                                 |
| `main/data/interfaces/BookingRepository.kt`        | Contrato de persistência de bookings.                               |
| `main/data/interfaces/LocationRepository.kt`       | Contrato de persistência de locations (com métodos hierárquicos).   |
| `main/data/impl/mem/InMemoryUsersRepository.kt`    | Repositório em memória para users (índices por id/token/email).     |
| `main/data/impl/mem/InMemoryHouseRepository.kt`    | Repositório em memória para houses.                                 |
| `main/data/impl/mem/InMemoryBookingRepository.kt`  | Repositório em memória para bookings.                               |
| `main/data/impl/mem/InMemoryLocationRepository.kt` | Repositório em memória para locations (children/path/exists).       |
| `main/data/impl/jdbc/JdbcUsersRepository.kt`       | Persistência JDBC users (CRUD + getByToken/email).                  |
| `main/data/impl/jdbc/JdbcHouseRepository.kt`       | Persistência JDBC houses.                                           |
| `main/data/impl/jdbc/JdbcBookingRepository.kt`     | Persistência JDBC bookings.                                         |
| `main/data/impl/jdbc/JdbcLocationRepository.kt`    | Persistência JDBC locations com queries recursivas (path/children). |

#### Utilitários e erros

| Ficheiro                           | Função                                                                        |
|------------------------------------|-------------------------------------------------------------------------------|
| `main/utils/BookingDateUtils.kt`   | Parse/format/overlap de datas de booking.                                     |
| `main/errors/TTTErrorException.kt` | Hierarquia de exceções de domínio/servidor/repositório/autorização/not found. |

### 6.3 Outras pastas de `src/main`

| Pasta/Ficheiro                         | Função                                                                                    |
|----------------------------------------|-------------------------------------------------------------------------------------------|
| `src/main/kotlin/sql/createSchema.sql` | Script SQL de criação do schema PostgreSQL (users, locations, houses, booking e índices). |

### 6.4 `src/test` (ficheiro a ficheiro)

#### API HTTP

| Ficheiro                                            | Função                                                      |
|-----------------------------------------------------|-------------------------------------------------------------|
| `src/test/kotlin/http_server/HousesWebApiTest.kt`   | Testes de endpoints HTTP (JSON inválido, CRUD users, etc.). |
| `src/test/kotlin/http_server/HousesServicesTest.kt` | Testes da camada `HousesServices`.                          |
| `src/test/kotlin/http_server/HousesRouterTest.kt`   | Testa bind de porta e serving de requests.                  |
| `src/test/kotlin/http_server/HousesDataMemTest.kt`  | Testa seleção de backend mem vs jdbc.                       |

#### Domínio/API por módulo

| Ficheiro                                                       | Função                                              |
|----------------------------------------------------------------|-----------------------------------------------------|
| `src/test/kotlin/domain_model/user/UsersApiTest.kt`            | Testes API de users (status codes, validações).     |
| `src/test/kotlin/domain_model/user/UsersServiceTest.kt`        | Regras de negócio de users.                         |
| `src/test/kotlin/domain_model/location/LocationApiTest.kt`     | Testes API de locations (hierarquia, path, delete). |
| `src/test/kotlin/domain_model/location/LocationServiceTest.kt` | Regras de negócio de locations.                     |
| `src/test/kotlin/domain_model/house/HouseApiTest.kt`           | Testes API de houses (auth, ownership, disponível). |
| `src/test/kotlin/domain_model/house/HouseServiceTest.kt`       | Regras de house service.                            |
| `src/test/kotlin/domain_model/booking/BookingApiTest.kt`       | Testes API de bookings (auth, overlap, queries).    |
| `src/test/kotlin/domain_model/booking/BookingServiceTest.kt`   | Regras de booking service.                          |

#### Repositórios em memória

| Ficheiro                                                      | Função                                  |
|---------------------------------------------------------------|-----------------------------------------|
| `src/test/kotlin/repos/mem/InMemoryUsersRepositoryTest.kt`    | Índices e CRUD de users em memória.     |
| `src/test/kotlin/repos/mem/InMemoryHouseRepositoryTest.kt`    | CRUD houses em memória.                 |
| `src/test/kotlin/repos/mem/InMemoryBookingRepositoryTest.kt`  | CRUD bookings em memória.               |
| `src/test/kotlin/repos/mem/InMemoryLocationRepositoryTest.kt` | Hierarquia e CRUD locations em memória. |

#### Repositórios JDBC e integração

| Ficheiro                                                      | Função                                                      |
|---------------------------------------------------------------|-------------------------------------------------------------|
| `src/test/kotlin/repos/jdbc/PostgresTestContainer.kt`         | Base de testes com Testcontainers PostgreSQL e init schema. |
| `src/test/kotlin/repos/jdbc/JdbcUsersRepositoryTest.kt`       | CRUD + constraints users em JDBC.                           |
| `src/test/kotlin/repos/jdbc/JdbcHouseRepositoryTest.kt`       | CRUD + foreign keys houses em JDBC.                         |
| `src/test/kotlin/repos/jdbc/JdbcBookingRepositoryTest.kt`     | CRUD bookings JDBC.                                         |
| `src/test/kotlin/repos/jdbc/JdbcLocationRepositoryTest.kt`    | Hierarquia/path/children locations JDBC.                    |
| `src/test/kotlin/repos/jdbc/JdbcRepositoryIntegrationTest.kt` | Cenários integrados multi-entidade e cascatas.              |

#### Predição linear

| Ficheiro                                             | Função                                |
|------------------------------------------------------|---------------------------------------|
| `src/test/kotlin/linear_preview/PricePreviewTest.kt` | Testes do módulo de regressão linear. |

#### Utilitários de paginação e pedidos manuais

| Ficheiro                                                     | Função                                                |
|--------------------------------------------------------------|-------------------------------------------------------|
| `src/test/kotlin/main/ls/api/utils/PagingOfTest.kt`          | Testes de parse/validação de `Paging.of`.             |
| `src/test/kotlin/main/ls/api/utils/PagingPageTest.kt`        | Testes da extensão `List<T>.page`.                    |
| `src/test/kotlin/main/ls/api/utils/PagingIntegrationTest.kt` | Testes de integração de paginação em rotas/listagens. |
| `src/test/kotlin/main/ls/test.http`                          | Coleção de requests HTTP para testes manuais.         |

#### SQL de testes

| Ficheiro                                          | Função                                                             |
|---------------------------------------------------|--------------------------------------------------------------------|
| `src/test/resources/sql/createSchema.sql`         | Schema de base para testes JDBC.                                   |
| `src/test/resources/sql/insert_locs_for_test.sql` | Script de inserção de dados de localização para cenários de teste. |

## 7) Como correr o projeto localmente

### 7.1 Pré-requisitos

- JDK compatível com Gradle/Kotlin do projeto.
- Docker (se usar PostgreSQL em container).

### 7.2 Arranque com PostgreSQL (intenção do projeto)

#### 7.2.1 Subir BD:

```bash
./gradlew allUp
```

ou

```bash
docker compose up -d
```

#### 7.2.2 Exportar variáveis (ou garantir que o shell já as tem):

```bash
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5433/houses
export DATABASE_USER=houses
export DATABASE_PASS=houses
```

#### 7.2.3 Arrancar aplicação:

```bash
./gradlew run
```

Nota: o `App.kt` lê variáveis do ambiente de processo; não faz load direto de `.env`.

### 7.3 Arranque em memória

- Se `JDBC_DATABASE_URL` não estiver definido, `HousesDataMem` usa repositórios `InMemory`.

## 8) Como testar a API

### 8.1 Testes automáticos

```bash
./gradlew test
```

### 8.2 Teste manual rápido com `curl`

Criar user:

```bash
curl -s -X POST http://localhost:8080/users \
  -H 'Authorization: Bearer <TOKEN_EXISTENTE>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

Nota: no estado atual, `POST /users` requer token válido de um utilizador já existente.

Criar house (com token Bearer):

```bash
curl -s -X POST http://localhost:8080/houses \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Casa Azul","lid":"<LOCATION_UUID>","areaSqMt":120,"pricePerNight":95.0,"description":"Casa perto da praia"}'
```

Listar casas disponíveis:

```bash
curl -s "http://localhost:8080/houses/available?startDate=2026-06-10&endDate=2026-06-12"
```

## 9) Variáveis de ambiente relevantes

| Variável            | Uso                                    | Default no código             | Obrigatória                           |
|---------------------|----------------------------------------|-------------------------------|---------------------------------------|
| `PORT`              | Porta HTTP do servidor                 | `8080`                        | Não                                   |
| `JDBC_DATABASE_URL` | Define uso de backend JDBC e URL da BD | sem default                   | Não (se ausente, usa memória)         |
| `DATABASE_USER`     | User BD para `createDataSource`        | fallback para `DATABASE_NAME` | Depende de uso de `createDataSource`  |
| `DATABASE_NAME`     | Fallback de user em `createDataSource` | sem default                   | Só se `DATABASE_USER` ausente         |
| `DATABASE_PASS`     | Password BD para `createDataSource`    | sem default                   | Sim quando `createDataSource` é usado |

## 10) Resumo curto final

Foi documentada a arquitetura completa (HTTP -> serviços -> domínio -> repositórios), a estrutura de pastas/ficheiros 
relevantes e todos os contratos principais da API, com prioridade para endpoints, autenticação, validações e tratamento de erros.

Ficheiros mais críticos para a API:

- `src/main/kotlin/main/api/http_server/housesWebApi.kt`
- `src/main/kotlin/main/api/http_server/housesServices.kt`
- `src/main/kotlin/main/api/http_server/housesDataMem.kt`
- `src/main/kotlin/main/api/utils/Auth.kt`
- `src/main/kotlin/main/api/utils/Paging.kt`
- `src/main/kotlin/main/domain_model/*/*Service.kt`
