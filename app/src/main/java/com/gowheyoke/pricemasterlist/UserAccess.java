package com.gowheyoke.pricemasterlist;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

public class UserAccess extends AppCompatActivity {

    public static final String ACTION_REFRESH = "com.gowheyoke.pricemasterlist.ACTION_REFRESH";
    private PriceListDBAdapter dbHelper;
    private SimpleCursorAdapter dataAdapter;
    private EditText myFilter;
    private Handler h;
    private Runnable r;
    private SharedPreferences sharedpreferences;
    private Cursor cursor;
    private String[] columns;
    private int[] to;
    private SwipeMenuListView listView;
    private FirstReceiver firstReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_access);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new PriceListDBAdapter(this);
        dbHelper.open();

        firstReceiver = new FirstReceiver();
        try {
            cursor = dbHelper.fetchAllUserAccess();

            // The desired columns to be bound
            columns = new String[]{
                    PriceListDBAdapter.KEY_ROWID,
                    PriceListDBAdapter.KEY_USERID,
                    PriceListDBAdapter.KEY_EMAIL,
                    PriceListDBAdapter.KEY_ACCESS_LEVEL,
                    "active_text",
                    PriceListDBAdapter.KEY_PWD,
            };

            // the XML defined views which the data will be bound to
            to = new int[]{
                    R.id.rec_id,
                    R.id.user_id,
                    R.id.email,
                    R.id.accesslevel,
                    R.id.active,
                    R.id.pwd,
            };

            // create the adapter using the cursor pointing to the desired data
            //as well as the layout information
            dataAdapter = new SimpleCursorAdapter(
                    this, R.layout.user_access,
                    cursor,
                    columns,
                    to,
                    0);

            dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                public Cursor runQuery(CharSequence constraint) {
                    try {
                        return dbHelper.fetchUserAccessByItem(constraint.toString());
                    } catch (Exception e) {
                        Toast.makeText(UserAccess.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        return null;
                    }
                }
            });

            displayListView();
            handleIntent(getIntent());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString().trim(), Toast.LENGTH_LONG).show();
        }

        //Logout due to inactivity
        h = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                try {
                    sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
                    Boolean isLogOut = sharedpreferences.getBoolean(MainActivity.PREF_LOGOUT, true);
                    if (!isLogOut) {
                        logout(getCurrentFocus());
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, Intent.FILL_IN_ACTION);
                        pi.send();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            fab.show();
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myFilter = (EditText) findViewById(R.id.myFilter);
                int nshow = (myFilter.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE;
                myFilter.setVisibility(nshow);
                myFilter.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (myFilter.getVisibility() == View.VISIBLE)
                    imm.showSoftInput(myFilter, InputMethodManager.SHOW_IMPLICIT);
                else
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            }
        });
    }

    public void clickNewUser(View view) {
        IntentFilter newfilter = new IntentFilter(ACTION_REFRESH);
        registerReceiver(firstReceiver, newfilter);
        Intent intent = new Intent(getApplicationContext(), CreateLoginActivity.class);
        intent.putExtra("rowID", 0);
        startActivity(intent);
    }
    /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(firstReceiver);
    }

    public void onPause() {
        super.onPause();
        h.postDelayed(r, 180000); // 3 minutes delay
    }

    public void onResume() {
        super.onResume();
        h.removeCallbacks(r);
    }
    */

    public void logout(View view) {
        sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(MainActivity.PREF_USERID, null);
        editor.putString(MainActivity.PREF_PWD, null);
        editor.apply();
        editor.putBoolean(MainActivity.PREF_LOGOUT, true);
        editor.apply();
    }

    private void displayListView() {

        listView = (SwipeMenuListView) findViewById(R.id.listView);
        listView.setAdapter(dataAdapter);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem editItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                editItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                // set item width
                editItem.setWidth(dp2px(90));
                // set a icon
                editItem.setIcon(android.R.drawable.ic_menu_edit);
                // set item title fontsize
                editItem.setTitleSize(18);
                // set item title font color
                editItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(editItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(dp2px(90));
                // set a icon
                deleteItem.setIcon(android.R.drawable.ic_menu_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };

// set creator
        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        TextView recId = (TextView) findViewById(R.id.rec_id);
                        Intent intent = new Intent(getBaseContext(), CreateLoginActivity.class);
                        intent.putExtra("rowID", Integer.parseInt(recId.getText().toString()));
                        startActivity(intent);
                        break;
                    case 1:
                        // delete
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });


        myFilter = (EditText) findViewById(R.id.myFilter);
        myFilter.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                dataAdapter.getFilter().filter(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                dataAdapter.getFilter().filter(s.toString());
            }
        });

        /*
        ImageButton btnEditUser = (ImageButton) listView.findViewById(R.id.btnEditUser);
        Toast.makeText(UserAccess.this, "displayListView btnEditUser", Toast.LENGTH_SHORT).show();
        btnEditUser.getRootView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(UserAccess.this, "displayListView", Toast.LENGTH_SHORT).show();
                int rowId = 1; //= dataAdapter.getCursor().getInt(dataAdapter.getCursor().getColumnIndexOrThrow(dbHelper.KEY_ROWID));
                Toast.makeText(UserAccess.this, "displayListView"+rowId, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(UserAccess.this, CreateLoginActivity.class);
                Toast.makeText(UserAccess.this, "displayListView intent", Toast.LENGTH_SHORT).show();
                intent.putExtra("rowID", rowId);
                Toast.makeText(UserAccess.this, "displayListView rowid", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        //searchView.setSearchableInfo(
        //        searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                dataAdapter.getFilter().filter(newText.trim());
                return false;
            }
        });

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }
    /*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.listView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_popup, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.action_edit:
                // edit stuff here
                return true;
            case R.id.action_delete:
                // remove stuff here
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    */

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String q = intent.getStringExtra(SearchManager.QUERY);
            /*
            try {
                dataAdapter.getFilter().filter(q.trim());
            }catch (Exception e){
                Toast.makeText(SearchProduct.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
           */
        }
    }

    class FirstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("FirstReceiver", "FirstReceiver ACTION_REFRESH");
            if (intent.getAction().equals(ACTION_REFRESH)) {
                listView = (SwipeMenuListView) findViewById(R.id.listView);
                listView.setAdapter(null);
                //unregisterForContextMenu(listView);

                try {
                    cursor = dbHelper.fetchAllUserAccess();

                    // The desired columns to be bound
                    columns = new String[]{
                            PriceListDBAdapter.KEY_ROWID,
                            PriceListDBAdapter.KEY_USERID,
                            PriceListDBAdapter.KEY_EMAIL,
                            PriceListDBAdapter.KEY_ACCESS_LEVEL,
                            "active_text",
                            PriceListDBAdapter.KEY_PWD,
                    };

                    // the XML defined views which the data will be bound to
                    to = new int[]{
                            R.id.rec_id,
                            R.id.user_id,
                            R.id.email,
                            R.id.accesslevel,
                            R.id.active,
                            R.id.pwd,
                    };

                    // create the adapter using the cursor pointing to the desired data
                    //as well as the layout information
                    dataAdapter = new SimpleCursorAdapter(
                            UserAccess.this, R.layout.user_access,
                            cursor,
                            columns,
                            to,
                            0);

                    dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                        public Cursor runQuery(CharSequence constraint) {
                            try {
                                return dbHelper.fetchUserAccessByItem(constraint.toString());
                            } catch (Exception e) {
                                Toast.makeText(UserAccess.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                return null;
                            }
                        }
                    });

                    displayListView();
                    handleIntent(getIntent());
                } catch (Exception e) {
                    Toast.makeText(UserAccess.this, e.toString().trim(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
