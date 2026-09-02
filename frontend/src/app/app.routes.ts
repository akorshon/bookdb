import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/catalog/book-list/book-list.component').then((m) => m.BookListComponent),
  },
  {
    path: 'books/:id',
    loadComponent: () =>
      import('./features/catalog/book-detail/book-detail.component').then((m) => m.BookDetailComponent),
  },
  {
    path: 'authors',
    loadComponent: () =>
      import('./features/authors/author-list/author-list.component').then((m) => m.AuthorListComponent),
  },
  {
    path: 'authors/:id',
    loadComponent: () =>
      import('./features/authors/author-detail/author-detail.component').then((m) => m.AuthorDetailComponent),
  },
  {
    path: 'developers',
    loadComponent: () =>
      import('./features/api-docs/api-docs.component').then((m) => m.ApiDocsComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'my-books',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-books/my-books-list/my-books-list.component').then((m) => m.MyBooksListComponent),
  },
  {
    path: 'my-books/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-books/book-form/book-form.component').then((m) => m.BookFormComponent),
  },
  {
    path: 'my-books/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-books/book-form/book-form.component').then((m) => m.BookFormComponent),
  },
  {
    path: 'my-reading-list',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reading-list/reading-list.component').then((m) => m.ReadingListComponent),
  },
  {
    path: 'my-authors',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-authors/my-authors-list/my-authors-list.component').then(
        (m) => m.MyAuthorsListComponent,
      ),
  },
  {
    path: 'my-authors/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-authors/author-form/author-form.component').then((m) => m.AuthorFormComponent),
  },
  {
    path: 'my-authors/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-authors/author-form/author-form.component').then((m) => m.AuthorFormComponent),
  },
  {
    path: 'admin/pending',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/pending-books/pending-books.component').then((m) => m.PendingBooksComponent),
  },
  {
    path: 'admin/pending-authors',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/pending-authors/pending-authors.component').then(
        (m) => m.PendingAuthorsComponent,
      ),
  },
  {
    path: 'admin/users',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/user-management/user-management.component').then(
        (m) => m.UserManagementComponent,
      ),
  },
  {
    path: 'admin/scrape-import',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/scrape-import/scrape-import.component').then(
        (m) => m.ScrapeImportComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
