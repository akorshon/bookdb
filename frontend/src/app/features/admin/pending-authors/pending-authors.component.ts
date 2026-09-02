import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthorService } from '../../../core/author.service';
import { AuthorResponse } from '../../../core/models';

@Component({
  selector: 'app-pending-authors',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './pending-authors.component.html',
})
export class PendingAuthorsComponent implements OnInit {
  authors = signal<AuthorResponse[]>([]);
  loading = signal(true);
  rejectReasons: Record<number, string> = {};

  constructor(private readonly authorService: AuthorService) {}

  ngOnInit(): void {
    this.load();
  }

  approve(author: AuthorResponse): void {
    this.authorService.adminApprove(author.id).subscribe(() => this.load());
  }

  reject(author: AuthorResponse): void {
    const reason = this.rejectReasons[author.id]?.trim();
    if (!reason) {
      return;
    }
    this.authorService.adminReject(author.id, reason).subscribe(() => this.load());
  }

  private load(): void {
    this.loading.set(true);
    this.authorService.adminList('PENDING_REVIEW').subscribe({
      next: (authors) => {
        this.authors.set(authors);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
