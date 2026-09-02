import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ReadingListEntry, ReadingStatus, ReadingStatusInfo } from './models';

@Injectable({ providedIn: 'root' })
export class ReadingListService {
  constructor(private readonly http: HttpClient) {}

  getStatus(bookId: number): Observable<ReadingStatusInfo> {
    return this.http.get<ReadingStatusInfo>(`/api/books/${bookId}/reading-status`);
  }

  setStatus(bookId: number, status: ReadingStatus): Observable<ReadingStatusInfo> {
    return this.http.put<ReadingStatusInfo>(`/api/books/${bookId}/reading-status`, { status });
  }

  remove(bookId: number): Observable<void> {
    return this.http.delete<void>(`/api/books/${bookId}/reading-status`);
  }

  mine(): Observable<ReadingListEntry[]> {
    return this.http.get<ReadingListEntry[]>('/api/my/reading-list');
  }
}
