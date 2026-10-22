import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
//import { AuthService } from '../../core/services/auth.service';

export interface Destination {
  id: number;
  city: string;
  country: string;
  subtitle: string;
  image: string;
  price: string;
  tag: string;
  coordinates: string;
  description: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  //private authService = inject(AuthService);

  selectedCity: Destination | null = null;

  destinations: Destination[] = [
    {
      id: 1,
      city: 'Los Angeles',
      country: 'United States',
      subtitle: 'Griffith Observatory & Twilight Skyline',
      image: 'https://images.unsplash.com/photo-1580655653885-65763b2597d0?q=80&w=1000',
      price: '$1,650',
      tag: 'City of Stars',
      coordinates: '34.0522° N, 118.2437° W',
      description: 'Perched above the lights of the city, where evening twilight bridges classic Hollywood romance with modern coastal elegance.'
    },
    {
      id: 2,
      city: 'Marrakesh',
      country: 'Morocco',
      subtitle: '36 Hours in the Red Oasis',
      image: 'https://images.unsplash.com/photo-1597212618440-806262de4f6b?q=80&w=1000',
      price: '$1,150',
      tag: 'Heritage Sanctuary',
      coordinates: '31.6295° N, 7.9811° W',
      description: 'Lantern-lit riads, quiet courtyards, and starlit rooftops carved out of terracotta walls in the heart of North Africa.'
    },
    {
      id: 3,
      city: 'Santorini',
      country: 'Greece',
      subtitle: 'Aegean Coastal Estate & Sunsets',
      image: 'https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?q=80&w=1000',
      price: '$1,490',
      tag: 'Island Getaway',
      coordinates: '36.3932° N, 25.4615° E',
      description: 'Volcanic cliffs falling into dark blue waters, crowned with crisp whitewashed architecture that reflects the twilight sky.'
    },
    {
      id: 4,
      city: 'Tokyo',
      country: 'Japan',
      subtitle: 'Neon Skyline & Minimalist Architecture',
      image: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?q=80&w=1000',
      price: '$1,850',
      tag: 'Urban Metropolis',
      coordinates: '35.6764° N, 139.6500° E',
      description: 'An ethereal collision of glowing neon light, quiet wooden sanctuaries, and high-altitude architectural views.'
    }
  ];

  displayedDestinations: Destination[] = [];

  ngOnInit(): void {
    this.displayedDestinations = [...this.destinations].slice(0, 3);
  }

  shuffle(): void {
    this.displayedDestinations = [...this.destinations]
      .sort(() => 0.5 - Math.random())
      .slice(0, 3);
  }

  openPreview(item: Destination): void {
    this.selectedCity = item;
  }

  closePreview(): void {
    this.selectedCity = null;
  }

}