import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ScrapeImportSummary, UserSummary } from './models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private readonly http: HttpClient) {}

  listUsers(page: number, size = 20): Observable<PageResponse<UserSummary>> {
    return this.http.get<PageResponse<UserSummary>>(`/api/admin/users?page=${page}&size=${size}`);
  }

  disable(id: number): Observable<UserSummary> {
    return this.http.post<UserSummary>(`/api/admin/users/${id}/disable`, {});
  }

  enable(id: number): Observable<UserSummary> {
    return this.http.post<UserSummary>(`/api/admin/users/${id}/enable`, {});
  }

  promote(id: number): Observable<UserSummary> {
    return this.http.post<UserSummary>(`/api/admin/users/${id}/promote`, {});
  }

  importScrape(file: File): Observable<ScrapeImportSummary> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ScrapeImportSummary>('/api/admin/scrape-imports', formData);
  }
}
