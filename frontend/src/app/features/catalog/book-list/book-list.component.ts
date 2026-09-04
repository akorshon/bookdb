import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BookService } from '../../../core/book.service';
import { Book, CategorySection } from '../../../core/models';
import { BookCardComponent } from '../../../shared/book-card/book-card.component';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [FormsModule, BookCardComponent],
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.css',
})
export class BookListComponent implements OnInit {
  // Browse mode (no search active): published books grouped into category sections, each capped.
  sections = signal<CategorySection[]>([]);
  browseLoading = signal(true);

  // Search mode: flat, paginated results for the active query.
  searching = signal(false);
  books = signal<Book[]>([]);
  totalElements = signal(0);
  page = signal(0);
  totalPages = signal(0);
  loading = signal(false);

  query = '';

  constructor(private readonly bookService: BookService) {}

  ngOnInit(): void {
    this.loadSections();
  }

  search(): void {
    if (!this.query.trim()) {
      this.searching.set(false);
      return;
    }
    this.searching.set(true);
    this.page.set(0);
    this.load();
  }

  clearSearch(): void {
    this.query = '';
    this.searching.set(false);
  }

  goToPage(p: number): void {
    this.page.set(p);
    this.load();
  }

  private loadSections(): void {
    this.browseLoading.set(true);
    // Server-side grouped and capped (see BookController.sections) - the browse page only ever
    // needs a preview per category, not the whole catalog fetched and grouped client-side.
    this.bookService.sections().subscribe({
      next: (res) => {
        this.sections.set(res);
        this.browseLoading.set(false);
      },
      error: () => this.browseLoading.set(false),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.bookService.list(this.query, this.page()).subscribe({
      next: (res) => {
        this.books.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
