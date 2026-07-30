# Restaurant Ordering Platform - Enterprise Architecture Blueprint

## 📋 Executive Summary

A production-ready, scalable restaurant ordering and table booking platform built with:
- **Clean Architecture** with Domain-Driven Design (DDD)
- **Multi-Agent AI System** using LangGraph/Semantic Kernel
- **Dual-Mode Interface** - Manual UI & Natural Language AI
- **Enterprise-Grade** - Microservice-ready, CQRS-capable, Cloud-native

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  React/Next  │  │ Mobile Apps  │  │ Voice API    │           │
│  │   Web UI     │  │ (iOS/Android)│  │ (Future)     │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────────┐
│                 API GATEWAY / BFF LAYER                          │
│     REST / GraphQL / WebSocket Endpoints                         │
└──────────────────────┬──────────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────────────────┐  ┌────▼──────────────────────┐
│  APPLICATION LAYER         │  │  AI AGENT LAYER           │
│ (Use Cases / Commands)     │  │ (LangGraph Orchestrator)  │
│                            │  │ ┌──────────────────────┐  │
│ - SearchMenuUseCase        │  │ │ Orchestrator Agent   │  │
│ - AddToCartUseCase         │  │ └──────────────────────┘  │
│ - BookTableUseCase         │  │ ┌─────┬────┬───┬───┬───┐ │
│ - CheckoutUseCase          │  │ │Menu │Cart│Book│Price│  │
│ - ETC                      │  │ │Agent│Agent│Agent│Agent│  │
│                            │  │ └─────┴────┴───┴───┴───┘ │
└───────┬────────────────────┘  └────┬──────────────────────┘
        │                            │
        └──────────────┬─────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                  DOMAIN LAYER (DDD)                             │
│                                                                  │
│ ┌──────────┐ ┌───────┐ ┌──────┐ ┌────────┐ ┌─────────┐        │
│ │   Menu   │ │ Cart  │ │Order │ │Booking │ │Payment  │        │
│ │ Entities │ │Entity │ │Entity│ │Entity  │ │ Entity  │        │
│ └──────────┘ └───────┘ └──────┘ └────────┘ └─────────┘        │
│                                                                  │
│ Value Objects, Aggregates, Domain Events                       │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│              INFRASTRUCTURE LAYER                               │
│                                                                  │
│ ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│ │ Repositories │  │  LLM Clients │  │Event Emitter │           │
│ │  (Database)  │  │ (OpenAI, etc)│  │  (Kafka)     │           │
│ └──────────────┘  └──────────────┘  └──────────────┘           │
│ ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│ │Cache Service │  │Payment Gate  │  │ Email/SMS    │           │
│ │ (Redis)      │  │  (Stripe)    │  │ (Twilio)     │           │
│ └──────────────┘  └──────────────┘  └──────────────┘           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Core Principles

### 1. **Clean Architecture**
- Independent frameworks
- Testable business logic
- Dependency Injection
- Clear separation of concerns

### 2. **Domain-Driven Design (DDD)**
- Rich domain models (not anemic)
- Ubiquitous language
- Bounded contexts
- Aggregate patterns

### 3. **SOLID Principles**
- Single Responsibility: Each class has one reason to change
- Open/Closed: Open to extension, closed to modification
- Liskov Substitution: Interfaces are properly inherited
- Interface Segregation: Clients don't depend on unused methods
- Dependency Inversion: Depend on abstractions, not concretions

### 4. **AI Integration**
- LangGraph for multi-agent orchestration
- Semantic Kernel for LLM abstraction
- Tool calling pattern
- Shared context/memory

---

## 📊 Data Flow Comparison

### Manual Flow
```
User (UI) 
  ↓
  ├→ SearchMenuScreen → SearchMenuUseCase → MenuRepository → Database
  ├→ AddToCartButton → AddToCartUseCase → CartRepository → Memory/DB
  └→ BookTableForm → BookTableUseCase → BookingRepository → Database
```

### AI Agent Flow
```
User (Chat)
  ↓
  └→ Natural Language Query
      ↓
      └→ Orchestrator Agent
          ├→ Intent Detection (NER, Classification)
          ├→ Task Planning (Multi-intent handling)
          ├→ Agent Routing
          │   ├→ Menu Agent → MenuUseCase
          │   ├→ Cart Agent → CartUseCase
          │   ├→ Booking Agent → BookingUseCase
          │   └→ Pricing Agent → PricingUseCase
          ├→ Parallel Execution
          └→ Response Aggregation
              ↓
              └→ Structured Response (JSON/UI)
```

### Shared Logic Layer
```
Both flows → Use Case Implementations
          → Domain Business Logic
          → Repository Interfaces
          → Database Operations
```

---

## 🏢 Microservice-Ready Structure

