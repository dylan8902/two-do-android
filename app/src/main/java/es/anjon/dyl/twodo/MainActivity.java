package es.anjon.dyl.twodo;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import es.anjon.dyl.twodo.models.List;
import es.anjon.dyl.twodo.models.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private User mUser;
    private FloatingActionButton mFab;
    private NavigationView mNavigationView;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addList();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Login
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            Intent intent = new Intent(this, GoogleSignInActivity.class);
            startActivity(intent);
        } else {
            mUser = new User(fbUser);
            updateUI();
        }
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
            Intent intent = new Intent(this, GoogleSignInActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_new_list) {
            // New List
        } else if (id == R.id.nav_pair) {
            // Start pair Activity
        } else if (id == R.id.nav_account) {
            Intent intent = new Intent(this, GoogleSignInActivity.class);
            startActivity(intent);
        } else {
            // Setup listeners for list based on ID
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
        List list = new List("Test List", items);
        mDb.collection(mUser.getListsCollectionPath()).add(list);
    }

    /**
     * Based on the state of the user, update the navigation UI and handlers
     */
    private void updateUI() {
        if (mUser == null) {
            return;
        }

        View header = mNavigationView.getHeaderView(0);
        TextView nameView = (TextView) header.findViewById(R.id.name);
        nameView.setText(mUser.getName());
        TextView emailView = (TextView) header.findViewById(R.id.email);
        emailView.setText(mUser.getEmail());

        final SubMenu menu = mNavigationView.getMenu().getItem(0).getSubMenu();

        mDb.collection(mUser.getListsCollectionPath())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    List list = dc.getDocument().toObject(List.class);
                                    list.setId(dc.getDocument().getId());
                                    Log.d(TAG, "New list: " + list);
                                    menu.add(Menu.NONE, 1, 1, list.getTitle()).setCheckable(true);
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified list: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed list: " + dc.getDocument().getData());
                                    break;
                            }
                        }
                    }
                });
    }

}
