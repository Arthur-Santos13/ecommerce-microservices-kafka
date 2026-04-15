# ecommerce-microservices-kafka

Plataforma de e-commerce construída com arquitetura de microsserviços, comunicação assíncrona via Apache Kafka e orquestrada via Docker.

---

## Arquitetura

Diagrama detalhado, padrões de comunicação e decisões de design: [docs/architecture.md](./docs/architecture.md)

```
  Cliente
     │ HTTP
     ▼
  API Gateway :8080
     ├──► product-service :8081  [product_db]
     ├──► order-service   :8082  [order_db]
     └──► payment-service :8083  [payment_db]

  order-service / payment-service
     └──► Apache Kafka :9092
               └──► notification-service :8084
```

## Documentação

| Documento | Descrição |
|-----------|-----------|
| [docs/architecture.md](./docs/architecture.md) | Diagrama completo, comunicação síncrona/assíncrona, decisões de design |
| [docs/domain.md](./docs/domain.md) | Bounded contexts, responsabilidades, fluxos e eventos de domínio |

---

## Microsserviços

| Serviço               | Porta | Descrição                                          |
|-----------------------|-------|----------------------------------------------------|
| `api-gateway`         | 8080  | Ponto de entrada único (Spring Cloud Gateway)      |
| `product-service`     | 8081  | Catálogo e gerenciamento de produtos               |
| `order-service`       | 8082  | Ciclo de vida dos pedidos                          |
| `payment-service`     | 8083  | Processamento de pagamentos                        |
| `notification-service`| 8084  | Envio de notificações via eventos Kafka            |

## Tech Stack

- **Java 17** + **Spring Boot 3.3**
- **Spring Cloud Gateway** — API Gateway
- **Apache Kafka** — Mensageria assíncrona
- **PostgreSQL** — Banco de dados por serviço (Database-per-Service pattern)
- **Flyway** — Migrations de banco de dados
- **Docker / Docker Compose** — Containerização e infraestrutura local

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker & Docker Compose

## Como executar a infraestrutura

> **Fase atual:** o `docker-compose.yml` contém apenas os bancos de dados.  
> Kafka/Zookeeper e os próprios microsserviços serão adicionados nas fases seguintes.

```bash
# Subir os bancos de dados
docker compose up -d

# Verificar os containers
docker compose ps
```

## Portas dos serviços de infraestrutura

| Serviço      | Porta | Fase        |
|--------------|-------|-------------|
| product-db   | 5432  | atual       |
| order-db     | 5433  | atual       |
| payment-db   | 5434  | atual       |
| Kafka        | 9092  | fase 6      |
| Zookeeper    | 2181  | fase 6      |

## Estrutura do projeto

```
ecommerce-microservices-kafka/
├── api-gateway/
├── product-service/
├── order-service/
├── payment-service/
├── notification-service/
├── docker-compose.yml
├── pom.xml                 ← Parent POM (multi-module Maven)
└── README.md
```

## Estratégia de branches

```
main          ← código estável / releases
└── develop   ← integração contínua
    └── feature/* / chore/* / fix/*  ← desenvolvimento
```

## Roadmap

- [x] 1. Setup inicial do projeto
- [x] 2. Documentação inicial
- [x] 3. API Gateway + versionamento
- [x] 4. Product Service (base + domínio real)
- [ ] 5. Order Service (REST síncrono)
- [ ] 6. Introdução do Kafka (fundação correta)
- [ ] 7. Payment Service
- [ ] 8. Notification Service
- [ ] 9. Resiliência + consistência
- [ ] 10. Dockerização completa
- [ ] 11. Observabilidade
- [ ] 12. Segurança
- [ ] 13. Testes
- [ ] 14. README final

