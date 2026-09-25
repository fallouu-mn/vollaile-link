import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Le meter de disponibilité — élément signature de Vollaile Link.
 *
 * Il rend visible la règle qui governement réellement la plateforme :
 *
 *     disponible = total − réservé − vendu
 *
 * C'est la même règle que celle du moteur de stock, exposée au visiteur.
 * Une jauge unique « stock : 130 » serait plus courte, mais elle
 * masquerait le fait qu'une partie du lot est déjà engagée par
 * d'autres acheteurs — l'information qui explique pourquoi une offre
 * à 200 unités peut n'en proposer que 130.
 *
 * Les segments sont donc proportionnels au total, pas à l'inverse.
 */
@Component({
  selector: 'app-lot-meter',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="lot" [class.lot--compact]="compact" [class.lot--flipped]="flipped">
      @if (label) {
        <div class="lot__head">
          <span class="lot__label">{{ label }}</span>
          @if (unit) {
            <span class="lot__unit">{{ unit }}</span>
          }
        </div>
      }

      <div
        class="lot__track"
        role="img"
        [attr.aria-label]="ariaLabel()"
      >
        @if (disponible > 0) {
          <div class="lot__seg lot__seg--libre" [style.flex-grow]="disponible"></div>
        }
        @if (reserve > 0) {
          <div class="lot__seg lot__seg--reserve" [style.flex-grow]="reserve"></div>
        }
        @if (vendu > 0) {
          <div class="lot__seg lot__seg--vendu" [style.flex-grow]="vendu"></div>
        }
        @if (total === 0) {
          <div class="lot__seg lot__seg--vide"></div>
        }
      </div>

      @if (!compact) {
        <div class="lot__legend">
          <span class="lot__key">
            <i class="lot__dot lot__dot--libre"></i>
            <b>{{ disponible }}</b> disponible{{ disponible > 1 ? 's' : '' }}
          </span>
          @if (reserve > 0) {
            <span class="lot__key">
              <i class="lot__dot lot__dot--reserve"></i>
              <b>{{ reserve }}</b> réservé{{ reserve > 1 ? 's' : '' }}
            </span>
          }
          @if (vendu > 0) {
            <span class="lot__key">
              <i class="lot__dot lot__dot--vendu"></i>
              <b>{{ vendu }}</b> vendu{{ vendu > 1 ? 's' : '' }}
            </span>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }

    .lot { display: flex; flex-direction: column; gap: .5rem; }

    .lot__head {
      display: flex; align-items: baseline; justify-content: space-between;
      gap: .75rem;
    }

    .lot__label {
      font-size: var(--text-xs);
      font-weight: 600;
      letter-spacing: .1em;
      text-transform: uppercase;
      color: var(--text-stone);
    }

    .lot__unit {
      font-family: var(--font-mono);
      font-size: var(--text-xs);
      color: var(--text-stone-2);
    }

    /* La piste porte une trame diagonale sur le segment réservé :
       elle rappelle le carrelage des caisses du marché sans
       devenir un motif décoratif. */
    .lot__track {
      display: flex;
      height: 0.625rem;
      border-radius: 999px;
      overflow: hidden;
      background: var(--bone-deep);
      box-shadow: inset 0 0 0 1px rgba(41,28,20,.06);
    }

    .lot__seg { height: 100%; min-width: 2px; }

    .lot__seg--libre {
      background: var(--petrol);
    }

    .lot__seg--reserve {
      background: var(--saffron);
      background-image: repeating-linear-gradient(
        135deg,
        rgba(255,255,255,.28) 0 3px,
        transparent 3px 7px
      );
    }

    .lot__seg--vendu {
      background: var(--clay);
      opacity: .32;
    }

    .lot__seg--vide {
      flex: 1;
      background: repeating-linear-gradient(
        135deg,
        var(--bone-deep) 0 5px,
        var(--chalk) 5px 10px
      );
    }

    .lot__legend {
      display: flex; flex-wrap: wrap; gap: .25rem 1rem;
      font-size: var(--text-xs);
      color: var(--text-stone);
    }

    .lot__key { display: inline-flex; align-items: center; gap: .375rem; }
    .lot__key b { font-family: var(--font-mono); color: var(--text-ink); }

    .lot__dot { width: .5rem; height: .5rem; border-radius: 2px; display: inline-block; }
    .lot__dot--libre    { background: var(--petrol); }
    .lot__dot--reserve { background: var(--saffron); }
    .lot__dot--vendu   { background: var(--clay); opacity: .45; }

    /* Version sur fond sombre */
    .lot--flipped {
      .lot__label { color: var(--text-on-ink-2); }
      .lot__unit, .lot__legend { color: var(--text-on-ink-3); }
      .lot__key b { color: var(--text-on-ink); }
      .lot__track {
        background: rgba(247,244,239,.10);
        box-shadow: inset 0 0 0 1px rgba(247,244,239,.12);
      }
    }

    .lot--compact {
      .lot__track { height: .375rem; }
    }
  `]
})
export class LotMeterComponent {
  /** Quantité totale du lot. Base de toutes les proportions. */
  @Input() total = 0;
  /** Quantité encore vendable. */
  @Input() disponible = 0;
  /** Quantité engagée par d'autres commandes. */
  @Input() reserve = 0;
  /** Quantité définitivement vendue. */
  @Input() vendu = 0;
  @Input() label = '';
  @Input() unit = '';
  /** Version sans légende, pour les cartes d'offres. */
  @Input() compact = false;
  /** Variante claire, pour fond encre. */
  @Input() flipped = false;

  protected readonly ariaLabel = computed(() => {
    const parts = [`${this.total} unités au total`];
    if (this.disponible > 0) parts.push(`${this.disponible} disponibles`);
    if (this.reserve > 0) parts.push(`${this.reserve} réservées`);
    if (this.vendu > 0) parts.push(`${this.vendu} vendues`);
    return parts.join(', ');
  });
}
