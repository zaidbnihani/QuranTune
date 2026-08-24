// Supabase Edge Function: send-push-notification
// Serves as a central dispatcher for future Push Notification integrations.
// This function can be triggered via a Supabase Database Webhook on INSERT or UPDATE of `remote_messages`.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const payload = await req.json()
    console.log("Database Webhook received payload:", JSON.stringify(payload, null, 2))

    // Payload structure from Supabase Database Webhooks:
    // {
    //   type: 'INSERT' | 'UPDATE' | 'DELETE',
    //   table: 'remote_messages',
    //   record: { id, app_id, message, version, is_active, ... },
    //   old_record: { ... }
    // }
    const { type, table, record } = payload

    if (table !== 'remote_messages') {
      return new Response(JSON.stringify({ error: `Unsupported table: ${table}` }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    if (!record || !record.is_active) {
      return new Response(JSON.stringify({ message: "No active record or message to send." }), {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const { app_id, message, version } = record

    console.log(`Preparing to send push notification for App ID: ${app_id}, Message: "${message}", Version: ${version}`)

    // =========================================================================
    // FUTURE PUSH NOTIFICATION PROVIDER INTEGRATION
    // =========================================================================
    // When you're ready to integrate a push notification provider (OneSignal, Pusher,
    // Courier, custom HTTP endpoint, etc.), you can easily add the code below.
    //
    // Example Integration with a generic HTTP-based push provider:
    //
    // const PUSH_API_KEY = Deno.env.get("PUSH_PROVIDER_API_KEY");
    // const response = await fetch("https://api.yourpushprovider.com/v1/send", {
    //   method: "POST",
    //   headers: {
    //     "Authorization": `Bearer ${PUSH_API_KEY}`,
    //     "Content-Type": "application/json"
    //   },
    //   body: JSON.stringify({
    //     app_id: app_id,
    //     title: app_id === "qurantune" ? "تنبيه جديد" : app_id,
    //     body: message,
    //     data: {
    //       version: version,
    //       app_id: app_id
    //     }
    //   })
    // });
    // =========================================================================

    return new Response(
      JSON.stringify({
        success: true,
        message: `Edge function successfully processed event type: ${type}`,
        data: {
          app_id,
          message,
          version,
          notified: false,
          info: "Ready for integration with your push notification provider."
        }
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    console.error("Error executing Edge Function:", error)
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500
    )
  }
})
