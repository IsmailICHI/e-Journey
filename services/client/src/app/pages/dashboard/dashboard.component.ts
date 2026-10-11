import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

interface Journey {
  destination: string;
  detail: string;
  dates: string;
  status: string;
  image: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  readonly journeys: Journey[] = [
    { destination: 'Marrakesh', detail: 'Riad & medina stay', dates: '12 — 17 JUN 2026', status: 'Planning', image: 'https://images.unsplash.com/photo-1597212618440-806262de4f6b?q=80&w=700' },
    { destination: 'Santorini', detail: 'Aegean coast escape', dates: '03 — 09 SEP 2026', status: 'Saved', image: 'https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?q=80&w=700' }
  ];

}