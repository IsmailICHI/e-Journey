import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FeedbackDialogComponent } from './shared/feedback-dialog/feedback-dialog.component';
import { ConfirmationDialogComponent } from './shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, FeedbackDialogComponent, ConfirmationDialogComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('client');
}