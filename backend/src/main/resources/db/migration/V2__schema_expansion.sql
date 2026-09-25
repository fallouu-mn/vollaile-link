-- Flyway V2__schema_expansion.sql
-- Database schema expansion for Vollaile Link Phase 1
-- Tables: product_categories, products, producers, offers, price_history

-- Product categories table (hierarchical/categorized)
CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT REFERENCES product_categories(id) ON DELETE SET NULL,
    level INTEGER NOT NULL CHECK (level >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES product_categories(id) ON DELETE SET NULL,
    sku VARCHAR(50) UNIQUE,
    unit_type VARCHAR(50) NOT NULL, -- e.g., "piece", "kg", "box"
    default_unit_price DECIMAL(15, 2), -- Suggested price
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Producers table
CREATE TABLE producers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(200),
    phone VARCHAR(20) NOT NULL CHECK (phone ~ '^\+\d{1,3}\d{9,15}$'), -- E.164 format
    email VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(100),
    region VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Offers table (linking products to producers with pricing and availability)
CREATE TABLE offers (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    producer_id BIGINT NOT NULL REFERENCES producers(id) ON DELETE CASCADE,
    quantity_available INTEGER NOT NULL CHECK (quantity_available >= 0),
    unit_price DECIMAL(15, 2) NOT NULL CHECK (unit_price >= 0),
    total_value DECIMAL(15, 2) GENERATED ALWAYS AS (quantity_available * unit_price) STORED,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'WITHDRAWN')),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_negotiable BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Price history table (tracking price changes over time)
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT REFERENCES administrator(id) ON DELETE SET NULL,
    reason VARCHAR(500)
);

-- Indexes for performance
CREATE INDEX idx_product_categories_parent_id ON product_categories(parent_id);
CREATE INDEX idx_product_categories_is_active ON product_categories(is_active);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_is_active ON products(is_active);
CREATE INDEX idx_producers_status ON producers(status);
CREATE INDEX idx_producers_is_verified ON producers(is_verified);
CREATE INDEX idx_offers_product_id ON offers(product_id);
CREATE INDEX idx_offers_producer_id ON offers(producer_id);
CREATE INDEX idx_offers_status ON offers(status);
CREATE INDEX idx_offers_expires_at ON offers(expires_at);
CREATE INDEX idx_offers_starts_at ON offers(starts_at);
CREATE INDEX idx_price_history_product_id ON price_history(product_id);
CREATE INDEX idx_price_history_changed_at ON price_history(changed_at);

-- Comment on columns for documentation
COMMENT ON TABLE product_categories IS 'Hierarchical product categories for organizing products';
COMMENT ON COLUMN product_categories.name IS 'Category name (must be unique)';
COMMENT ON COLUMN product_categories.description IS 'Optional category description';
COMMENT ON COLUMN product_categories.parent_id IS 'Reference to parent category for hierarchy (NULL for root categories)';
COMMENT ON COLUMN product_categories.level IS 'Depth in hierarchy (0 for root categories)';
COMMENT ON COLUMN product_categories.is_active IS 'Whether the category is active and usable';

COMMENT ON TABLE products IS 'Products available in the system';
COMMENT ON COLUMN products.name IS 'Product name';
COMMENT ON COLUMN products.description IS 'Product description';
COMMENT ON COLUMN products.category_id IS 'Reference to product category';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit (unique identifier)';
COMMENT ON COLUMN products.unit_type IS 'Unit of measurement (piece, kg, box, etc.)';
COMMENT ON COLUMN products.default_unit_price IS 'Suggested unit price for the product';
COMMENT ON COLUMN products.is_active IS 'Whether the product is active and available';

COMMENT ON TABLE producers IS 'Producers/suppliers of products';
COMMENT ON COLUMN producers.name IS 'Producer/farm name';
COMMENT ON COLUMN producers.contact_person IS 'Contact person at the producer';
COMMENT ON COLUMN producers.phone IS 'Phone number in E.164 format (+221...)';
COMMENT ON COLUMN producers.email IS 'Email address (optional)';
COMMENT ON COLUMN producers.address IS 'Physical address';
COMMENT ON COLUMN producers.city IS 'City';
COMMENT ON COLUMN producers.region IS 'Region/state';
COMMENT ON COLUMN producers.status IS 'Producer status (ACTIVE, INACTIVE, SUSPENDED)';
COMMENT ON COLUMN producers.rating IS 'Producer rating (1-5 stars, optional)';
COMMENT ON COLUMN producers.is_verified IS 'Whether producer has been verified/approved';
COMMENT ON COLUMN producers.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN producers.updated_at IS 'Record last update timestamp';

COMMENT ON TABLE offers IS 'Product offers from producers';
COMMENT ON COLUMN offers.product_id IS 'Reference to the product being offered';
COMMENT ON COLUMN offers.producer_id IS 'Reference to the producer making the offer';
COMMENT ON COLUMN offers.quantity_available IS 'Quantity available for sale';
COMMENT ON COLUMN offers.unit_price IS 'Price per unit (seller''s price)';
COMMENT ON COLUMN offers.total_value IS 'Total value (quantity * unit_price), computed and stored';
COMMENT ON COLUMN offers.status IS 'Offer status (DRAFT, ACTIVE, EXPIRED, WITHDRAWN)';
COMMENT ON COLUMN offers.starts_at IS 'Offer start timestamp';
COMMENT ON COLUMN offers.expires_at IS 'Offer expiration timestamp';
COMMENT ON COLUMN offers.is_negotiable IS 'Whether price is negotiable';
COMMENT ON COLUMN offers.description IS 'Optional offer description';
COMMENT ON COLUMN offers.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN offers.updated_at IS 'Record last update timestamp';

COMMENT ON TABLE price_history IS 'Historical price changes for products';
COMMENT ON COLUMN price_history.product_id IS 'Reference to the product';
COMMENT ON COLUMN price_history.price IS 'Price at the time of change';
COMMENT ON COLUMN price_history.changed_at IS 'Timestamp when price was changed';
COMMENT ON COLUMN price_history.changed_by IS 'Administrator who made the change (optional)';
COMMENT ON COLUMN price_history.reason IS 'Reason for the price change (optional)';