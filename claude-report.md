# e-Journey ("Let's Travel"): Requirements Coverage Report

**Date:** 2026-11-21
**Basis:** `subject.md` and `audit.md`, checked against the code on `main` (commit `29089c4`).
**Method:** Static analysis only. Nothing was built, run or tested for this report. Every "Done" below means "implemented in code". Whether it actually works is for the runtime test report that comes next.
**Bonus features** (PWA, multilingual, innovative feature) are not required and are only noted briefly at the end.

Legend: ✅ Done · ⚠️ Partial / has gaps · ❌ Missing

---

## 1. Executive summary

The project covers most of the functional subject. There are five Spring Boot services (user, travel, booking, payment, gateway) plus an Angular 20 client. The search stack uses PostgreSQL, Elasticsearch, Neo4j and Kafka, with Redis for rate limiting and token revocation. Admin, Travel Manager and Traveler each have their own dashboard. Elasticsearch search with autocomplete, Neo4j recommendations, the 3-day cutoff, Stripe and PayPal checkout, feedback, reports and statistics are all present.

The main weak points:

1. **Admin "can do everything" is not true.** The backend and frontend block the Admin from the manager and traveler features.
2. **A Travel Manager can book a trip but cannot pay for it.** The payment page is limited to travelers.
3. **The traveler cannot choose a payment method.** The provider is fixed per travel by the manager.
4. **No fallback or resilience.** If Elasticsearch, Neo4j or a downstream service fails, the request fails.
5. **Testing is thin.** About 41 tests in total, no payment tests, no real integration tests for search, recommendations or booking, and no end-to-end tests.
6. **GDPR / compliance is mostly missing.** There is no data export, no self-service account deletion, and no consent or privacy notice.
7. **Paid bookings cannot be cancelled** (there is no refund flow). The 3-day cutoff only applies to unpaid bookings.

| Area (evaluation axis) | Status |
|---|---|
| 🧩 Role implementation | ⚠️ Mostly done; Admin/Manager inheritance gaps |
| 🔍 Search & recommendations | ✅ Implemented (with consistency and fallback caveats) |
| 💳 Transactions | ⚠️ Two providers, secure flow; no traveler choice, no refunds, no tests |
| 🎨 UI/UX | ✅ Likely good (Tailwind, responsive breakpoints); to verify at runtime |
| 🧪 Testing + CI/CD | ⚠️ CI pipeline solid; test coverage weak, no E2E |
| 🔐 Security | ⚠️ Good basics (BCrypt, JWT, RBAC, TLS on k3s); compliance and secret hygiene gaps |

---

## 2. Role features (subject → code)

### 2.1 Admin

| Requirement | Status | Evidence / notes |
|---|---|---|
| Top-ranking managers and travels, with income reports for recent months | ✅ | `PaymentService.getAnalytics(months)` returns monthly income, top travels and income per manager (`GET /api/v1/admin/payments/analytics`). The admin dashboard shows a "Captured income" chart and "By captured income" for travels. |
| Number of organized travels | ✅ | Shown in the manager ranking (`admin.component.ts` `managerRankings`, from `/admin/travels/analytics`). |
| Detailed travel history list | ✅ | `GET /api/v1/bookings/admin/history` plus payment history; "Detailed travel history" section. |
| Feedbacks to assess satisfaction | ✅ | `GET /api/v1/bookings/admin/feedback`; "Feedback & satisfaction" section. |
| Ranked list of managers by performance score | ⚠️ | The score is computed **client-side**: rating 50%, income 30%, bookings 10%, travels 10%. It is built from `this.users`, which is **paginated 5 at a time** (`UserService.ADMIN_PAGE_SIZE = 5`, infinite scroll). Managers who are not loaded yet are missing from the ranking. The ranking should be computed on the backend over all managers. |
| Review reports filed by travelers | ✅ | `GET /admin/reports`, `PATCH /admin/reports/{id}` (OPEN/CLOSED). |
| Perform **all** Travel Manager and Traveler actions | ❌ | Backend: every manager booking endpoint is `hasRole('TRAVEL_MANAGER')` only (`BookingController` lines 98–125), and so are `/manager/payments/analytics` and `/users/manager/bookings/{id}/traveler-profile`. Frontend: `traveler-dashboard` allows `['TRAVELER','TRAVEL_MANAGER']`, and `payment/:id` and `managers/:id` allow `TRAVELER` only, so the Admin is redirected to 403. The Admin *can* manage every travel through `/admin/travels`. |

