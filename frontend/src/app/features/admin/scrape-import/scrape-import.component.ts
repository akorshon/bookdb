import { Component, signal } from '@angular/core';
import { AdminService } from '../../../core/admin.service';
import { ScrapeImportSummary } from '../../../core/models';

@Component({
  selector: 'app-scrape-import',
  standalone: true,
  imports: [],
  templateUrl: './scrape-import.component.html',
})
export class ScrapeImportComponent {
  loading = signal(false);
  summary = signal<ScrapeImportSummary | null>(null);
  error = signal<string | null>(null);
  selectedFile: File | null = null;

  constructor(private readonly adminService: AdminService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.summary.set(null);
    this.error.set(null);
  }

  import(): void {
    if (!this.selectedFile) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.adminService.importScrape(this.selectedFile).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Import failed.');
        this.loading.set(false);
      },
    });
  }
}
