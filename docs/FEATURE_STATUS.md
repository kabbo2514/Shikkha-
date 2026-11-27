# Feature Status Tracker

Statuses follow the master spec §1.5 convention. **Nothing below is TESTED until
you run it on a device/emulator and confirm acceptance criteria.** Update this file
from repository evidence only (Spec §29.11).

## Foundation & Security (Phases 1-2)

| Feature | Spec | Status | Evidence / gap |
|---|---|---|---|
| Gradle project, version catalog, Compose+M3, MVVM skeleton | §19, §27-P1 | IMPLEMENTED | `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/` |
| Hilt DI, Coroutines/Flow, Navigation Compose | §19.1 | IMPLEMENTED | `core/network`, `core/common/SerializationModule.kt` (Json provider), `navigation/AppNavigation.kt` |
| Registration (name/username/class/medium, validation) | §2.1 | IMPLEMENTED | `RegisterScreen.kt`, `RegisterViewModel` |
| Pending-approval gate, restricted screen | §2.2 | IMPLEMENTED (server-enforced) | `PendingApprovalScreen.kt`, RLS `profiles_insert_self_pending` |
| Admin approve/reject via audited function | §2.2, §14.2 | IMPLEMENTED | `admin_set_approval()` + `audit_logs` |
| Email login, logout, session persistence | §2.3 | IMPLEMENTED | `AuthRepository`, supabase-kt Auth |
| Password recovery, Google sign-in, session management UI | §2.3, §2.6 | PLANNED | Supabase dashboard enables recovery; UI not built |
| Roles, account states (suspended/banned/deleted) | §2.5-2.7 | IMPLEMENTED (schema) / PLANNED (admin UI) | `profiles` table; console in Phase 9 |
| Column-level privileges prevent privilege self-assignment | §22.3 | IMPLEMENTED | `0003_rls_policies.sql` grants |

## Academic Core (Phase 3)

| Feature | Spec | Status | Evidence / gap |
|---|---|---|---|
| Dashboard with shortcuts, empty states, class/medium display | §3 | IMPLEMENTED | `DashboardScreen.kt` |
| Curriculum: subjects by class+medium, chapters | §4.1-4.2 | IMPLEMENTED | `curriculum/` + seed |
| MCQ model with options/explanation/difficulty/bilingual | §4.3 | IMPLEMENTED | `mcqs` table, `McqDto` |
| MCQ practice: single question, immediate feedback, explanation | §5.1-5.3 | IMPLEMENTED | `PracticeScreen.kt` (untimed chapter mode; timers/timed modes PLANNED) |
| Bookmark / report question | §5.4 | PLANNED | Tables/UI not built |
| Attempt recording, accuracy, offline queue, idempotent retries | §5.5, §21 | IMPLEMENTED | `practice_attempts` (client UUID PK), Room queue |
| Result review with per-question breakdown | §5 / §7.7-lite | IMPLEMENTED | `PracticeResultScreen.kt` |
| CQ browsing + model answers | §6.1-6.2 | IMPLEMENTED (not yet run) | `cq/`, Chapters → "Creative questions"; demo data in `seed_cq.sql`; online only, no offline cache |
| CQ writing practice (drafts, sync) | §6.3-6.4 | PLANNED | — |
| Mistake recording (auto from wrong answers) | §8.1 | IMPLEMENTED | `recordMistakes()` in `McqRepository` |
| Mistake library + retry practice | §8.2 | IMPLEMENTED (not yet run) | `mistakes/`, dashboard "Mistake Review"; one correct retry resolves a mistake, a new wrong answer re-opens it; online only |
| Spaced-repetition scheduler / revision queue | §8.3 | PLANNED | Simple resolve rule only |
| Mastery algorithm | §8.5 | PLANNED | — |

## Later phases (all PLANNED unless noted)

| Feature | Spec | Status |
|---|---|---|
| Examination system (lifecycle, scoring, timer, restore) | §7 | PLANNED |
| Analytics charts, reports | §15 | PLANNED (practice counts exist) |
| XP, achievements, streaks, leaderboards, leagues, ELO | §12 | PLANNED |
| Friends, blocking, reporting | §9 | PLANNED |
| 1:1 chat (text/voice), typing receipts | §10 | PLANNED |
| Live 1v1 matches, matchmaking, ratings | §11 | PLANNED |
| Video lessons, player, offline downloads | §13 | PLANNED |
| Notifications (FCM, preferences) | §16 | PLANNED |
| Premium/ads | §17 | DEFERRED BY DECISION (disabled per §17.4) |
| Bangla font with full conjunct coverage (e.g. Hind Siliguri) | §18.4 | PLANNED |
| Unit/UI/security tests | §25 | PLANNED |
| Beta build, bug tracker, release | §26 | PLANNED |
