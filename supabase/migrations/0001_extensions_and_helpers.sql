-- 0001_extensions_and_helpers.sql
-- Run first. Extensions, helper functions, and audit foundation.

create extension if not exists citext;

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- Role checks. SECURITY DEFINER so RLS policies can evaluate roles without
-- granting broad read access to profiles (Spec 20.4).
-- ---------------------------------------------------------------------------
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles
        where id = auth.uid() and role = 'admin'
    );
$$;

create or replace function public.has_role(roles text[])
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles
        where id = auth.uid() and role = any(roles)
    );
$$;

-- ---------------------------------------------------------------------------
-- Audit log. NO grants to anon/authenticated: only service_role can read it.
-- The app never sees audit rows; admin dashboards (Phase 9) reach them through
-- trusted server functions.
-- ---------------------------------------------------------------------------
create table if not exists public.audit_logs (
    id bigint generated always as identity primary key,
    actor_id uuid,
    action text not null,
    target_table text,
    target_id text,
    detail jsonb,
    created_at timestamptz not null default now()
);

revoke all on public.audit_logs from anon;
revoke all on public.audit_logs from authenticated;

-- ---------------------------------------------------------------------------
-- Trusted admin operation: approve / reject / suspend a registration
-- (Spec 2.2/14.2). Clients cannot UPDATE profiles.approval_status because
-- column privileges (migration 0003) do not include it — this function is the
-- only path, and every call is audited.
-- ---------------------------------------------------------------------------
create or replace function public.admin_set_approval(
    p_user_id uuid,
    p_status text,
    p_reason text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_admin() then
        raise exception 'Forbidden';
    end if;
    if p_status not in ('approved', 'rejected', 'suspended', 'banned') then
        raise exception 'Invalid status: %', p_status;
    end if;

    update public.profiles
    set approval_status = p_status,
        rejection_reason = p_reason,
        updated_at = now()
    where id = p_user_id;

    insert into public.audit_logs (actor_id, action, target_table, target_id, detail)
    values (auth.uid(), 'admin_set_approval', 'profiles', p_user_id::text,
            jsonb_build_object('status', p_status, 'reason', p_reason));
end;
$$;

grant execute on function public.admin_set_approval(uuid, text, text) to authenticated;
