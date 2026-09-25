-- Flyway V3__add_content_tables.sql
-- Add tables for static content and FAQ

-- Page table
CREATE TABLE page (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    content TEXT,
    meta_title VARCHAR(200),
    meta_description VARCHAR(200),
    og_image VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- FAQ table
CREATE TABLE faq (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_page_slug ON page(slug);
CREATE INDEX idx_page_is_active ON page(is_active);
CREATE INDEX idx_faq_display_order ON faq(display_order);
CREATE INDEX idx_faq_is_active ON faq(is_active);

-- Comment on columns for documentation
COMMENT ON TABLE page IS 'Static content pages for the public website';
COMMENT ON COLUMN page.title IS 'Page title';
COMMENT ON COLUMN page.slug IS 'URL-friendly identifier (unique)';
COMMENT ON COLUMN page.content IS 'HTML content of the page';
COMMENT ON COLUMN page.meta_title IS 'Title for SEO (optional)';
COMMENT ON COLUMN page.meta_description IS 'Description for SEO (optional)';
COMMENT ON COLUMN page.og_image IS 'Open Graph image URL (optional)';
COMMENT ON COLUMN page.is_active IS 'Whether the page is active and visible';
COMMENT ON TABLE faq IS 'Frequently asked questions';
COMMENT ON COLUMN faq.question IS 'The question';
COMMENT ON COLUMN faq.answer IS 'The answer';
COMMENT ON COLUMN faq.display_order IS 'Order in which to display the FAQ (ascending)';
COMMENT ON COLUMN faq.is_active IS 'Whether the FAQ is active and visible';