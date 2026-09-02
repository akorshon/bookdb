import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Book } from '../../core/models';
import { StarRatingComponent } from '../star-rating/star-rating.component';

@Component({
  selector: 'app-book-card',
  standalone: true,
  imports: [RouterLink, StarRatingComponent],
  templateUrl: './book-card.component.html',
  styleUrl: './book-card.component.css',
})
export class BookCardComponent {
  @Input({ required: true }) book!: Book;

  authorNames(): string {
    return this.book.authors.length > 0
      ? this.book.authors.map((a) => a.name).join(', ')
      : 'Unknown author';
  }
}
