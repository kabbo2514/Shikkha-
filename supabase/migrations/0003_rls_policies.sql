-- 0003_rls_policies.sql
-- Row Level Security for every table. Principle (Spec 20.4/22.3): the client is
-- never trusted for roles, approval status, or ownership.

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
alter table public.profiles enable row level security;

revoke insert, update, delete on public.profiles from anon, authenticated;

-- Students may create ONLY their own row, ONLY as pending student (Spec 2.2/2.7).
grant insert (id, username, full_name, class_level, medium) on public.profiles to authenticated;
-- Students may edit ONLY harmless profile fields — never role or approval_status.
grant update (full_name, avatar_url, show_online_status) on public.profiles to authenticated;
grant select on public.profiles to authenticated;

create policy profiles_select on public.profiles
    for select to authenticated
    using (auth.uid() = id or public.is_admin());

create policy profiles_insert_self_pending on public.profiles
    for insert to authenticated
    with check (
        auth.uid() = id
        and role = 'student'
        and approval_status = 'pending'
    );

-- UPDATE policy still needed for the column-granted fields above.
create policy profiles_update_safe_columns on public.profiles
    for update to authenticated
    using (auth.uid() = id)
    with check (auth.uid() = id);

-- ---------------------------------------------------------------------------
-- audit_logs: no client access at all
-- ---------------------------------------------------------------------------
alter table public.audit_logs enable row level security;
revoke all on public.audit_logs from anon, authenticated;
-- (service_role bypasses RLS by design and is never present in the app.)

-- ---------------------------------------------------------------------------
-- curriculum catalog: readable by any authenticated user; writable by admins
-- ---------------------------------------------------------------------------
alter table public.curriculum_versions enable row level security;
alter table public.subjects enable row level security;
alter table public.chapters enable row level security;
alter table public.topics enable row level security;

grant select on public.curriculum_versions, public.subjects, public.chapters, public.topics
    to authenticated;

-- curriculum_versions
create policy curriculum_select on public.curriculum_versions
    for select to authenticated using (true);
create policy curriculum_admin_write on public.curriculum_versions
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- subjects
create policy subjects_select on public.subjects
    for select to authenticated using (true);
create policy subjects_admin_write on public.subjects
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- chapters
create policy chapters_select on public.chapters
    for select to authenticated using (true);
create policy chapters_admin_write on public.chapters
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- topics
create policy topics_select on public.topics
    for select to authenticated using (true);
create policy topics_admin_write on public.topics
    for all to authenticated
    using (public.is_admin()) with check (public.is_admin());

-- ---------------------------------------------------------------------------
-- question bank: published content readable; write restricted to admins
-- (Spec 4.6 / 4.7 — answer keys never exposed for unpublished content)
-- ---------------------------------------------------------------------------
alter table public.mcqs enable row level security;
alter table public.creative_questions enable row level security;

revoke insert, update, delete on public.mcqs, public.creative_questions from anon, authenticated;
grant select on public.mcqs, public.creative_questions to authenticated;

create policy mcqs_published_select on public.mcqs
    for select to authenticated
    using (status = 'published' or public.is_admin());

create policy mcqs_admin_insert on public.mcqs
    for insert to authenticated with check (public.is_admin());
create policy mcqs_admin_update on public.mcqs
    for update to authenticated using (public.is_admin()) with check (public.is_admin());
create policy mcqs_admin_delete on public.mcqs
    for delete to authenticated using (public.is_admin());

create policy cqs_published_select on public.creative_questions
    for select to authenticated
    using (status = 'published' or public.is_admin());

create policy cqs_admin_insert on public.creative_questions
    for insert to authenticated with check (public.is_admin());
create policy cqs_admin_update on public.creative_questions
    for update to authenticated using (public.is_admin()) with check (public.is_admin());
create policy cqs_admin_delete on public.creative_questions
    for delete to authenticated using (public.is_admin());

-- ---------------------------------------------------------------------------
-- practice attempts/answers: strictly owner-scoped (Spec 5.5)
-- ---------------------------------------------------------------------------
alter table public.practice_attempts enable row level security;
alter table public.practice_answers enable row level security;

revoke all on public.practice_attempts, public.practice_answers from anon, authenticated;
grant select, insert on public.practice_attempts to authenticated;
grant select, insert on public.practice_answers to authenticated;

create policy attempts_select_own on public.practice_attempts
    for select to authenticated using (student_id = auth.uid());

create policy attempts_insert_own on public.practice_attempts
    for insert to authenticated with check (student_id = auth.uid());

create policy answers_select_own on public.practice_answers
    for select to authenticated
    using (exists (
        select 1 from public.practice_attempts a
        where a.id = attempt_id and a.student_id = auth.uid()
    ));

create policy answers_insert_own on public.practice_answers
    for insert to authenticated
    with check (
        question_id in (select id from public.mcqs where status = 'published')
        and exists (
            select 1 from public.practice_attempts a
            where a.id = attempt_id and a.student_id = auth.uid()
        )
    );

-- ---------------------------------------------------------------------------
-- mistakes: strictly owner-scoped (Spec 8.4)
-- ---------------------------------------------------------------------------
alter table public.mistakes enable row level security;

revoke all on public.mistakes from anon, authenticated;
grant select, insert, update on public.mistakes to authenticated;

create policy mistakes_select_own on public.mistakes
    for select to authenticated using (student_id = auth.uid());

create policy mistakes_insert_own on public.mistakes
    for insert to authenticated with check (student_id = auth.uid());

create policy mistakes_update_own on public.mistakes
    for update to authenticated
    using (student_id = auth.uid()) with check (student_id = auth.uid());
