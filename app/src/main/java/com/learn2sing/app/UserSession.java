package com.learn2sing.app;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

/**
 * Singleton that manages the signed-in Google account and OAuth token lifecycle.
 *
 * Token acquisition:
 *   {@link #getFreshToken(Context)} is a BLOCKING call and must be invoked
 *   from a background thread (e.g., inside an OkHttp interceptor or AsyncTask).
 *   It automatically refreshes expired tokens via GoogleAuthUtil.
 */
public class UserSession {

    private static final String TAG = "UserSession";
    private static UserSession instance;

    private GoogleSignInAccount account;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    // ── GoogleSignInClient factory ────────────────────────────────────────────

    /**
     * Build a GoogleSignInClient configured for YouTube access.
     * Pass the result to {@link com.google.android.gms.auth.api.signin.GoogleSignIn#getSignedInAccountFromIntent}.
     */
    public static GoogleSignInClient buildSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(Constants.OAUTH_WEB_CLIENT_ID)
                // Request the full YouTube scope so we can call YouTube Data API v3
                .requestScopes(new Scope(Constants.YOUTUBE_SCOPE))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    // ── Session state ─────────────────────────────────────────────────────────

    /**
     * Call after a successful Google Sign-In to store the account.
     */
    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }

    /**
     * Restore a previously-signed-in account on app launch.
     * Returns true if a valid account was found.
     */
    public boolean restoreFromLastSignIn(Context context) {
        GoogleSignInAccount last = GoogleSignIn.getLastSignedInAccount(context);
        if (last != null) {
            account = last;
            return true;
        }
        return false;
    }

    public boolean isSignedIn() {
        return account != null;
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    /** Display name, e.g. "Jenghau" */
    public String getDisplayName() {
        return account != null ? account.getDisplayName() : "";
    }

    /** Email address */
    public String getEmail() {
        return account != null ? account.getEmail() : "";
    }

    /** URL of the user's Google profile photo (may be null) */
    public String getPhotoUrl() {
        if (account != null && account.getPhotoUrl() != null) {
            return account.getPhotoUrl().toString();
        }
        return null;
    }

    /**
     * Sign the user out and clear the stored account.
     * Call on the UI thread; the actual revocation is async.
     */
    public void signOut(Context context, Runnable onComplete) {
        GoogleSignInClient client = buildSignInClient(context);
        client.signOut().addOnCompleteListener(task -> {
            account = null;
            if (onComplete != null) onComplete.run();
        });
    }

    // ── Token acquisition ─────────────────────────────────────────────────────

    /**
     * Returns a valid OAuth 2.0 access token for the YouTube scope.
     *
     * BLOCKING — must be called from a background thread.
     * GoogleAuthUtil handles caching and silent refresh automatically.
     *
     * @return Bearer token string, or null if not signed in / error
     */
    public String getFreshToken(Context context) {
        if (account == null) {
            Log.w(TAG, "getFreshToken: no account signed in");
            return null;
        }
        Account androidAccount = account.getAccount();
        if (androidAccount == null) {
            Log.w(TAG, "getFreshToken: account.getAccount() returned null");
            return null;
        }
        try {
            String scope = "oauth2:" + Constants.YOUTUBE_SCOPE;
            return GoogleAuthUtil.getToken(context.getApplicationContext(), androidAccount, scope);
        } catch (Exception e) {
            Log.e(TAG, "getFreshToken failed: " + e.getMessage(), e);
            return null;
        }
    }
}
