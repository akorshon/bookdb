import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategoryService } from '../../../core/category.service';
import { CategorySummary } from '../../../core/models';

@Component({
  selector: 'app-category-management',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  categories = signal<CategorySummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  newName = '';
  adding = signal(false);

  editingId: number | null = null;
  editName = '';
  saving = signal(false);

  constructor(private readonly categoryService: CategoryService) {}

  ngOnInit(): void {
    this.load();
  }

  add(): void {
    const name = this.newName.trim();
    if (!name) {
      return;
    }
    this.error.set(null);
    this.adding.set(true);
    this.categoryService.create(name).subscribe({
      next: () => {
        this.newName = '';
        this.adding.set(false);
        this.load();
      },
      error: (err) => {
        this.adding.set(false);
        this.error.set(err.error?.message || 'Could not add category.');
      },
    });
  }

  startEdit(category: CategorySummary): void {
    this.editingId = category.id;
    this.editName = category.name;
    this.error.set(null);
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editName = '';
  }

  saveEdit(category: CategorySummary): void {
    const name = this.editName.trim();
    if (!name) {
      return;
    }
    this.error.set(null);
    this.saving.set(true);
    this.categoryService.update(category.id, name).subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message || 'Could not update category.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.categoryService.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
