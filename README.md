# ecommerce-microservices-kafka

Plataforma de e-commerce construída com arquitetura de microsserviços, comunicação assíncrona via Apache Kafka e orquestrada via Docker Compose.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Fluxo de eventos](#fluxo-de-eventos)
- [Padrão de eventos](#padrão-de-eventos)
- [Decisões técnicas](#decisões-técnicas)
- [Trade-offs](#trade-offs)
- [Tech Stack](#tech-stack)
- [Infraestrutura](#infraestrutura)
- [Como executar](#como-executar)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Estratégia de branches](#estratégia-de-branches)
- [Roadmap](#roadmap)

---

## Arquitetura

```
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                              CLIENT / UI                                    │
  └──────────────────────────────────┬──────────────────────────────────────────┘
                                     │ HTTPS
                                     ▼
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                          API Gateway  :8080                                 │
  │                       (Spring Cloud Gateway + JWT + Redis rate-limit)       │
  │                                                                             │
  │  /api/v1/products/**  →  product-service                                   │
  │  /api/v1/orders/**    →  order-service                                     │
  │  /api/v1/payments/**  →  payment-service                                   │
  └──────┬──────────────────────────┬────────────────────────┬─────────────────┘
         │ REST                     │ REST                    │ REST
         ▼                          ▼                         ▼
  ┌──────────────┐        ┌─────────────────┐       ┌──────────────────┐
  │product-service│       │  order-service  │       │ payment-service  │
  │    :8081      │◄──────│     :8082       │       │     :8083        │
  │               │ REST  │                 │       │                  │
  │ - catalog     │       │ - order lifecycle│       │ - payment proc.  │
  │ - inventory   │       │ - state machine  │       │ - idempotency    │
  │               │       │                 │       │                  │
  │ [product_db]  │       │  [order_db]     │       │  [payment_db]    │
  └──────────────┘        └────────┬────────┘       └────────┬─────────┘
                                   │                          │
                           ┌───────▼──────────────────────────▼────────┐
                           │                                            │
                           │             Apache Kafka  :9092            │
                           │                                            │
                           │  topics:                                   │
                           │  • order.created          (+ .DLT)        │
                           │  • order.confirmed        (+ .DLT)        │
                           │  • order.cancelled        (+ .DLT)        │
                           │  • payment.confirmed      (+ .DLT)        │
                           │  • payment.failed         (+ .DLT)        │
                           │                                            │
                           └───────────────────┬────────────────────────┘
                                               │
                                               ▼
                                   ┌───────────────────────┐
                                   │  notification-service  │
                                   │         :8084          │
                                   │                        │
                                   │  - email (Mailpit)     │
                                   │  [notification_db]     │
                                   └───────────────────────┘

  ─── Observabilidade ────────────────────────────────────────────────────────
  Logstash :5044  →  Elasticsearch :9200  →  Kibana :5601
  Todos os serviços enviam logs estruturados (JSON) via Logback → Logstash

  ─── Config centralizado ────────────────────────────────────────────────────
  Config Server :8888  →  lido por todos os microsserviços ao iniciar
```

Diagrama completo e padrões de comunicação: [docs/architecture.md](./docs/architecture.md)  
Bounded contexts, entidades e responsabilidades: [docs/domain.md](./docs/domain.md)

---

## Fluxo de eventos

O fluxo principal cobre a jornada completa de um pedido, do momento da criação até a notificação final ao cliente.

```
  Cliente
     │
     │  POST /api/v1/orders
     ▼
  API Gateway ──► order-service
                       │
                       ├── persiste Order [PENDING]
                       │
                       └── publica ──► order.created
                                            │
                       ┌────────────────────┘
                       │
                       ▼
                 payment-service
                       │
                       ├── valida idempotência (orderId já processado?)
                       │
                       ├── processa pagamento
                       │
                       ├── [APROVADO] publica ──► payment.confirmed
                       │                               │
                       │             ┌─────────────────┴──────────────────┐
                       │             ▼                                     ▼
                       │       order-service                  notification-service
                       │       [PENDING → PAID]               envia e-mail de confirmação
                       │             │
                       │             └── publica ──► order.confirmed
                       │                                  │
                       │                                  ▼
                       │                       notification-service
                       │                       envia resumo do pedido
                       │
                       └── [RECUSADO] publica ──► payment.failed
                                                       │
                                    ┌──────────────────┴──────────────────┐
                                    ▼                                      ▼
                              order-service                   notification-service
                              [PENDING → CANCELLED]           envia e-mail de falha
                              libera estoque (product-service)
```

### Estados dos agregados

**Order** (`order-service`)

```
PENDING ──► CONFIRMED ──► PAID ──► SHIPPED ──► DELIVERED
        └──► CANCELLED
```

**Payment** (`payment-service`)

```
PENDING ──► PROCESSING ──► PAID
                       └──► FAILED ──► REFUNDED
```

---

## Padrão de eventos

### Tópicos Kafka

| Tópico                | Publicado por       | Consumido por                                   |
|-----------------------|---------------------|-------------------------------------------------|
| `order.created`       | order-service       | payment-service                                 |
| `order.confirmed`     | order-service       | notification-service                            |
| `order.cancelled`     | order-service       | notification-service, product-service           |
| `payment.confirmed`   | payment-service     | order-service, notification-service             |
| `payment.failed`      | payment-service     | order-service, notification-service             |

> Cada tópico possui um tópico DLT (`<topic>.DLT`) para mensagens que falharam após todas as tentativas de retry.

### Retry e Dead Letter Topic (DLT)

A anotação `@RetryableTopic` é usada nos consumers para gerenciar falhas transientes:

```
Mensagem recebida
       │
       ▼
 Consumer tenta processar
       │
       ├── [sucesso] → ack + commit offset
       │
       └── [falha]   → retry tópico (backoff exponencial: 1s, 2s, 4s)
                             │
                             └── [esgotou retries] → DLT (audit + alerta)
```

- **Backoff exponencial** evita sobrecarga em falha em cascata
- **`include = {GatewayUnavailableException.class}`** — apenas erros recuperáveis disparam retry
- **Erros de negócio** (`BusinessRuleViolationException`) falham imediatamente sem retry

### Idempotência

Todos os consumers verificam se o evento já foi processado antes de executar a lógica de negócio, garantindo que mensagens duplicadas (por redelivery) não causem efeitos colaterais:

```java
if (paymentRepository.existsByOrderId(event.getOrderId())) {
    return; // já processado — ignora silenciosamente
}
```

---

## Decisões técnicas

### Apache Kafka

| Aspecto | Decisão |
|---------|---------|
| **Por que Kafka** | Alta throughput, retenção configurável de mensagens, replay de eventos, desacoplamento real entre produtores e consumidores |
| **Topologia** | Um broker local (Confluent Platform 7.6), `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` — todos os tópicos criados explicitamente |
| **Serialização** | JSON (Spring Kafka `JsonSerializer`/`JsonDeserializer`) — legível, sem schema registry nesta fase |
| **Consumer groups** | Cada serviço possui seu próprio group-id, garantindo que cada instância consuma a partição que lhe compete |
| **DLT** | Tópico `.DLT` por tópico principal, consumido separadamente para auditoria e re-processamento manual |

### Microsserviços

| Aspecto | Decisão |
|---------|---------|
| **Database-per-Service** | Cada serviço possui seu próprio PostgreSQL isolado — autonomia de schema, sem acoplamento de dados |
| **Snapshot de preço** | O preço do produto é copiado no `OrderItem` no momento da criação do pedido — mudanças no catálogo não afetam pedidos existentes |
| **Inventory dentro de product-service** | Coesão imediata entre catálogo e estoque; extração para `inventory-service` justifica-se apenas com equipes ou escalas distintas |
| **Migrations** | Flyway em todos os serviços com banco — versionamento auditável do schema |
| **Config centralizado** | Spring Cloud Config Server (`config-server:8888`) — propriedades externalizadas por ambiente; microsserviços buscam config ao iniciar |

### API Gateway

| Aspecto | Decisão |
|---------|---------|
| **Spring Cloud Gateway** | Roteamento reativo, integrado ao ecossistema Spring Boot sem fricção |
| **JWT** | Autenticação stateless — o gateway valida o token e propaga claims para os serviços downstream |
| **Rate limiting** | Redis (Sliding Window) no gateway — proteção contra abuso sem afetar os serviços internos |
| **Gateway Secret** | Header `X-Gateway-Secret` validado nos serviços internos — impede chamadas diretas que bypassem o gateway |
| **CORS** | Configurado no gateway, não nos serviços individuais — ponto único de controle |

### Observabilidade

| Aspecto | Decisão |
|---------|---------|
| **Stack ELK** | Elasticsearch + Logstash + Kibana — logs estruturados em JSON pesquisáveis e visualizáveis |
| **Logback → Logstash** | Todos os serviços enviam logs via `LogstashTcpSocketAppender` — sem acoplamento direto ao Elasticsearch |
| **Spring Actuator** | Endpoints `/actuator/health`, `/actuator/metrics` habilitados em todos os serviços — usados também nos health checks do Docker Compose |
| **Correlation ID** | Propagado via header em todas as chamadas REST — rastreabilidade end-to-end nos logs |

---

## Trade-offs

### Consistência eventual

A comunicação assíncrona via Kafka implica que **os dados ficam temporariamente inconsistentes** entre os serviços.

| Situação | Comportamento |
|----------|--------------|
| `payment-service` confirmou, `order-service` ainda não processou | Order permanece `PENDING` por alguns milissegundos |
| Consumer restart após falha | Evento é reprocessado — idempotência garante que o estado final seja o correto |
| Kafka indisponível | Publicação falha; a operação de negócio ainda pode completar localmente, mas eventos são perdidos |

**Mitigação:** retry com backoff + DLT para eventos não processáveis, idempotência em todos os consumers.

### Complexidade operacional

Microsserviços introduzem complexidade que um monólito não possui:

| Aspecto | Custo |
|---------|-------|
| Múltiplos processos | 8+ containers para rodar o sistema completo localmente |
| Debugging distribuído | Uma requisição atravessa 3–4 serviços; rastrear erros exige correlation ID e logs centralizados |
| Testcontainers | Testes de integração requerem Docker em execução; mais lentos que testes unitários |
| Deploy | Cada serviço tem seu próprio ciclo de build/deploy; CI/CD mais elaborado |

**Mitigação:** Docker Compose para desenvolvimento local, ELK para correlação de logs, Testcontainers para ITs confiáveis.

### Latência

A cadeia de eventos adiciona latência end-to-end em comparação com chamadas síncronas diretas:

| Etapa | Latência típica (dev local) |
|-------|-----------------------------|
| REST (gateway → serviço) | < 10 ms |
| Publicação Kafka | < 5 ms |
| Consumer polling interval | até 500 ms |
| Round-trip completo (pedido → notificação) | 500 ms – 2 s |

**Mitigação:** para operações que exigem resposta síncrona (ex.: verificar estoque antes de confirmar pedido), REST direto entre serviços é mantido. Kafka é usado apenas para eventos de domínio que podem ser processados de forma assíncrona.

---

## Tech Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3 + Spring Cloud 2023.0.3 |
| Gateway | Spring Cloud Gateway (WebFlux) |
| Mensageria | Apache Kafka 7.6 (Confluent Platform) |
| Banco de dados | PostgreSQL 16 (um por serviço) |
| Migrations | Flyway |
| Segurança | Spring Security + JWT (JJWT) |
| Rate limiting | Redis 7.2 (Sliding Window via Spring Cloud Gateway) |
| Observabilidade | ELK Stack 8.13 (Elasticsearch, Logstash, Kibana) |
| E-mail (dev) | Mailpit |
| Containerização | Docker + Docker Compose |
| Testes | JUnit 5 + Mockito + Testcontainers + Spring Boot Test |
| Build | Maven 3.9 (multi-module) |

---

## Infraestrutura

### Portas expostas

| Componente          | Porta(s) | Descrição                        |
|---------------------|----------|----------------------------------|
| `api-gateway`       | 8080     | Ponto de entrada único           |
| `product-service`   | 8081     | Serviço de produtos              |
| `order-service`     | 8082     | Serviço de pedidos               |
| `payment-service`   | 8083     | Serviço de pagamentos            |
| `notification-service` | 8084  | Serviço de notificações          |
| `config-server`     | 8888     | Spring Cloud Config Server       |
| `product-db`        | 5432     | PostgreSQL — produtos            |
| `order-db`          | 5433     | PostgreSQL — pedidos             |
| `payment-db`        | 5434     | PostgreSQL — pagamentos          |
| `notification-db`   | 5435     | PostgreSQL — notificações        |
| `kafka`             | 9092     | Apache Kafka broker              |
| `zookeeper`         | 2181     | ZooKeeper (coordenação Kafka)    |
| `redis`             | 6379     | Redis (rate limiting)            |
| `elasticsearch`     | 9200     | Elasticsearch (logs)             |
| `kibana`            | 5601     | Kibana (dashboard de logs)       |
| `mailpit` (SMTP)    | 1025     | SMTP para envio de e-mails       |
| `mailpit` (UI)      | 8025     | Interface web do Mailpit         |

---

## Como executar

### Pré-requisitos

- Docker & Docker Compose
- Java 17+ (para build local)
- Maven 3.9+ (para build local)

### Subir toda a stack

```bash
# Build de todos os serviços e subir containers
docker compose up --build -d

# Verificar status
docker compose ps

# Acompanhar logs
docker compose logs -f api-gateway order-service
```

### Acessos após inicialização

| Recurso                | URL                         |
|------------------------|-----------------------------|
| API Gateway            | http://localhost:8080       |
| Kibana (logs)          | http://localhost:5601        |
| Mailpit (e-mails)      | http://localhost:8025        |
| Config Server          | http://localhost:8888        |
| Elasticsearch          | http://localhost:9200        |

### Autenticação

O gateway expõe `/api/v1/auth/login` para obter um JWT:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}'
```

Use o token retornado no header `Authorization: Bearer <token>` nas chamadas subsequentes.

### Exemplo de fluxo completo

```bash
TOKEN="<jwt_obtido_acima>"

# 1. Criar produto
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Notebook","description":"16GB RAM","price":4999.90,"sku":"NB-001","categoryId":1}'

# 2. Criar pedido
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"uuid-cliente","items":[{"productId":"uuid-produto","quantity":1}]}'

# 3. Verificar notificação no Mailpit
# Acesse http://localhost:8025
```

---

## Estrutura do projeto

```
ecommerce-microservices-kafka/
├── api-gateway/               ← Spring Cloud Gateway + JWT + Redis
├── config-server/             ← Spring Cloud Config Server
├── product-service/           ← Catálogo + Estoque
├── order-service/             ← Pedidos + State Machine
├── payment-service/           ← Pagamentos + Idempotência
├── notification-service/      ← Notificações event-driven
├── logstash/
│   └── pipeline/
│       └── logstash.conf      ← Pipeline de ingestão de logs
├── docs/
│   ├── architecture.md        ← Diagrama completo e decisões de design
│   └── domain.md              ← Bounded contexts e fluxos de domínio
├── docker-compose.yml         ← Stack completa (infra + serviços)
├── pom.xml                    ← Parent POM (multi-module Maven)
└── README.md
```

---

## Estratégia de branches

```
main          ← código estável / releases
└── develop   ← integração contínua
    └── feature/* / chore/* / fix/* / docs/* / test/*  ← desenvolvimento
```

Cada fase é desenvolvida em uma branch dedicada, integrada ao `develop` via Pull Request, e promovida para `main` ao final da fase.

---

## Roadmap

- [x] 1. Setup inicial do projeto
- [x] 2. Documentação inicial
- [x] 3. API Gateway + versionamento
- [x] 4. Product Service (base + domínio real)
- [x] 5. Order Service (REST síncrono)
- [x] 6. Introdução do Kafka (fundação correta)
- [x] 7. Payment Service
- [x] 8. Notification Service
- [x] 9. Resiliência + consistência
- [x] 10. Dockerização completa
- [x] 11. Observabilidade
- [x] 12. Segurança
- [x] 13. Testes
- [x] 14. README final

