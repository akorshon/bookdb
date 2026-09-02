import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

type ParamLocation = 'path' | 'query';
type ParamType = 'string' | 'number';

interface ApiParam {
  name: string;
  in: ParamLocation;
  type: ParamType;
  required: boolean;
  default: string;
  description: string;
}

interface ApiEndpoint {
  id: string;
  method: 'GET' | 'POST';
  group: string;
  path: string;
  summary: string;
  description: string;
  params: ApiParam[];
  tryable: boolean;
  bodyExample?: string;
  responseNote?: string;
}

const ENDPOINTS: ApiEndpoint[] = [
  {
    id: 'books-list',
    method: 'GET',
    group: 'Books',
    path: '/api/books',
    summary: 'Browse or search published books',
    description:
      'Returns a page of published books. Omit query to browse everything; search matches title, ' +
      'a Latin phonetic key (so "tomake" matches "তোমাকে"), and author name in either script.',
    params: [
      { name: 'query', in: 'query', type: 'string', required: false, default: '', description: 'Search text (title or author, any supported script)' },
      { name: 'page', in: 'query', type: 'number', required: false, default: '0', description: 'Zero-based page number' },
      { name: 'size', in: 'query', type: 'number', required: false, default: '20', description: 'Page size' },
    ],
    tryable: true,
  },
  {
    id: 'books-get',
    method: 'GET',
    group: 'Books',
    path: '/api/books/{id}',
    summary: 'Get one book',
    description: 'Full detail for a single published book, including authors, categories, and rating.',
    params: [{ name: 'id', in: 'path', type: 'number', required: true, default: '1', description: 'Book id' }],
    tryable: true,
  },
  {
    id: 'books-reviews',
    method: 'GET',
    group: 'Books',
    path: '/api/books/{id}/reviews',
    summary: "List a book's reviews",
    description: 'All reviews for a published book, newest first.',
    params: [{ name: 'id', in: 'path', type: 'number', required: true, default: '1', description: 'Book id' }],
    tryable: true,
  },
  {
    id: 'authors-list',
    method: 'GET',
    group: 'Authors',
    path: '/api/authors',
    summary: 'List published authors',
    description: 'Every publicly visible author - not yet-approved submissions are excluded.',
    params: [],
    tryable: true,
  },
  {
    id: 'authors-get',
    method: 'GET',
    group: 'Authors',
    path: '/api/authors/{id}',
    summary: 'Get one author',
    description: 'Full profile for a single published author.',
    params: [{ name: 'id', in: 'path', type: 'number', required: true, default: '1', description: 'Author id' }],
    tryable: true,
  },
  {
    id: 'authors-books',
    method: 'GET',
    group: 'Authors',
    path: '/api/authors/{id}/books',
    summary: "List an author's books",
    description: 'All published books credited to this author.',
    params: [{ name: 'id', in: 'path', type: 'number', required: true, default: '1', description: 'Author id' }],
    tryable: true,
  },
];

const GROUP_ORDER = ['Books', 'Authors'];

interface TryResponse {
  status: number;
  ok: boolean;
  elapsedMs: number;
  body: string;
}

@Component({
  selector: 'app-api-docs',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './api-docs.component.html',
  styleUrl: './api-docs.component.css',
})
export class ApiDocsComponent {
  readonly groups = GROUP_ORDER.map((name) => ({
    name,
    endpoints: ENDPOINTS.filter((e) => e.group === name),
  }));

  selected = signal<ApiEndpoint>(ENDPOINTS[0]);
  paramValues: Record<string, string> = {};
  sampleTab = signal<'curl' | 'fetch'>('curl');
  sending = signal(false);
  response = signal<TryResponse | null>(null);
  copied = signal<string | null>(null);

  constructor(private readonly http: HttpClient) {
    this.resetParams();
  }

  select(endpoint: ApiEndpoint): void {
    this.selected.set(endpoint);
    this.response.set(null);
    this.resetParams();
  }

  send(): void {
    const endpoint = this.selected();
    if (!endpoint.tryable || this.sending()) {
      return;
    }
    this.sending.set(true);
    this.response.set(null);
    const start = performance.now();
    this.http
      .request(endpoint.method, this.path(), { params: this.queryParams(), observe: 'response' })
      .subscribe({
        next: (res) => {
          this.response.set({
            status: res.status,
            ok: true,
            elapsedMs: Math.round(performance.now() - start),
            body: JSON.stringify(res.body, null, 2),
          });
          this.sending.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.response.set({
            status: err.status,
            ok: false,
            elapsedMs: Math.round(performance.now() - start),
            body: JSON.stringify(err.error ?? { error: err.message }, null, 2),
          });
          this.sending.set(false);
        },
      });
  }

  curlSample(): string {
    const endpoint = this.selected();
    const url = this.fullUrl();
    if (endpoint.method === 'GET') {
      return `curl -s "${url}"`;
    }
    const body = endpoint.bodyExample ? ` \\\n  -H "Content-Type: application/json" \\\n  -d '${endpoint.bodyExample.replace(/\n\s*/g, ' ')}'` : '';
    return `curl -s -X ${endpoint.method} "${url}"${body}`;
  }

  fetchSample(): string {
    const endpoint = this.selected();
    const url = this.fullUrl();
    if (endpoint.method === 'GET') {
      return `const res = await fetch("${url}");\nconst data = await res.json();`;
    }
    const body = endpoint.bodyExample ? `,\n  headers: { "Content-Type": "application/json" },\n  body: JSON.stringify(${endpoint.bodyExample})` : '';
    return `const res = await fetch("${url}", {\n  method: "${endpoint.method}"${body}\n});\nconst data = await res.json();`;
  }

  copy(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.copied.set(label);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }

  onParamChange(name: string, value: string): void {
    this.paramValues[name] = value;
  }

  statusClass(status: number): string {
    if (status >= 200 && status < 300) {
      return 'response-status-ok';
    }
    if (status >= 400 && status < 500) {
      return 'response-status-client-error';
    }
    return 'response-status-error';
  }

  private resetParams(): void {
    const values: Record<string, string> = {};
    for (const p of this.selected().params) {
      values[p.name] = p.default;
    }
    this.paramValues = values;
  }

  private path(): string {
    let path = this.selected().path;
    for (const p of this.selected().params.filter((p) => p.in === 'path')) {
      path = path.replace(`{${p.name}}`, encodeURIComponent(this.paramValues[p.name] || ''));
    }
    return path;
  }

  private queryParams(): HttpParams {
    let params = new HttpParams();
    for (const p of this.selected().params.filter((p) => p.in === 'query')) {
      const value = this.paramValues[p.name];
      if (value !== undefined && value !== '') {
        params = params.set(p.name, value);
      }
    }
    return params;
  }

  private fullUrl(): string {
    const query = this.queryParams().toString();
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    return origin + this.path() + (query ? `?${query}` : '');
  }
}
