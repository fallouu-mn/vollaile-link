-- Flyway V6__add_demande_commande_tables.sql
-- Database schema expansion for Vollaile Link Phase 3
-- Tables: demandes, commandes

-- Demandes table (requests/quotes)
CREATE TABLE demandes (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    produit_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    quantite_souhaitee INTEGER,
    prix_unitaire_souhaite DECIMAL(15, 2),
    nom_produit VARCHAR(200),
    description TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_COURS' CHECK (statut IN ('EN_COURS', 'VALIDEE', 'REFUSEE', 'EXPIREE', 'SANS_SUITE')),
    date_souhaitee TIMESTAMP WITH TIME ZONE,
    lieu_livraison VARCHAR(200),
    commentaires_admin TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, -- Reference to administrator
    updated_by BIGINT  -- Reference to administrator
);

-- Commandes table (orders)
CREATE TABLE commandes (
    id BIGSERIAL PRIMARY KEY,
    demande_id BIGINT REFERENCES demandes(id) ON DELETE SET NULL,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    produit_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantite INTEGER NOT NULL CHECK (quantite > 0),
    prix_unitaire DECIMAL(15, 2) NOT NULL CHECK (prix_unitaire >= 0),
    prix_unitaire_achat DECIMAL(15, 2), -- Purchase price from producer (for margin calculation)
    remise_pourcentage DECIMAL(5, 2) NOT NULL DEFAULT 0 CHECK (remise_pourcentage >= 0 AND remise_pourcentage <= 100),
    remise_montant DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (remise_montant >= 0),
    total_ht DECIMAL(15, 2) NOT NULL, -- Calculated: quantite * prix_unitaire * (1 - remise_pourcentage/100) - remise_montant
    total_ttc DECIMAL(15, 2) NOT NULL, -- Same as HT for now - TVA to add if necessary
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE_PAIEMENT' CHECK (statut IN ('EN_ATTENTE_PAIEMENT', 'PAYEE', 'EN_PREPARATION', 'EXPEDIEE', 'LIVREE', 'ANNULEE')),
    mode_paiement VARCHAR(20), -- VIREMENT, ESPECE, MOBILE_MONEY
    date_paiement TIMESTAMP WITH TIME ZONE,
    date_livraison_prev TIMESTAMP WITH TIME ZONE NOT NULL,
    date_livraison_reelle TIMESTAMP WITH TIME ZONE,
    lieu_livraison VARCHAR(200) NOT NULL,
    numero_suivi VARCHAR(50),
    commentaires TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, -- Reference to administrator
    updated_by BIGINT  -- Reference to administrator
);

-- Indexes for performance
CREATE INDEX idx_demandes_client_id ON demandes(client_id);
CREATE INDEX idx_demandes_produit_id ON demandes(produit_id);
CREATE INDEX idx_demandes_statut ON demandes(statut);
CREATE INDEX idx_demandes_date_souhaitee ON demandes(date_souhaitee);

CREATE INDEX idx_commandes_client_id ON commandes(client_id);
CREATE INDEX idx_commandes_demande_id ON commandes(demande_id);
CREATE INDEX idx_commandes_produit_id ON commandes(produit_id);
CREATE INDEX idx_commandes_statut ON commandes(statut);
CREATE INDEX idx_commandes_date_livraison_prev ON commandes(date_livraison_prev);

-- Comment on columns for documentation
COMMENT ON TABLE demandes IS 'Client requests/quotes for products';
COMMENT ON COLUMN demandes.client_id IS 'Reference to the client making the request';
COMMENT ON COLUMN demandes.produit_id IS 'Reference to the product being requested (optional for general requests)';
COMMENT ON COLUMN demandes.quantite_souhaitee IS 'Requested quantity';
COMMENT ON COLUMN demandes.prix_unitaire_souhaite IS 'Desired unit price';
COMMENT ON COLUMN demandes.nom_produit IS 'Product name (for general requests without specific product)';
COMMENT ON COLUMN demandes.description IS 'Request details';
COMMENT ON COLUMN demandes.statut IS 'Request status (EN_COURS, VALIDEE, REFUSEE, EXPIREE, SANS_SUITE)';
COMMENT ON COLUMN demandes.date_souhaitee IS 'Desired date for delivery';
COMMENT ON COLUMN demandes.lieu_livraison IS 'Delivery location';
COMMENT ON COLUMN demandes.commentaires_admin IS 'Admin internal comments';
COMMENT ON COLUMN demandes.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN demandes.updated_at IS 'Record last update timestamp';

COMMENT ON TABLE commandes IS 'Client orders for products';
COMMENT ON COLUMN commandes.demande_id IS 'Reference to the demande that led to this order (optional)';
COMMENT ON COLUMN commandes.client_id IS 'Reference to the client placing the order';
COMMENT ON COLUMN commandes.produit_id IS 'Reference to the product being ordered';
COMMENT ON COLUMN commandes.quantite IS 'Ordered quantity (> 0)';
COMMENT ON COLUMN commandes.prix_unitaire IS 'Unit sale price';
COMMENT ON COLUMN commandes.prix_unitaire_achat IS 'Unit purchase price from producer (for margin calculation)';
COMMENT ON COLUMN commandes.remise_pourcentage IS 'Discount percentage (0-100)';
COMMENT ON COLUMN commandes.remise_montant IS 'Discount amount';
COMMENT ON COLUMN commandes.total_ht IS 'Total amount before taxes';
COMMENT ON COLUMN commandes.total_ttc IS 'Total amount including taxes';
COMMENT ON COLUMN commandes.statut IS 'Order status (EN_ATTENTE_PAIEMENT, PAYEE, EN_PREPARATION, EXPEDIEE, LIVREE, ANNULEE)';
COMMENT ON COLUMN commandes.mode_paiement IS 'Payment method (VIREMENT, ESPECE, MOBILE_MONEY)';
COMMENT ON COLUMN commandes.date_paiement IS 'Payment timestamp';
COMMENT ON COLUMN commandes.date_livraison_prev IS 'Expected delivery date';
COMMENT ON COLUMN commandes.date_livraison_reelle IS 'Actual delivery date';
COMMENT ON COLUMN commandes.lieu_livraison IS 'Delivery location';
COMMENT ON COLUMN commandes.numero_suivi IS 'Tracking number';
COMMENT ON COLUMN commandes.commentaires IS 'Order comments';
COMMENT ON COLUMN commandes.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN commandes.updated_at IS 'Record last update timestamp';