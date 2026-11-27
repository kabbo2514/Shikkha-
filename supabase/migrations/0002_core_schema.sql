-- 0002_core_schema.sql
-- Core tables for Phase 1-3. Later phases (exams, chat, matches, gamification,
-- videos) extend this schema following the same patterns.

-- ---------------------------------------------------------------------------
-- Profiles: extends Supabase auth.users (Spec 2.4 / 2.5)
-- ---------------------------------------------------------------------------
create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username citext not null unique,
    full_name text not null,
    class_level int not null check (class_level between 1 and 10),
    medium text not null check (medium in ('bangla', 'english')),
    role text not null default 'student' check (role in ('student', 'teacher', 'moderator', 'admin')),
    approval_status text not null default 'pending'
        check (approval_status in ('pending', 'approved', 'rejected', 'suspended', 'banned', 'deletion_pending')),
    rejection_reason text,
    avatar_url text,
    show_online_status boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create trigger profiles_touch_updated
before update on public.profiles
for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------
-- Curriculum structure (Spec 4.1 / 4.2)
-- ---------------------------------------------------------------------------
create table public.curriculum_versions (
    id uuid primary key default gen_random_uuid(),
    code text not null unique,
    name_en text not null,
    name_bn text not null,
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);

create table public.subjects (
    id uuid primary key default gen_random_uuid(),
    curriculum_id uuid not null references public.curriculum_versions(id) on delete cascade,
    class_level int not null check (class_level between 1 and 10),
    medium text not null check (medium in ('bangla', 'english')),
    name_en text not null,
    name_bn text not null,
    sort_order int not null default 0,
    unique (curriculum_id, class_level, medium, name_en)
);

create table public.chapters (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references public.subjects(id) on delete cascade,
    name_en text not null,
    name_bn text not null,
    sort_order int not null default 0
);

create index chapters_subject_idx on public.chapters (subject_id, sort_order);

create table public.topics (
    id uuid primary key default gen_random_uuid(),
    chapter_id uuid not null references public.chapters(id) on delete cascade,
    name_en text,
    name_bn text,
    sort_order int not null default 0
);

create index topics_chapter_idx on public.topics (chapter_id, sort_order);

-- ---------------------------------------------------------------------------
-- Question bank (Spec 4.3 / 4.4). options/subquestions stored as JSONB;
-- application code validates shape. old versions are kept via version + status.
-- ---------------------------------------------------------------------------
create table public.mcqs (
    id uuid primary key default gen_random_uuid(),
    chapter_id uuid not null references public.chapters(id) on delete cascade,
    topic_id uuid references public.topics(id) on delete set null,
    question_en text,
    question_bn text,
    options jsonb not null default '[]'::jsonb,
    correct_index int not null check (correct_index >= 0),
    explanation_en text,
    explanation_bn text,
    difficulty int not null default 1 check (difficulty between 1 and 3),
    marks numeric not null default 1 check (marks > 0),
    image_url text,
    source text,
    status text not null default 'draft' check (status in ('draft', 'in_review', 'published', 'archived')),
    version int not null default 1,
    created_by uuid references public.profiles(id),
    reviewed_by uuid references public.profiles(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (question_en is not null or question_bn is not null),
    check (jsonb_typeof(options) = 'array' and jsonb_array_length(options) >= 2),
    check (correct_index < jsonb_array_length(options))
);

create index mcqs_chapter_status_idx on public.mcqs (chapter_id, status);
create index mcqs_status_idx on public.mcqs (status) where status = 'published';

create trigger mcqs_touch_updated
before update on public.mcqs
for each row execute function public.touch_updated_at();

create table public.creative_questions (
    id uuid primary key default gen_random_uuid(),
    chapter_id uuid not null references public.chapters(id) on delete cascade,
    stem_en text,
    stem_bn text,
    -- [{"label":"ক","text_en":"","text_bn":"","marks":2.5,
    --  "model_answer_en":"","model_answer_bn":""}]
    subquestions jsonb not null default '[]'::jsonb,
    rubric jsonb,
    total_marks numeric not null default 10 check (total_marks > 0),
    status text not null default 'draft' check (status in ('draft', 'in_review', 'published', 'archived')),
    version int not null default 1,
    created_by uuid references public.profiles(id),
    reviewed_by uuid references public.profiles(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (stem_en is not null or stem_bn is not null)
);

create index cqs_chapter_status_idx on public.creative_questions (chapter_id, status);

-- ---------------------------------------------------------------------------
-- Practice attempts (Spec 5.5). id is client-generated => idempotent retries.
-- ---------------------------------------------------------------------------
create table public.practice_attempts (
    id uuid primary key,                       -- client-generated idempotency key
    student_id uuid not null default auth.uid() references public.profiles(id) on delete cascade,
    chapter_id uuid references public.chapters(id) on delete set null,
    mode text not null default 'chapter' check (mode in ('chapter', 'subject', 'mixed', 'mistakes', 'bookmarks')),
    started_at timestamptz not null default now(),
    completed_at timestamptz,
    total_questions int not null default 0,
    correct_count int not null default 0
);

create index practice_attempts_student_idx
    on public.practice_attempts (student_id, completed_at desc nulls last);

create table public.practice_answers (
    id bigint generated always as identity primary key,
    attempt_id uuid not null references public.practice_attempts(id) on delete cascade,
    question_id uuid not null references public.mcqs(id) on delete cascade,
    selected_index int,
    is_correct boolean not null,
    time_spent_seconds int not null default 0,
    unique (attempt_id, question_id)
);

create index practice_answers_attempt_idx on public.practice_answers (attempt_id);

-- ---------------------------------------------------------------------------
-- Mistake tracker (Spec 8). Composite PK prevents duplicate inflation (8.4).
-- ---------------------------------------------------------------------------
create table public.mistakes (
    student_id uuid not null default auth.uid() references public.profiles(id) on delete cascade,
    question_id uuid not null references public.mcqs(id) on delete cascade,
    first_seen_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    repetition_count int not null default 1,
    resolved boolean not null default false,
    primary key (student_id, question_id)
);
