import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
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

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authorService: AuthorService,
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
}
