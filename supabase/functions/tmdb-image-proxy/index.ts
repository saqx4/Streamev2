// TMDB Image Proxy - Secured with basic rate limiting and allowlisted paths
// Deploy with: npx supabase functions deploy tmdb-image-proxy
// Set secrets:
//   npx supabase secrets set APP_ANON_KEY=your_anon_key

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const TMDB_IMAGE_BASE_URL = "https://image.tmdb.org"

// Rate limiting: 240 requests per minute per IP (images are heavier but fewer metadata calls)
const RATE_LIMIT = 240
const RATE_WINDOW_MS = 60 * 1000
const rateLimitMap = new Map<string, { count: number; resetTime: number }>()

// Allowed paths (prefix matching). TMDB images live under /t/p/
const ALLOWED_PATH_PREFIXES = ["/t/p/"]

function isPathAllowed(path: string): boolean {
  return ALLOWED_PATH_PREFIXES.some((p) => path.startsWith(p))
}

function getClientIP(req: Request): string {
  return (
    req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
    req.headers.get("x-real-ip") ||
    req.headers.get("cf-connecting-ip") ||
    "unknown"
  )
}

function checkRateLimit(ip: string): { allowed: boolean; remaining: number; resetIn: number } {
  const now = Date.now()
  const record = rateLimitMap.get(ip)

  if (!record || now > record.resetTime) {
    rateLimitMap.set(ip, { count: 1, resetTime: now + RATE_WINDOW_MS })
    return { allowed: true, remaining: RATE_LIMIT - 1, resetIn: RATE_WINDOW_MS }
  }

  if (record.count >= RATE_LIMIT) {
    return { allowed: false, remaining: 0, resetIn: record.resetTime - now }
  }

  record.count++
  return { allowed: true, remaining: RATE_LIMIT - record.count, resetIn: record.resetTime - now }
}

setInterval(() => {
  const now = Date.now()
  for (const [ip, record] of rateLimitMap.entries()) {
    if (now > record.resetTime) rateLimitMap.delete(ip)
  }
}, 60000)

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const clientIP = getClientIP(req)
    const rateCheck = checkRateLimit(clientIP)
    if (!rateCheck.allowed) {
      return new Response(
        JSON.stringify({ error: "Rate limit exceeded", retryAfter: Math.ceil(rateCheck.resetIn / 1000) }),
        {
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json",
            "Retry-After": String(Math.ceil(rateCheck.resetIn / 1000)),
            "X-RateLimit-Limit": String(RATE_LIMIT),
            "X-RateLimit-Remaining": "0",
            "X-RateLimit-Reset": String(Math.ceil(rateCheck.resetIn / 1000)),
          },
          status: 429,
        },
      )
    }

    const apiKey = req.headers.get("apikey")
    const authHeader = req.headers.get("authorization")
    const expectedAnon = Deno.env.get("APP_ANON_KEY")

    const hasValidApiKey = apiKey && expectedAnon && apiKey === expectedAnon
    const hasValidAuth =
      authHeader?.startsWith("Bearer ") && expectedAnon && authHeader.replace("Bearer ", "") === expectedAnon

    if (!hasValidApiKey && !hasValidAuth) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      })
    }

    const url = new URL(req.url)
    const path = url.searchParams.get("path")

    if (!path) throw new Error("Missing path parameter")
    if (!path.startsWith("/")) throw new Error("Invalid path")
    if (!isPathAllowed(path)) {
      return new Response(JSON.stringify({ error: "Path not allowed" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 403,
      })
    }

    const imageUrl = new URL(`${TMDB_IMAGE_BASE_URL}${path}`)

    const upstream = await fetch(imageUrl.toString(), {
      method: "GET",
      headers: {
        // Keep it simple. We mainly want to forward bytes.
        "Accept": "image/*,*/*;q=0.8",
      },
    })

    if (!upstream.ok) {
      const txt = await upstream.text().catch(() => "")
      return new Response(txt || JSON.stringify({ error: "Upstream error" }), {
        headers: { ...corsHeaders, "Content-Type": upstream.headers.get("content-type") ?? "text/plain" },
        status: upstream.status,
      })
    }

    const contentType = upstream.headers.get("content-type") ?? "application/octet-stream"
    const cacheControl = upstream.headers.get("cache-control") ?? "public, max-age=86400"

    return new Response(upstream.body, {
      status: 200,
      headers: {
        ...corsHeaders,
        "Content-Type": contentType,
        "Cache-Control": cacheControl,
        "X-RateLimit-Limit": String(RATE_LIMIT),
        "X-RateLimit-Remaining": String(rateCheck.remaining),
      },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    })
  }
})
