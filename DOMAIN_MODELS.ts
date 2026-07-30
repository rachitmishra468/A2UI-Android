// File: packages/core/src/domain/entities/Menu.entity.ts

import { AggregateRoot } from '../base/AggregateRoot';
import { Price } from '../value-objects/Price.vo';
import { Rating } from '../value-objects/Rating.vo';
import { MenuItem } from './MenuItem.entity';

export enum DietaryType {
  VEG = 'Veg',
  NON_VEG = 'Non-Veg',
  VEGAN = 'Vegan'
}

export class MenuCategory {
  constructor(
    public readonly id: string,
    public readonly name: string,
    public readonly description: string,
    public readonly iconUrl: string,
    public readonly order: number
  ) {}
}

export class MenuItemSnapshot {
  constructor(
    public readonly id: string,
    public readonly name: string,
    public readonly description: string,
    public readonly price: Price,
    public readonly category: MenuCategory,
    public readonly dietaryType: DietaryType,
    public readonly imageUrl: string,
    public readonly rating: Rating,
    public readonly isAvailable: boolean,
    public readonly prepTime: number
  ) {}
}

export class Menu extends AggregateRoot {
  private items: Map<string, MenuItem> = new Map();
  private categories: Map<string, MenuCategory> = new Map();

  constructor(
    public readonly id: string,
    public readonly restaurantId: string,
    public readonly name: string,
    public readonly lastUpdated: Date
  ) {
    super();
  }

  static create(
    id: string,
    restaurantId: string,
    name: string
  ): Menu {
    const menu = new Menu(id, restaurantId, name, new Date());
    menu.addDomainEvent({
      aggregateId: id,
      type: 'MenuCreated',
      timestamp: new Date(),
      data: { restaurantId, name }
    });
    return menu;
  }

  addMenuItem(item: MenuItem): void {
    if (this.items.has(item.id)) {
      throw new Error(`MenuItem ${item.id} already exists`);
    }
    this.items.set(item.id, item);
    this.lastUpdated = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'MenuItemAdded',
      timestamp: new Date(),
      data: { itemId: item.id, itemName: item.name }
    });
  }

  removeMenuItem(itemId: string): void {
    if (!this.items.has(itemId)) {
      throw new Error(`MenuItem ${itemId} not found`);
    }
    this.items.delete(itemId);
    this.lastUpdated = new Date();
  }

  updateMenuItem(itemId: string, updates: Partial<MenuItemSnapshot>): void {
    const item = this.items.get(itemId);
    if (!item) throw new Error(`MenuItem ${itemId} not found`);

    item.update(updates);
    this.lastUpdated = new Date();
  }

  getMenuItem(itemId: string): MenuItem | undefined {
    return this.items.get(itemId);
  }

  getAllItems(): MenuItem[] {
    return Array.from(this.items.values());
  }

  searchByCategory(categoryId: string): MenuItem[] {
    return this.getAllItems().filter(item => item.category.id === categoryId);
  }

  searchByDietaryType(type: DietaryType): MenuItem[] {
    return this.getAllItems().filter(item => item.dietaryType === type);
  }

  searchByName(query: string): MenuItem[] {
    const lowerQuery = query.toLowerCase();
    return this.getAllItems().filter(item =>
      item.name.toLowerCase().includes(lowerQuery) ||
      item.description.toLowerCase().includes(lowerQuery)
    );
  }

  searchAdvanced(criteria: {
    category?: string;
    dietaryType?: DietaryType;
    priceRange?: { min: number; max: number };
    minRating?: number;
    search?: string;
  }): MenuItem[] {
    return this.getAllItems().filter(item => {
      if (criteria.category && item.category.id !== criteria.category) return false;
      if (criteria.dietaryType && item.dietaryType !== criteria.dietaryType) return false;
      if (criteria.priceRange) {
        const price = item.price.amount;
        if (price < criteria.priceRange.min || price > criteria.priceRange.max) return false;
      }
      if (criteria.minRating && item.rating.value < criteria.minRating) return false;
      if (criteria.search) {
        const lowerSearch = criteria.search.toLowerCase();
        if (!item.name.toLowerCase().includes(lowerSearch) &&
            !item.description.toLowerCase().includes(lowerSearch)) {
          return false;
        }
      }
      return true;
    });
  }

  addCategory(category: MenuCategory): void {
    this.categories.set(category.id, category);
  }

  getCategories(): MenuCategory[] {
    return Array.from(this.categories.values())
      .sort((a, b) => a.order - b.order);
  }
}

