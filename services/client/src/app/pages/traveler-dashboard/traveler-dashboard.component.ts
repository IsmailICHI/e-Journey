import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

@Component({
  selector: 'app-traveler-dashboard',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './traveler-dashboard.component.html'
})
export class TravelerDashboardComponent {
  readonly stats = [
    { label: 'Trips booked', value: '12', delta: '+3 this quarter' },
    { label: 'Reports filed', value: '2', delta: '1 pending review' },
    { label: 'Cancellations', value: '1', delta: 'Within cutoff window' },
    { label: 'Preferred payment', value: 'Visa •••• 4242', delta: 'Preferred' }
  ];

  readonly capabilities = [
    'Autocomplete search across destinations, dates, and travel themes',
    'Personalized recommendations powered by trip history and community feedback',
    'Secure checkout with cards, wallets, and invoice payment methods',
    'Travel subscriptions and cancellation policies monitored before departure'
  ];

  readonly activity = [
    { title: 'City escape to Kyoto', time: 'Booked 4 days ago', status: 'Confirmed' },
    { title: 'Ski retreat in Zermatt', time: 'Rated 5/5 by travelers', status: 'Reviewed' },
    { title: 'Beach tour in Bali', time: 'Report closed by support', status: 'Resolved' }
  ];
}