### 2.2 Travel Manager

| Requirement | Status | Evidence / notes |
|---|---|---|
| Create and manage own travel offerings | ✅ | `travelController` (`/api/v1/admin/travels`) checks ownership on read, update and delete, with strong validation in `TravelRequest`. |
| View feedback for own travels | ✅ | `GET /bookings/manager/feedback`. |
| Dashboard: income, number of trips, number of travelers | ✅ | Manager workspace shows TRIPS ORGANIZED, TRAVELERS, TRAVELER RATING and monthly completed income (`/manager/payments/analytics`). |
| Manage subscriber lists per travel (view profile, unsubscribe) | ⚠️ | Both are implemented: `GET /users/manager/bookings/{id}/traveler-profile` and `DELETE /bookings/manager/bookings/{id}`. **Gap:** unsubscribe only works for `PENDING_PAYMENT` bookings, so paid subscribers cannot be removed. The traveler profile only shows name, email, role and state, not history. |
| Access all Traveler functionalities | ⚠️ | The traveler dashboard is open to managers, and they can create bookings. **But `/payment/:bookingId` uses `travelerGuard` (TRAVELER only)**, so a manager who books gets redirected to 403 and cannot pay. `/managers/:id` (manager page) is also blocked for managers. |
| Analytics and feedback to improve planning | ✅ | Monthly income chart, per-journey feedback and ratings. |

### 2.3 Traveler

| Requirement | Status | Evidence / notes |
|---|---|---|
| Elasticsearch search with autocomplete | ✅ | `TravelSearchService`: `search_as_you_type` fields plus `multi_match` `bool_prefix` over title, city, country, destinations and description, with boosting. Frontend uses `debounceTime(250)` and shows the top 5 suggestions. Search needs authentication at the gateway. |
| Personalized recommendations via Neo4j, ≥3 travel fields | ✅ | `BookingGraphService.RECOMMEND_TRAVELS` scores candidates on **city, country, duration and paymentMethod** (4 fields), weighted by past booking **rating**, using only CONFIRMED participations. **Caveat:** there is no cold-start fallback, so a new user with no confirmed booking gets an empty list. |
| Subscribe / unsubscribe with 3-day cutoff | ⚠️ | Booking is refused when departure is ≤ 3 days away, and cancellation is refused within 3 days (`BookingService`). **Gap:** only `PENDING_PAYMENT` bookings can be cancelled ("paid bookings require a refund flow"). A paid subscription can never be cancelled, even 30 days before departure. There are no duplicate-booking or capacity checks. |
| Payments through multiple methods | ⚠️ | Stripe Checkout and PayPal Orders v2 are both implemented, with server-side amount and currency verification. **Gap:** the provider is **fixed per travel** (`TravelRequest.paymentMethod` is `Stripe` or `PayPal`, chosen by the manager). The traveler never chooses. There are no webhooks: confirmation depends on the user returning to the client. |
| Feedback for participated travels | ✅ | Allowed only for CONFIRMED bookings after the departure date, once per booking. |
| Travel Manager page (stats, past ratings, report count) | ✅ | `manager-profile` page: bookings, feedback count, average rating, community reports, organized travels. The seeded sample travels have **no manager** (`createdByUserId = null`), so their manager page is unavailable. |
| Report managers or travelers | ⚠️ | `POST /bookings/reports` with target TRAVEL, MANAGER or TRAVELER. **Gaps:** the target is never checked for existence, and a traveler is reported by typing a raw **account ID**, which is poor UX. |
| Personal statistics (past trips, reports, cancellations, preferred payment) | ✅ | `GET /bookings/me/statistics`, shown as 4 stat cards on the traveler dashboard. |