```
restaurant-ordering-platform/
├── core-services/
│   ├── menu-service/          # Menu domain
│   ├── cart-service/          # Cart domain
│   ├── order-service/         # Order domain
│   ├── booking-service/       # Booking domain
│   └── payment-service/       # Payment domain
│
├── ai-services/
│   ├── orchestrator-agent/    # Main agent coordinator
│   ├── menu-agent/            # Menu specialization
│   ├── cart-agent/            # Cart specialization
│   ├── booking-agent/         # Booking specialization
│   └── agent-common/          # Shared agent utilities
│
├── infrastructure/
│   ├── database/              # Repository implementations
│   ├── cache/                 # Redis, in-memory
│   ├── message-queue/         # Kafka, RabbitMQ
│   ├── payment-gateway/       # Stripe, PayPal
│   └── llm-providers/         # OpenAI, Azure
│
└── api-gateway/
    ├── rest-endpoints/
    ├── graphql-endpoints/
    └── websocket-handlers/
```

---

## 🔄 Request-Response Lifecycle

### Single-Intent Example: "Show me veg pizzas"

```
1. REQUEST ARRIVAL
   ├─ Endpoint: POST /api/query
   └─ Body: { "message": "Show me veg pizzas" }

2. REQUEST PARSING
   ├─ Extract: query, session_id, user_context
   └─ Store in RequestContext

3. INTENT DETECTION (Orchestrator)
   ├─ LLM: Classify intent → "SEARCH_MENU"
   ├─ Extract entities: category="Pizza", type="Veg"
   └─ Confidence score: 0.95

4. AGENT ROUTING
   ├─ Intent: SEARCH_MENU → Menu Agent
   └─ Params: { category: "Pizza", type: "Veg" }

5. USE CASE EXECUTION
   ├─ SearchMenuUseCase.execute()
   ├─ MenuRepository.searchByCategory("Pizza")
   ├─ Filter by type "Veg"
   └─ Return: List<MenuItem>

6. RESPONSE BUILDING
   ├─ Format for UI (Cards)
   ├─ Add agent commentary
   └─ A2UI JSON generation

7. RESPONSE SENDING
   ├─ HTTP 200 OK
   └─ Body: { "result": MenuItems[], "message": "..." }
```

### Multi-Intent Example: "Add 2 farmhouse pizzas and book a table for 5"

```
1. REQUEST ARRIVAL
   └─ Message: "Add 2 farmhouse pizzas and book a table for 5"

2. INTENT DETECTION
   ├─ Intent 1: ADD_TO_CART
   │  └─ item="Farmhouse Pizza", quantity=2
   └─ Intent 2: BOOK_TABLE
      └─ people=5, time=null

3. PARALLEL AGENT EXECUTION
   ├─ Cart Agent
   │  ├─ AddToCartUseCase.execute(itemId, qty=2)
   │  ├─ Calculations: price, taxes
   │  └─ Response: { success: true, cartTotal: 500 }
   │
   └─ Booking Agent
      ├─ CheckAvailabilityUseCase.execute(people=5)
      ├─ BookTableUseCase.execute(people=5, time=now+1h)
      └─ Response: { bookingId: "TB-12345", time: "4:00 PM" }

4. RESPONSE AGGREGATION
   ├─ Combine: cart_response + booking_response
   ├─ Create unified message
   └─ Generate A2UI JSON

5. FINAL RESPONSE
   ```json
   {
     "success": true,
     "intents_executed": 2,
     "actions": [
       {
         "type": "ADD_TO_CART",
         "result": { "item": "Farmhouse Pizza", "qty": 2, "price": 500 }
       },
       {
         "type": "BOOK_TABLE",
         "result": { "bookingId": "TB-12345", "people": 5, "time": "4:00 PM" }
       }
     ],
     "message": "✅ 2 Farmhouse Pizzas added. ✅ Table booked for 5 at 4:00 PM"
   }
   ```
```

---

## 🗄️ Database Schema Normalization

### Entity-Relationship Diagram

```
┌──────────────────┐         ┌──────────────────┐
│     Customer     │         │     Booking      │
├──────────────────┤         ├──────────────────┤
│ customer_id (PK) │◄────────│ booking_id (PK)  │
│ email            │1    *   │ customer_id (FK) │
│ phone            │         │ table_id (FK)    │
│ name             │         │ num_people       │
└──────────────────┘         │ booking_time     │
                             │ status           │
                             └──────────────────┘
                                      │
                             ┌────────▼──────────┐
                             │     Table         │
                             ├───────────────────┤
                             │ table_id (PK)     │
                             │ capacity          │
                             │ location          │
                             └───────────────────┘

┌──────────────────┐         ┌──────────────────┐
│      Order       │         │     MenuItem     │
├──────────────────┤         ├──────────────────┤
│ order_id (PK)    │◄────┐   │ item_id (PK)     │
│ customer_id (FK) │     │   │ name             │
│ total_price      │     │   │ price            │
│ status           │     │   │ category_id (FK) │
│ created_at       │     │   │ type (Veg/NonVeg)│
└──────────────────┘     │   └──────────────────┘
         ▲               │          △
         │               │          │
    ┌────┼───────────────┼──────────┤
    │    │               │          │
┌───┴────▼───┐   ┌──────▼────────┐ │
│  OrderItem  │   │    CartItem   │ │
├─────────────┤   ├───────────────┤ │
│ order_id(FK)├──→│ item_id(FK)───┴─┘
│ item_id(FK) │   │ quantity      │
│ quantity    │   │ price         │
│ price       │   │ cart_id(FK)   │
└─────────────┘   └───────────────┘
                          ▲
                          │
                   ┌──────┴────────┐
                   │     Cart      │
                   ├───────────────┤
                   │ cart_id (PK)  │
                   │ customer_id(FK)
                   │ total_price   │
                   │ created_at    │
                   └───────────────┘

┌──────────────────┐         ┌──────────────────┐
│     Payment      │         │     Category     │
├──────────────────┤         ├──────────────────┤
│ payment_id (PK)  │         │ category_id (PK) │
│ order_id (FK)    │         │ name             │
│ amount           │         │ description      │
│ status           │         │ icon_url         │
│ method           │         └──────────────────┘
│ transaction_id   │
└──────────────────┘
```

