-- Flyway V5__add_client_table.sql
-- Database schema expansion for Vollaile Link Phase 3
-- Table: clients

-- Clients table
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    prenom VARCHAR(200) NOT NULL,
    telephone VARCHAR(20) NOT NULL UNIQUE CHECK (telephone ~ '^\+\d{1,3}\d{9,15}$'), -- E.164 format
    email VARCHAR(255),
    adresse VARCHAR(500),
    ville VARCHAR(100),
    region VARCHAR(100),
    date_naissance DATE,
    sexe VARCHAR(20) CHECK (sexe IN ('M', 'F', 'AUTRE')),
    professionnel BOOLEAN NOT NULL DEFAULT FALSE,
    entreprise VARCHAR(200), -- Required if professionnel = true
    secteur_activite VARCHAR(200), -- Required if professionnel = true
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT, -- Reference to administrator (to be added when administrator table exists)
    updated_by BIGINT  -- Reference to administrator (to be added when administrator table exists)
);

-- Indexes for performance
CREATE INDEX idx_clients_telephone ON clients(telephone);
CREATE INDEX idx_clients_is_active ON clients(is_active);
CREATE INDEX idx_clients_nom_prenom ON clients(nom, prenom);
CREATE INDEX idx_clients_professionnel ON clients(professionnel);

-- Comment on columns for documentation
COMMENT ON TABLE clients IS 'Clients/customers of the platform';
COMMENT ON COLUMN clients.nom IS 'Last name';
COMMENT ON COLUMN clients.prenom IS 'First name';
COMMENT ON COLUMN clients.telephone IS 'Phone number in E.164 format (+221...) - UNIQUE for anti-doublon';
COMMENT ON COLUMN clients.email IS 'Email address (optional)';
COMMENT ON COLUMN clients.adresse IS 'Physical address';
COMMENT ON COLUMN clients.ville IS 'City';
COMMENT ON COLUMN clients.region IS 'Region/state';
COMMENT ON COLUMN clients.date_naissance IS 'Date of birth';
COMMENT ON COLUMN clients.sexe IS 'Gender (M/F/AUTRE)';
COMMENT ON COLUMN clients.professionnel IS 'Whether the client is a professional customer';
COMMENT ON COLUMN clients.entreprise IS 'Company name (required if professionnel = true)';
COMMENT ON COLUMN clients.secteur_activite IS 'Business sector (required if professionnel = true)';
COMMENT ON COLUMN clients.is_active IS 'Whether the client is active';
COMMENT ON COLUMN clients.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN clients.updated_at IS 'Record last update timestamp';