---

## 3. Cross-cutting requirements

### 3.1 Responsive and intuitive UI

| Requirement | Status | Notes |
|---|---|---|
| Responsive, works on all devices | ✅ (to verify) | Tailwind v4 with `sm:`/`md:`/`lg:` breakpoints throughout (~130 uses). |
| Accessible | ⚠️ | Some `aria-*`/`role` attributes (~23), labelled dialogs, `role="alert"` on errors. There is no systematic a11y pass. |
| Smooth navigation from search to booking | ✅ (to verify) | Search, travel details modal, Book, then `/payment/:id`. This is broken for managers (see §2.2). |

### 3.2 Testing and QA

| Requirement | Status | Notes |
|---|---|---|
| Unit tests for every feature | ⚠️ | Backend has 30 `@Test`: gateway 9, user 11, travel 7, booking 3, **payment 0**. Frontend has 16 `it()` specs, all for services and interceptors; **no component specs** for the dashboards, payment or search. |
| Integration tests | ⚠️ | `@SpringBootTest` with H2 for user and travel. Elasticsearch, Neo4j and Kafka are mocked. Nothing covers search relevance, recommendations, the outbox pipeline or payments. |
| End-to-end tests | ❌ | No Cypress or Playwright setup. |
| CI pipeline | ✅ | `Jenkinsfile`: repository validation, parallel frontend and backend tests (all 5 services), image builds, SonarQube quality gate, then deployment to k3s through Ansible. |
| Regression testing after updates | ⚠️ | Happens implicitly through CI on every build, but the suite is too small to count as meaningful regression coverage. |

### 3.3 Security and compliance

| Requirement | Status | Notes |
|---|---|---|
| Passwords encrypted | ✅ | `BCryptPasswordEncoder`. The password policy is weak (min 6 characters, no complexity rule). |
| Authentication & authorization | ✅ | HMAC JWT validated at the gateway **and** again in each service (defense in depth). Roles are enforced with `@PreAuthorize` and path matchers. Token revocation and blacklisting use Redis. Rate limiting uses Redis. |
| RBAC correctly enforced | ⚠️ | Unauthorized access is blocked correctly. The issue is the opposite one: the Admin and Manager are *over-restricted* (§2). |
| SSL/TLS | ⚠️ | k3s: Traefik Ingress with TLS (`travel-plan-local-tls`, created by `roles/k3s/tasks/tls.yml`, `scripts/https.sh` for local certs on :8443). Local Compose and dev use plain HTTP (`environment.apiUrl = http://localhost:8089`). Internal service-to-service traffic is unencrypted. There is no HSTS or CSP header. |
| SQL injection protection | ✅ | Spring Data JPA derived queries and JPQL with `@Param`. Cypher uses parameter maps. No string-concatenated queries were found. |
| XSS protection | ✅ | Angular template escaping, no `innerHTML` or `bypassSecurityTrust*`. Strict regex validation on travel fields. No CSP header. |
| Sensitive data handling (tokens, payment info) | ⚠️ | Card data never reaches the backend (hosted Stripe Checkout and PayPal approval), which is good for PCI. The payment→booking confirmation uses a shared secret compared in constant time. **But:** `infrastructure/ansible/files/travel-plan-secrets.yaml` is **committed** with dev passwords and a dev JWT secret. Every service's `application.yaml` has a hard-coded fallback `JWT_SECRET` (`defaultsecret…`). The default admin (`admin@admin.com` / `Admin123456@@`) is seeded and documented. The JWT is stored in `localStorage`. |
| Data protection laws (GDPR) | ❌ | There is no user data export, no self-service account deletion or right to be forgotten, no consent or privacy-policy screen, and no data retention policy. Only an admin can delete a user, and the user's bookings, payments and Neo4j nodes are not cleaned up. |

### 3.4 Constraints

| Constraint | Status |
|---|---|
| Elasticsearch for search | ✅ |
| Neo4j for recommendations | ✅ |
| Multiple payment providers (Stripe, PayPal) | ✅ for the platform / ⚠️ for traveler choice |
| Strong RBAC | ⚠️ (see §2) |
| Fully responsive frontend | ✅ (to verify) |
| Secure, tested, compliant deployment | ⚠️ (tests and GDPR) |

