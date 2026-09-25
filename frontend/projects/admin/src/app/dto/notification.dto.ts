export interface NotificationDTO {
  id?: number;
  title: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  isRead?: boolean;
  createdAt?: string; // ISO date string
  relatedEntityId?: number;
  relatedEntityType?: string; // DEMANDE, COMMANDE, PRODUIT, STOCK, etc.
  actionUrl?: string;
  actionLabel?: string;
  isActive?: boolean;
}