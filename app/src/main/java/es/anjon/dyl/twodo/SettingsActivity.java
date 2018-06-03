package es.anjon.dyl.twodo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import es.anjon.dyl.twodo.models.Pair;
import es.anjon.dyl.twodo.models.User;

public class SettingsActivity extends Activity implements
        View.OnClickListener, NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "SettingsActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView mNameTextView;
    private TextView mEmailTextView;
    private TextView mPairIdTextView;
    private TextView mPairNameTextView;
    private NfcAdapter mNfcAdapter;
    private Pair mPair;
    private User mUser;
    private SharedPreferences mSharedPrefs;
    private ListenerRegistration mPairListener;

    /**
     * Find views and setup button handlers. Check if NFC, close if not
     * @param savedInstanceState the previous saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSharedPrefs = getSharedPreferences(Pair.SHARED_PREFS_KEY, Context.MODE_PRIVATE);

        mNameTextView = findViewById(R.id.name);
        mEmailTextView = findViewById(R.id.email);
        mPairIdTextView = findViewById(R.id.pair_id);
        mPairNameTextView = findViewById(R.id.pair_name);

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
        mDb = FirebaseFirestore.getInstance();

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available for pairing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    /**
     * Load user and pair and update UI
     */
    @Override
    public void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            mUser = new User(mAuth.getCurrentUser());
            loadPair(mSharedPrefs.getString(Pair.SHARED_PREFS_KEY, null));
        }
        updateUI();
    }

    /**
     * Complete authentication if the Google Sign In is complete
     * @param requestCode the code to check to see if it is google's results
     * @param resultCode the code from the activity
     * @param data the data provied from the activity
     */
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

    /**
     * Create the NFC message to send based on the new Pair Request
     * @param event NFC event
     * @return the NFC message to beam with the Pair Request id
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return mPair.createNfcMessage();
    }

    /**
     * Button click handlers
     * @param v the view clicked
     */
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

    /**
     * Reload the user if there is one, if resumed due to pairing intent, process
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            mUser = new User(mAuth.getCurrentUser());
            // Check to see that the Activity started due to an Android Beam
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                processPairIntent(getIntent());
            }
        }
        updateUI();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Complete authentication with Google Account and update UI
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
                        destroy();
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
                        destroy();
                    }
                });
    }

    /**
     * Generate the pairing id (firebase uid) and update UI
     * Add a listener for document to wait for pairing complete
     */
    private void pair() {
        mPair = new Pair();
        mPair.setFrom(mUser);
        mDb.collection(Pair.COLLECTION_NAME).add(mPair)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference ref) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + ref.getId());
                        mPair.setId(ref.getId());
                        mNfcAdapter.setNdefPushMessageCallback(
                                SettingsActivity.this, SettingsActivity.this);
                        mSharedPrefs.edit().putString(Pair.SHARED_PREFS_KEY, mPair.getId()).apply();
                        addPairingListener();
                        updateUI();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding document", e);
                    }
                });
        updateUI();
    }

    /**
     * Load the pair details from the database and update UI
     * @param pairId database key
     */
    private void loadPair(String pairId) {
        if (pairId == null) {
            updateUI();
            return;
        }
        //TODO add spinner while the pair details load to prevent trying to pair again?
        DocumentReference docRef = mDb.collection(Pair.COLLECTION_NAME).document(pairId);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                mPair = doc.toObject(Pair.class);
                mPair.setId(doc.getId());
                updateUI();
            }
        });
    }

    /**
     * Process the received beam and update pair details with user and update UI
     * @param intent Intent with beam data
     */
    void processPairIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        mPair = new Pair(msg);
        mPair.setTo(mUser);
        mSharedPrefs.edit().putString(Pair.SHARED_PREFS_KEY, mPair.getId()).apply();
        updateUI();
        DocumentReference docRef = mDb.collection(Pair.COLLECTION_NAME).document(mPair.getId());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                Pair pair = doc.toObject(Pair.class);
                mPair.setFrom(pair.getFrom());
                mDb.collection(Pair.COLLECTION_NAME).document(mPair.getId()).set(mPair);
                updateUI();
            }
        });
    }

    /**
     * Destroy all user data and refresh the UI
     */
    private void destroy() {
        mPair = null;
        mUser = null;
        mSharedPrefs.edit().remove(Pair.SHARED_PREFS_KEY).apply();
        updateUI();
        return;
    }

    /**
     * Add listener for pairing data and update UI
     */
    private void addPairingListener() {
        mPairListener = mDb.document(mPair.getPath()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "listen:error", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    mPair = snapshot.toObject(Pair.class);
                    mPair.setId(snapshot.getId());
                    updateUI();
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    /**
     * Update UI with user and pair information
     */
    private void updateUI() {
        if (mUser != null) {
            // User is logged in
            mNameTextView.setText(mUser.getName());
            mEmailTextView.setText(mUser.getEmail());
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            // User is not logged in
            mNameTextView.setText(null);
            mEmailTextView.setText(null);
            mPairIdTextView.setText(null);
            mPairNameTextView.setText(null);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            findViewById(R.id.pair_button).setVisibility(View.GONE);
            findViewById(R.id.unpair_button).setVisibility(View.GONE);
            findViewById(R.id.pair_instructions).setVisibility(View.GONE);
            return;
        }
        if (mPair == null) {
            // No pairing underway
            findViewById(R.id.pair_button).setVisibility(View.VISIBLE);
            findViewById(R.id.unpair_button).setVisibility(View.GONE);
            findViewById(R.id.pair_instructions).setVisibility(View.GONE);
            mPairNameTextView.setText(null);
        } else if (mPair.isComplete()) {
            // Pairing complete
            findViewById(R.id.pair_button).setVisibility(View.GONE);
            findViewById(R.id.unpair_button).setVisibility(View.VISIBLE);
            findViewById(R.id.pair_instructions).setVisibility(View.GONE);
            mPairNameTextView.setText(mPair.getWith(mUser).getName());
        } else if (mPair.getId() == null) {
            // Starting to pair
            mPairIdTextView.setText("Generating Pairing ID");
            findViewById(R.id.pair_button).setVisibility(View.GONE);
            findViewById(R.id.unpair_button).setVisibility(View.GONE);
        } else {
            // Waiting to pair
            mPairIdTextView.setText(mPair.getId());
            findViewById(R.id.pair_button).setVisibility(View.GONE);
            findViewById(R.id.unpair_button).setVisibility(View.GONE);
            findViewById(R.id.pair_instructions).setVisibility(View.VISIBLE);
        }
    }

}
