# Restaurant Platform - API Design & Integration Guide

## 🔌 REST API Endpoints

### Menu Endpoints

```typescript
// GET /api/v1/menu
// Get all menu items with filters
Query Parameters:
  - category?: string
  - dietaryType?: 'Veg' | 'Non-Veg' | 'Vegan'
  - search?: string
  - priceRange?: { min: number; max: number }
  - minRating?: number
  - page?: number (default: 1)
  - limit?: number (default: 20)

Response:
{
  "success": true,
  "data": {
    "items": MenuItem[],
    "totalCount": number,
    "page": number,
    "limit": number
  }
}

// GET /api/v1/menu/categories
// Get all menu categories
Response:
{
  "success": true,
  "data": {
    "categories": Category[]
  }
}

// GET /api/v1/menu/:itemId
// Get menu item details
Response:
{
  "success": true,
  "data": MenuItem
}
```

### Cart Endpoints

```typescript
// POST /api/v1/cart
// Create new cart for session
Request: { customerId: string }
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "customerId": string,
    "createdAt": string
  }
}

// POST /api/v1/cart/:cartId/items
// Add item to cart
Request:
{
  "menuItemId": string,
  "quantity": number,
  "specialInstructions"?: string
}
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "itemCount": number,
    "totalPrice": number,
    "message": string
  }
}

// DELETE /api/v1/cart/:cartId/items/:menuItemId
// Remove item from cart
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "message": "Item removed successfully"
  }
}

// GET /api/v1/cart/:cartId
// View cart contents
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "items": CartItem[],
    "itemCount": number,
    "subtotal": number,
    "tax": number,
    "total": number,
    "isEmpty": boolean
  }
}

// PATCH /api/v1/cart/:cartId/items/:menuItemId
// Update cart item quantity
Request: { quantity: number }
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "message": "Item quantity updated"
  }
}

// DELETE /api/v1/cart/:cartId
// Clear entire cart
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "message": "Cart cleared successfully"
  }
}
```

### Booking Endpoints

```typescript
// POST /api/v1/bookings/check-availability
// Check table availability
Request:
{
  "partySize": number,
  "date": string, // YYYY-MM-DD
  "time": string  // HH:mm
}
Response:
{
  "success": true,
  "data": {
    "available": boolean,
    "availableTables": Table[],
    "message": string
  }
}

// POST /api/v1/bookings
// Book a table
Request:
{
  "customerId": string,
  "partySize": number,
  "date": string,
  "time": string,
  "specialRequests"?: string
}
Response:
{
  "success": true,
  "data": {
    "bookingId": string,
    "tableNumber": number,
    "partySize": number,
    "bookingTime": string,
    "status": string,
    "message": string
  }
}

// GET /api/v1/bookings/:bookingId
// Get booking details
Response:
{
  "success": true,
  "data": Booking
}

// PATCH /api/v1/bookings/:bookingId
// Update booking
Request: { partySize?: number; time?: string; specialRequests?: string }
Response:
{
  "success": true,
  "data": Booking
}

// DELETE /api/v1/bookings/:bookingId
// Cancel booking
Response:
{
  "success": true,
  "data": {
    "bookingId": string,
    "message": "Booking cancelled successfully"
  }
}
```

### Order Endpoints

```typescript
// POST /api/v1/orders
// Create order from cart
Request:
{
  "customerId": string,
  "cartId": string,
  "deliveryType": 'PICKUP' | 'DELIVERY' | 'DINE_IN'
}
Response:
{
  "success": true,
  "data": {
    "orderId": string,
    "customerId": string,
    "itemCount": number,
    "subtotal": number,
    "tax": number,
    "total": number,
    "status": string,
    "message": string
  }
}

// GET /api/v1/orders/:orderId
// Get order details
Response:
{
  "success": true,
  "data": Order
}

// PATCH /api/v1/orders/:orderId
// Update order status (admin only)
Request: { status: OrderStatus }
Response:
{
  "success": true,
  "data": Order
}
```

