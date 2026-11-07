import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

@Component({
  selector: 'app-travel-manager-dashboard',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './travel-manager-dashboard.component.html'
})
export class TravelManagerDashboardComponent {
  readonly stats = [
    { label: 'Income', value: '$48.2K', delta: 'vs. last month' },
    { label: 'Trips organized', value: '18', delta: '+5 this cycle' },
    { label: 'Travelers served', value: '364', delta: 'Across 11 regions' },
    { label: 'Feedback score', value: '4.8/5', delta: 'Strong satisfaction' }
  ];

  readonly priorities = [
    'Create and update travel offerings with clear schedule, pricing, and itinerary details.',
    'Track traveler engagement, feedback, and cancellations for service quality improvement.',
    'Review subscriber lists and moderate cancellations before the three-day cutoff window.',
    'Measure income, trip volume, and traveler retention to identify growth trends.'
  ];

  readonly subscribers = [
    { name: 'Alicia R.', status: 'Confirmed' },
    { name: 'Noah B.', status: 'Pending review' },
    { name: 'Marina P.', status: 'Subscribed' }
  ];
}
