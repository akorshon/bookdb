# bookdb - frontend

Angular 20 (standalone components) + Bootstrap 5 SPA for the book platform. Talks to the
Spring Boot API in `../backend` (see its README for setup/auth details).

## Development server

Start the backend first (`docker compose up -d` and `mvn spring-boot:run` in `../backend`),
then:

```bash
npm install
npm start
```

Open http://localhost:4200. `proxy.conf.json` forwards `/api/**` requests to
`http://localhost:8080`, so no CORS setup is needed in dev.

## Structure

- `src/app/core/` - `AuthService` (JWT in localStorage, exposed as Angular signals), the
  `authInterceptor` (attaches the bearer token, redirects to `/login` on 401), route
  guards (`authGuard`, `adminGuard`), and one HTTP service per API resource
  (books/authors/reviews/admin users).
- `src/app/shared/` - navbar, book card, star rating - reused across pages.
- `src/app/features/` - one folder per area: `catalog` (browse/search + book detail with
  reviews), `auth` (login/register), `my-books` (a user's own submissions + the
  create/edit form with cover upload), `admin` (pending-books queue, user management),
  `authors` (author bio + their published books).

Routes are wired in `app.routes.ts` with lazy `loadComponent` per page.

## Building

```bash
npm run build
```

Production build output goes to `dist/frontend`.
