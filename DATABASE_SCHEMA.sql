-- Restaurant Ordering Platform - Database Schema (PostgreSQL)

-- ============================================
-- CUSTOMERS TABLE
-- ============================================
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_phone ON customers(phone_number);

-- ============================================
-- CATEGORIES TABLE
-- ============================================
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(500),
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- MENU_ITEMS TABLE
-- ============================================
CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    category_id UUID NOT NULL REFERENCES categories(id),
    dietary_type VARCHAR(20) NOT NULL CHECK (dietary_type IN ('Veg', 'Non-Veg', 'Vegan')),
    image_url VARCHAR(500),
    rating DECIMAL(3, 2) DEFAULT 0,
    rating_count INTEGER DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    prep_time_seconds INTEGER NOT NULL CHECK (prep_time_seconds > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_menu_item_category ON menu_items(category_id);
CREATE INDEX idx_menu_item_dietary_type ON menu_items(dietary_type);
CREATE INDEX idx_menu_item_availability ON menu_items(is_available);

-- ============================================
-- CARTS TABLE
-- ============================================
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    total_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cart_customer ON carts(customer_id);

-- ============================================
-- CART_ITEMS TABLE
-- ============================================
CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    special_instructions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cart_id, menu_item_id)
);

CREATE INDEX idx_cart_item_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_item_menu_item ON cart_items(menu_item_id);

-- ============================================
-- TABLES TABLE (Restaurant tables)
-- ============================================
CREATE TABLE tables (
    id UUID PRIMARY KEY,
    table_number INTEGER NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_table_capacity ON tables(capacity);

-- ============================================
-- BOOKINGS TABLE
-- ============================================
CREATE TABLE bookings (
    id VARCHAR(50) PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    table_id UUID NOT NULL REFERENCES tables(id),
    num_people INTEGER NOT NULL CHECK (num_people > 0),
    booking_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED')),
    special_requests TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_booking_customer ON bookings(customer_id);
CREATE INDEX idx_booking_table ON bookings(table_id);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_time ON bookings(booking_time);

-- ============================================
-- ORDERS TABLE
-- ============================================
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    delivery_type VARCHAR(20) NOT NULL CHECK (delivery_type IN ('PICKUP', 'DELIVERY', 'DINE_IN')),
    subtotal DECIMAL(12, 2) NOT NULL,
    tax DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_customer ON orders(customer_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_delivery_type ON orders(delivery_type);
CREATE INDEX idx_order_created_at ON orders(created_at);

-- ============================================
-- ORDER_ITEMS TABLE
-- ============================================
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    special_instructions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_item_order ON order_items(order_id);
CREATE INDEX idx_order_item_menu_item ON order_items(menu_item_id);

-- ============================================
-- PAYMENTS TABLE
-- ============================================
CREATE TABLE payments (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(20) NOT NULL CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'WALLET', 'NET_BANKING', 'CASH')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    transaction_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order ON payments(order_id);
CREATE INDEX idx_payment_customer ON payments(customer_id);
CREATE INDEX idx_payment_status ON payments(status);

-- ============================================
-- COUPONS TABLE
-- ============================================
CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(20) CHECK (discount_type IN ('FIXED', 'PERCENTAGE')),
    discount_value DECIMAL(10, 2) NOT NULL,
    max_uses INTEGER,
    current_uses INTEGER DEFAULT 0,
    min_order_value DECIMAL(12, 2),
    valid_from TIMESTAMP NOT NULL,
    valid_upto TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coupon_code ON coupons(code);
CREATE INDEX idx_coupon_validity ON coupons(valid_from, valid_upto);

-- ============================================
-- DOMAIN_EVENTS TABLE (Event Sourcing)
-- ============================================
CREATE TABLE domain_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_domain_event_aggregate ON domain_events(aggregate_id);
CREATE INDEX idx_domain_event_type ON domain_events(event_type);
CREATE INDEX idx_domain_event_created_at ON domain_events(created_at);

-- ============================================
-- AUDIT_LOG TABLE
-- ============================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customers(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255),
    changes JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_customer ON audit_log(customer_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- ============================================
-- SEED DATA
-- ============================================

INSERT INTO categories (id, name, description, icon_url, order_index) VALUES
('cat-001', 'Burgers', 'Delicious handcrafted burgers', 'https://example.com/burger.png', 1),
('cat-002', 'Pizzas', 'Fresh baked pizzas', 'https://example.com/pizza.png', 2),
('cat-003', 'South Indian', 'Traditional South Indian cuisine', 'https://example.com/dosa.png', 3),
('cat-004', 'Biryani', 'Aromatic rice dishes', 'https://example.com/biryani.png', 4),
('cat-005', 'Appetizers', 'Starters and appetizers', 'https://example.com/appetizer.png', 5),
('cat-006', 'Rice', 'Rice based dishes', 'https://example.com/rice.png', 6),
('cat-007', 'Curries', 'Curries and gravies', 'https://example.com/curry.png', 7);

INSERT INTO tables (id, table_number, capacity, location) VALUES
('tbl-001', 1, 2, 'Window Seat'),
('tbl-002', 2, 2, 'Window Seat'),
('tbl-003', 3, 4, 'Corner Section'),
('tbl-004', 4, 4, 'Corner Section'),
('tbl-005', 5, 6, 'Main Hall'),
('tbl-006', 6, 6, 'Main Hall'),
('tbl-007', 7, 8, 'Large Section'),
('tbl-008', 8, 8, 'Large Section');

-- Add more menu items, customers, etc. as needed

