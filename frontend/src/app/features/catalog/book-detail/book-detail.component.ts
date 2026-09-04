import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { BookService } from '../../../core/book.service';
import { Book, ReadingStatus, Review } from '../../../core/models';
import { ReadingListService } from '../../../core/reading-list.service';
import { ReviewService } from '../../../core/review.service';
import { StarRatingComponent } from '../../../shared/star-rating/star-rating.component';

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [FormsModule, RouterLink, StarRatingComponent],
  templateUrl: './book-detail.component.html',
  styleUrl: './book-detail.component.css',
})
export class BookDetailComponent implements OnInit {
  book = signal<Book | null>(null);
  reviews = signal<Review[]>([]);
  notFound = signal(false);
  loading = signal(true);

  myRating = 0;
  myReviewText = '';
  savingReview = signal(false);

  readingStatus = signal<ReadingStatus | null>(null);
  savingReadingStatus = signal(false);

  deleting = signal(false);
  deleteError = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookService: BookService,
    private readonly reviewService: ReviewService,
    private readonly readingListService: ReadingListService,
    readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.bookService.get(id).subscribe({
      next: (b) => {
        this.book.set(b);
        this.loadReviews(id);
        const mine = this.reviews().find((r) => r.userId === this.auth.currentUser()?.id);
        if (mine) {
          this.myRating = mine.rating;
          this.myReviewText = mine.reviewText || '';
        }
        if (this.auth.isLoggedIn()) {
          this.readingListService.getStatus(id).subscribe((info) => this.readingStatus.set(info.status));
        }
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  setReadingStatus(status: ReadingStatus): void {
    const b = this.book();
    if (!b || this.savingReadingStatus()) {
      return;
    }
    this.savingReadingStatus.set(true);
    if (this.readingStatus() === status) {
      this.readingListService.remove(b.id).subscribe({
        next: () => {
          this.readingStatus.set(null);
          this.savingReadingStatus.set(false);
        },
        error: () => this.savingReadingStatus.set(false),
      });
      return;
    }
    this.readingListService.setStatus(b.id, status).subscribe({
      next: (info) => {
        this.readingStatus.set(info.status);
        this.savingReadingStatus.set(false);
      },
      error: () => this.savingReadingStatus.set(false),
    });
  }

  loadReviews(bookId: number): void {
    this.reviewService.list(bookId).subscribe((reviews) => {
      this.reviews.set(reviews);
      const mine = reviews.find((r) => r.userId === this.auth.currentUser()?.id);
      if (mine) {
        this.myRating = mine.rating;
        this.myReviewText = mine.reviewText || '';
      }
    });
  }

  submitReview(): void {
    const b = this.book();
    if (!b || this.myRating < 1) {
      return;
    }
    this.savingReview.set(true);
    this.reviewService.upsert(b.id, this.myRating, this.myReviewText).subscribe({
      next: () => {
        this.savingReview.set(false);
        this.loadReviews(b.id);
        this.bookService.get(b.id).subscribe((updated) => this.book.set(updated));
      },
      error: () => this.savingReview.set(false),
    });
  }

  myExistingReview(): Review | undefined {
    return this.reviews().find((r) => r.userId === this.auth.currentUser()?.id);
  }

  deleteBook(): void {
    const b = this.book();
    if (!b || !window.confirm(`Delete "${b.title}"? This cannot be undone.`)) {
      return;
    }
    this.deleteError.set(null);
    this.deleting.set(true);
    this.bookService.adminDelete(b.id).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.deleting.set(false);
        this.deleteError.set(err.error?.message || 'Could not delete book.');
      },
    });
  }
}
