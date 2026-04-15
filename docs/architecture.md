# Arquitetura — ecommerce-microservices-kafka

## Visão geral

A plataforma é composta por microsserviços independentes organizados em duas camadas de comunicação:

- **Síncrona (REST):** cliente → API Gateway → serviços de negócio
- **Assíncrona (Kafka):** serviços publicam e consomem eventos de domínio

---

## Diagrama de arquitetura

```
  ┌──────────────────────────────────────────────────────────────────────────┐
  │                              CLIENT / UI                                 │
  └───────────────────────────────────┬────────────────────────────────────  ┘
                                      │ HTTP
                                      ▼
  ┌──────────────────────────────────────────────────────────────────────────┐
  │                           API Gateway  :8080                             │
  │                        (Spring Cloud Gateway)                            │
  │                                                                          │
  │   /api/v1/products/**  →  product-service                                │
  │   /api/v1/orders/**    →  order-service                                  │
  │   /api/v1/payments/**  →  payment-service                                │
  └───────┬───────────────────────┬──────────────────────┬───────────────────┘
          │ REST                  │ REST                  │ REST
          ▼                       ▼                       ▼
  ┌───────────────┐      ┌────────────────┐      ┌────────────────────┐
  │product-service│      │ order-service  │      │  payment-service   │
  │    :8081      │◄─────│    :8082       │      │      :8083         │
  │               │ REST │                │      │                    │
  │ - catalog     │      │ - order mgmt   │      │ - payment process  │
  │ - inventory * │      │ - item reserve │      │ - status tracking  │
  │               │      │                │      │                    │
  │ [product_db]  │      │ [order_db]     │      │ [payment_db]       │
  └───────────────┘      └───────┬────────┘      └─────────┬──────────┘
                                 │                          │
                         ┌───────▼──────────────────────────▼───────┐
                         │                                           │
                         │             Apache Kafka  :9092           │
                         │                                           │
                         │  topics:                                  │
                         │  • order.created                          │
                         │  • order.confirmed                        │
                         │  • payment.processed                      │
                         │  • payment.failed                         │
                         │                                           │
                         └──────────────────┬────────────────────────┘
                                            │
                                            ▼
                                ┌───────────────────────┐
                                │  notification-service  │
                                │         :8084          │
                                │                        │
                                │  - email               │
                                │  - push (futuro)       │
                                └───────────────────────┘
```

> `*` Inventory está dentro do `product-service` nesta fase. Ver [domain.md](./domain.md#inventory-dentro-do-product-service).

---

## Comunicação entre serviços

### Fase atual (REST síncrono)

```
order-service  ──REST──►  product-service   (verificar/reservar estoque)
order-service  ──REST──►  payment-service   (iniciar pagamento)
```

### Fase 6+ (Kafka assíncrono)

```
order-service      ──publish──►  order.created       ──►  payment-service
payment-service    ──publish──►  payment.processed   ──►  order-service
                                                     ──►  notification-service
payment-service    ──publish──►  payment.failed      ──►  order-service
                                                     ──►  notification-service
```

---

## Infraestrutura

| Componente    | Imagem                        | Porta  | Fase   |
|---------------|-------------------------------|--------|--------|
| product-db    | postgres:16-alpine            | 5432   | atual  |
| order-db      | postgres:16-alpine            | 5433   | atual  |
| payment-db    | postgres:16-alpine            | 5434   | atual  |
| Kafka         | confluentinc/cp-kafka:7.6.1   | 9092   | fase 6 |
| Zookeeper     | confluentinc/cp-zookeeper     | 2181   | fase 6 |

---

## Decisões de design

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Banco por serviço | PostgreSQL isolado | Autonomia de schema, sem acoplamento de dados |
| Gateway único | Spring Cloud Gateway | Roteamento centralizado, ponto único de entrada |
| Migrations | Flyway | Controle de versão do schema auditável |
| Mensageria | Apache Kafka | Alta throughput, replay de eventos, desacoplamento |
| Comunicação inicial | REST síncrono | Simplicidade — Kafka introduzido apenas na fase 6 |