// ============================================
// File: packages/core/src/domain/entities/MenuItem.entity.ts

export class MenuItem extends AggregateRoot {
  constructor(
    public readonly id: string,
    public name: string,
    public description: string,
    public price: Price,
    public category: MenuCategory,
    public dietaryType: DietaryType,
    public imageUrl: string,
    public rating: Rating,
    public isAvailable: boolean,
    public prepTime: number, // in seconds
    public createdAt: Date,
    public updatedAt: Date
  ) {
    super();
  }

  static create(
    id: string,
    name: string,
    description: string,
    price: Price,
    category: MenuCategory,
    dietaryType: DietaryType,
    imageUrl: string,
    prepTime: number
  ): MenuItem {
    if (!name || name.trim().length === 0) {
      throw new Error('MenuItem name cannot be empty');
    }
    if (price.amount <= 0) {
      throw new Error('MenuItem price must be positive');
    }
    if (prepTime <= 0) {
      throw new Error('MenuItem prepTime must be positive');
    }

    const item = new MenuItem(
      id,
      name,
      description,
      price,
      category,
      dietaryType,
      imageUrl,
      Rating.empty(),
      true,
      prepTime,
      new Date(),
      new Date()
    );

    item.addDomainEvent({
      aggregateId: id,
      type: 'MenuItemCreated',
      timestamp: new Date(),
      data: { name, price: price.amount, prepTime }
    });

    return item;
  }

  update(updates: Partial<MenuItemSnapshot>): void {
    if (updates.name !== undefined) {
      if (!updates.name.trim()) throw new Error('Name cannot be empty');
      this.name = updates.name;
    }
    if (updates.description !== undefined) {
      this.description = updates.description;
    }
    if (updates.price !== undefined) {
      if (updates.price.amount <= 0) throw new Error('Price must be positive');
      this.price = updates.price;
    }
    if (updates.dietaryType !== undefined) {
      this.dietaryType = updates.dietaryType;
    }
    if (updates.imageUrl !== undefined) {
      this.imageUrl = updates.imageUrl;
    }
    if (updates.isAvailable !== undefined) {
      this.isAvailable = updates.isAvailable;
    }
    if (updates.prepTime !== undefined) {
      if (updates.prepTime <= 0) throw new Error('PrepTime must be positive');
      this.prepTime = updates.prepTime;
    }

    this.updatedAt = new Date();
  }

  addRating(score: number, count: number = 1): void {
    this.rating = this.rating.addVote(score, count);
  }

  markAsUnavailable(): void {
    this.isAvailable = false;
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'MenuItemMarkedUnavailable',
      timestamp: new Date(),
      data: { itemId: this.id }
    });
  }

  markAsAvailable(): void {
    this.isAvailable = true;
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'MenuItemMarkedAvailable',
      timestamp: new Date(),
      data: { itemId: this.id }
    });
  }

  isValidForOrder(): boolean {
    return this.isAvailable;
  }

  getSnapshot(): MenuItemSnapshot {
    return new MenuItemSnapshot(
      this.id,
      this.name,
      this.description,
      this.price,
      this.category,
      this.dietaryType,
      this.imageUrl,
      this.rating,
      this.isAvailable,
      this.prepTime
    );
  }
}

// ============================================
// File: packages/core/src/domain/entities/Cart.entity.ts

export class CartItem {
  constructor(
    public readonly menuItemId: string,
    public readonly menuItem: MenuItem,
    public quantity: number,
    public specialInstructions?: string
  ) {
    if (quantity <= 0) throw new Error('Quantity must be positive');
  }

  getTotalPrice(): Price {
    return this.menuItem.price.multiply(this.quantity);
  }

  updateQuantity(newQuantity: number): void {
    if (newQuantity <= 0) throw new Error('Quantity must be positive');
    this.quantity = newQuantity;
  }
}

