package es.anjon.dyl.twodo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import es.anjon.dyl.twodo.models.Pair;
import es.anjon.dyl.twodo.models.TwoDoList;
import es.anjon.dyl.twodo.models.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private User mUser;
    private Pair mPair;
    private FloatingActionButton mFab;
    private NavigationView mNavigationView;
    private SubMenu mListMenu;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mListMenu = mNavigationView.getMenu().findItem(R.id.lists).getSubMenu();
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        mUser = new User(fbUser);
        updateUserUI();

        SharedPreferences sharedPrefs = getSharedPreferences(Pair.SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        loadPair(sharedPrefs.getString(Pair.SHARED_PREFS_KEY, null));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_new_list) {
            // New list
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else {
            // Update main content
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Add a test list to the pair collection
     */
    private void addList() {
        Map<String, Boolean> items = new HashMap<>();
        items.put("Do the shopping", Boolean.FALSE);
        TwoDoList twoDoList = new TwoDoList("Test TwoDoList", items);
        mDb.collection(mPair.getListsCollectionPath()).add(twoDoList);
    }

    /**
     * When the user is logged in, update the navigation UI and handlers
     */
    private void updateUserUI() {
        if (mUser == null) {
            return;
        }

        final View header = mNavigationView.getHeaderView(0);
        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });

        TextView nameView = (TextView) header.findViewById(R.id.name);
        nameView.setText(mUser.getName());
        TextView emailView = (TextView) header.findViewById(R.id.email);
        emailView.setText(mUser.getEmail());
    }

    /**
     * When the pair is available, update the lists
     */
    private void updatePairUI() {
        if (mPair == null) {
            return;
        }
        mDb.collection(mPair.getListsCollectionPath()).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "listen:error", e);
                    return;
                }

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    TwoDoList twoDoList = dc.getDocument().toObject(TwoDoList.class);
                    twoDoList.setId(dc.getDocument().getId());
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d(TAG, "New twoDoList: " + twoDoList);
                            mListMenu.add(Menu.NONE, twoDoList.hashCode(), 1, twoDoList.getTitle())
                                    .setCheckable(true);
                            break;
                        case MODIFIED:
                            Log.d(TAG, "Modified twoDoList: " + dc.getDocument().getData());
                            mListMenu.findItem(twoDoList.hashCode()).setTitle(twoDoList.getTitle());
                            break;
                        case REMOVED:
                            Log.d(TAG, "Removed twoDoList: " + dc.getDocument().getData());
                            mListMenu.removeItem(twoDoList.hashCode());
                            break;
                    }
                }
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //addList();
            }
        });
    }

    /**
     * Load the pair details from the database
     * @param pairId database key
     */
    private void loadPair(String pairId) {
        if (pairId == null) {
            return;
        }
        //TODO add spinner while the pair details load?
        DocumentReference docRef = mDb.collection(Pair.COLLECTION_NAME).document(pairId);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                mPair = doc.toObject(Pair.class);
                mPair.setId(doc.getId());
                updatePairUI();
            }
        });
    }

}
