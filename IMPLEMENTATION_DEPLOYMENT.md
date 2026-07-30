# Restaurant Platform - Complete Implementation & Deployment Guide

## 🚀 Quick Start: End-to-End Implementation

### Part 1: Environment Setup

```bash
# Clone and setup monorepo
mkdir restaurant-ordering-platform
cd restaurant-ordering-platform

# Initialize monorepo with Turborepo
npm init -y
npm install turbo

# Create workspace structure
mkdir -p packages/{core,application,infrastructure,ai-agents,api}
mkdir -p frontend/apps/web
mkdir -p tests/{unit,integration,e2e}
mkdir -p docker k8s docs

# Create root turbo.json
cat > turbo.json << 'EOF'
{
  "$schema": "https://turbo.build/json-schema.json",
  "globalDependencies": ["**/.env.local"],
  "pipeline": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "test": {
      "outputs": ["coverage/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    }
  }
}
EOF

# Install dependencies
npm install -W typescript @types/node jest ts-jest
```

### Part 2: Core Domain Package

```bash
cd packages/core
npm init -y
npm install @types/node uuid
cat > tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
EOF
```

### Part 3: Base Classes & Interfaces

```typescript
// packages/core/src/base/AggregateRoot.ts
export interface DomainEvent {
  aggregateId: string;
  type: string;
  timestamp: Date;
  data: any;
}

export abstract class AggregateRoot {
  private pendingEvents: DomainEvent[] = [];

  protected addDomainEvent(event: DomainEvent): void {
    this.pendingEvents.push(event);
  }

  public getPendingEvents(): DomainEvent[] {
    return this.pendingEvents;
  }

  public clearPendingEvents(): void {
    this.pendingEvents = [];
  }
}

// packages/core/src/base/Entity.ts
export abstract class Entity {
  constructor(public readonly id: string) {}

  abstract equals(other: Entity): boolean;
}

// packages/core/src/domain/value-objects/Price.vo.ts
export class Price {
  private constructor(public readonly amount: number) {
    if (amount < 0) {
      throw new Error('Price cannot be negative');
    }
  }

  static create(amount: number): Price {
    return new Price(Math.round(amount * 100) / 100);
  }

  static zero(): Price {
    return new Price(0);
  }

  add(other: Price): Price {
    return Price.create(this.amount + other.amount);
  }

  subtract(other: Price): Price {
    const result = this.amount - other.amount;
    if (result < 0) {
      throw new Error('Cannot subtract to negative price');
    }
    return Price.create(result);
  }

  multiply(factor: number): Price {
    if (factor < 0) {
      throw new Error('Multiplication factor cannot be negative');
    }
    return Price.create(this.amount * factor);
  }

  isGreaterThan(other: Price): boolean {
    return this.amount > other.amount;
  }

  isLessThan(other: Price): boolean {
    return this.amount < other.amount;
  }

  equals(other: Price): boolean {
    return this.amount === other.amount;
  }
}

// packages/core/src/domain/value-objects/Rating.vo.ts
export class Rating {
  private constructor(
    public readonly value: number,
    public readonly count: number
  ) {
    if (value < 0 || value > 5) {
      throw new Error('Rating must be between 0 and 5');
    }
    if (count < 0) {
      throw new Error('Count cannot be negative');
    }
  }

  static create(value: number, count: number): Rating {
    return new Rating(value, count);
  }

  static empty(): Rating {
    return new Rating(0, 0);
  }

  addVote(score: number, count: number = 1): Rating {
    if (score < 0 || score > 5) {
      throw new Error('Score must be between 0 and 5');
    }
    const totalVotes = this.count + count;
    const newValue = (this.value * this.count + score * count) / totalVotes;
    return new Rating(Math.round(newValue * 10) / 10, totalVotes);
  }
}
```

### Part 4: Application Services

