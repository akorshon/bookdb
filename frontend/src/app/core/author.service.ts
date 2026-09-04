import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthorFormValue, AuthorResponse, AuthorStatus, Book } from './models';

@Injectable({ providedIn: 'root' })
export class AuthorService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<AuthorResponse[]> {
    return this.http.get<AuthorResponse[]>('/api/authors');
  }

  get(id: number): Observable<AuthorResponse> {
    return this.http.get<AuthorResponse>(`/api/authors/${id}`);
  }

  books(id: number): Observable<Book[]> {
    return this.http.get<Book[]>(`/api/authors/${id}/books`);
  }

  mine(): Observable<AuthorResponse[]> {
    return this.http.get<AuthorResponse[]>('/api/my/authors');
  }

  create(value: AuthorFormValue): Observable<AuthorResponse> {
    return this.http.post<AuthorResponse>('/api/authors', value);
  }

  update(id: number, value: AuthorFormValue): Observable<AuthorResponse> {
    return this.http.put<AuthorResponse>(`/api/authors/${id}`, value);
  }

  submit(id: number): Observable<AuthorResponse> {
    return this.http.post<AuthorResponse>(`/api/authors/${id}/submit`, {});
  }

  uploadPhoto(id: number, file: File): Observable<AuthorResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AuthorResponse>(`/api/authors/${id}/photo`, formData);
  }

  adminList(status: AuthorStatus): Observable<AuthorResponse[]> {
    return this.http.get<AuthorResponse[]>(`/api/admin/authors?status=${status}`);
  }

  adminApprove(id: number): Observable<AuthorResponse> {
    return this.http.post<AuthorResponse>(`/api/admin/authors/${id}/approve`, {});
  }

  adminReject(id: number, reason: string): Observable<AuthorResponse> {
    return this.http.post<AuthorResponse>(`/api/admin/authors/${id}/reject`, { reason });
  }

  adminDelete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/authors/${id}`);
  }
}
