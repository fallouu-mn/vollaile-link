import { Component, OnInit, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {

  sidebarOpen = false;
  pageTitle = 'Tableau de bord';
  unreadNotificationsCount = 0;
  unreadDemandesCount = 0;

  private routerSub?: Subscription;

  /** Correspondance route → titre de page affiché dans le header */
  private readonly PAGE_TITLES: Record<string, string> = {
    '/dashboard':       'Tableau de bord',
    '/clients':         'Clients',
    '/demandes':        'Demandes',
    '/commandes':       'Commandes',
    '/offres':          'Offres & prix',
    '/stock':           'Gestion du stock',
    '/notifications':   'Notifications',
    '/zones-livraison': 'Zones de livraison',
    '/audit':           "Journal d'audit",
    '/login':           'Connexion',
    '/change-password': 'Changer le mot de passe',
  };

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Mettre à jour le titre de page à chaque navigation
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      const path = '/' + event.urlAfterRedirects.split('/').filter(Boolean)[0];
      this.pageTitle = this.PAGE_TITLES[path] ?? 'Administration';
      // Fermer la sidebar mobile à chaque navigation
      this.sidebarOpen = false;
    });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  /** Indique si le shell (sidebar + header) doit être affiché */
  get isAuthPage(): boolean {
    const url = this.router.url;
    return url.includes('/login') || url.includes('/change-password');
  }
}