---

## 4. Audit checklist (`audit.md`)

### 4.1 Comprehension questions: what the code lets you explain

| Question | Material in the code |
|---|---|
| How Elasticsearch powers search and autocomplete | Index `travelplan-travels`, `search_as_you_type` sub-fields (`_2gram`, `_3gram`), `bool_prefix` multi-match with field boosting, debounced client calls. |
| Neo4j's role in personalization | Graph `(Traveler)-[:BOOKED]->(Booking)-[:FOR_TRAVEL]->(Travel)`. The score combines city, country, duration and payment-method similarity, multiplied by the rating, and excludes travels the user has already booked or that are already past. |
| Scalability and independence of ES and Neo4j | Separate containers and k8s workloads. HPAs exist only for gateway, user and travel. ES, Kafka and Neo4j are single-node dev deployments (documented in `infra-to-add.md`). |
| Data consistency between PostgreSQL, Neo4j and ES | **Bookings:** transactional **outbox** (`BookingEventOutbox`), a scheduled publisher, Kafka, `BookingCreatedListener`, then idempotent Cypher `MERGE`, so delivery is at-least-once. **Travels:** synchronous dual-write to ES and Neo4j inside the DB transaction, plus a lazy full reindex on the first search or recommendation after startup. This dual-write can leave the stores out of sync, because the ES/Neo4j writes are not rolled back if the DB commit fails. The Kafka listener has no dead-letter topic. |
| Role permissions | §2 above. |

### 4.2 Functional checks

| Audit item | Expected verdict | Reason |
|---|---|---|
| ES returns accurate results | ✅ likely | |
| Autocomplete relevant and fast | ✅ likely | 250 ms debounce, top 5. |
| Neo4j recommendations precise, and different across accounts | ⚠️ | Works only once an account has CONFIRMED bookings, which requires a completed payment. Feedback affects the score only after the travel date. Hard to demo without seeding past, paid trips. |
| Admin dashboard complete | ⚠️ | Everything is there, but the ranking is built from paginated data and the "all actions" requirement fails. |
| Manager dashboard statistics | ✅ | |
| Traveler recommendations and history accessible | ✅ | |
| Easy navigation and subscription | ✅ for travelers / ❌ for managers (payment blocked) | |
| Cancellations respect the 3-day cutoff | ⚠️ | Correct for unpaid bookings; paid ones cannot be cancelled at all. |
| Payment secure and supports various methods | ⚠️ | Secure, but the traveler has no choice of method. Needs real sandbox keys in `.env`. |
| Travelers submit feedback easily | ✅ | Only after the trip date, which is hard to demo with future-dated seeds. |
| Feedback visible to managers and admins | ✅ | |
| Managers manage listings | ✅ | |
| Managers interact with subscriber lists | ⚠️ | Only pending bookings can be unsubscribed. |
| Managers access analytics | ✅ | |
| Traveler profiles comprehensive (participations, feedback, reports) | ⚠️ | The traveler's own dashboard has all of these. The profile a manager sees is minimal. |
| Login secure and straightforward | ✅ | |
| RBAC enforced | ✅ for blocking / ⚠️ for over-restriction | |
| SSL/TLS | ⚠️ | Only on the k3s ingress. |
| Credentials handled securely | ⚠️ | Committed dev secrets file and default JWT fallback. |
| High traffic, actions under 5 s | ⚠️ | Gateway response timeout of 5 s, Redis rate limiting and HPAs exist. There are no load tests and no caching. |
| Fallback mechanism for service failure | ❌ | No circuit breaker (Resilience4j or Spring Cloud CircuitBreaker). If ES is down, search fails with no PostgreSQL fallback. If ES or Neo4j is down, travel create and update fail. If booking or travel is down, payment and booking fail. The only partial resilience is the Kafka outbox retry. |
| UI responsive | ✅ (to verify) | |
| Easy navigation for all roles | ⚠️ | The Admin cannot reach traveler and manager pages. |
| Data protection compliance | ❌ | |
| Code readable and well separated | ✅ / ⚠️ | Clean per-service layering (controller / service / repository / dto). Some style issues: lower-case class names (`publicController`, `travelController`), fully-qualified class names inline instead of imports, `travel-service/DockerFile` capitalization. |
| SQL injection protection | ✅ | |
| XSS protection | ✅ | |
| Passwords encrypted | ✅ | |

