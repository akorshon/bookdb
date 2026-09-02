import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Review } from './models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private readonly http: HttpClient) {}

  list(bookId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`/api/books/${bookId}/reviews`);
  }

  upsert(bookId: number, rating: number, reviewText: string): Observable<Review> {
    return this.http.post<Review>(`/api/books/${bookId}/reviews`, { rating, reviewText });
  }

  delete(reviewId: number): Observable<void> {
    return this.http.delete<void>(`/api/reviews/${reviewId}`);
  }
}
