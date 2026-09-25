-- Flyway V13__add_zone_livraison.sql
-- Zones desservies et frais de livraison.
--
-- L'ecran "Zones de livraison" du back-office existait dans la navigation
-- et appelait /api/admin/zones-livraison, mais ni la table ni le controller
-- n'existaient : l'ecran etait mort (erreur reseau au clic).
-- Le CDC (zone de lancement : Dakar et region) rend cette donnee necessaire.

CREATE TABLE IF NOT EXISTS zone_livraison (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    description TEXT,
    frais_livraison DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (frais_livraison >= 0),
    delai_min_heures INTEGER NOT NULL DEFAULT 24 CHECK (delai_min_heures >= 0),
    delai_max_heures INTEGER NOT NULL DEFAULT 72 CHECK (delai_max_heures >= 0),
    est_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_zone_delai CHECK (delai_max_heures >= delai_min_heures)
);

COMMENT ON TABLE zone_livraison IS 'Zones de livraison desservies par Vollaile Link';
COMMENT ON COLUMN zone_livraison.frais_livraison IS 'Frais de livraison en FCFA (0 = inclus)';
COMMENT ON COLUMN zone_livraison.est_active IS 'Zone proposable aux clients';

CREATE UNIQUE INDEX IF NOT EXISTS uq_zone_livraison_code ON zone_livraison(code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_zone_livraison_nom ON zone_livraison(nom);
CREATE INDEX IF NOT EXISTS idx_zone_livraison_active ON zone_livraison(est_active);

-- Zones de reference pour Dakar et sa region (zone de lancement du CDC).
-- INSERT ... WHERE NOT EXISTS rend l'operation repetable.
INSERT INTO zone_livraison (nom, code, description, frais_livraison, delai_min_heures, delai_max_heures)
SELECT 'Dakar (Dakar Plateau, Almadies, Parcelles)', 'DAKAR',
       'Livraison dans Dakar intra-muros',
       0, 12, 24
WHERE NOT EXISTS (SELECT 1 FROM zone_livraison WHERE code = 'DAKAR');

INSERT INTO zone_livraison (nom, code, description, frais_livraison, delai_min_heures, delai_max_heures)
SELECT 'Banlieue (Pikine, Guédiawaye, Rufisque)', 'BANLIEUE',
       'Livraisons en banlieue dakaroise',
       2000, 24, 48
WHERE NOT EXISTS (SELECT 1 FROM zone_livraison WHERE code = 'BANLIEUE');

INSERT INTO zone_livraison (nom, code, description, frais_livraison, delai_min_heures, delai_max_heures)
SELECT 'Thiès', 'THIES',
       'Thiès ville et environs',
       3500, 24, 48
WHERE NOT EXISTS (SELECT 1 FROM zone_livraison WHERE code = 'THIES');