---

## 5. Infrastructure and DevOps (supporting the requirements)

| Item | Status | Notes |
|---|---|---|
| Docker Compose (all backend services + PG, Redis, Neo4j, ES, Kafka) | ✅ | The Angular client is **not** in Compose and runs with `ng serve`. |
| k3s manifests (Kustomize) for all services, including booking, payment, Kafka and ES | ✅ | Added per `infra-to-add.md`. Step 6 there, "validate rendered manifests and deploy", is still unchecked. |
| Ansible provisioning and deploy, Vagrant VM | ✅ | |
| Jenkins + SonarQube CI platform | ✅ | |
| Prometheus / Grafana / tracing | ✅ | Actuator Prometheus endpoints; recent commit "tracing in all services". |
| Documentation up to date | ❌ | `README.md` still says payment is excluded and lists only user and travel services, which contradicts the code, Compose and k8s. |

---

## 6. Bonus (not required)

| Bonus | Status |
|---|---|
| PWA | ❌ No manifest or service worker. |
| Multilingual | ❌ Only the default Angular `extract-i18n` builder is configured; no translations. |
| Innovative feature | ⚠️ Nothing explicit. The transactional outbox, rating-weighted graph scoring and Redis token revocation are solid engineering choices but not "features". |

---

## 7. Prioritized to-do list (required scope only)

**High: directly hurts the audit**
1. Let the **Admin** use manager and traveler features. Change `hasRole('TRAVEL_MANAGER')` to `hasAnyRole('ADMIN','TRAVEL_MANAGER')` where it makes sense, and add `ADMIN` (and `TRAVEL_MANAGER`) to the route guards for `traveler-dashboard`, `payment/:id` and `managers/:id`.
2. Fix **managers being unable to pay**: add `TRAVEL_MANAGER` to the `payment/:bookingId` guard.
3. Let the **traveler choose the payment method** (Stripe or PayPal) at checkout instead of fixing it per travel.
4. Add a **fallback mechanism**: a circuit breaker with fallbacks, for example ES search falling back to a PostgreSQL `LIKE` query, recommendations falling back to popular or upcoming travels, and ES/Neo4j indexing failures logged and retried instead of failing the request.
5. Handle **cancellation of paid bookings** with the 3-day cutoff (refund call or a refund-pending state), and let managers unsubscribe paid travelers.
6. Add a minimum **GDPR** layer: account self-deletion or anonymization across services, personal data export, a consent/privacy notice at registration.

**Medium**

7. Compute the **manager ranking on the backend** over all managers, not from the 5-per-page user list.
8. Add **tests**: payment service (provider clients mocked), booking cutoff and feedback rules, recommendation query (Neo4j Testcontainers), ES search (Testcontainers), component tests for the dashboards, and one **E2E** suite (Playwright or Cypress) covering search → book → pay → feedback.
9. Clean up secrets: remove the committed `travel-plan-secrets.yaml` values and the hard-coded `JWT_SECRET` fallbacks; enforce a stronger password policy.
10. Seed demo data that makes recommendations and feedback demonstrable: past confirmed bookings, sample travels assigned to a seeded manager account.
11. Validate report targets and replace the raw "Traveler account ID" input with a picker.
12. Prevent duplicate bookings for the same travel and user.

**Low**

13. Add HSTS and CSP security headers, and HTTPS for local dev if possible.
14. Update `README.md` to reflect the real architecture.
15. Add a dead-letter topic or error handler for the Kafka listener.

---

*Next step, when you're ready: run the full stack (Compose + `ng serve`), go through each role's flow and every audit item at runtime, and write a second report on the project's live state.*
