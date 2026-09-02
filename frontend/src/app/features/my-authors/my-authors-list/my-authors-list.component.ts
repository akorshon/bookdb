import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthorService } from '../../../core/author.service';
import { AuthorResponse } from '../../../core/models';

@Component({
  selector: 'app-my-authors-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-authors-list.component.html',
  styleUrl: './my-authors-list.component.css',
})
export class MyAuthorsListComponent implements OnInit {
  authors = signal<AuthorResponse[]>([]);
  loading = signal(true);

  constructor(private readonly authorService: AuthorService) {}

  ngOnInit(): void {
    this.load();
  }

  submit(author: AuthorResponse): void {
    this.authorService.submit(author.id).subscribe(() => this.load());
  }

  statusLabel(status: string): string {
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

  statusClass(status: string): string {
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
    this.authorService.mine().subscribe({
      next: (authors) => {
        this.authors.set(authors);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
