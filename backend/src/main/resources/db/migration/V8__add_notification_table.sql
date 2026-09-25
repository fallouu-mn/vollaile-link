-- Flyway V8__add_notification_table.sql
-- Database schema expansion for Vollaile Link Phase 3
-- Table: notifications

-- Notifications table (in-app notifications)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('NOUVELLE_DEMANDE', 'CHANGEMENT_STATUT', 'STOCK_BAS', 'RESERVATION_EXPIREE', 'COMMANDE_CONFIRMEE', 'COMMANDE_LIVREE', 'DEMANDE_EXPIREE', 'RAPPEL_LIVRAISON', 'MESSAGE_ADMIN', 'SYSTEME')),
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    related_entity_id BIGINT,
    related_entity_type VARCHAR(20), -- DEMANDE, COMMANDE, PRODUIT, etc.
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT -- Reference to administrator
);

-- Indexes for performance
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_lu ON notifications(lu);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_related_entity ON notifications(related_entity_id, related_entity_type);

-- Comment on columns for documentation
COMMENT ON TABLE notifications IS 'In-app notifications for administrators';
COMMENT ON COLUMN notifications.titre IS 'Notification title';
COMMENT ON COLUMN notifications.message IS 'Notification message';
COMMENT ON COLUMN notifications.type IS 'Notification type';
COMMENT ON COLUMN notifications.lu IS 'Whether the notification has been read';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID of related entity (if applicable)';
COMMENT ON COLUMN notifications.related_entity_type IS 'Type of related entity (DEMANDE, COMMANDE, PRODUIT, etc.)';
COMMENT ON COLUMN notifications.is_active IS 'Whether the notification is active';
COMMENT ON COLUMN notifications.created_at IS 'Record creation timestamp';