---

## 🔌 Agent Tool Definition

### Tool Registry Pattern

```typescript
interface AgentTool {
  name: string;
  description: string;
  inputSchema: JSONSchema;
  execute: (input: any) => Promise<ToolResult>;
}

// Tools available to ALL agents
const SHARED_TOOLS = [
  // Menu Tools
  SearchMenuTool,
  GetMenuItemDetailsTool,
  GetCategoriesTool,
  
  // Cart Tools
  AddToCartTool,
  RemoveFromCartTool,
  UpdateCartItemTool,
  GetCartTool,
  ClearCartTool,
  
  // Booking Tools
  CheckTableAvailabilityTool,
  BookTableTool,
  UpdateBookingTool,
  CancelBookingTool,
  
  // Pricing Tools
  CalculatePriceTool,
  ApplyCouponTool,
  CalculateTaxTool,
  
  // Checkout Tools
  CreateOrderTool,
  ProcessPaymentTool,
  GenerateReceiptTool
];

// Agent-specific overrides (optional)
MENU_AGENT_TOOLS = SHARED_TOOLS.filter(x => x.category === 'menu');
BOOKING_AGENT_TOOLS = SHARED_TOOLS.filter(x => x.category === 'booking');
```

---

## 📈 Scaling Strategy

### Horizontal Scaling
```
Load Balancer (NGINX/HAProxy)
        ↓
    ┌───┴───┬───────┬───────┐
    ↓       ↓       ↓       ↓
  [API-1] [API-2] [API-3] [API-N]
    ├───────┴───────┴───────┤
         Database (Read Replicas)
         Cache (Redis Cluster)
         Message Queue (Kafka Cluster)
```

### Microservice Deployment
```
Docker Containers
  → Kubernetes Orchestration
    → Service Mesh (Istio)
      → Load Balancing
      → Circuit Breaking
      → Distributed Tracing
```

---

## 🔒 Security Architecture

```
┌─────────────────┐
│  Authentication │ ← JWT, OAuth 2.0
├─────────────────┤
│  Authorization  │ ← RBAC, ABAC
├─────────────────┤
│    Encryption   │ ← TLS 1.3, AES-256
├─────────────────┤
│   Audit Logs    │ ← All user actions
├─────────────────┤
│  Rate Limiting  │ ← DDoS protection
├─────────────────┤
│  Input Validation│ ← Sanitization
└─────────────────┘
```

---

## 📊 Monitoring & Observability

```
Application Metrics
  └→ Prometheus (collection)
     └→ Grafana (visualization)

Distributed Tracing
  └→ OpenTelemetry (instrumentation)
     └→ Jaeger/Zipkin (backend)

Logging
  └→ Structured Logging (JSON)
     └→ ELK Stack / Splunk
        └→ Real-time dashboards

Health Checks
  └→ Service Health API
     └→ Database connections
        └→ External service status
```

---

## 🚀 Deployment Pipeline

```
Code Push (GitHub)
  ↓
GitHub Actions CI
  ├─ Unit Tests
  ├─ Integration Tests
  ├─ Code Quality (SonarQube)
  └─ Security Scan (SAST)
  ↓
Build Docker Image
  ↓
Push to Container Registry
  ↓
Deploy to Dev Environment
  ├─ Smoke Tests
  └─ Contract Tests
  ↓
Deploy to Staging
  ├─ Performance Tests
  └─ Security Tests
  ↓
Deploy to Production
  ├─ Blue-Green Deployment
  ├─ Canary Release
  └─ Rollback Plan
```

---

## 📋 Next Steps

1. Create detailed folder structure
2. Define domain models with DDD
3. Implement use cases and repository interfaces
4. Design agent orchestrator
5. Create database schema SQL
6. Write TypeScript implementation
7. Set up API endpoints
8. Configure LLM integration
9. Build state management
10. Create deployment configs

**Continue to next file for complete implementation details...**

