// File: packages/application/src/use-cases/menu/SearchMenu.usecase.ts

import { Menu, DietaryType } from '@restaurant/core/domain/entities/Menu.entity';
import { MenuItem } from '@restaurant/core/domain/entities/MenuItem.entity';
import { UseCase } from '../base/UseCase.interface';

export interface SearchMenuRequest {
  query?: string;
  category?: string;
  dietaryType?: DietaryType;
  priceRange?: { min: number; max: number };
  minRating?: number;
}

export interface MenuItemDTO {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  dietaryType: string;
  imageUrl: string;
  rating: number;
  ratingCount: number;
  isAvailable: boolean;
  prepTime: number;
}

export interface SearchMenuResponse {
  items: MenuItemDTO[];
  totalCount: number;
  query?: string;
}

export class SearchMenuUseCase implements UseCase<SearchMenuRequest, SearchMenuResponse> {
  constructor(private menuRepository: MenuRepository) {}

  async execute(request: SearchMenuRequest): Promise<SearchMenuResponse> {
    // Get menu from repository
    const menu = await this.menuRepository.getMenu();

    // Search based on criteria
    const items = menu.searchAdvanced({
      category: request.category,
      dietaryType: request.dietaryType,
      priceRange: request.priceRange,
      minRating: request.minRating,
      search: request.query
    });

    // Map to DTOs
    const itemDTOs = items.map(this.mapToDTO);

    return {
      items: itemDTOs,
      totalCount: itemDTOs.length,
      query: request.query
    };
  }

  private mapToDTO(item: MenuItem): MenuItemDTO {
    return {
      id: item.id,
      name: item.name,
      description: item.description,
      price: item.price.amount,
      category: item.category.name,
      dietaryType: item.dietaryType,
      imageUrl: item.imageUrl,
      rating: item.rating.value,
      ratingCount: item.rating.count,
      isAvailable: item.isAvailable,
      prepTime: item.prepTime
    };
  }
}

// ============================================
// File: packages/application/src/use-cases/cart/AddToCart.usecase.ts

import { Cart, CartItem } from '@restaurant/core/domain/entities/Cart.entity';
import { UseCase } from '../base/UseCase.interface';

export interface AddToCartRequest {
  cartId: string;
  menuItemId: string;
  quantity: number;
  specialInstructions?: string;
}

export interface AddToCartResponse {
  success: boolean;
  cartId: string;
  itemCount: number;
  totalPrice: number;
  message: string;
}

export class AddToCartUseCase implements UseCase<AddToCartRequest, AddToCartResponse> {
  constructor(
    private cartRepository: CartRepository,
    private menuRepository: MenuRepository
  ) {}

  async execute(request: AddToCartRequest): Promise<AddToCartResponse> {
    // Validate request
    if (request.quantity <= 0) {
      throw new Error('Quantity must be greater than 0');
    }

    // Get cart or create if doesn't exist
    let cart = await this.cartRepository.getById(request.cartId);
    if (!cart) {
      throw new Error(`Cart ${request.cartId} not found`);
    }

    // Get menu item
    const menu = await this.menuRepository.getMenu();
    const menuItem = menu.getMenuItem(request.menuItemId);
    if (!menuItem) {
      throw new Error(`MenuItem ${request.menuItemId} not found`);
    }

    // Validate item is available
    if (!menuItem.isValidForOrder()) {
      throw new Error(`MenuItem ${menuItem.name} is not available`);
    }

    // Add to cart
    const cartItem = new CartItem(
      menuItem.id,
      menuItem,
      request.quantity,
      request.specialInstructions
    );
    cart.addItem(cartItem);

    // Save cart
    await this.cartRepository.save(cart);

    // Publish domain events
    for (const event of cart.getPendingEvents()) {
      await this.eventBus.publish(event);
    }

    return {
      success: true,
      cartId: cart.id,
      itemCount: cart.getItemCount(),
      totalPrice: cart.getTotalPrice().amount,
      message: `${menuItem.name} added to cart successfully`
    };
  }
}

// ============================================
// File: packages/application/src/use-cases/cart/ViewCart.usecase.ts

export interface ViewCartRequest {
  cartId: string;
}

export interface CartItemDTO {
  menuItemId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  specialInstructions?: string;
}

export interface ViewCartResponse {
  cartId: string;
  items: CartItemDTO[];
  itemCount: number;
  subtotal: number;
  tax: number;
  total: number;
  isEmpty: boolean;
}

export class ViewCartUseCase implements UseCase<ViewCartRequest, ViewCartResponse> {
  constructor(
    private cartRepository: CartRepository,
    private pricingService: PricingService
  ) {}

