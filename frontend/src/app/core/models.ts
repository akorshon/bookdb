export interface AuthorSummary {
  id: number;
  name: string;
  nameNative: string | null;
  slug: string | null;
  imageUrl: string | null;
}

export type AuthorStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED';

/** Superset of AuthorSummary returned by the submission/moderation-aware endpoints. */
export interface AuthorResponse {
  id: number;
  name: string;
  nameNative: string | null;
  slug: string | null;
  imageUrl: string | null;
  bio: string | null;
  /** Which site this row was imported from (e.g. "rokomari.com", "boitoi.com.bd"); null for user-submitted authors. */
  source: string | null;
  status: AuthorStatus;
  submittedByUserId: number | null;
  submittedByDisplayName: string | null;
  rejectionReason: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AuthorFormValue {
  name: string;
  nameNative: string;
  bio: string;
}

export type BookStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED';

export interface CategorySummary {
  id: number;
  name: string;
}

export interface Book {
  id: number;
  title: string;
  /** Null on the public response shape (GET /api/books, /api/books/{id}); present for the owner/admin. */
  edition: string | null;
  thumbnailUrl: string | null;
  summary: string | null;
  detailUrl: string | null;
  /** Which site this row was imported from (e.g. "rokomari.com", "boitoi.com.bd"); null on the
   * public response shape and for user-submitted books; present for the owner/admin. */
  source: string | null;
  /** Null on the public response shape; present for the owner/admin (see `edition`). */
  status: BookStatus | null;
  authors: AuthorSummary[];
  categories: CategorySummary[];
  averageRating: number | null;
  ratingCount: number;
  submittedByUserId: number | null;
  submittedByDisplayName: string | null;
  rejectionReason: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface BookFormValue {
  title: string;
  authorNames: string[];
  summary: string;
  categories: string[];
}

export interface Review {
  id: number;
  bookId: number;
  userId: number;
  userDisplayName: string;
  rating: number;
  reviewText: string | null;
  createdAt: string;
  updatedAt: string;
}

export type Role = 'USER' | 'ADMIN';

export interface UserSummary {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
}

export type ReadingStatus = 'WANT_TO_READ' | 'CURRENTLY_READING' | 'READ';

export interface ReadingStatusInfo {
  status: ReadingStatus | null;
  updatedAt: string | null;
}

export interface ReadingListEntry {
  id: number;
  book: Book;
  status: ReadingStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ScrapeImportSummary {
  authorsCreated: number;
  authorsUpdated: number;
  booksCreated: number;
  booksUpdated: number;
  errors: string[];
}