export class Cart extends AggregateRoot {
  private items: Map<string, CartItem> = new Map();

  constructor(
    public readonly id: string,
    public readonly customerId: string,
    public createdAt: Date,
    public updatedAt: Date
  ) {
    super();
  }

  static create(id: string, customerId: string): Cart {
    return new Cart(id, customerId, new Date(), new Date());
  }

  addItem(cartItem: CartItem): void {
    if (!cartItem.menuItem.isValidForOrder()) {
      throw new Error(`MenuItem ${cartItem.menuItemId} is not available`);
    }

    if (this.items.has(cartItem.menuItemId)) {
      const existing = this.items.get(cartItem.menuItemId)!;
      existing.updateQuantity(existing.quantity + cartItem.quantity);
    } else {
      this.items.set(cartItem.menuItemId, cartItem);
    }

    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'ItemAddedToCart',
      timestamp: new Date(),
      data: {
        itemId: cartItem.menuItemId,
        quantity: cartItem.quantity,
        price: cartItem.getTotalPrice().amount
      }
    });
  }

  removeItem(menuItemId: string): void {
    if (!this.items.has(menuItemId)) {
      throw new Error(`Item ${menuItemId} not in cart`);
    }
    this.items.delete(menuItemId);
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'ItemRemovedFromCart',
      timestamp: new Date(),
      data: { itemId: menuItemId }
    });
  }

  updateItemQuantity(menuItemId: string, quantity: number): void {
    const item = this.items.get(menuItemId);
    if (!item) throw new Error(`Item ${menuItemId} not in cart`);
    item.updateQuantity(quantity);
    this.updatedAt = new Date();
  }

  getItems(): CartItem[] {
    return Array.from(this.items.values());
  }

  getTotalPrice(): Price {
    return this.getItems().reduce(
      (total, item) => total.add(item.getTotalPrice()),
      Price.zero()
    );
  }

  getItemCount(): number {
    return this.getItems().reduce((count, item) => count + item.quantity, 0);
  }

  isEmpty(): boolean {
    return this.items.size === 0;
  }

  clear(): void {
    this.items.clear();
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'CartCleared',
      timestamp: new Date(),
      data: { cartId: this.id }
    });
  }
}

// ============================================
// File: packages/core/src/domain/entities/Booking.entity.ts

export enum BookingStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  CHECKED_IN = 'CHECKED_IN',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export class Table {
  constructor(
    public readonly id: string,
    public readonly number: number,
    public readonly capacity: number,
    public readonly location: string
  ) {
    if (capacity <= 0) throw new Error('Table capacity must be positive');
  }

  canAccommodate(partySize: number): boolean {
    return partySize <= this.capacity && partySize > 0;
  }
}

export class TimeSlot {
  constructor(
    public readonly date: Date,
    public readonly time: string // HH:mm format
  ) {}

  toDateTime(): Date {
    const [hours, minutes] = this.time.split(':').map(Number);
    const dateTime = new Date(this.date);
    dateTime.setHours(hours, minutes, 0, 0);
    return dateTime;
  }

  isInFuture(): boolean {
    return this.toDateTime() > new Date();
  }
}

export class Booking extends AggregateRoot {
  static nextBookingId = 0;

  constructor(
    public readonly id: string,
    public readonly customerId: string,
    public table: Table,
    public partySize: number,
    public timeSlot: TimeSlot,
    public status: BookingStatus,
    public specialRequests?: string,
    public createdAt?: Date,
    public updatedAt?: Date
  ) {
    super();
    if (partySize <= 0 || partySize > table.capacity) {
      throw new Error(`Party size ${partySize} invalid for table capacity ${table.capacity}`);
    }
  }

  static create(
    customerId: string,
    table: Table,
    partySize: number,
    timeSlot: TimeSlot,
    specialRequests?: string
  ): Booking {
    if (!timeSlot.isInFuture()) {
      throw new Error('Booking time must be in the future');
    }

    const bookingId = `TB-${Date.now()}-${++this.nextBookingId}`;
    const booking = new Booking(
      bookingId,
      customerId,
      table,
      partySize,
      timeSlot,
      BookingStatus.PENDING,
      specialRequests,
      new Date(),
      new Date()
    );

    booking.addDomainEvent({
      aggregateId: bookingId,
      type: 'BookingCreated',
      timestamp: new Date(),
      data: {
        customerId,
        tableId: table.id,
        partySize,
        timeSlot: timeSlot.time
      }
    });

    return booking;
  }

