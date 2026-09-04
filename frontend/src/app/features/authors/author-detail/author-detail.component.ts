import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { AuthorService } from '../../../core/author.service';
import { AuthorResponse, Book } from '../../../core/models';
import { BookCardComponent } from '../../../shared/book-card/book-card.component';

@Component({
  selector: 'app-author-detail',
  standalone: true,
  imports: [RouterLink, BookCardComponent],
  templateUrl: './author-detail.component.html',
  styleUrl: './author-detail.component.css',
})
export class AuthorDetailComponent implements OnInit {
  author = signal<AuthorResponse | null>(null);
  books = signal<Book[]>([]);
  loading = signal(true);
  booksLoading = signal(true);
  notFound = signal(false);

  deleting = signal(false);
  deleteError = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authorService: AuthorService,
    readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.authorService.get(id).subscribe({
      next: (a) => {
        this.author.set(a);
        this.loading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
    this.authorService.books(id).subscribe({
      next: (b) => {
        this.books.set(b);
        this.booksLoading.set(false);
      },
      error: () => this.booksLoading.set(false),
    });
  }

  deleteAuthor(): void {
    const a = this.author();
    if (!a || !window.confirm(`Delete "${a.name}"? This cannot be undone.`)) {
      return;
    }
    this.deleteError.set(null);
    this.deleting.set(true);
    this.authorService.adminDelete(a.id).subscribe({
      next: () => this.router.navigate(['/authors']),
      error: (err) => {
        this.deleting.set(false);
        this.deleteError.set(err.error?.message || 'Could not delete author.');
      },
    });
  }
}
