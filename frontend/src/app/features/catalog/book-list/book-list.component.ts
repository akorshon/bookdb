import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BookService } from '../../../core/book.service';
import { Book } from '../../../core/models';
import { BookCardComponent } from '../../../shared/book-card/book-card.component';

interface CategorySection {
  name: string;
  books: Book[];
}

const UNCATEGORIZED = 'Uncategorized';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [FormsModule, BookCardComponent],
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.css',
})
export class BookListComponent implements OnInit {
  // Browse mode (no search active): all published books grouped into category sections.
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
    // The catalog is small enough (a few hundred books) to fetch in one page and group
    // client-side, rather than adding a dedicated grouped-by-category backend endpoint.
    this.bookService.list('', 0, 500).subscribe({
      next: (res) => {
        this.sections.set(this.groupByCategory(res.content));
        this.browseLoading.set(false);
      },
      error: () => this.browseLoading.set(false),
    });
  }

  private groupByCategory(books: Book[]): CategorySection[] {
    const byCategory = new Map<string, Book[]>();
    for (const book of books) {
      const names = book.categories.length > 0 ? book.categories.map((c) => c.name) : [UNCATEGORIZED];
      for (const name of names) {
        const list = byCategory.get(name) ?? [];
        list.push(book);
        byCategory.set(name, list);
      }
    }
    const sections = [...byCategory.entries()]
      .filter(([name]) => name !== UNCATEGORIZED)
      .map(([name, list]) => ({ name, books: list }))
      .sort((a, b) => b.books.length - a.books.length);
    const uncategorized = byCategory.get(UNCATEGORIZED);
    if (uncategorized) {
      sections.push({ name: UNCATEGORIZED, books: uncategorized });
    }
    return sections;
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
