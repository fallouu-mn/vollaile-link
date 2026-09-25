import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { AvailabilityComponent } from './pages/availability/availability.component';
import { OfferDetailComponent } from './pages/offer-detail/offer-detail.component';
import { HowItWorksComponent } from './pages/how-it-works/how-it-works.component';
import { AboutComponent } from './pages/about/about.component';
import { ServiceAreasComponent } from './pages/service-areas/service-areas.component';
import { FAQComponent } from './pages/faq/faq.component';
import { ContactComponent } from './pages/contact/contact.component';
import { RequestComponent } from './pages/request/request.component';
import { RequestConfirmationComponent } from './pages/request-confirmation/request-confirmation.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    pathMatch: 'full'
  },
  {
    path: 'home',
    redirectTo: '',
    pathMatch: 'full'
  },
  {
    path: 'disponibilites',
    component: AvailabilityComponent
  },
  {
    path: 'availability',
    redirectTo: 'disponibilites',
    pathMatch: 'full'
  },
  {
    path: 'offre/:id',
    component: OfferDetailComponent
  },
  {
    path: 'offer/:id',
    redirectTo: 'offre/:id',
    pathMatch: 'full'
  },
  {
    path: 'comment-ca-marche',
    component: HowItWorksComponent
  },
  {
    path: 'how-it-works',
    redirectTo: 'comment-ca-marche',
    pathMatch: 'full'
  },
  {
    path: 'a-propos',
    component: AboutComponent
  },
  {
    path: 'about',
    redirectTo: 'a-propos',
    pathMatch: 'full'
  },
  {
    path: 'zones-desservees',
    component: ServiceAreasComponent
  },
  {
    path: 'service-areas',
    redirectTo: 'zones-desservees',
    pathMatch: 'full'
  },
  {
    path: 'faq',
    component: FAQComponent
  },
  {
    path: 'contact',
    component: ContactComponent
  },
  {
    path: 'demande-devis',
    component: RequestComponent
  },
  {
    path: 'request-quote',
    redirectTo: 'demande-devis',
    pathMatch: 'full'
  },
  {
    path: 'confirmation-demande',
    component: RequestConfirmationComponent
  },
  {
    path: 'request-confirmation',
    redirectTo: 'confirmation-demande',
    pathMatch: 'full'
  }
];