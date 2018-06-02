package es.anjon.dyl.twodo;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.UUID;

import es.anjon.dyl.twodo.models.User;

import static android.nfc.NdefRecord.createMime;

public class SettingsActivity extends Activity implements
        View.OnClickListener, NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "SettingsActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final String MIME_TYPE = "application/vnd.es.anjon.dyl.twodo";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView mNameTextView;
    private TextView mEmailTextView;
    private TextView mPairingIdTextView;
    private NfcAdapter mNfcAdapter;
    private String mPairingId;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Views
        mNameTextView = findViewById(R.id.name);
        mEmailTextView = findViewById(R.id.email);
        mPairingIdTextView = findViewById(R.id.pairing_id);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        findViewById(R.id.pair_button).setOnClickListener(this);
        findViewById(R.id.unpair_button).setOnClickListener(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available for pairing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            mUser = new User(mAuth.getCurrentUser());
        }
        updateUI();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                updateUI();
            }
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefMessage msg = new NdefMessage(new NdefRecord[] {
            createMime(MIME_TYPE, mPairingId.getBytes()),
            NdefRecord.createApplicationRecord("es.anjon.dyl.twodo")
        });
        return msg;
    }

    /**
     * Authenticate to Firebase with Google Account
     * @param acct Google Account
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            mUser = new User(mAuth.getCurrentUser());
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI();
                        }
                    }
                });
    }

    /**
     * Sign in with Google
     */
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Sign out of Google
     */
    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mUser = null;
                        updateUI();
                    }
                });
    }

    /**
     * Revoke access to Google
     */
    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mUser = null;
                        updateUI();
                    }
                });
    }

    /**
     * Generate the pairing id (firebase uid)
     * Add a listener for document to wait for pairing complete
     */
    private void pair() {
        mPairingId = UUID.randomUUID().toString();
        updateUI();
        mNfcAdapter.setNdefPushMessageCallback(this, this);
    }

    /**
     * Update UI with user information
     */
    private void updateUI() {
        if (mUser != null) {
            mNameTextView.setText(mUser.getName());
            mEmailTextView.setText(mUser.getEmail());
            mPairingIdTextView.setText(mUser.getPairingId());
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
            if (mUser.isPaired()) {
                findViewById(R.id.pair_button).setVisibility(View.GONE);
                findViewById(R.id.unpair_button).setVisibility(View.VISIBLE);
                findViewById(R.id.pair_instructions).setVisibility(View.GONE);
            } else if (mPairingId != null) {
                mPairingIdTextView.setText(mPairingId);
                findViewById(R.id.pair_button).setVisibility(View.GONE);
                findViewById(R.id.unpair_button).setVisibility(View.GONE);
                findViewById(R.id.pair_instructions).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.pair_button).setVisibility(View.VISIBLE);
                findViewById(R.id.unpair_button).setVisibility(View.GONE);
                findViewById(R.id.pair_instructions).setVisibility(View.GONE);
            }
        } else {
            mNameTextView.setText(null);
            mEmailTextView.setText(null);
            mPairingIdTextView.setText(null);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            findViewById(R.id.pair_button).setVisibility(View.GONE);
            findViewById(R.id.unpair_button).setVisibility(View.GONE);
            findViewById(R.id.pair_instructions).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            signIn();
        } else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.disconnect_button) {
            revokeAccess();
        } else if (i == R.id.pair_button) {
            pair();
        } else if (i == R.id.unpair_button) {
            // TODO Removing pairing
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            mUser = new User(mAuth.getCurrentUser());
        }
        updateUI();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        mPairingId = new String(msg.getRecords()[0].getPayload());
        if (mUser != null) {
            mUser.setPairingId(mPairingId);
            //TODO save user to database
            updateUI();
        } else {
            Toast.makeText(this, "Please sign in before pairing", Toast.LENGTH_LONG).show();
        }
    }

}
