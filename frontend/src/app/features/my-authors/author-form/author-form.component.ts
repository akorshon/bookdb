import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthorService } from '../../../core/author.service';
import { AuthorFormValue, AuthorResponse } from '../../../core/models';

@Component({
  selector: 'app-author-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './author-form.component.html',
  styleUrl: './author-form.component.css',
})
export class AuthorFormComponent implements OnInit {
  authorId: number | null = null;
  author = signal<AuthorResponse | null>(null);
  form: AuthorFormValue = { name: '', nameNative: '', bio: '' };
  saving = signal(false);
  error = signal<string | null>(null);
  uploadingPhoto = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authorService: AuthorService,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.authorId = Number(idParam);
      this.authorService.get(this.authorId).subscribe((a) => {
        this.author.set(a);
        this.form = {
          name: a.name,
          nameNative: a.nameNative || '',
          bio: a.bio || '',
        };
      });
    }
  }

  save(): void {
    this.error.set(null);
    this.saving.set(true);
    const req = this.authorId
      ? this.authorService.update(this.authorId, this.form)
      : this.authorService.create(this.form);
    req.subscribe({
      next: (a) => {
        this.saving.set(false);
        if (!this.authorId) {
          this.router.navigate(['/my-authors', a.id, 'edit']);
        } else {
          this.author.set(a);
        }
      },
      error: () => {
        this.saving.set(false);
        this.error.set('Could not save. Check the required fields.');
      },
    });
  }

  onPhotoSelected(event: Event): void {
    if (!this.authorId) {
      return;
    }
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploadingPhoto.set(true);
    this.authorService.uploadPhoto(this.authorId, file).subscribe({
      next: (a) => {
        this.author.set(a);
        this.uploadingPhoto.set(false);
      },
      error: () => this.uploadingPhoto.set(false),
    });
  }
}
