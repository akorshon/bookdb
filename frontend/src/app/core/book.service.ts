import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Book, BookFormValue, BookStatus, CategorySection, PageResponse } from './models';

@Injectable({ providedIn: 'root' })
export class BookService {
  constructor(private readonly http: HttpClient) {}

  list(query: string, page: number, size = 20): Observable<PageResponse<Book>> {
    let url = `/api/books?page=${page}&size=${size}`;
    if (query.trim()) {
      url += `&query=${encodeURIComponent(query.trim())}`;
    }
    return this.http.get<PageResponse<Book>>(url);
  }

  /** Browse-page data: published books grouped into category sections, each capped to `size` books. */
  sections(size = 20): Observable<CategorySection[]> {
    return this.http.get<CategorySection[]>(`/api/books/sections?size=${size}`);
  }

  get(id: number): Observable<Book> {
    return this.http.get<Book>(`/api/books/${id}`);
  }

  mine(): Observable<Book[]> {
    return this.http.get<Book[]>('/api/my/books');
  }

  create(value: BookFormValue): Observable<Book> {
    return this.http.post<Book>('/api/books', value);
  }

  update(id: number, value: BookFormValue): Observable<Book> {
    return this.http.put<Book>(`/api/books/${id}`, value);
  }

  submit(id: number): Observable<Book> {
    return this.http.post<Book>(`/api/books/${id}/submit`, {});
  }

  uploadCover(id: number, file: File): Observable<Book> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Book>(`/api/books/${id}/cover`, formData);
  }

  adminList(status: BookStatus, page: number, size = 20): Observable<PageResponse<Book>> {
    return this.http.get<PageResponse<Book>>(`/api/admin/books?status=${status}&page=${page}&size=${size}`);
  }

  approve(id: number): Observable<Book> {
    return this.http.post<Book>(`/api/admin/books/${id}/approve`, {});
  }

  reject(id: number, reason: string): Observable<Book> {
    return this.http.post<Book>(`/api/admin/books/${id}/reject`, { reason });
  }

  adminDelete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/books/${id}`);
  }
}
