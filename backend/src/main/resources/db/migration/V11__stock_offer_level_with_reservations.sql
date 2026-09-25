-- Flyway V11__stock_offer_level_with_reservations.sql
-- Aligne le stock sur la regle metier du CDC (section 2.3) :
--   stock disponible = total - reserve - vendu
-- Le stock est desormais porte par l'OFFRE (et non plus par le produit),
-- chaque ecriture passant par une transaction avec verrouillage de la ligne offre.

-- 1) Compteurs par offre + version pour verrou optimiste
ALTER TABLE offers
    ADD COLUMN IF NOT EXISTS quantity_total INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quantity_reserved INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quantity_sold INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Reprise de l'existant : la disponibilite historique devient le total initial
UPDATE offers
SET quantity_total = quantity_available
WHERE quantity_total = 0;

-- 2) La disponibilite devient une colonne derivee, l'invariant est impose par la base
--    (la colonne existante quantity_available est reconstruite en GENERATED)
ALTER TABLE offers DROP COLUMN IF EXISTS total_value;
ALTER TABLE offers DROP COLUMN IF EXISTS quantity_available;

ALTER TABLE offers
    ADD COLUMN quantity_available INTEGER
        GENERATED ALWAYS AS (quantity_total - quantity_reserved - quantity_sold) STORED,
    ADD COLUMN total_value DECIMAL(15, 2)
        GENERATED ALWAYS AS (quantity_total * unit_price) STORED;

COMMENT ON COLUMN offers.quantity_available IS 'Stock disponible = total - reserve - vendu (colonne derivee)';
COMMENT ON COLUMN offers.total_value IS 'Valeur totale du stock = quantite totale x prix unitaire (colonne derivee)';

-- 3) Invariants : jamais de stock negatif, jamais de survente
ALTER TABLE offers
    DROP CONSTRAINT IF EXISTS offers_quantity_non_negative,
    DROP CONSTRAINT IF EXISTS offers_quantity_not_oversold;

ALTER TABLE offers
    ADD CONSTRAINT offers_quantity_non_negative
        CHECK (quantity_total >= 0 AND quantity_reserved >= 0 AND quantity_sold >= 0),
    ADD CONSTRAINT offers_quantity_not_oversold
        CHECK (quantity_reserved + quantity_sold <= quantity_total);

-- 4) La commande reference desormais l'offre concernee (une offre = un lot de production)
ALTER TABLE commandes
    ADD COLUMN IF NOT EXISTS offre_id BIGINT REFERENCES offers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_commandes_offre_id ON commandes(offre_id);

-- 5) Nouveau statut de commande : expiration de la reservation
ALTER TABLE commandes DROP CONSTRAINT IF EXISTS commandes_statut_check;
ALTER TABLE commandes
    ADD CONSTRAINT commandes_statut_check
        CHECK (statut IN ('EN_ATTENTE_PAIEMENT', 'PAYEE', 'EN_PREPARATION', 'EXPEDIEE',
                          'LIVREE', 'ANNULEE', 'EXPIREE'));

-- 6) Table des reservations de stock
CREATE TABLE IF NOT EXISTS stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
    commande_id BIGINT REFERENCES commandes(id) ON DELETE CASCADE,
    quantite INTEGER NOT NULL CHECK (quantite > 0),
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (statut IN ('ACTIVE', 'RELEASED', 'CONSUMED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    close_reason VARCHAR(200),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE
);

COMMENT ON TABLE stock_reservations IS 'Reservations de stock par offre (disponible = total - reserve - vendu)';
COMMENT ON COLUMN stock_reservations.idempotency_key IS 'Cle d idempotence : une reservation ne peut etre creee deux fois';

CREATE INDEX IF NOT EXISTS idx_stock_reservations_offer_active
    ON stock_reservations(offer_id) WHERE statut = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_stock_reservations_expiry
    ON stock_reservations(expires_at) WHERE statut = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_stock_reservations_commande
    ON stock_reservations(commande_id);

-- Une seule reservation active par commande
CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_reservations_active_commande
    ON stock_reservations(commande_id) WHERE statut = 'ACTIVE';

-- 7) Journal des mouvements : rattache a l'offre et a la reservation
ALTER TABLE mouvement_stocks
    ADD COLUMN IF NOT EXISTS offer_id BIGINT REFERENCES offers(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS reservation_id BIGINT REFERENCES stock_reservations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_mouvement_stocks_offer_id
    ON mouvement_stocks(offer_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_mouvement_stocks_reservation_id
    ON mouvement_stocks(reservation_id);