  async execute(request: ViewCartRequest): Promise<ViewCartResponse> {
    const cart = await this.cartRepository.getById(request.cartId);
    if (!cart) {
      throw new Error(`Cart ${request.cartId} not found`);
    }

    const items = cart.getItems().map(item => ({
      menuItemId: item.menuItemId,
      name: item.menuItem.name,
      quantity: item.quantity,
      unitPrice: item.menuItem.price.amount,
      totalPrice: item.getTotalPrice().amount,
      specialInstructions: item.specialInstructions
    }));

    const subtotal = cart.getTotalPrice().amount;
    const tax = await this.pricingService.calculateTax(subtotal);

    return {
      cartId: cart.id,
      items,
      itemCount: cart.getItemCount(),
      subtotal,
      tax,
      total: subtotal + tax,
      isEmpty: cart.isEmpty()
    };
  }
}

// ============================================
// File: packages/application/src/use-cases/booking/BookTable.usecase.ts

import { Booking, Table, TimeSlot, BookingStatus } from '@restaurant/core/domain/entities/Booking.entity';
import { UseCase } from '../base/UseCase.interface';

export interface BookTableRequest {
  customerId: string;
  partySize: number;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  specialRequests?: string;
}

export interface BookTableResponse {
  success: boolean;
  bookingId: string;
  tableNumber: number;
  partySize: number;
  bookingTime: string;
  status: string;
  message: string;
}

export class BookTableUseCase implements UseCase<BookTableRequest, BookTableResponse> {
  constructor(
    private bookingRepository: BookingRepository,
    private tableRepository: TableRepository,
    private bookingService: BookingService
  ) {}

  async execute(request: BookTableRequest): Promise<BookTableResponse> {
    // Validate request
    if (request.partySize <= 0) {
      throw new Error('Party size must be greater than 0');
    }

    // Find available table
    const availableTable = await this.bookingService.findAvailableTable(
      request.partySize,
      new Date(`${request.date}T${request.time}`)
    );

    if (!availableTable) {
      throw new Error(`No available table for party of ${request.partySize} at ${request.time}`);
    }

    // Create booking
    const timeSlot = new TimeSlot(new Date(request.date), request.time);
    const booking = Booking.create(
      request.customerId,
      availableTable,
      request.partySize,
      timeSlot,
      request.specialRequests
    );

    // Confirm booking
    booking.confirm();

    // Save booking
    await this.bookingRepository.save(booking);

    // Publish domain events
    for (const event of booking.getPendingEvents()) {
      await this.eventBus.publish(event);
    }

    return {
      success: true,
      bookingId: booking.id,
      tableNumber: availableTable.number,
      partySize: booking.partySize,
      bookingTime: request.time,
      status: BookingStatus.CONFIRMED,
      message: `Table ${availableTable.number} booked successfully for ${request.partySize} people at ${request.time}`
    };
  }
}

// ============================================
// File: packages/application/src/use-cases/pricing/CalculatePrice.usecase.ts

export interface CalculatePriceRequest {
  cartId: string;
  couponCode?: string;
}

export interface CalculatePriceResponse {
  cartId: string;
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
  breakdown: {
    label: string;
    amount: number;
  }[];
}

export class CalculatePriceUseCase implements UseCase<CalculatePriceRequest, CalculatePriceResponse> {
  constructor(
    private cartRepository: CartRepository,
    private pricingService: PricingService,
    private couponRepository: CouponRepository
  ) {}

  async execute(request: CalculatePriceRequest): Promise<CalculatePriceResponse> {
    // Get cart
    const cart = await this.cartRepository.getById(request.cartId);
    if (!cart) {
      throw new Error(`Cart ${request.cartId} not found`);
    }

    const subtotal = cart.getTotalPrice().amount;
    let discount = 0;
    let discountLabel = 'No discount';

    // Apply coupon if provided
    if (request.couponCode) {
      const coupon = await this.couponRepository.getByCode(request.couponCode);
      if (!coupon) {
        throw new Error(`Coupon ${request.couponCode} not found`);
      }

      if (!coupon.isValid()) {
        throw new Error(`Coupon ${request.couponCode} is expired`);
      }

      discount = this.pricingService.calculateDiscount(subtotal, coupon);
      discountLabel = `Discount (${coupon.description})`;
    }

    // Calculate tax
    const taxableAmount = subtotal - discount;
    const tax = await this.pricingService.calculateTax(taxableAmount);

    const total = subtotal - discount + tax;

    return {
      cartId: request.cartId,
      subtotal,
      discount,
      tax,
      total,
      breakdown: [
        { label: 'Subtotal', amount: subtotal },
        { label: discountLabel, amount: -discount },
        { label: 'Tax', amount: tax },
        { label: 'Total', amount: total }
      ]
    };
  }
}

// ============================================
// File: packages/application/src/use-cases/checkout/CreateOrder.usecase.ts

import { Order, OrderItem, DeliveryType } from '@restaurant/core/domain/entities/Order.entity';
import { UseCase } from '../base/UseCase.interface';

export interface CreateOrderRequest {
  customerId: string;
  cartId: string;
  deliveryType: 'PICKUP' | 'DELIVERY' | 'DINE_IN';
}

export interface CreateOrderResponse {
  orderId: string;
  customerId: string;
  itemCount: number;
  subtotal: number;
  tax: number;
  total: number;
  status: string;
  message: string;
}