### Pricing Endpoints

```typescript
// POST /api/v1/pricing/calculate
// Calculate price with discounts and tax
Request:
{
  "cartId": string,
  "couponCode"?: string
}
Response:
{
  "success": true,
  "data": {
    "cartId": string,
    "subtotal": number,
    "discount": number,
    "tax": number,
    "total": number,
    "breakdown": { label: string; amount: number }[]
  }
}
```

### Payment Endpoints

```typescript
// POST /api/v1/payments
// Process payment
Request:
{
  "orderId": string,
  "customerId": string,
  "amount": number,
  "method": PaymentMethod,
  "paymentDetails": any
}
Response:
{
  "success": true,
  "data": {
    "paymentId": string,
    "orderId": string,
    "amount": number,
    "status": string,
    "transactionId"?: string,
    "message": string
  }
}

// GET /api/v1/payments/:paymentId
// Get payment status
Response:
{
  "success": true,
  "data": Payment
}
```

### AI Chat Endpoint

```typescript
// POST /api/v1/chat
// Process natural language query through AI
Request:
{
  "message": string,
  "sessionId": string,
  "customerId"?: string
}
Response:
{
  "success": true,
  "data": {
    "message": string,
    "actions": AgentAction[],
    "intents": Intent[],
    "data": any
  }
}

// WebSocket: ws://api.example.com/api/v1/chat/live
// Streaming AI responses
Event: {
  "type": "message" | "action" | "complete" | "error",
  "payload": any
}
```

---

## 📊 Sequence Diagrams

### Scenario 1: Simple Menu Search

```
User                Orchestrator          MenuAgent        Repository        Database
 |                      |                    |                |               |
 |--chat("show pizzas")---->|                 |                |               |
 |                      |--detect-intent---->|                 |               |
 |                      |<--SEARCH_MENU-----| |                |               |
 |                      |                    |                 |               |
 |                      |--route-to-agent----|                 |               |
 |                      |                    |--execute------->|               |
 |                      |                    |                 |--query-menu-->|
 |                      |                    |                 |<--pizzas------|
 |                      |                    |<--result--------|               |
 |                      |                    |                 |               |
 |                      |<--map-result-------|                 |               |
 |                      |                    |                 |               |
 |                      |--aggregate-------response            |               |
 |                      |                    |                 |               |
 |<----response---------| |                  |                |               |
 |  [5 pizzas found]    | |                  |                |               |
```

### Scenario 2: Multi-Intent - Add Item + Book Table

```
User                Orchestrator       CartAgent        BookingAgent      Repositories
 |                      |                  |                  |               |
 |--chat("Add 2 pizzas  |                  |                  |               |
 | and book table for 5") -->|             |                  |               |
 |                      |                  |                  |               |
 |                      |--detect-intent-->|                  |               |
 |                      |<--[ADD_TO_CART, BOOK_TABLE]         |               |
 |                      |                  |                  |               |
 |     ┌────────────────┴──────────────────┴──────────┐        |               |
 |     |                                               |        |               |
 |     |--route-ADD_TO_CART---->|                      |        |               |
 |     |                        |--get-cart-----------|--------|----->|        |
 |     |                        |                      |        |<--cart--|       |
 |     |                        |--add-item-----------|--------|------->|        |
 |     |                        |<--result-----------|------|<--saved--|        |
 |     |                        |                      |        |               |
 |     |--route-BOOK_TABLE------------>|               |        |               |
 |     |                               |--check-availability---|----->|        |
 |     |                               |                       |<--available--|  |
 |     |                               |--create-booking------|----->|        |
 |     |                               |<--bookingId-----------|<--saved--|     |
 |     |                               |                       |               |
 |     └────────────────┬──────────────┬───────────────────────┘               |
 |                      |              |                       |               |
 |<--aggregate-response-| |             |                      |               |
 |  ✅ 2 pizzas added   | |             |                      |               |
 |  ✅ Table booked     | |             |                      |               |
```

