package demo.pratiked.cognitoauth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    private final static int RC_SIGN_IN = 1;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.identity_pool_id),
                Regions.AP_NORTHEAST_1);

    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return intent;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {}

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            Log.i(TAG, "Successfully signed in to Google!");
            GoogleSignInAccount account = result.getSignInAccount();

            Map<String, String> logins = new HashMap<>();
            logins.put(getString(R.string.google_provider), account.getIdToken());
            credentialsProvider.setLogins(logins);

            new GetCognitoCredentialsTask(this).execute();
        } else {
            Log.e(TAG, "Failed to sign in to Google.");
            Log.e(TAG, "Error code: " + result.getStatus().getStatusCode());
        }
    }

    class GetCognitoCredentialsTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;

        public GetCognitoCredentialsTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                credentialsProvider.getIdentityId();
                Log.i(TAG, "Successfully got credentials from Cognito!");

                Log.i(TAG, "Start MainActivity!");
                this.activity.startActivity(MainActivity.createIntent(this.activity));
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Failed to get credentials from Cognito.");
                e.printStackTrace();
                return null;
            }
        }
    }
}