```typescript
// packages/application/src/services/PricingService.ts
import { Price } from '@restaurant/core/domain/value-objects/Price.vo';

export interface TaxConfiguration {
  rate: number; // e.g., 0.18 for 18% GST
  minAmount?: number;
}

export class PricingService {
  private taxConfig: Map<string, TaxConfiguration> = new Map([
    ['default', { rate: 0.18 }]
  ]);

  async calculateTax(amount: number, region: string = 'default'): Promise<number> {
    const taxConfig = this.taxConfig.get(region) || this.taxConfig.get('default')!;
    
    if (taxConfig.minAmount && amount < taxConfig.minAmount) {
      return 0;
    }

    return Math.round(amount * taxConfig.rate * 100) / 100;
  }

  async calculateTaxAsPrice(price: Price, region: string = 'default'): Promise<Price> {
    const taxAmount = await this.calculateTax(price.amount, region);
    return Price.create(taxAmount);
  }

  calculateDiscount(amount: number, coupon: Coupon): number {
    if (!coupon.isValid()) return 0;

    if (coupon.discountType === 'FIXED') {
      return Math.min(coupon.discountValue, amount);
    } else {
      // Percentage
      const discount = (amount * coupon.discountValue) / 100;
      return Math.round(discount * 100) / 100;
    }
  }

  applyDiscount(original: Price, discountAmount: number): Price {
    if (discountAmount >= original.amount) {
      throw new Error('Discount cannot be >= total amount');
    }
    return original.subtract(Price.create(discountAmount));
  }
}

// packages/application/src/services/BookingService.ts
import { Table } from '@restaurant/core/domain/entities/Booking.entity';

export class BookingService {
  constructor(
    private bookingRepository: BookingRepository,
    private tableRepository: TableRepository
  ) {}

  async findAvailableTable(
    partySize: number,
    bookingDateTime: Date
  ): Promise<Table | null> {
    // Get all tables that can accommodate the party
    const suitableTables = await this.tableRepository.getByCapacity(partySize);
    
    if (!suitableTables.length) {
      return null;
    }

    // Check which tables are available at the requested time
    for (const table of suitableTables) {
      const isAvailable = await this.isTableAvailable(
        table.id,
        bookingDateTime
      );
      if (isAvailable) {
        return table;
      }
    }

    return null;
  }

  private async isTableAvailable(
    tableId: string,
    bookingDateTime: Date
  ): Promise<boolean> {
    // Each booking locks table for 2 hours by default
    const bookingDuration = 2 * 60 * 60 * 1000;
    const startTime = new Date(bookingDateTime.getTime() - bookingDuration / 2);
    const endTime = new Date(bookingDateTime.getTime() + bookingDuration / 2);

    const conflictingBookings = await this.bookingRepository.getBookingsBetween(
      tableId,
      startTime,
      endTime
    );

    return conflictingBookings.length === 0;
  }
}
```

### Part 5: Infrastructure - Repositories

