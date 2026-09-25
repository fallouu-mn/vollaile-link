-- Flyway V10__add_public_demand_fields.sql
-- Fields collected by the public quotation request form.

ALTER TABLE demandes
    ADD COLUMN type_client VARCHAR(50) NOT NULL DEFAULT 'AUTRE',
    ADD COLUMN zone_livraison VARCHAR(100),
    ADD COLUMN poids_souhaite NUMERIC(8, 2),
    ADD COLUMN adresse_livraison VARCHAR(300),
    ADD COLUMN preference_contact VARCHAR(50),
    ADD COLUMN offre_id BIGINT REFERENCES offers(id) ON DELETE SET NULL,
    ADD COLUMN consentement BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_demandes_zone_livraison ON demandes(zone_livraison);
CREATE INDEX idx_demandes_offre_id ON demandes(offre_id);
