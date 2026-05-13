package com.learn2sing.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/**
 * Login screen shown when no Google account is signed in.
 *
 * OAuth setup reminder:
 *   Make sure you have set Constants.OAUTH_WEB_CLIENT_ID to your Web Client ID
 *   from Google Cloud Console, and that the SHA-1 fingerprint of your debug/release
 *   keystore is registered as an Android OAuth client in that same project.
 *
 *   To get the SHA-1 of your debug key:
 *     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
 *             -storepass android -keypass android
 */
public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient signInClient;
    private ProgressBar        progressBar;
    private Button             btnSignIn;
    private TextView           tvError;

    // Modern Activity Result API — replaces deprecated onActivityResult
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = findViewById(R.id.progress_login);
        btnSignIn   = findViewById(R.id.btn_google_sign_in);
        tvError     = findViewById(R.id.tv_login_error);

        signInClient = UserSession.buildSignInClient(this);

        btnSignIn.setOnClickListener(v -> startSignIn());
    }

    // ── Sign-in flow ──────────────────────────────────────────────────────────

    private void startSignIn() {
        setLoading(true);
        tvError.setVisibility(View.GONE);
        signInLauncher.launch(signInClient.getSignInIntent());
    }

    private void handleSignInResult(ActivityResult result) {
        setLoading(false);
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            onSignInSuccess(account);
        } catch (ApiException e) {
            onSignInFailure(e);
        }
    }

    private void onSignInSuccess(GoogleSignInAccount account) {
        UserSession.getInstance().setAccount(account);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void onSignInFailure(ApiException e) {
        String msg;
        switch (e.getStatusCode()) {
            case 10:
                msg = "Developer error: check your OAuth client ID and SHA-1 fingerprint in Google Cloud Console.";
                break;
            case 12501:
                msg = "Sign-in cancelled.";
                break;
            case 7:
                msg = "Network error — check your internet connection.";
                break;
            default:
                msg = "Sign-in failed (code " + e.getStatusCode() + "). See README for setup.";
        }
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
        btnSignIn.setAlpha(loading ? 0.5f : 1.0f);
    }
}