  confirm(): void {
    if (this.status !== BookingStatus.PENDING) {
      throw new Error(`Cannot confirm booking with status ${this.status}`);
    }
    this.status = BookingStatus.CONFIRMED;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'BookingConfirmed',
      timestamp: new Date(),
      data: { bookingId: this.id }
    });
  }

  checkIn(): void {
    if (this.status !== BookingStatus.CONFIRMED) {
      throw new Error(`Cannot check in booking with status ${this.status}`);
    }
    this.status = BookingStatus.CHECKED_IN;
    this.updatedAt = new Date();
  }

  complete(): void {
    if (![BookingStatus.CHECKED_IN, BookingStatus.CONFIRMED].includes(this.status)) {
      throw new Error(`Cannot complete booking with status ${this.status}`);
    }
    this.status = BookingStatus.COMPLETED;
    this.updatedAt = new Date();
  }

  cancel(): void {
    if ([BookingStatus.COMPLETED, BookingStatus.CANCELLED].includes(this.status)) {
      throw new Error(`Cannot cancel booking with status ${this.status}`);
    }
    this.status = BookingStatus.CANCELLED;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'BookingCancelled',
      timestamp: new Date(),
      data: { bookingId: this.id }
    });
  }

  updateSpecialRequests(requests: string): void {
    this.specialRequests = requests;
    this.updatedAt = new Date();
  }
}

// ============================================
// File: packages/core/src/domain/entities/Order.entity.ts

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PREPARING = 'PREPARING',
  READY = 'READY',
  PICKED_UP = 'PICKED_UP',
  DELIVERED = 'DELIVERED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum DeliveryType {
  PICKUP = 'PICKUP',
  DELIVERY = 'DELIVERY',
  DINE_IN = 'DINE_IN'
}

export class OrderItem {
  constructor(
    public readonly menuItemId: string,
    public readonly menuItem: MenuItem,
    public quantity: number,
    public unitPrice: Price,
    public readonly specialInstructions?: string
  ) {}

  getTotal(): Price {
    return this.unitPrice.multiply(this.quantity);
  }
}

export class Order extends AggregateRoot {
  private items: OrderItem[] = [];
  private discount: Price = Price.zero();

  constructor(
    public readonly id: string,
    public readonly customerId: string,
    public status: OrderStatus,
    public deliveryType: DeliveryType,
    public subtotal: Price,
    public tax: Price,
    public total: Price,
    public createdAt: Date,
    public updatedAt: Date
  ) {
    super();
  }

  static create(
    id: string,
    customerId: string,
    items: OrderItem[],
    deliveryType: DeliveryType,
    tax: Price
  ): Order {
    if (items.length === 0) {
      throw new Error('Order must have at least one item');
    }

    const subtotal = items.reduce(
      (sum, item) => sum.add(item.getTotal()),
      Price.zero()
    );
    const total = subtotal.add(tax);

    const order = new Order(
      id,
      customerId,
      OrderStatus.PENDING,
      deliveryType,
      subtotal,
      tax,
      total,
      new Date(),
      new Date()
    );

    order.items = items;

    order.addDomainEvent({
      aggregateId: id,
      type: 'OrderCreated',
      timestamp: new Date(),
      data: {
        customerId,
        itemCount: items.length,
        total: total.amount
      }
    });

    return order;
  }

  addDiscount(discount: Price): void {
    if (discount.amount >= this.total.amount) {
      throw new Error('Discount cannot be greater than or equal to total');
    }
    this.discount = discount;
    this.total = this.total.subtract(discount);
    this.updatedAt = new Date();
  }

  getItems(): OrderItem[] {
    return [...this.items];
  }

  confirm(): void {
    if (this.status !== OrderStatus.PENDING) {
      throw new Error(`Cannot confirm order with status ${this.status}`);
    }
    this.status = OrderStatus.CONFIRMED;
    this.updatedAt = new Date();
  }

