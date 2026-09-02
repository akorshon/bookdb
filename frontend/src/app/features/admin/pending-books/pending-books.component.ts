import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BookService } from '../../../core/book.service';
import { Book } from '../../../core/models';

@Component({
  selector: 'app-pending-books',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './pending-books.component.html',
})
export class PendingBooksComponent implements OnInit {
  books = signal<Book[]>([]);
  loading = signal(true);
  rejectReasons: Record<number, string> = {};

  constructor(private readonly bookService: BookService) {}

  ngOnInit(): void {
    this.load();
  }

  approve(book: Book): void {
    this.bookService.approve(book.id).subscribe(() => this.load());
  }

  reject(book: Book): void {
    const reason = this.rejectReasons[book.id]?.trim();
    if (!reason) {
      return;
    }
    this.bookService.reject(book.id, reason).subscribe(() => this.load());
  }

  authorNames(book: Book): string {
    return book.authors.length > 0 ? book.authors.map((a) => a.name).join(', ') : 'Unknown author';
  }

  private load(): void {
    this.loading.set(true);
    this.bookService.adminList('PENDING_REVIEW', 0, 100).subscribe({
      next: (res) => {
        this.books.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