### Scenario 3: Full Checkout Flow

```
User           Chat API        Orchestrator       CheckoutAgent      Repositories
 |                |                |                   |                  |
 |--checkout------>|                |                   |                  |
 |                 |--detect-intent->|                   |                  |
 |                 |<--CHECKOUT-----| |                  |                  |
 |                 |                 |--route-to------>|                  |
 |                 |                 |   checkout      |                  |
 |                 |                 |                 |--get-cart------->|
 |                 |                 |                 |<--cart-items-----|
 |                 |                 |                 |                  |
 |                 |                 |                 |--create-order-->|
 |                 |                 |                 |<--orderId--------|
 |                 |                 |                 |                  |
 |                 |                 |                 |--process-payment->|
 |                 |                 |                 |<--transactionId---|
 |                 |                 |                 |                  |
 |                 |                 |                 |--update-status-->|
 |                 |                 |                 |<--order-updated--|
 |                 |                 |<--result--------|                  |
 |                 |<--aggregate-----| |                |                  |
 |<--response------| |                 |                |                  |
 |  ✅ Order ORD-XXX created           |                |                  |
 |  ✅ Payment processed                |                |                  |
 |  📧 Confirmation sent                |                |                  |
```

---

## 🔐 Authentication & Authorization

### JWT Token Structure

```typescript
{
  "iss": "restaurant-api",
  "sub": "customer-id",
  "email": "user@example.com",
  "phone": "+91-XXXXXXXXXX",
  "role": "customer", // admin, staff, customer
  "permissions": ["order:read", "order:write", "booking:read"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

### Request with Authentication

```
POST /api/v1/checkout
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "cartId": "cart-123",
  "deliveryType": "DELIVERY"
}
```

---

## 🌐 GraphQL Schema (Alternative)

```graphql
type Query {
  menu(
    category: String
    dietaryType: DietaryType
    search: String
    priceRange: PriceRange
  ): [MenuItem!]!
  
  cart(cartId: ID!): Cart!
  order(orderId: ID!): Order!
  booking(bookingId: ID!): Booking!
}

type Mutation {
  addToCart(cartId: ID!, menuItemId: ID!, quantity: Int!): CartResult!
  removeFromCart(cartId: ID!, menuItemId: ID!): CartResult!
  
  bookTable(
    customerId: ID!
    partySize: Int!
    date: String!
    time: String!
  ): BookingResult!
  
  checkout(
    cartId: ID!
    deliveryType: DeliveryType!
  ): OrderResult!
  
  processPayment(
    orderId: ID!
    method: PaymentMethod!
    paymentDetails: JSON!
  ): PaymentResult!
}

type Subscription {
  orderStatus(orderId: ID!): OrderStatusUpdate!
  bookingStatus(bookingId: ID!): BookingStatusUpdate!
}
```

---

## 🚀 Integration Points

### With Frontend (React/Next.js)

```typescript
// API Client (Shared package)
export class RestaurantAPIClient {
  private baseUrl = process.env.REACT_APP_API_URL;
  private token = localStorage.getItem('auth_token');

  async searchMenu(params: SearchMenuRequest) {
    return fetch(`${this.baseUrl}/menu`, {
      params,
      headers: { Authorization: `Bearer ${this.token}` }
    }).then(r => r.json());
  }

