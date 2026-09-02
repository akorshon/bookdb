import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookService } from '../../../core/book.service';
import { Book } from '../../../core/models';

@Component({
  selector: 'app-my-books-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-books-list.component.html',
  styleUrl: './my-books-list.component.css',
})
export class MyBooksListComponent implements OnInit {
  books = signal<Book[]>([]);
  loading = signal(true);

  constructor(private readonly bookService: BookService) {}

  ngOnInit(): void {
    this.load();
  }

  submit(book: Book): void {
    this.bookService.submit(book.id).subscribe(() => this.load());
  }

  authorNames(book: Book): string {
    return book.authors.length > 0 ? book.authors.map((a) => a.name).join(', ') : 'Unknown author';
  }

  statusLabel(status: string | null): string {
    switch (status) {
      case 'PUBLISHED':
        return 'Published';
      case 'PENDING_REVIEW':
        return 'Pending review';
      case 'REJECTED':
        return 'Rejected';
      default:
        return 'Draft';
    }
  }

  statusClass(status: string | null): string {
    switch (status) {
      case 'PUBLISHED':
        return 'status-published';
      case 'PENDING_REVIEW':
        return 'status-pending';
      case 'REJECTED':
        return 'status-rejected';
      default:
        return 'status-draft';
    }
  }

  private load(): void {
    this.loading.set(true);
    this.bookService.mine().subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
