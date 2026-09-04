import { Component, ElementRef, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthorService } from '../../../core/author.service';
import { BookService } from '../../../core/book.service';
import { CategoryService } from '../../../core/category.service';
import { AuthorResponse, Book, BookFormValue, CategorySummary } from '../../../core/models';

interface TextSegment {
  start: number;
  end: number;
  value: string;
}

@Component({
  selector: 'app-book-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './book-form.component.html',
  styleUrl: './book-form.component.css',
})
export class BookFormComponent implements OnInit {
  @ViewChild('authorsInput') authorsInputRef?: ElementRef<HTMLInputElement>;

  bookId: number | null = null;
  book = signal<Book | null>(null);
  form: BookFormValue = { title: '', titleNative: '', authorNames: [], summary: '', categories: [] };
  authorsText = '';
  categoriesText = '';
  knownCategories = signal<CategorySummary[]>([]);
  saving = signal(false);
  error = signal<string | null>(null);
  uploadingCover = signal(false);

  knownAuthors = signal<AuthorResponse[]>([]);
  authorSuggestions = signal<AuthorResponse[]>([]);
  showAuthorSuggestions = signal(false);
  activeSuggestionIndex = signal(-1);
  private authorCursorSegment: TextSegment | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookService: BookService,
    private readonly categoryService: CategoryService,
    private readonly authorService: AuthorService,
  ) {}

  ngOnInit(): void {
    this.categoryService.list().subscribe((categories) => this.knownCategories.set(categories));
    this.authorService.list().subscribe((authors) => this.knownAuthors.set(authors));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.bookId = Number(idParam);
      this.bookService.get(this.bookId).subscribe((b) => {
        this.book.set(b);
        this.form = {
          title: b.title,
          titleNative: b.titleNative || '',
          authorNames: b.authors.map((a) => a.name),
          summary: b.summary || '',
          categories: b.categories.map((c) => c.name),
        };
        this.authorsText = this.form.authorNames.join(', ');
        this.categoriesText = this.form.categories.join(', ');
      });
    }
  }

  onAuthorsInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const cursorPos = input.selectionStart ?? this.authorsText.length;
    const segment = this.currentSegment(this.authorsText, cursorPos);
    this.authorCursorSegment = segment;

    const q = segment.value.toLowerCase();
    if (!q) {
      this.showAuthorSuggestions.set(false);
      this.authorSuggestions.set([]);
      return;
    }
    const matches = this.knownAuthors()
      .filter((a) => a.name.toLowerCase().includes(q))
      .slice(0, 8);
    this.authorSuggestions.set(matches);
    this.activeSuggestionIndex.set(-1);
    this.showAuthorSuggestions.set(matches.length > 0);
  }

  onAuthorsKeydown(event: KeyboardEvent): void {
    if (!this.showAuthorSuggestions()) {
      return;
    }
    const suggestions = this.authorSuggestions();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeSuggestionIndex.set(Math.min(this.activeSuggestionIndex() + 1, suggestions.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeSuggestionIndex.set(Math.max(this.activeSuggestionIndex() - 1, 0));
    } else if (event.key === 'Enter' && this.activeSuggestionIndex() >= 0) {
      event.preventDefault();
      this.selectAuthorSuggestion(suggestions[this.activeSuggestionIndex()].name);
    } else if (event.key === 'Escape') {
      this.showAuthorSuggestions.set(false);
    }
  }

  selectAuthorSuggestion(name: string): void {
    const segment = this.authorCursorSegment;
    if (!segment) {
      return;
    }
    const before = this.authorsText.slice(0, segment.start).replace(/[,\s]+$/, '');
    const after = this.authorsText.slice(segment.end).replace(/^\s*,?\s*/, '');
    const prefix = before.length > 0 ? before + ', ' : '';
    const newText = prefix + name + ', ' + after;
    this.authorsText = newText;
    this.showAuthorSuggestions.set(false);
    this.authorSuggestions.set([]);

    const cursorPos = (prefix + name + ', ').length;
    setTimeout(() => {
      const el = this.authorsInputRef?.nativeElement;
      el?.focus();
      el?.setSelectionRange(cursorPos, cursorPos);
    });
  }

  hideAuthorSuggestions(): void {
    this.showAuthorSuggestions.set(false);
  }

  private currentSegment(text: string, cursorPos: number): TextSegment {
    const lastComma = text.lastIndexOf(',', Math.max(cursorPos - 1, 0));
    const start = lastComma === -1 ? 0 : lastComma + 1;
    const nextComma = text.indexOf(',', cursorPos);
    const end = nextComma === -1 ? text.length : nextComma;
    return { start, end, value: text.slice(start, end).trim() };
  }

  save(): void {
    this.error.set(null);
    this.saving.set(true);
    this.form.authorNames = this.splitCommaList(this.authorsText);
    this.form.categories = this.splitCommaList(this.categoriesText);
    const req = this.bookId
      ? this.bookService.update(this.bookId, this.form)
      : this.bookService.create(this.form);
    req.subscribe({
      next: (b) => {
        this.saving.set(false);
        if (!this.bookId) {
          this.router.navigate(['/my-books', b.id, 'edit']);
        } else {
          this.book.set(b);
        }
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Could not save. Check the required fields.');
      },
    });
  }

  onCoverSelected(event: Event): void {
    if (!this.bookId) {
      return;
    }
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploadingCover.set(true);
    this.bookService.uploadCover(this.bookId, file).subscribe({
      next: (b) => {
        this.book.set(b);
        this.uploadingCover.set(false);
      },
      error: () => this.uploadingCover.set(false),
    });
  }

  private splitCommaList(text: string): string[] {
    return text
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0);
  }
}