export class CreateOrderUseCase implements UseCase<CreateOrderRequest, CreateOrderResponse> {
  constructor(
    private cartRepository: CartRepository,
    private orderRepository: OrderRepository,
    private pricingService: PricingService
  ) {}

  async execute(request: CreateOrderRequest): Promise<CreateOrderResponse> {
    // Get cart
    const cart = await this.cartRepository.getById(request.cartId);
    if (!cart) {
      throw new Error(`Cart ${request.cartId} not found`);
    }

    if (cart.isEmpty()) {
      throw new Error('Cannot create order from empty cart');
    }

    // Create order items from cart
    const orderItems = cart.getItems().map(cartItem => {
      return new OrderItem(
        cartItem.menuItemId,
        cartItem.menuItem,
        cartItem.quantity,
        cartItem.menuItem.price,
        cartItem.specialInstructions
      );
    });

    // Calculate tax
    const subtotal = cart.getTotalPrice();
    const tax = await this.pricingService.calculateTaxAsPrice(subtotal);

    // Create order
    const order = Order.create(
      this.generateOrderId(),
      request.customerId,
      orderItems,
      request.deliveryType as DeliveryType,
      tax
    );

    // Confirm order
    order.confirm();

    // Save order
    await this.orderRepository.save(order);

    // Publish domain events
    for (const event of order.getPendingEvents()) {
      await this.eventBus.publish(event);
    }

    // Clear cart
    cart.clear();
    await this.cartRepository.save(cart);

    return {
      orderId: order.id,
      customerId: order.customerId,
      itemCount: orderItems.length,
      subtotal: order.subtotal.amount,
      tax: order.tax.amount,
      total: order.total.amount,
      status: order.status,
      message: `Order ${order.id} created successfully`
    };
  }

  private generateOrderId(): string {
    return `ORD-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

// ============================================
// File: packages/application/src/use-cases/checkout/ProcessPayment.usecase.ts

import { Payment, PaymentMethod, PaymentStatus } from '@restaurant/core/domain/entities/Payment.entity';
import { UseCase } from '../base/UseCase.interface';

export interface ProcessPaymentRequest {
  orderId: string;
  customerId: string;
  amount: number;
  method: 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'WALLET' | 'NET_BANKING' | 'CASH';
  paymentDetails?: any; // Card number, UPI ID, etc.
}

export interface ProcessPaymentResponse {
  success: boolean;
  paymentId: string;
  orderId: string;
  amount: number;
  status: string;
  transactionId?: string;
  message: string;
}

export class ProcessPaymentUseCase implements UseCase<ProcessPaymentRequest, ProcessPaymentResponse> {
  constructor(
    private paymentRepository: PaymentRepository,
    private paymentGateway: PaymentGateway,
    private orderRepository: OrderRepository
  ) {}

  async execute(request: ProcessPaymentRequest): Promise<ProcessPaymentResponse> {
    // Validate order exists
    const order = await this.orderRepository.getById(request.orderId);
    if (!order) {
      throw new Error(`Order ${request.orderId} not found`);
    }

    // Validate amount matches order total
    if (request.amount !== order.total.amount) {
      throw new Error(`Payment amount ${request.amount} does not match order total ${order.total.amount}`);
    }

    // Create payment entity
    const payment = Payment.create(
      this.generatePaymentId(),
      request.orderId,
      request.customerId,
      order.total,
      request.method as PaymentMethod
    );

    // Start payment processing
    payment.startProcessing();
    await this.paymentRepository.save(payment);

    // Process payment through gateway
    try {
      const result = await this.paymentGateway.process({
        paymentId: payment.id,
        amount: request.amount,
        method: request.method,
        paymentDetails: request.paymentDetails
      });

      // Mark payment as complete
      payment.complete(result.transactionId);
      await this.paymentRepository.save(payment);

      // Publish domain events
      for (const event of payment.getPendingEvents()) {
        await this.eventBus.publish(event);
      }

      return {
        success: true,
        paymentId: payment.id,
        orderId: request.orderId,
        amount: request.amount,
        status: PaymentStatus.COMPLETED,
        transactionId: result.transactionId,
        message: 'Payment processed successfully'
      };
    } catch (error) {
      // Mark payment as failed
      payment.fail(error.message);
      await this.paymentRepository.save(payment);

      // Publish domain events
      for (const event of payment.getPendingEvents()) {
        await this.eventBus.publish(event);
      }

      throw new Error(`Payment processing failed: ${error.message}`);
    }
  }

  private generatePaymentId(): string {
    return `PAY-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

// ============================================
// File: packages/application/src/use-cases/base/UseCase.interface.ts

export interface UseCase<TRequest, TResponse> {
  execute(request: TRequest): Promise<TResponse>;
}

// ============================================
// Export all use cases

export {
  SearchMenuUseCase,
  AddToCartUseCase,
  ViewCartUseCase,
  BookTableUseCase,
  CalculatePriceUseCase,
  CreateOrderUseCase,
  ProcessPaymentUseCase
};

