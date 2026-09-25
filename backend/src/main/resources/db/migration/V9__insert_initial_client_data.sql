-- Flyway V9__insert_initial_client_data.sql
-- Database schema expansion for Vollaile Link Phase 3
-- Initial client data (optional)

-- Insert some initial clients for testing
INSERT INTO clients (nom, prenom, telephone, email, adresse, ville, region, date_naissance, sexe, professionnel, entreprise, secteur_activite, is_active, created_by, updated_by)
VALUES
    ('Diop', 'Fatou', '+221771234567', 'fatou.diop@email.com', '123 Rue de la Liberté', 'Dakar', 'Dakar', '1990-05-15', 'F', FALSE, NULL, NULL, TRUE, 1, 1),
    ('Ndiaye', 'Mamadou', '+221772345678', 'mamadou.ndiaye@email.com', '456 Avenue Lamine Gueye', 'Thiès', 'Thiès', '1985-08-22', 'M', TRUE, 'Ndiaye Distribution', 'Agro-alimentaire', TRUE, 1, 1),
    ('Sow', 'Aminata', '+221773456789', 'aminata.sow@email.com', '789 Boulevard du Centenaire', 'Saint-Louis', 'Saint-Louis', '1992-11-30', 'F', TRUE, 'Sow Import-Export', 'Commerce général', TRUE, 1, 1);

-- Note: created_by and updated_by reference administrator IDs
-- These assume administrator with ID 1 exists (to be created separately)