  startPreparing(): void {
    if (this.status !== OrderStatus.CONFIRMED) {
      throw new Error(`Cannot start preparing order with status ${this.status}`);
    }
    this.status = OrderStatus.PREPARING;
    this.updatedAt = new Date();
  }

  markAsReady(): void {
    if (this.status !== OrderStatus.PREPARING) {
      throw new Error(`Cannot mark order as ready with status ${this.status}`);
    }
    this.status = OrderStatus.READY;
    this.updatedAt = new Date();
  }

  markAsPickedUp(): void {
    if (this.deliveryType === DeliveryType.DELIVERY && this.status !== OrderStatus.READY) {
      throw new Error('Delivery order must be ready before pickup');
    }
    this.status = OrderStatus.PICKED_UP;
    this.updatedAt = new Date();
  }

  markAsDelivered(): void {
    if (this.deliveryType !== DeliveryType.DELIVERY) {
      throw new Error('Only delivery orders can be marked as delivered');
    }
    this.status = OrderStatus.DELIVERED;
    this.updatedAt = new Date();
  }

  complete(): void {
    this.status = OrderStatus.COMPLETED;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'OrderCompleted',
      timestamp: new Date(),
      data: { orderId: this.id, customerId: this.customerId }
    });
  }

  cancel(): void {
    if ([OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.COMPLETED].includes(this.status)) {
      throw new Error(`Cannot cancel order with status ${this.status}`);
    }
    this.status = OrderStatus.CANCELLED;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'OrderCancelled',
      timestamp: new Date(),
      data: { orderId: this.id }
    });
  }
}

// ============================================
// File: packages/core/src/domain/entities/Payment.entity.ts

export enum PaymentMethod {
  CREDIT_CARD = 'CREDIT_CARD',
  DEBIT_CARD = 'DEBIT_CARD',
  UPI = 'UPI',
  WALLET = 'WALLET',
  NET_BANKING = 'NET_BANKING',
  CASH = 'CASH'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED'
}

export class Payment extends AggregateRoot {
  constructor(
    public readonly id: string,
    public readonly orderId: string,
    public readonly customerId: string,
    public amount: Price,
    public method: PaymentMethod,
    public status: PaymentStatus,
    public transactionId?: string,
    public failureReason?: string,
    public createdAt?: Date,
    public updatedAt?: Date
  ) {
    super();
  }

  static create(
    id: string,
    orderId: string,
    customerId: string,
    amount: Price,
    method: PaymentMethod
  ): Payment {
    return new Payment(
      id,
      orderId,
      customerId,
      amount,
      method,
      PaymentStatus.PENDING,
      undefined,
      undefined,
      new Date(),
      new Date()
    );
  }

  startProcessing(): void {
    if (this.status !== PaymentStatus.PENDING) {
      throw new Error(`Cannot process payment with status ${this.status}`);
    }
    this.status = PaymentStatus.PROCESSING;
    this.updatedAt = new Date();
  }

  complete(transactionId: string): void {
    if (this.status !== PaymentStatus.PROCESSING) {
      throw new Error(`Cannot complete payment with status ${this.status}`);
    }
    this.status = PaymentStatus.COMPLETED;
    this.transactionId = transactionId;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'PaymentCompleted',
      timestamp: new Date(),
      data: { orderId: this.orderId, amount: this.amount.amount }
    });
  }

  fail(reason: string): void {
    this.status = PaymentStatus.FAILED;
    this.failureReason = reason;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'PaymentFailed',
      timestamp: new Date(),
      data: { orderId: this.orderId, reason }
    });
  }

  refund(): void {
    if (this.status !== PaymentStatus.COMPLETED) {
      throw new Error(`Cannot refund payment with status ${this.status}`);
    }
    this.status = PaymentStatus.REFUNDED;
    this.updatedAt = new Date();
    this.addDomainEvent({
      aggregateId: this.id,
      type: 'PaymentRefunded',
      timestamp: new Date(),
      data: { orderId: this.orderId, amount: this.amount.amount }
    });
  }
}

