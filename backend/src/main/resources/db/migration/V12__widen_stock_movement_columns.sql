-- Flyway V12__widen_stock_movement_columns.sql
-- Corrections de largeur introduites par le moteur de stock par offre (V11).
--
-- 1) mouvement_stocks.type_mouvement est VARCHAR(20) depuis V7, mais
--    'LIBERATION_RESERVATION' en compte 22. Le type etait donc inutilisable :
--    toute liberation de reservation echouait sur une erreur de longueur.
--    La contrainte CHECK de V7 autorisait pourtant deja cette valeur.
-- 2) reference_type VARCHAR(15) : 'AJUSTEMENT' (10) passe, mais la marge
--    est trop juste pour les types a venir.
-- 3) reservation_id : le nom de colonne le plus court et explicite.

ALTER TABLE mouvement_stocks
    ALTER COLUMN type_mouvement TYPE VARCHAR(30),
    ALTER COLUMN reference_type TYPE VARCHAR(30),
    ALTER COLUMN reference TYPE VARCHAR(120);

COMMENT ON COLUMN mouvement_stocks.type_mouvement IS
    'Type de mouvement (ENTREE, SORTIE, RESERVATION, LIBERATION_RESERVATION, VENTE, AJUSTEMENT, INVENTAIRE)';
COMMENT ON COLUMN mouvement_stocks.reference_type IS
    'Nature de la reference (DEMANDE, COMMANDE, INVENTAIRE, AJUSTEMENT)';
COMMENT ON COLUMN mouvement_stocks.reference IS
    'Reference metier du mouvement (ex: CMD-VTE-42)';
COMMENT ON COLUMN mouvement_stocks.reservation_id IS
    'Reservation de stock a l origine du mouvement';
