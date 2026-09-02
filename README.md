# bookdb

An IMDb-style platform for Bangla books: browse a catalog, register/log in, submit books
that start as drafts, and have an admin review/approve them, plus ratings and reviews.
Grown by importing JSON scrape exports (rokomari.com, boitoi.com.bd) through the admin panel.

Split into two projects, plus a separate scraper:

- [`backend/`](backend/README.md) - Spring Boot REST API, MySQL, JWT auth, admin scrape import.
- [`frontend/`](frontend/README.md) - Angular 20 + Bootstrap single-page app.
- [`../bookdbscraper`](../bookdbscraper) - standalone Selenium-based scraper (separate
  project, run ad hoc on a dev machine); writes JSON files for the admin import above.

Start the backend first, then the frontend — each README has the exact steps.
