import { Directive, ElementRef, OnInit, OnDestroy, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Révèle un élément lorsqu'il entre dans le viewport.
 *
 * Deux précautions :
 *
 * 1. Serveur. Le rendu serveur n'exécute pas JavaScript : la directive
 *    ne masque donc jamais l'élément côté serveur, et sur le client
 *    l'état masqué n'est appliqué qu'après hydratation. Sans cela,
 *    un utilisateur sans JavaScript ne verrait qu'une page vide.
 *
 * 2. `prefers-reduced-motion`. Le CSS neutralise alors la transition
 *    (voir animations.scss) ; la classe est posée quand même, sans
 *    conséquence visuelle.
 */
@Directive({
  selector: '[appReveal]',
  standalone: true
})
export class RevealDirective implements OnInit, OnDestroy {

  private observer?: IntersectionObserver;
  private readonly isBrowser: boolean;

  constructor(
    private host: ElementRef<HTMLElement>,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (!this.isBrowser) {
      return;
    }

    const el = this.host.nativeElement;

    // Pas d'IntersectionObserver : on montre tout, on n'anime pas.
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add('is-revealed');
      return;
    }

    el.classList.add('reveal');

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            el.classList.add('is-revealed');
            // Une seule fois : au-delà, l'élément est visible en permanence.
            this.observer?.unobserve(el);
          }
        }
      },
      {
        // Le déclenchement légèrement anticipé rend l'apparition
        // naturelle : l'élément est déjà là quand l'œil arrive.
        rootMargin: '0px 0px -12% 0px',
        threshold: 0.08
      }
    );

    this.observer.observe(el);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
