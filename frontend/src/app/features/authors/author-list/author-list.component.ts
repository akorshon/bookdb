import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthorService } from '../../../core/author.service';
import { AuthorResponse } from '../../../core/models';

@Component({
  selector: 'app-author-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './author-list.component.html',
  styleUrl: './author-list.component.css',
})
export class AuthorListComponent implements OnInit {
  authors = signal<AuthorResponse[]>([]);
  loading = signal(true);
  query = '';

  constructor(private readonly authorService: AuthorService) {}

  ngOnInit(): void {
    this.authorService.list().subscribe({
      next: (authors) => {
        this.authors.set(authors);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  filtered(): AuthorResponse[] {
    const q = this.query.trim().toLowerCase();
    if (!q) {
      return this.authors();
    }
    return this.authors().filter(
      (a) => a.name.toLowerCase().includes(q) || (a.nameNative?.toLowerCase().includes(q) ?? false),
    );
  }
}
