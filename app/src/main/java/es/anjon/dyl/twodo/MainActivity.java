package es.anjon.dyl.twodo;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import es.anjon.dyl.twodo.models.ListItem;
import es.anjon.dyl.twodo.models.Pair;
import es.anjon.dyl.twodo.models.TwoDoList;
import es.anjon.dyl.twodo.models.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ListAdapter.OnItemCheckedListener, ListAdapter.OnEditItemClickedListener {

    private static final String TAG = "MainActivity";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private User mUser;
    private Pair mPair;
    private FloatingActionButton mFab;
    private NavigationView mNavigationView;
    private SubMenu mListMenu;
    private FirebaseFirestore mDb;
    private RecyclerView mListView;
    private ListAdapter mListAdapter;
    private List<ListItem> mListItems;
    private List<String> mListItemKeys;
    private ListenerRegistration mListsListener;
    private ListenerRegistration mListItemsListener;
    private Comparator<ListItem> mOrderBy;
    private DocumentReference mListRef;
    private CollectionReference mListItemsRef;

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
        mListView = (RecyclerView) findViewById(R.id.list);
        mListView.hasFixedSize();
        mListItems = new ArrayList<>();
        mListItemKeys = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mListView.setLayoutManager(layoutManager);
        mListAdapter = new ListAdapter(mListItems, this, this);
        mListView.setAdapter(mListAdapter);
        mListMenu = mNavigationView.getMenu().findItem(R.id.lists).getSubMenu();
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
        mOrderBy = ListItem.orderByPriority();
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
        } else if (id == R.id.action_order_by_title) {
            mOrderBy = ListItem.orderByTitle();
        } else if (id == R.id.action_order_by_priority) {
            mOrderBy = ListItem.orderByPriority();
        } else if (id == R.id.action_order_by_due_date) {
            mOrderBy = ListItem.orderByDueDate();
        } else if (id == R.id.action_delete_done) {
            deleteDoneItems();
            return true;
        } else if (id == R.id.action_delete_list) {
            deleteList();
            return true;
        }
        Collections.sort(mListItems, mOrderBy);
        mListAdapter.notifyDataSetChanged();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_new_list) {
            addListDialog();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else {
            loadList(item.getTitleCondensed().toString());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPause() {
        if (mListsListener != null) {
            mListsListener.remove();
        }
        if (mListItemsListener != null) {
            mListItemsListener.remove();
        }
        super.onPause();
    }

    @Override
    public void onItemChecked(int position) {
        ListItem item = mListItems.get(position);
        Log.i(TAG, "onItemChecked: " + item);
        mListItemsRef.document(item.getId()).set(item);
    }

    @Override
    public void onEditItemClicked(int position) {
        ListItem item = mListItems.get(position);
        Log.i(TAG, "onEditItemClicked: " + item);
        addEditListItemDialog(mListItemsRef, item);
    }

    /**
     * Creates a dialog to allow user to add list to pair
     */
    private void addListDialog() {
        if (mPair == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add list");
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_list, null);
        final EditText input = view.findViewById(R.id.list_title);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                TwoDoList twoDoList = new TwoDoList(input.getText().toString());
                mDb.collection(mPair.getListsCollectionPath()).add(twoDoList);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /**
     * Creates a dialog to allow user to enter list item
     * @param ref the collection to add the item to
     */
    private void addListItemDialog(final CollectionReference ref) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add to list");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_list_item, null);
        final EditText input = view.findViewById(R.id.list_item_title);
        final Spinner spinner = (Spinner) view.findViewById(R.id.priority_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        final Button addDueDateButton = view.findViewById(R.id.due_date);
        final Calendar dueDate = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener dueDateSet = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                dueDate.set(year, month, dayOfMonth);
                addDueDateButton.setText(DATE_FORMAT.format(dueDate.getTime()));
            }
        };
        final DatePickerDialog datePickerDialog = new DatePickerDialog(
                view.getContext(), dueDateSet,
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DATE));
        addDueDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String priority = spinner.getSelectedItem().toString();
                ListItem listItem = new ListItem(input.getText().toString(), priority, Boolean.FALSE);
                if (!addDueDateButton.getText().equals("yyyy-MM-dd")) {
                    listItem.setDueDate(dueDate.getTime());
                }
                ref.add(listItem);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /**
     * Creates a dialog to allow user to edit list item
     * @param ref the collection to save the item to
     * @param item the item to edit
     */
    private void addEditListItemDialog(final CollectionReference ref, final ListItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_list_item, null);
        final EditText input = view.findViewById(R.id.list_item_title);
        input.setText(item.getTitle());
        final Spinner spinner = (Spinner) view.findViewById(R.id.priority_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (item.getPrioirty() != null) {
            spinner.setSelection(adapter.getPosition(item.getPrioirty()));
        }
        final Button addDueDateButton = view.findViewById(R.id.due_date);
        final Calendar dueDate = Calendar.getInstance();
        if (item.getDueDate() != null) {
            dueDate.setTime(item.getDueDate());
            addDueDateButton.setText(DATE_FORMAT.format(dueDate.getTime()));
        }
        final DatePickerDialog.OnDateSetListener dueDateSet = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                dueDate.set(year, month, dayOfMonth);
                addDueDateButton.setText(DATE_FORMAT.format(dueDate.getTime()));
            }
        };
        final DatePickerDialog datePickerDialog = new DatePickerDialog(
                view.getContext(), dueDateSet,
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DATE));
        addDueDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                item.setPrioirty(spinner.getSelectedItem().toString());
                item.setTitle(input.getText().toString());
                if (!addDueDateButton.getText().equals("yyyy-MM-dd")) {
                    item.setDueDate(dueDate.getTime());
                }
                ref.document(item.getId()).set(item);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListItemsRef.document(item.getId()).delete();
            }
        });
        builder.show();
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
     * When the pair is available, add listener to update the lists
     */
    private void addListsListener() {
        if (mPair == null) {
            return;
        }
        mListMenu.clear();
        if (mListsListener != null) {
            mListsListener.remove();
        }
        mListsListener = mDb.collection(mPair.getListsCollectionPath()).addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                                    .setCheckable(true).setTitleCondensed(twoDoList.getId());
                            break;
                        case MODIFIED:
                            Log.d(TAG, "Modified twoDoList: " + dc.getDocument().getData());
                            mListMenu.findItem(twoDoList.hashCode()).setTitle(twoDoList.getId());
                            break;
                        case REMOVED:
                            Log.d(TAG, "Removed twoDoList: " + dc.getDocument().getData());
                            mListMenu.removeItem(twoDoList.hashCode());
                            break;
                    }
                }
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
                addListsListener();
            }
        });
    }

    /**
     * Load the list details from the database and add listeners
     * @param listId database key
     */
    private void loadList(String listId) {
        if ((listId == null) || (mPair == null)) {
            return;
        }
        //TODO add spinner while the list details load?
        mListItems.clear();
        mListItemKeys.clear();
        mListAdapter.notifyDataSetChanged();
        if (mListItemsListener != null) {
            mListItemsListener.remove();
        }
        mListRef = mDb.collection(mPair.getListsCollectionPath()).document(listId);
        mListItemsRef = mListRef.collection("items");
        mListItemsListener = mListItemsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                            Log.d(TAG, "New List Item: " + dc.getDocument().getData());
                            ListItem added = dc.getDocument().toObject(ListItem.class);
                            added.setId(dc.getDocument().getId());
                            mListItems.add(added);
                            Collections.sort(mListItems, mOrderBy);
                            mListItemKeys.add(added.getId());
                            mListAdapter.notifyDataSetChanged();
                            break;
                        case MODIFIED:
                            Log.d(TAG, "Modified List Item: " + dc.getDocument().getData());
                            ListItem modified = dc.getDocument().toObject(ListItem.class);
                            modified.setId(dc.getDocument().getId());
                            int index = mListItems.indexOf(modified);
                            if (index > -1) {
                                mListItems.set(index, modified);
                            } else {
                                Log.w(TAG, "Unknown list item :" + modified.getId());
                            }
                            Collections.sort(mListItems, mOrderBy);
                            mListAdapter.notifyDataSetChanged();
                            break;
                        case REMOVED:
                            Log.d(TAG, "Removed List Item: " + dc.getDocument().getData());
                            ListItem removed = dc.getDocument().toObject(ListItem.class);
                            removed.setId(dc.getDocument().getId());
                            int itemIndex = mListItems.indexOf(removed);
                            if (itemIndex > -1) {
                                mListItems.remove(removed);
                                mListItemKeys.remove(removed);
                                mListAdapter.notifyItemRemoved(itemIndex);
                            } else {
                                Log.w(TAG, "Unknown list item :" + removed.getId());
                            }
                            break;
                    }
                }

            }
        });
        mFab.setEnabled(true);
        mFab.setClickable(true);
        mFab.setAlpha(1.0f);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addListItemDialog(mListItemsRef);
            }
        });
    }

    /**
     * Delete the items that have been marked as done
     */
    private void deleteDoneItems() {
        for (ListItem item : mListItems) {
            if (item.getChecked()) {
                mListItemsRef.document(item.getId()).delete();
            }
        }
    }

    /**
     * Delete the list and clear the list items
     */
    private void deleteList() {
        mFab.setEnabled(false);
        mFab.setClickable(false);
        mFab.setAlpha(0.2f);
        if (mListItemsListener != null) {
            mListItemsListener.remove();
        }
        mListItems.clear();
        mListItemKeys.clear();
        mListAdapter.notifyDataSetChanged();
        if (mListRef != null) {
            mListRef.delete();
        }
    }

}
