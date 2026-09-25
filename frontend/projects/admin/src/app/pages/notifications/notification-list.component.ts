import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { NotificationDTO } from '../../dto/notification.dto';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatCardModule,
    MatProgressBarModule
  ],
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss']
})
export class NotificationListComponent implements OnInit {
  notifications: NotificationDTO[] = [];

  displayedColumns: string[] = ['type', 'title', 'message', 'createdAt', 'actions'];
  dataSource!: MatTableDataSource<NotificationDTO>;
  isLoading = false;
  unreadCount = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.notificationService.getAllNotifications().subscribe({
      next: (notifications) => {
        this.notifications = notifications;
        this.dataSource = new MatTableDataSource(this.notifications);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;

        // Calculate unread count
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;

        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  markAsRead(notificationId: number): void {
    this.notificationService.markAsRead(notificationId).subscribe({
      next: () => {
        // Update the notification in the list
        const index = this.notifications.findIndex(n => n.id === notificationId);
        if (index !== -1) {
          this.notifications[index].isRead = true;
          this.dataSource.data = this.notifications;
          this.unreadCount = this.notifications.filter(n => !n.isRead).length;
        }
      },
      error: (error) => {
        console.error('Error marking notification as read:', error);
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.dataSource.data = this.notifications;
        this.unreadCount = 0;
      },
      error: (error) => {
        console.error('Error marking all notifications as read:', error);
      }
    });
  }

  deleteNotification(notificationId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette notification ?')) {
      this.notificationService.deleteNotification(notificationId).subscribe({
        next: () => {
          // Remove the notification from the list
          this.notifications = this.notifications.filter(n => n.id !== notificationId);
          this.dataSource.data = this.notifications;
          this.unreadCount = this.notifications.filter(n => !n.isRead).length;
        },
        error: (error) => {
          console.error('Error deleting notification:', error);
        }
      });
    }
  }

  clearAllNotifications(): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer toutes les notifications ?')) {
      this.notificationService.clearAllNotifications().subscribe({
        next: () => {
          this.notifications = [];
          this.dataSource.data = this.notifications;
          this.unreadCount = 0;
        },
        error: (error) => {
          console.error('Error clearing all notifications:', error);
        }
      });
    }
  }

  // Helper method to get notification type color
  getTypeColor(type: string): string {
    const colors: Record<string, string> = {
      'info': '#2196f3', // blue
      'success': '#4caf50', // green
      'warning': '#ff9800', // orange
      'error': '#f44336' // red
    };
    return colors[type] || '#9e9e9e';
  }

  // Helper method to get notification type label
  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'info': 'Information',
      'success': 'Succès',
      'warning': 'Avertissement',
      'error': 'Erreur'
    };
    return labels[type] || type;
  }
}