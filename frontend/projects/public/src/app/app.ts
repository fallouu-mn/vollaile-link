import {
  Component,
  HostListener,
  Inject,
  PLATFORM_ID,
  signal
} from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd, Router } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { filter } from 'rxjs/operators';

/**
 * Coquille du site public : en-tête, navigation, pied de page.
 *
 * Deux comportements sont pilotés ici plutôt que dans les pages,
 * pour qu'ils survivent à la navigation :
 *  - l'en-tête se compacte au défilement et lit la progression de lecture ;
 *  - le menu mobile se ferme à chaque changement de route.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  /** L'en-tête change d'aspect au-delà de 12 px de défilement. */
  protected readonly stuck = signal(false);
  /** 0 → 1, progression de lecture dans la page. */
  protected readonly progress = signal(0);
  protected readonly menuOpen = signal(false);
  protected readonly year = new Date().getFullYear();

  private readonly isBrowser: boolean;

  constructor(
    router: Router,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

    // Une route différente referme le menu : sinon il reste ouvert
    // par-dessus la page atteinte.
    router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        this.menuOpen.set(false);
        this.resetScroll();
      });
  }

  @HostListener('window:scroll')
  onScroll(): void {
    if (!this.isBrowser) {
      return;
    }

    const y = window.scrollY;
    this.stuck.set(y > 12);

    const hauteur = document.documentElement.scrollHeight - window.innerHeight;
    this.progress.set(hauteur > 0 ? Math.min(y / hauteur, 1) : 0);
  }

  // Échap ferme le menu : attendu sur une navigation clavier.
  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeMenu();
  }

  protected toggleMenu(): void {
    this.menuOpen.update(v => !v);
    if (this.isBrowser) {
      // Verrouille le défilement de fond quand le menu est ouvert.
      document.body.style.overflow = this.menuOpen() ? 'hidden' : '';
    }
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
    if (this.isBrowser) {
      document.body.style.overflow = '';
    }
  }

  /**
   * Chaque navigation repart du haut.
   * Sans cela, on arrive au milieu de la page suivante.
   */
  private resetScroll(): void {
    if (this.isBrowser) {
      window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior });
    }
  }
}
