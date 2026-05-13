# Skill: Android Debugging & Logcat

## Reading Logcat Output

Logcat lines follow this format:
```
DATE TIME PID TID LEVEL TAG: MESSAGE
```
Example:
```
05-10 14:23:01.452  8312  8312 E PlexRetrofitClient: Request failed: GET https://192.168.0.29:32400/identity
```

| Column | Meaning |
|--------|---------|
| `E`    | Error — always investigate |
| `W`    | Warning — usually important |
| `I`    | Info — normal operation logs |
| `D`    | Debug — verbose, can be noisy |
| `V`    | Verbose — very noisy, often system |

## Filtering Noise from Logcat

Real device logcats contain huge amounts of GPU/system noise. Filter by your app's PID or TAG:

```bash
# Filter to your app only
adb logcat --pid=$(adb shell pidof com.learn2sing.app)

# Filter by TAG
adb logcat -s MainActivity PlexRetrofitClient PlayerActivity

# Filter level ERROR and above
adb logcat *:E
```

Common noise to ignore:
- `Adreno` — GPU driver messages
- `ashmem` — Android memory system
- `libpenguin` — device vendor library
- `BpBinder` — IPC framework

## Common Android Network Errors

| Error | Meaning | Fix |
|-------|---------|-----|
| `EOFException: \n not found: limit=0` | Server sent 0 bytes — likely rejecting plain HTTP | Upgrade to HTTPS |
| `SSLHandshakeException` | SSL cert not trusted by Android | Add trust-all X509TrustManager |
| `CertPathValidatorException` | Cert chain invalid (raw IP, self-signed) | Same as above |
| `ConnectException: Connection refused` | Server not running or wrong port | Check server, port, firewall |
| `SocketTimeoutException` | Server too slow / unreachable | Increase timeout or check network |
| `UnknownHostException` | DNS failed to resolve hostname | Check hostname, network config |
| `NetworkOnMainThreadException` | HTTP call on UI thread | Move to background thread / Retrofit callback |

## Adding Useful Debug Logging

Always tag your logs for easy filtering:

```java
private static final String TAG = "MainActivity";  // or class name

Log.d(TAG, "Searching for: " + query);
Log.i(TAG, "Found " + tracks.size() + " tracks");
Log.w(TAG, "Section ID unknown, falling back to /library/all");
Log.e(TAG, "Network error: " + t.getMessage(), t);
```

For network calls, log the full URL before and response code after:

```java
// In OkHttp interceptor
Log.d(TAG, "→ " + request.method() + " " + request.url());
Response response = chain.proceed(request);
Log.d(TAG, "← " + response.code() + " " + request.url());
```

## Testing Network Reachability Before Full Request

Add a lightweight `/identity` or ping call at startup to verify connectivity:

```java
serverApi.identity().enqueue(new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        Log.i(TAG, "Server reachable: HTTP " + response.code());
    }
    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
        Log.e(TAG, "Server unreachable: " + t.getMessage());
    }
});
```

## Diagnosing Auth / Token Issues

If API calls return 401 or 403:
1. Verify the token is not null: `Log.d(TAG, "Token: " + session.getAuthToken());`
2. Verify the token is sent in the request (check OkHttp interceptor logs)
3. Try sending the token as a URL query parameter in addition to the header:
   ```java
   urlBuilder.addQueryParameter("X-Plex-Token", authToken);
   ```

## Activity / Intent Debugging

Check what was passed in an Intent:

```java
String url = getIntent().getStringExtra("EXTRA_STREAM_URL");
Log.d(TAG, "Received stream URL: " + url);
if (url == null) {
    Log.w(TAG, "No stream URL in intent — extras: " + getIntent().getExtras());
}
```

## Checking Build Variants

Android Studio may run the wrong build variant. If your changes aren't showing up:
- Build > Clean Project
- Build > Rebuild Project
- Make sure the active build variant is `debug` (not `release`) unless you're testing signed APKs

## Useful ADB Commands

```bash
# See app logs live
adb logcat -s YOUR_TAG

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch activity
adb shell am start -n com.learn2sing.app/.PlexAuthActivity

# Clear app data (forces fresh login)
adb shell pm clear com.learn2sing.app

# Check if device is reachable
adb devices
```

## Retrofit Callback Threading

Retrofit callbacks (`onResponse`, `onFailure`) run on the **main thread** automatically when used with `.enqueue()`. Do NOT call `runOnUiThread()` inside them — it's already there. Only use `runOnUiThread()` if you're on a background thread yourself.
