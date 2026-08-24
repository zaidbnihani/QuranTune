-- Migration: Setup Remote Messages & History Tables for QuranTune
-- Created: 2026-08-13

-- 1. Create remote_messages table (contains the single active message per app_id)
CREATE TABLE IF NOT EXISTS remote_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_id TEXT UNIQUE NOT NULL,
    message TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Create remote_message_history table to track all historical changes
CREATE TABLE IF NOT EXISTS remote_message_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_id TEXT NOT NULL,
    message TEXT NOT NULL,
    version INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Create Trigger Function for BEFORE INSERT
CREATE OR REPLACE FUNCTION handle_remote_message_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version := 1;
    NEW.created_at := COALESCE(NEW.created_at, NOW());
    NEW.updated_at := COALESCE(NEW.updated_at, NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Create Trigger Function for BEFORE UPDATE
-- This ensures version is incremented ONLY when message actually changes,
-- and ignores/no-ops any duplicate messages.
CREATE OR REPLACE FUNCTION handle_remote_message_before_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.message IS DISTINCT FROM OLD.message THEN
        NEW.version := OLD.version + 1;
        NEW.updated_at := NOW();
    ELSE
        -- No actual change to message.
        -- We return NULL to completely cancel/ignore the update, avoiding database noise and duplicates.
        RETURN NULL; 
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Create Trigger Function for AFTER INSERT OR UPDATE to log into history
CREATE OR REPLACE FUNCTION handle_remote_message_after_save()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO remote_message_history (app_id, message, version, created_at)
    VALUES (NEW.app_id, NEW.message, NEW.version, NEW.updated_at);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Attach Triggers to remote_messages table
CREATE TRIGGER tr_remote_message_before_insert
    BEFORE INSERT ON remote_messages
    FOR EACH ROW
    EXECUTE FUNCTION handle_remote_message_before_insert();

CREATE TRIGGER tr_remote_message_before_update
    BEFORE UPDATE ON remote_messages
    FOR EACH ROW
    EXECUTE FUNCTION handle_remote_message_before_update();

CREATE TRIGGER tr_remote_message_after_save
    AFTER INSERT OR UPDATE ON remote_messages
    FOR EACH ROW
    EXECUTE FUNCTION handle_remote_message_after_save();

-- 7. Enable Row Level Security (RLS)
ALTER TABLE remote_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE remote_message_history ENABLE ROW LEVEL SECURITY;

-- 8. Create RLS Policies
-- Allow anyone (public/anon) to read current active messages
CREATE POLICY "Allow public read-only access to remote_messages"
ON remote_messages
FOR SELECT
USING (true);

-- Allow anyone (public/anon) to read message history
CREATE POLICY "Allow public read-only access to remote_message_history"
ON remote_message_history
FOR SELECT
USING (true);

-- Allow authenticated admin users to perform all operations
CREATE POLICY "Allow authenticated admins full access to remote_messages"
ON remote_messages
FOR ALL
TO authenticated
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow authenticated admins full access to remote_message_history"
ON remote_message_history
FOR ALL
TO authenticated
USING (true)
WITH CHECK (true);

-- 9. Enable Realtime on the remote_messages table
-- Note: Check if the table is already in the supabase_realtime publication
do $$
begin
  if not exists (
    select 1 
    from pg_publication_tables 
    where pubname = 'supabase_realtime' 
      and schemaname = 'public' 
      and tablename = 'remote_messages'
  ) then
    alter publication supabase_realtime add table remote_messages;
  end if;
end;
$$;
