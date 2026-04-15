# Domínios e fronteiras de serviço

## Bounded Contexts

Cada microsserviço representa um **Bounded Context** independente — com seu próprio modelo de domínio, banco de dados e linguagem ubíqua. Eles não compartilham tabelas nem entidades diretamente; a comunicação ocorre via API REST (fases iniciais) ou eventos Kafka (fase 6+).

---

## Serviços e responsabilidades

### `api-gateway`
- **Responsabilidade:** roteamento de requisições externas para os serviços internos
- **Não possui** lógica de negócio nem banco de dados
- **Expõe:** versionamento de API (`/api/v1/...`)
- **Futuro:** autenticação/autorização via JWT (fase 12)

---

### `product-service`
- **Responsabilidade:** gerenciamento do catálogo de produtos e controle de estoque
- **Entidades principais:**
  - `Product` — dados do produto (nome, descrição, preço, categoria, SKU)
  - `Inventory` — quantidade em estoque, reservas, threshold mínimo
- **Operações:**
  - CRUD de produtos
  - Consulta de disponibilidade de estoque
  - Reserva e liberação de estoque (chamado pelo `order-service`)
- **Banco:** `product_db`

#### Inventory dentro do product-service

O controle de estoque (`Inventory`) reside **intencionalmente** dentro do `product-service` nesta fase, pois:

1. **Coesão imediata:** produto e estoque estão fortemente acoplados — raramente um produto existe sem estoque e vice-versa.
2. **Simplicidade operacional:** evita uma chamada de serviço extra em operações de leitura de catálogo.
3. **Transação local:** reserva e atualização de estoque podem ser feitas em uma única transação ACID.

**Quando extrair para um `inventory-service` separado?**

A extração faz sentido quando um ou mais dos seguintes critérios for atendido:

| Critério | Indicador |
|----------|-----------|
| Escala diferente | Estoque tem volume de escritas muito maior que o catálogo |
| Times independentes | Equipes distintas gerenciam produtos vs. logística |
| Integrações específicas | Estoque precisa integrar com ERPs externos |
| Complexidade crescente | Regras de warehouse, múltiplos depósitos, RFID |

Até lá, `Inventory` é um **agregado interno** do `product-service`, acessível via sua própria API.

---

### `order-service`
- **Responsabilidade:** ciclo de vida dos pedidos
- **Entidades principais:**
  - `Order` — pedido com status e totais
  - `OrderItem` — linha de item (produto, quantidade, preço snapshot)
- **Fluxo de estados:**
  ```
  PENDING → CONFIRMED → PAID → SHIPPED → DELIVERED
                    ↘ CANCELLED
  ```
- **Interações:**
  - Consulta estoque em `product-service` (REST síncrono)
  - Solicita pagamento em `payment-service` (REST síncrono → Kafka na fase 6)
- **Banco:** `order_db`

> **Nota:** o preço do produto é copiado como **snapshot** no momento da criação do pedido. Mudanças futuras no catálogo não afetam pedidos existentes.

---

### `payment-service`
- **Responsabilidade:** processamento e rastreamento de pagamentos
- **Entidades principais:**
  - `Payment` — registro de pagamento com status e método
- **Fluxo de estados:**
  ```
  PENDING → PROCESSING → PAID
                     ↘ FAILED → REFUNDED
  ```
- **Integrações futuras:** gateway de pagamento externo (Stripe, PagSeguro)
- **Banco:** `payment_db`

> ✅ Identificado no roadmap como serviço com lógica própria consolidada.

---

### `notification-service`
- **Responsabilidade:** envio de notificações disparadas por eventos do sistema
- **Não possui banco de dados** nesta fase (stateless)
- **Consome eventos Kafka:**
  - `payment.processed` → envia e-mail de confirmação ao cliente
  - `payment.failed` → envia e-mail de falha com instruções
  - `order.confirmed` → envia resumo do pedido
- **Canal inicial:** e-mail (via SMTP / MailHog em desenvolvimento)
- **Futuro:** push notification, SMS, webhook

> ✅ Identificado no roadmap como serviço event-driven puro.

---

## Fluxo de criação de pedido (fase atual — REST síncrono)

```
Cliente
  │
  │  POST /api/v1/orders
  ▼
API Gateway
  │
  ▼
order-service
  │
  ├──► GET /api/v1/products/{id}/inventory   ──► product-service
  │         [verifica disponibilidade]
  │
  ├──► POST /api/v1/payments                 ──► payment-service
  │         [inicia pagamento]
  │
  └──► persiste Order [status: CONFIRMED]
```

## Fluxo de criação de pedido (fase 6+ — Kafka assíncrono)

```
Cliente
  │
  │  POST /api/v1/orders
  ▼
API Gateway ──► order-service
                    │
                    ├── persiste Order [status: PENDING]
                    │
                    └── publica → order.created
                                       │
                    ┌──────────────────┘
                    │
                    ▼
              payment-service
                    │
                    ├── processa pagamento
                    │
                    ├── publica → payment.processed
                    │                    │
                    │       ┌────────────┴────────────┐
                    │       ▼                         ▼
                    │  order-service          notification-service
                    │  [PENDING → PAID]       [envia e-mail]
                    │
                    └── publica → payment.failed (em caso de erro)
                                       │
                         ┌─────────────┴─────────────┐
                         ▼                            ▼
                    order-service           notification-service
                    [PENDING → CANCELLED]   [envia e-mail de falha]
```

---

## Eventos de domínio planejados (fase 6)

| Evento              | Publicado por       | Consumido por                          |
|---------------------|---------------------|----------------------------------------|
| `order.created`     | order-service       | payment-service                        |
| `order.confirmed`   | order-service       | notification-service                   |
| `order.cancelled`   | order-service       | notification-service, product-service  |
| `payment.processed` | payment-service     | order-service, notification-service    |
| `payment.failed`    | payment-service     | order-service, notification-service    |
