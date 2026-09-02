import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ReadingListService } from '../../core/reading-list.service';
import { ReadingListEntry, ReadingStatus } from '../../core/models';
import { BookCardComponent } from '../../shared/book-card/book-card.component';

interface StatusSection {
  status: ReadingStatus;
  label: string;
  icon: string;
  entries: ReadingListEntry[];
}

@Component({
  selector: 'app-reading-list',
  standalone: true,
  imports: [RouterLink, BookCardComponent],
  templateUrl: './reading-list.component.html',
  styleUrl: './reading-list.component.css',
})
export class ReadingListComponent implements OnInit {
  loading = signal(true);
  sections = signal<StatusSection[]>([]);
  isEmpty = signal(false);

  constructor(private readonly readingListService: ReadingListService) {}

  ngOnInit(): void {
    this.readingListService.mine().subscribe({
      next: (entries) => {
        const statuses: { status: ReadingStatus; label: string; icon: string }[] = [
          { status: 'CURRENTLY_READING', label: 'Currently Reading', icon: '📖' },
          { status: 'WANT_TO_READ', label: 'Want to Read', icon: '🔖' },
          { status: 'READ', label: 'Read', icon: '✅' },
        ];
        this.sections.set(
          statuses.map((section) => ({
            ...section,
            entries: entries.filter((e) => e.status === section.status),
          })),
        );
        this.isEmpty.set(entries.length === 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