  async addToCart(cartId: string, item: CartItemRequest) {
    return fetch(`${this.baseUrl}/cart/${cartId}/items`, {
      method: 'POST',
      body: JSON.stringify(item),
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${this.token}`
      }
    }).then(r => r.json());
  }

  // Similar for other operations
}

// React Hook
export function useMenuSearch(params: SearchMenuRequest) {
  const [data, setData] = useState<MenuItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    apiClient
      .searchMenu(params)
      .then(res => setData(res.data.items))
      .finally(() => setLoading(false));
  }, [JSON.stringify(params)]);

  return { data, loading };
}
```

### With Mobile Apps (iOS/Android)

```kotlin
// Kotlin example for Android
class RestaurantAPIService(private val retrofit: Retrofit) {
  private val api = retrofit.create(RestaurantAPI::class.java)

  suspend fun searchMenu(params: SearchMenuRequest): SearchMenuResponse {
    return api.searchMenu(params)
  }

  suspend fun addToCart(cartId: String, item: CartItemRequest): CartResult {
    return api.addToCart(cartId, item)
  }
}
```

### With Voice Assistants (Future)

```
User: "Alexa, order me 2 paneer pizzas from Restaurant X"
     |
     v
Cloud Gateway recognizes intent
     |
     v
REST API call: POST /api/v1/chat
{
  "message": "Alexa, order me 2 paneer pizzas from Restaurant X",
  "customerId": "user-123",
  "source": "voice_assistant"
}
     |
     v
Orchestrator Agent processes multi-intent:
- Search menu (find pizzas)
- Add to cart (2 paneer pizzas)
- Checkout (place order)
     |
     v
Response back to Alexa:
"✅ Order placed! Your Pizza will be ready in 30 minutes"
```

---

## 📊 State Management Strategy

### Zustand Store (Frontend)

```typescript
import { create } from 'zustand';

interface CartStore {
  cart: Cart | null;
  items: CartItem[];
  
  // Actions
  setCart: (cart: Cart) => void;
  addItem: (item: CartItem) => void;
  removeItem: (itemId: string) => void;
}

export const useCartStore = create<CartStore>((set) => ({
  cart: null,
  items: [],
  
  setCart: (cart) => set({ cart }),
  addItem: (item) => set((state) => ({
    items: [...state.items, item]
  })),
  removeItem: (itemId) => set((state) => ({
    items: state.items.filter(item => item.id !== itemId)
  }))
}));

// Usage in component
function CartComponent() {
  const { items, removeItem } = useCartStore();
  
  return (
    <div>
      {items.map(item => (
        <CartItemCard 
          key={item.id} 
          item={item}
          onRemove={() => removeItem(item.id)}
        />
      ))}
    </div>
  );
}
```

### Backend State Machine

```typescript
// Ordering Flow state machine
const orderStateMachine = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PREPARING', 'CANCELLED'],
  PREPARING: ['READY'],
  READY: ['PICKED_UP'],
  PICKED_UP: {
    PICKUP: ['COMPLETED'],
    DELIVERY: ['DELIVERED']
  },
  DELIVERED: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: []
};

// Validate state transitions
function canTransition(currentStatus: string, newStatus: string): boolean {
  const allowedTransitions = orderStateMachine[currentStatus];
  return Array.isArray(allowedTransitions) 
    ? allowedTransitions.includes(newStatus)
    : false;
}
```

---

## ✅ Next Steps for Implementation

1. **Backend Setup**
   - Set up Node.js/TypeScript project with the monorepo structure
   - Implement database with Prisma ORM
   - Create repositories for all entities
   - Implement all use cases

2. **API Development**
   - Create Express/Fastify REST API
   - Set up authentication (JWT)
   - Implement error handling middleware
   - Add rate limiting and validation

3. **AI Integration**
   - Set up LangGraph for agent orchestration
   - Configure LLM providers (OpenAI, Azure)
   - Create agent implementations
   - Test multi-intent scenarios

4. **Frontend**
   - React/Next.js project setup
   - API client generation
   - State management setup
   - UI component library

5. **Testing**
   - Unit tests for use cases
   - Integration tests for flows
   - E2E tests for critical paths
   - Load testing

6. **Deployment**
   - Docker containerization
   - Kubernetes setup
   - CI/CD pipeline
   - Monitoring & logging