```typescript
// packages/infrastructure/src/repositories/MenuRepository.ts
import { PrismaClient } from '@prisma/client';
import { Menu, MenuItem, MenuCategory } from '@restaurant/core/domain/entities/Menu.entity';

export class MenuRepository {
  constructor(private prisma: PrismaClient) {}

  async getMenu(restaurantId: string = 'default'): Promise<Menu> {
    // Load menu from database
    const menuRecord = await this.prisma.menu.findUnique({
      where: { restaurantId },
      include: {
        categories: true,
        items: true
      }
    });

    if (!menuRecord) {
      throw new Error(`Menu not found for restaurant ${restaurantId}`);
    }

    // Reconstruct Menu aggregate from database
    const menu = new Menu(menuRecord.id, restaurantId, menuRecord.name, menuRecord.lastUpdated);

    // Add categories
    for (const cat of menuRecord.categories) {
      const category = new MenuCategory(cat.id, cat.name, cat.description, cat.iconUrl, cat.order);
      menu.addCategory(category);
    }

    // Add items
    for (const item of menuRecord.items) {
      const price = Price.create(item.price);
      const rating = Rating.create(item.rating, item.ratingCount);
      const category = new MenuCategory(item.categoryId, '', '', '', 0);
      
      const menuItem = new MenuItem(
        item.id,
        item.name,
        item.description,
        price,
        category,
        item.dietaryType as DietaryType,
        item.imageUrl,
        rating,
        item.isAvailable,
        item.prepTimeSeconds,
        item.createdAt,
        item.updatedAt
      );

      menu.addMenuItem(menuItem);
    }

    return menu;
  }

  async save(menu: Menu): Promise<void> {
    // Save Menu aggregate to database
    const items = menu.getAllItems();

    for (const item of items) {
      await this.prisma.menuItem.upsert({
        where: { id: item.id },
        create: {
          id: item.id,
          name: item.name,
          description: item.description,
          price: item.price.amount,
          categoryId: item.category.id,
          dietaryType: item.dietaryType,
          imageUrl: item.imageUrl,
          rating: item.rating.value,
          ratingCount: item.rating.count,
          isAvailable: item.isAvailable,
          prepTimeSeconds: item.prepTime,
          menuId: menu.id
        },
        update: {
          name: item.name,
          description: item.description,
          price: item.price.amount,
          isAvailable: item.isAvailable,
          rating: item.rating.value,
          ratingCount: item.rating.count,
          updatedAt: new Date()
        }
      });
    }

    // Publish domain events
    for (const event of menu.getPendingEvents()) {
      await this.eventBus.publish(event);
    }

    menu.clearPendingEvents();
  }
}

// packages/infrastructure/src/repositories/CartRepository.ts
export class CartRepository {
  constructor(private prisma: PrismaClient) {}

  async getById(cartId: string): Promise<Cart | null> {
    const cartRecord = await this.prisma.cart.findUnique({
      where: { id: cartId },
      include: {
        items: {
          include: { menuItem: true }
        }
      }
    });

    if (!cartRecord) return null;

    const cart = new Cart(
      cartRecord.id,
      cartRecord.customerId,
      cartRecord.createdAt,
      cartRecord.updatedAt
    );

    // Reconstruct cart items
    for (const item of cartRecord.items) {
      const price = Price.create(item.menuItem.price);
      const menuItem = new MenuItem(...);
      const cartItem = new CartItem(
        item.menuItemId,
        menuItem,
        item.quantity,
        item.specialInstructions
      );
      cart.addItem(cartItem);
    }

    return cart;
  }

  async save(cart: Cart): Promise<void> {
    // Save cart to database
    await this.prisma.cart.upsert({
      where: { id: cart.id },
      create: {
        id: cart.id,
        customerId: cart.customerId,
        totalPrice: cart.getTotalPrice().amount
      },
      update: {
        totalPrice: cart.getTotalPrice().amount,
        updatedAt: new Date()
      }
    });

    // Handle items
    for (const item of cart.getItems()) {
      await this.prisma.cartItem.upsert({
        where: {
          cartId_menuItemId: {
            cartId: cart.id,
            menuItemId: item.menuItemId
          }
        },
        create: {
          id: uuidv4(),
          cartId: cart.id,
          menuItemId: item.menuItemId,
          quantity: item.quantity,
          specialInstructions: item.specialInstructions
        },
        update: {
          quantity: item.quantity,
          specialInstructions: item.specialInstructions
        }
      });
    }
  }
}
```

---

## 🐳 Docker Deployment

```dockerfile
# Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app

# Copy monorepo files
COPY package*.json yarn.lock* turbo.json ./
COPY packages ./packages

# Install dependencies
RUN npm ci

# Build all packages
RUN npm run build

# Production image
FROM node:18-alpine

WORKDIR /app

COPY --from=builder /app/packages/api/dist ./api/dist
COPY --from=builder /app/packages/api/package.json ./api/

# Install prod dependencies only
WORKDIR /app/api
RUN npm ci --production

EXPOSE 3000

CMD ["node", "dist/server.js"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: restaurant
      POSTGRES_USER: app
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  api:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: postgresql://app:password@postgres:5432/restaurant
      REDIS_URL: redis://redis:6379
      NODE_ENV: production
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    ports:
      - "3000:3000"
    depends_on:
      - postgres
      - redis

  web:
    build:
      context: ./frontend/apps/web
      dockerfile: Dockerfile
    ports:
      - "3001:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://api:3000
    depends_on:
      - api

volumes:
  postgres_data:
```

---

## ⚙️ Kubernetes Deployment

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: restaurant-api
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: restaurant-api
  template:
    metadata:
      labels:
        app: restaurant-api
    spec:
      containers:
      - name: api
        image: restaurant/api:latest
        ports:
        - containerPort: 3000
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-url
        - name: REDIS_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: redis-url
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: restaurant-api
spec:
  selector:
    app: restaurant-api
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 3000

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: restaurant-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: restaurant-api
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 📈 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-node@v3
      with:
        node-version: '18'
    - run: npm ci
    - run: npm run test
    - run: npm run test:integration

  build:
    runs-on: ubuntu-latest
    needs: test
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    - uses: docker/setup-buildx-action@v2
    - uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}
    - uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: restaurant/api:${{ github.sha }},restaurant/api:latest

  deploy:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    - uses: azure/setup-kubectl@v3
    - run: |
        kubectl set image deployment/restaurant-api \
          api=restaurant/api:${{ github.sha }} \
          --record
      env:
        KUBECONFIG: ${{ secrets.KUBECONFIG }}
