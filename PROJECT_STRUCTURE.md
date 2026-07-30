# Restaurant Platform - Complete Project Structure

## 📂 Backend Project Structure (TypeScript/Node.js)

```
restaurant-ordering-platform/
│
├── 📦 packages/
│   ├── core/                              # Core domain logic (shared)
│   │   ├── src/
│   │   │   ├── domain/
│   │   │   │   ├── entities/
│   │   │   │   │   ├── Menu.entity.ts
│   │   │   │   │   ├── MenuItem.entity.ts
│   │   │   │   │   ├── Cart.entity.ts
│   │   │   │   │   ├── CartItem.entity.ts
│   │   │   │   │   ├── Order.entity.ts
│   │   │   │   │   ├── OrderItem.entity.ts
│   │   │   │   │   ├── Booking.entity.ts
│   │   │   │   │   ├── Table.entity.ts
│   │   │   │   │   ├── Customer.entity.ts
│   │   │   │   │   ├── Payment.entity.ts
│   │   │   │   │   ├── Category.entity.ts
│   │   │   │   │   └── Coupon.entity.ts
│   │   │   │   │
│   │   │   │   ├── value-objects/
│   │   │   │   │   ├── Money.vo.ts
│   │   │   │   │   ├── Price.vo.ts
│   │   │   │   │   ├── Discount.vo.ts
│   │   │   │   │   ├── Location.vo.ts
│   │   │   │   │   ├── Rating.vo.ts
│   │   │   │   │   ├── DateRange.vo.ts
│   │   │   │   │   └── TimeSlot.vo.ts
│   │   │   │   │
│   │   │   │   ├── aggregates/
│   │   │   │   │   ├── CartAggregate.ts
│   │   │   │   │   ├── OrderAggregate.ts
│   │   │   │   │   ├── BookingAggregate.ts
│   │   │   │   │   └── PaymentAggregate.ts
│   │   │   │   │
│   │   │   │   ├── events/
│   │   │   │   │   ├── MenuItemAdded.event.ts
│   │   │   │   │   ├── ItemAddedToCart.event.ts
│   │   │   │   │   ├── TableBooked.event.ts
│   │   │   │   │   ├── OrderCreated.event.ts
│   │   │   │   │   ├── PaymentProcessed.event.ts
│   │   │   │   │   └── OrderConfirmed.event.ts
│   │   │   │   │
│   │   │   │   └── errors/
│   │   │   │       ├── DomainError.ts
│   │   │   │       ├── BusinessRuleViolation.ts
│   │   │   │       └── ValidationError.ts
│   │   │   │
│   │   │   └── shared/
│   │   │       ├── interfaces/
│   │   │       │   ├── Repository.interface.ts
│   │   │       │   ├── UseCase.interface.ts
│   │   │       │   └── Entity.interface.ts
│   │   │       ├── utils/
│   │   │       ├── guards/
│   │   │       └── constants.ts
│   │   │
│   │   └── package.json
│   │
│   ├── application/                      # Application layer (use cases)
│   │   ├── src/
│   │   │   ├── use-cases/
│   │   │   │   ├── menu/
│   │   │   │   │   ├── SearchMenu.usecase.ts
│   │   │   │   │   ├── GetMenuDetails.usecase.ts
│   │   │   │   │   ├── GetCategories.usecase.ts
│   │   │   │   │   └── index.ts
│   │   │   │   │
│   │   │   │   ├── cart/
│   │   │   │   │   ├── AddToCart.usecase.ts
│   │   │   │   │   ├── RemoveFromCart.usecase.ts
│   │   │   │   │   ├── UpdateCartItem.usecase.ts
│   │   │   │   │   ├── ViewCart.usecase.ts
│   │   │   │   │   ├── ClearCart.usecase.ts
│   │   │   │   │   └── index.ts
│   │   │   │   │
│   │   │   │   ├── booking/
│   │   │   │   │   ├── CheckAvailability.usecase.ts
│   │   │   │   │   ├── BookTable.usecase.ts
│   │   │   │   │   ├── UpdateBooking.usecase.ts
│   │   │   │   │   ├── CancelBooking.usecase.ts
│   │   │   │   │   └── index.ts
│   │   │   │   │
│   │   │   │   ├── pricing/
│   │   │   │   │   ├── CalculatePrice.usecase.ts
│   │   │   │   │   ├── ApplyCoupon.usecase.ts
│   │   │   │   │   ├── CalculateTax.usecase.ts
│   │   │   │   │   └── index.ts
│   │   │   │   │
│   │   │   │   ├── checkout/
│   │   │   │   │   ├── CreateOrder.usecase.ts
│   │   │   │   │   ├── ProcessPayment.usecase.ts
│   │   │   │   │   ├── GenerateReceipt.usecase.ts
│   │   │   │   │   ├── ConfirmOrder.usecase.ts
│   │   │   │   │   └── index.ts
│   │   │   │   │
│   │   │   │   └── recommendation/
│   │   │   │       ├── GetRecommendations.usecase.ts
│   │   │   │       └── index.ts
│   │   │   │
│   │   │   ├── dtos/
│   │   │   │   ├── MenuDTO.ts
│   │   │   │   ├── CartDTO.ts
│   │   │   │   ├── OrderDTO.ts
│   │   │   │   ├── BookingDTO.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── mappers/
│   │   │   │   ├── MenuMapper.ts
│   │   │   │   ├── CartMapper.ts
│   │   │   │   ├── OrderMapper.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   └── services/
│   │   │       ├── CacheService.ts
│   │   │       ├── NotificationService.ts
│   │   │       └── index.ts
│   │   │
│   │   └── package.json
│   │
│   ├── infrastructure/                   # Infrastructure layer
│   │   ├── src/
│   │   │   ├── repositories/
│   │   │   │   ├── MenuRepository.ts
│   │   │   │   ├── CartRepository.ts
│   │   │   │   ├── OrderRepository.ts
│   │   │   │   ├── BookingRepository.ts
│   │   │   │   ├── PaymentRepository.ts
│   │   │   │   ├── CustomerRepository.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── database/
│   │   │   │   ├── migrations/
│   │   │   │   │   ├── 001_create_customers.sql
│   │   │   │   │   ├── 002_create_menu.sql
│   │   │   │   │   ├── 003_create_cart.sql
│   │   │   │   │   ├── 004_create_orders.sql
│   │   │   │   │   ├── 005_create_bookings.sql
│   │   │   │   │   └── 006_create_payments.sql
│   │   │   │   ├── seeds/
│   │   │   │   │   ├── seed_categories.sql
│   │   │   │   │   ├── seed_menu_items.sql
│   │   │   │   │   └── seed_tables.sql
│   │   │   │   ├── PrismaClient.ts
│   │   │   │   └── schema.prisma
│   │   │   │
│   │   │   ├── cache/
│   │   │   │   ├── RedisCache.ts
│   │   │   │   └── MemoryCache.ts
│   │   │   │
│   │   │   ├── event-bus/
│   │   │   │   ├── EventBus.ts
│   │   │   │   ├── KafkaEventBus.ts
│   │   │   │   └── InMemoryEventBus.ts
│   │   │   │
│   │   │   ├── payment-gateway/
│   │   │   │   ├── StripePaymentGateway.ts
│   │   │   │   ├── PayPalPaymentGateway.ts
│   │   │   │   └── PaymentGatewayFactory.ts
│   │   │   │
│   │   │   ├── llm-providers/
│   │   │   │   ├── OpenAIProvider.ts
│   │   │   │   ├── AzureOpenAIProvider.ts
│   │   │   │   ├── SemanticKernelAdapter.ts
│   │   │   │   └── LLMProviderFactory.ts
│   │   │   │
│   │   │   ├── notifications/
│   │   │   │   ├── EmailNotification.ts
│   │   │   │   ├── SMSNotification.ts
│   │   │   │   └── PushNotification.ts
│   │   │   │
│   │   │   └── config/
│   │   │       ├── database.config.ts
│   │   │       ├── cache.config.ts
│   │   │       └── payment.config.ts
│   │   │
│   │   └── package.json
│   │
│   ├── ai-agents/                        # Multi-Agent AI System
│   │   ├── src/
│   │   │   ├── orchestrator/
│   │   │   │   ├── OrchestratorAgent.ts
│   │   │   │   ├── IntentDetector.ts
│   │   │   │   ├── TaskPlanner.ts
│   │   │   │   ├── AgentRouter.ts
│   │   │   │   ├── ResponseAggregator.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── agents/
│   │   │   │   ├── MenuAgent.ts
│   │   │   │   ├── CartAgent.ts
│   │   │   │   ├── BookingAgent.ts
│   │   │   │   ├── PricingAgent.ts
│   │   │   │   ├── CheckoutAgent.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── tools/
│   │   │   │   ├── SearchMenuTool.ts
│   │   │   │   ├── AddToCartTool.ts
│   │   │   │   ├── BookTableTool.ts
│   │   │   │   ├── CalculatePriceTool.ts
│   │   │   │   ├── ProcessPaymentTool.ts
│   │   │   │   ├── ToolRegistry.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── memory/
│   │   │   │   ├── SessionMemory.ts
│   │   │   │   ├── ContextStore.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   ├── prompts/
│   │   │   │   ├── orchestrator.prompt.ts
│   │   │   │   ├── menu-agent.prompt.ts
│   │   │   │   ├── cart-agent.prompt.ts
│   │   │   │   └── index.ts
│   │   │   │
│   │   │   └── types/
│   │   │       ├── Agent.types.ts
│   │   │       ├── Tool.types.ts
│   │   │       └── Message.types.ts
│   │   │
│   │   └── package.json
│   │
│   └── api/                              # API Gateway / REST/GraphQL
│       ├── src/
│       │   ├── routes/
│       │   │   ├── menu.routes.ts
│       │   │   ├── cart.routes.ts
│       │   │   ├── orders.routes.ts
│       │   │   ├── bookings.routes.ts
│       │   │   ├── chat.routes.ts
│       │   │   └── payments.routes.ts
│       │   │
│       │   ├── controllers/
│       │   │   ├── MenuController.ts
│       │   │   ├── CartController.ts
│       │   │   ├── OrderController.ts
│       │   │   ├── BookingController.ts
│       │   │   ├── ChatController.ts
│       │   │   └── PaymentController.ts
│       │   │
│       │   ├── middlewares/
│       │   │   ├── auth.middleware.ts
│       │   │   ├── validation.middleware.ts
│       │   │   ├── error-handler.middleware.ts
│       │   │   └── logger.middleware.ts
│       │   │
│       │   ├── graphql/
│       │   │   ├── schema.graphql
│       │   │   ├── resolvers/
│       │   │   └── types.ts
│       │   │
│       │   ├── websocket/
│       │   │   ├── ChatSocket.ts
│       │   │   └── NotificationSocket.ts
│       │   │
│       │   ├── dependency-injection/
│       │   │   └── container.ts
│       │   │
│       │   └── app.ts
│       │
│       └── package.json
│
├── 📁 docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
│
├── 📁 k8s/
│   ├── namespace.yml
│   ├── deployment.yml
│   ├── service.yml
│   ├── ingress.yml
│   ├── configmap.yml
│   └── secrets.yml
│
├── 📁 frontend/
│   ├── apps/
│   │   ├── web/                          # Next.js Web App
│   │   │   ├── src/
│   │   │   │   ├── app/
│   │   │   │   ├── components/
│   │   │   │   ├── pages/
│   │   │   │   └── hooks/
│   │   │   └── package.json
│   │   │
│   │   └── mobile/                       # React Native (future)
│   │       ├── src/
│   │       └── package.json
│   │
│   └── packages/
│       ├── ui-components/                # Shared UI components
│       ├── api-client/                   # Shared API client
│       └── state-management/             # Zustand/Redux stores
│
├── 📁 tests/
│   ├── unit/
│   ├── integration/
│   ├── e2e/
│   └── performance/
│
├── 📁 docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DATABASE.md
│   ├── AGENTS.md
│   └── DEPLOYMENT.md
│
├── .env.example
├── .dockerignore
├── .gitignore
├── docker-compose.yml
├── turbo.json                            # Monorepo config
├── package.json
├── tsconfig.json
└── README.md
```

