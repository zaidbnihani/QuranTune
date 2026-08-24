# Supabase Remote Messages & Notifications Setup

This directory contains the database migration, Edge Function scaffold, and complete instructions for setting up the backend of **QuranTune**'s remote message and notifications system.

---

## 📂 Directory Structure

```text
supabase/
├── migrations/
│   └── 20260813000000_remote_messages.sql    # Database Tables, Triggers, RLS, and Realtime
├── functions/
│   └── send-push-notification/
│       └── index.ts                          # Edge Function Scaffold (ready for Push APIs)
└── README.md                                 # This Guide
```

---

## 🛠️ Step 1: Run SQL Migration in Supabase

1. Open your **Supabase Dashboard** (https://supabase.com).
2. Go to the **SQL Editor** from the left navigation panel.
3. Click on **New Query**.
4. Copy the entire contents of `supabase/migrations/20260813000000_remote_messages.sql` and paste it into the query editor.
5. Click **Run**. This will create:
   - Table `remote_messages` with automatic unique constraints per `app_id`.
   - Table `remote_message_history` for historic changes.
   - Database triggers to automatically increment `version` ONLY on actual message changes, block duplicate no-op saves, and record the history logs automatically.
   - Row Level Security (RLS) policies allowing secure read-only access for mobile clients (`anon`) and full control for administrators (`authenticated`).
   - Enabled **Realtime** on the `remote_messages` table.

---

## 🚀 Step 2: Deploy the Edge Function (Optional for Future Push Notifications)

To deploy the Edge Function using the Supabase CLI:

```bash
# Login to Supabase CLI
supabase login

# Link your local project to your Supabase project
supabase link --project-ref <your-project-ref-id>

# Deploy the Edge Function
supabase functions deploy send-push-notification
```

---

## 🔗 Step 3: Configure Database Webhook to Trigger Edge Function (FCM/Push)

To automatically invoke the Edge Function and trigger push notifications when messages are updated or created:

1. In the **Supabase Dashboard**, navigate to **Database** -> **Webhooks**.
2. Click **Create a new webhook**.
3. Fill in the following details:
   - **Name**: `send_push_notification`
   - **Table**: `remote_messages`
   - **Events**: Check `Insert` and `Update`.
   - **Type**: `Supabase Edge Function`
   - **Edge Function**: Select `send-push-notification`.
   - **Method**: `POST`
   - **Timeout**: `10000 ms`
4. Click **Save**.

Now, whenever a message is added or changed, your Edge Function will run automatically!

---

## 📡 Step 4: REST API Endpoints for the QuranTune Android App

Since RLS is enabled, you only need to use your project's **Anon Key** (Publishable Key) and **Supabase URL** to securely query active messages.

### Get the Current Active Message for QuranTune

* **URL**: `https://<YOUR_SUPABASE_ID>.supabase.co/rest/v1/remote_messages?app_id=eq.qurantune&is_active=eq.true&select=*`
* **Method**: `GET`
* **Headers**:
  - `apikey`: `<YOUR_SUPABASE_ANON_KEY>`
  - `Authorization`: `Bearer <YOUR_SUPABASE_ANON_KEY>`

---

## 🧪 Step 5: How to Test the Entire Flow

### Test A: Initial Upload ("السلام عليكم")

Using curl to simulate your administration panel uploading the first message:

```bash
curl -X POST "https://<YOUR_SUPABASE_ID>.supabase.co/rest/v1/remote_messages" \
  -H "apikey: <YOUR_SUPABASE_ANON_KEY>" \
  -H "Authorization: Bearer <YOUR_SUPABASE_ADMIN_OR_AUTHENTICATED_KEY>" \
  -H "Content-Type: application/json" \
  -H "Prefer: resolution=merge-duplicates" \
  -d '{
    "app_id": "qurantune",
    "message": "السلام عليكم",
    "is_active": true
  }'
```

**Result in Supabase Database**:
* `app_id`: `qurantune`
* `message`: `السلام عليكم`
* `version`: `1`
* A new entry is added to `remote_message_history` recording `version 1`.

---

### Test B: Duplicate Upload (No Changes)

If the administration panel attempts to upload the exact same text again:

```bash
curl -X POST "https://<YOUR_SUPABASE_ID>.supabase.co/rest/v1/remote_messages" \
  -H "apikey: <YOUR_SUPABASE_ANON_KEY>" \
  -H "Authorization: Bearer <YOUR_SUPABASE_ADMIN_OR_AUTHENTICATED_KEY>" \
  -H "Content-Type: application/json" \
  -H "Prefer: resolution=merge-duplicates" \
  -d '{
    "app_id": "qurantune",
    "message": "السلام عليكم",
    "is_active": true
  }'
```

**Result in Supabase Database**:
* The database trigger returns `NULL` and cancels/no-ops the update.
* `version` remains `1` (does not increment).
* `updated_at` does not change.
* **No** duplicate row is added to `remote_message_history`.

---

### Test C: Updating Content ("التطبيق متوقف مؤقتًا")

When you upload a different message:

```bash
curl -X POST "https://<YOUR_SUPABASE_ID>.supabase.co/rest/v1/remote_messages" \
  -H "apikey: <YOUR_SUPABASE_ANON_KEY>" \
  -H "Authorization: Bearer <YOUR_SUPABASE_ADMIN_OR_AUTHENTICATED_KEY>" \
  -H "Content-Type: application/json" \
  -d '{
    "app_id": "qurantune",
    "message": "التطبيق متوقف مؤقتًا",
    "is_active": true
  }'
```
*(Or perform a regular PUT/PATCH update).*

**Result in Supabase Database**:
* `message` updates to `"التطبيق متوقف مؤقتًا"`.
* `version` automatically increments to `2`.
* `updated_at` becomes the current timestamp.
* A new log is written to `remote_message_history` with `"التطبيق متوقف مؤقتًا"` and `version: 2`.
* Your Edge Function is triggered automatically via Webhook to broadcast the push notification if configured!