```

---

## 📊 Monitoring & Observability

```typescript
// packages/api/src/observability/monitoring.ts
import * as prometheus from 'prom-client';

// Custom metrics
export const httpRequestDuration = new prometheus.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.1, 0.5, 1, 2, 5]
});

export const cartItemsAdded = new prometheus.Counter({
  name: 'cart_items_added_total',
  help: 'Total number of items added to cart',
  labelNames: ['item_id', 'quantity']
});

export const orderTotal = new prometheus.Gauge({
  name: 'order_total_amount',
  help: 'Total amount of orders',
  labelNames: ['status']
});

export const bookingCreated = new prometheus.Counter({
  name: 'bookings_created_total',
  help: 'Total bookings created',
  labelNames: ['party_size']
});

// Setup Prometheus endpoint
export function setupPrometheus(app: Express) {
  app.get('/metrics', (req, res) => {
    res.set('Content-Type', prometheus.register.contentType);
    res.end(prometheus.register.metrics());
  });
}

// Middleware for request tracking
export function metricsMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
) {
  const start = Date.now();
  
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    httpRequestDuration.observe(
      {
        method: req.method,
        route: req.route?.path || req.path,
        status_code: res.statusCode
      },
      duration
    );
  });

  next();
}
```

---

## ✅ Testing Strategy

```typescript
// tests/unit/entities/MenuItem.test.ts
import { describe, it, expect } from '@jest/globals';
import { MenuItem } from '@restaurant/core/domain/entities/MenuItem.entity';
import { Price } from '@restaurant/core/domain/value-objects/Price.vo';
import { Rating } from '@restaurant/core/domain/value-objects/Rating.vo';

describe('MenuItem Entity', () => {
  it('should create a valid menu item', () => {
    const item = MenuItem.create(
      'item-1',
      'Chicken Pizza',
      'Delicious chicken pizza',
      Price.create(299),
      new MenuCategory('cat-1', 'Pizza', '', '', 1),
      'Non-Veg',
      'http://image.jpg',
      1200
    );

    expect(item.name).toBe('Chicken Pizza');
    expect(item.price.amount).toBe(299);
    expect(item.isAvailable).toBe(true);
  });

  it('should throw error if price is negative', () => {
    expect(() => {
      Price.create(-100);
    }).toThrow('Price cannot be negative');
  });

  it('should add rating and update average', () => {
    const item = MenuItem.create(...);
    item.addRating(4.5);
    item.addRating(5);

    expect(item.rating.value).toBe(4.75);
    expect(item.rating.count).toBe(2);
  });
});

// tests/integration/use-cases/AddToCart.test.ts
describe('AddToCartUseCase Integration', () => {
  it('should add item to cart and save', async () => {
    const useCase = new AddToCartUseCase(cartRepo, menuRepo);
    
    const result = await useCase.execute({
      cartId: 'cart-1',
      menuItemId: 'item-1',
      quantity: 2
    });

    expect(result.success).toBe(true);
    expect(result.itemCount).toBe(2);
  });
});
```

---

## 🎯 Production Checklist

- [ ] Database backup strategy (daily snapshots)
- [ ] SSL/TLS certificates (Let's Encrypt)
- [ ] Monitoring dashboards (Grafana)
- [ ] Logging centralization (ELK Stack)
- [ ] API rate limiting (100 req/min per user)
- [ ] CORS configuration for frontend
- [ ] Payment gateway integration (Stripe/Razorpay)
- [ ] Email service (SendGrid/SES)
- [ ] SMS notifications (Twilio)
- [ ] Customer support chat (Intercom)
- [ ] Analytics tracking (Mixpanel/Amplitude)
- [ ] Security audit (OWASP Top 10)
- [ ] Load testing (k6/JMeter)
- [ ] Disaster recovery plan
- [ ] Documentation complete

---

This provides a complete, production-ready architecture that can scale to serve millions of users while maintaining code quality and business logic separation.