---

## 🏛️ Core Folder Descriptions

### `packages/core/`
- **Pure domain logic** (no dependencies on frameworks)
- Entities, value objects, aggregates
- Domain errors and events
- Business rule implementations
- Can be used in any framework

### `packages/application/`
- **Use case orchestration**
- Input/output handling
- DTO transformations
- Service coordination
- **Re-usable by UI and AI agents**

### `packages/infrastructure/`
- **All external dependencies**
- Database (Prisma)
- Cache (Redis)
- LLM providers
- Payment gateways
- Easy to swap implementations

### `packages/ai-agents/`
- **Multi-agent orchestration**
- Intent detection via LLM
- Task planning
- Agent routing
- Tool registry
- **Uses same use cases as manual UI**

### `packages/api/`
- **API Gateway**
- REST endpoints
- GraphQL endpoints
- WebSocket connections
- Dependency injection setup
- Error handling

---

## 🔄 Monorepo Benefits

```
Single Codebase
  ├─ Shared domain logic
  ├─ Shared use cases
  ├─ Shared types/interfaces
  └─ Shared utilities

Independent Deployment
  ├─ API can deploy independently
  ├─ AI agents can scale separately
  ├─ Services can be extracted to microservices later
  └─ Frontend deployments separate

Development Efficiency
  ├─ Change domain → all packages updated
  ├─ Single `npm install`
  ├─ Unified testing
  └─ Shared build cache (Turbo)
```

---

## 🚀 Development Workflow

```bash
# Install dependencies
npm install

# Development
npm run dev              # Start all services

# Build
npm run build            # Build all packages

# Test
npm run test             # Run all tests
npm run test:watch      # Watch mode

# Deploy
npm run deploy:dev      # Deploy to dev
npm run deploy:staging  # Deploy to staging
npm run deploy:prod     # Deploy to production
```

---

**Continue to next file for Domain Models and Entity Definitions...**

