# bookdb - backend

Spring Boot REST API for a small IMDb-style book platform: browse a catalog, register/log
in, submit books that start as drafts, and have an admin review/approve them. Includes
ratings/reviews and admin user management. The catalog is grown by importing JSON exports
(title, author, thumbnail, author biography, book summary, categories) produced by the
standalone [`bookdbscraper`](../../bookdbscraper) project (rokomari.com, boitoi.com.bd) -
see "Scrape import" below. This backend has no scraping code of its own.

The Angular frontend lives in `../frontend`; see its README for running the UI.

## Setup

1. Start MySQL (+ Adminer for inspecting data at http://localhost:8081 — `System: MySQL`,
   `Server: mysql`, `Username: bookdb`, `Password: bookdb`, `Database: bangla_books`):
   ```bash
   docker compose up -d
   ```
2. Start the API:
   ```bash
   mvn spring-boot:run
   ```
   On first run this seeds an admin account from `app.admin.email` / `app.admin.password`
   in `application.properties` (dev-only defaults — **change them** before running this
   anywhere but a local machine) and serves the API at http://localhost:8080/api.

3. (Optional) Grow the catalog by importing a scrape export — see "Scrape import" below.

## Auth

Stateless JWT auth (`Authorization: Bearer <token>`), issued by `POST /api/auth/login` /
`POST /api/auth/register`. Roles are `USER` and `ADMIN`. Key endpoints:

| Endpoint | Access |
|---|---|
| `GET /api/books`, `GET /api/books/{id}`, `GET /api/authors/**` | public (published only) |
| `POST /api/books`, `PUT /api/books/{id}`, `POST /api/books/{id}/submit` | owner |
| `POST /api/books/{id}/reviews`, `DELETE /api/reviews/{id}` | authenticated |
| `GET/POST /api/admin/books/**`, `GET/POST /api/admin/users/**` | `ADMIN` |

A book's lifecycle: `DRAFT` → (owner submits) → `PENDING_REVIEW` → (admin approves/rejects)
→ `PUBLISHED` or `REJECTED` (editable and resubmittable from `REJECTED`).

## Scrape import

Scraping itself (Selenium, source-site-specific parsing) lives entirely in the separate
[`bookdbscraper`](../../bookdbscraper) project — run it there to produce a timestamped
JSON file (`scrape-output/<site>-<timestamp>.json`), then bring that file's books/authors
into this catalog via the admin-only endpoint:

```bash
curl -H "Authorization: Bearer <admin JWT>" -F file=@boitoi-*.json \
  http://localhost:8080/api/admin/scrape-imports
```

or through the frontend's "Import Scrape" admin page. `ScrapeImportService` upserts
authors by slug and books by `detailUrl` (find-or-update, same as the old direct scraper),
resolves/creates `categories` by name, sets `status=PUBLISHED`, and stamps `source` (e.g.
`"rokomari.com"`, `"boitoi.com.bd"`) and `scrapedAt` on each row. Safe to re-run — a second
import of the same file just updates the same rows. Per-row failures are collected into
the response instead of aborting the whole import.

## Bengali text and search

Bengali text can encode the same visible character in more than one way — e.g. "য়" as
either a single precomposed codepoint or as a base letter plus a separate nukta mark.
Both look identical but are different bytes, which silently breaks substring search
unless everything is normalized consistently. `TextNormalizer` (`src/main/java/com/bookdb/util/`)
normalizes both scraped/submitted text and incoming search queries to Unicode NFD for
this reason (NFD, not the more common NFC, because Bengali nukta letters are on Unicode's
composition exclusion list — see the class Javadoc). This has no visible effect on the
pages. If you ever change the normalization form, re-normalize what's already in the
database with:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--normalize-existing
```

## Known data quirks

- Not every scraped book has "flap text" on rokomari.com, so `summary` is occasionally
  empty — that's the source page, not a scraping bug.
- A handful of rokomari.com author IDs point at stale/placeholder records on their end
  (e.g. author 975 currently renders as "Test-HA" instead of Humayun Azad's real page).
  Nothing to fix on this side; it reflects what's live on the site.
