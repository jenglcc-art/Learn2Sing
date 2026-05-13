# Skill: Plex PIN OAuth & Server Discovery

## Overview
Plex uses a PIN-based OAuth flow (not browser redirect OAuth). The app requests a PIN from plex.tv, opens the browser, then polls until the user approves.

## The 6-Step Flow

```
1. POST https://plex.tv/api/v2/pins?strong=true
   → { id: 12345, code: "ABCD1234" }

2. Open browser to:
   https://app.plex.tv/auth#?clientID={uuid}&code={code}&context[device][product]={appName}

3. Poll GET https://plex.tv/api/v2/pins/{id} every 2s
   → wait until response.authToken != null

4. GET https://plex.tv/api/v2/resources  (with X-Plex-Token header)
   → list of servers with connection URIs

5. GET {serverUri}/library/sections
   → find Directory where type == "artist" → that's the music section key

6. Launch main screen
```

## Critical JSON Field Name
The Plex API v2 returns the token as **`authToken`** (camelCase), NOT `auth_token`.  
Using `@SerializedName("auth_token")` with Gson will silently fail — the token is never detected.

```java
// WRONG
@SerializedName("auth_token")
public String authToken;

// CORRECT — Gson maps camelCase by default, no annotation needed
public String authToken;
```

## Required Headers on Every Request
```java
X-Plex-Client-Identifier: {permanent UUID for this install}
X-Plex-Product:            {app name}
X-Plex-Version:            {app version}
X-Plex-Platform:           Android
Accept:                    application/json
X-Plex-Token:              {auth token}   // once obtained
```

Use `.header()` not `.addHeader()` in OkHttp to avoid duplicate headers.

## Token Must Also Be a Query Parameter
Some Plex server versions ignore the `X-Plex-Token` header and only check the URL query param.  
Send both to be safe:

```java
// In OkHttp interceptor
urlBuilder.addQueryParameter("X-Plex-Token", authToken);
req.header("X-Plex-Token", authToken);
```

## Server URI Selection Priority
Plex returns multiple connection URIs per server. Preferred order (since we trust all SSL):

1. **Local HTTPS via plex.direct hostname** — valid cert, works on LAN
2. **Local HTTPS via raw IP** — works with trust-all SSL
3. **Remote HTTPS** — for off-LAN access
4. **Relay** — always works but slowest
5. **Any HTTP → upgrade to HTTPS** — Plex often requires HTTPS

Never save `http://` URIs — Plex's "Require HTTPS" setting silently drops HTTP connections (returns 0 bytes, causing `java.io.EOFException: \n not found: limit=0`).

## Always Use HTTPS
Even if Plex returns an `http://` connection, upgrade it:
```java
if (uri.startsWith("http://")) return "https://" + uri.substring(7);
```

## Auth Check on Launch
Always check `session.isAuthenticated() && session.hasServer()` at startup and skip the PIN flow if already authenticated. Without this, every app launch restarts the flow.

## Plex Music Search Endpoint
```
GET /library/sections/{sectionId}/all?type=10&title={query}
```
- `type=10` = Track (audio)
- Use `/all?title=` not `/search?query=` — more reliable across Plex versions
- Fallback when sectionId is unknown: `GET /library/all?type=10&title={query}`

## Track Response Structure
```json
{
  "MediaContainer": {
    "Metadata": [
      {
        "title": "Song Name",
        "grandparentTitle": "Artist Name",
        "parentTitle": "Album Name",
        "thumb": "/library/metadata/123/thumb/...",
        "duration": 240000,
        "Media": [{ "Part": [{ "key": "/library/parts/456/..." }] }]
      }
    ]
  }
}
```

## Stream & Thumbnail URLs
```java
// Audio stream
String streamUrl = serverUri + partKey + "?X-Plex-Token=" + authToken;

// Album art
String thumbUrl = serverUri + thumbPath + "?X-Plex-Token=" + authToken;
```
