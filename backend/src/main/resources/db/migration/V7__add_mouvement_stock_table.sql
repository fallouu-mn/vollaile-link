-- Flyway V7__add_mouvement_stock_table.sql
-- Database schema expansion for Vollaile Link Phase 3
-- Table: mouvement_stocks

-- MouvementStocks table (stock movements)
CREATE TABLE mouvement_stocks (
    id BIGSERIAL PRIMARY KEY,
    produit_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type_mouvement VARCHAR(20) NOT NULL CHECK (type_mouvement IN ('ENTREE', 'SORTIE', 'RESERVATION', 'LIBERATION_RESERVATION', 'VENTE', 'AJUSTEMENT', 'INVENTAIRE')),
    quantite INTEGER NOT NULL, -- Positive for entrée, negative for sortie
    quantite_apres_mouvement INTEGER NOT NULL, -- Stock level after movement
    reference VARCHAR(100), -- Reference to demande/commande/etc.
    reference_type VARCHAR(15) CHECK (reference_type IN ('DEMANDE', 'COMMANDE', 'INVENTAIRE', 'AJUSTEMENT')),
    motif VARCHAR(500), -- Reason for movement
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT -- Reference to administrator
);

-- Indexes for performance
CREATE INDEX idx_mouvement_stocks_produit_id ON mouvement_stocks(produit_id);
CREATE INDEX idx_mouvement_stocks_type ON mouvement_stocks(type_mouvement);
CREATE INDEX idx_mouvement_stocks_reference ON mouvement_stocks(reference);
CREATE INDEX idx_mouvement_stocks_date ON mouvement_stocks(created_at);

-- Comment on columns for documentation
COMMENT ON TABLE mouvement_stocks IS 'Stock movements tracking inventory changes';
COMMENT ON COLUMN mouvement_stocks.produit_id IS 'Reference to the product';
COMMENT ON COLUMN mouvement_stocks.type_mouvement IS 'Type of movement (ENTREE, SORTIE, RESERVATION, LIBERATION_RESERVATION, VENTE, AJUSTEMENT, INVENTAIRE)';
COMMENT ON COLUMN mouvement_stocks.quantite IS 'Quantity change (positive for entrée, negative for sortie)';
COMMENT ON COLUMN mouvement_stocks.quantite_apres_mouvement IS 'Stock level after movement';
COMMENT ON COLUMN mouvement_stocks.reference IS 'Reference to related entity (demande ID, commande ID, etc.)';
COMMENT ON COLUMN mouvement_stocks.reference_type IS 'Type of reference (DEMANDE, COMMANDE, INVENTAIRE, AJUSTEMENT)';
COMMENT ON COLUMN mouvement_stocks.motif IS 'Reason for the movement';
COMMENT ON COLUMN mouvement_stocks.is_active IS 'Whether the movement record is active';
COMMENT ON COLUMN mouvement_stocks.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN mouvement_stocks.updated_at IS 'Record last update timestamp';