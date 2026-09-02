import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CategorySummary } from './models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>('/api/categories');
  }
}
