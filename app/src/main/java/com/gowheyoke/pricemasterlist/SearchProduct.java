package com.gowheyoke.pricemasterlist;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;

public class SearchProduct extends AppCompatActivity {

    private PriceListDBAdapter dbHelper;
    private SimpleCursorAdapter dataAdapter;
    private EditText myFilter;
    private Handler h;
    private Runnable r;
    private SharedPreferences sharedpreferences;
    private Cursor cursor;
    private String[] columns;
    private int[] to;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_product);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new PriceListDBAdapter(this);
        dbHelper.open();

        if (dbHelper.PriceListCount() < 1) {
            TextView noShow = (TextView) findViewById(R.id.no_rec);
            noShow.setVisibility(View.VISIBLE);
        }

        try {
            cursor = dbHelper.fetchAllPriceList();

            // The desired columns to be bound
            columns = new String[]{
                    PriceListDBAdapter.KEY_BARCODE,
                    PriceListDBAdapter.KEY_MAKE,
                    PriceListDBAdapter.KEY_ITEMDESC,
                    PriceListDBAdapter.KEY_UNIT,
                    PriceListDBAdapter.KEY_QTY,
                    PriceListDBAdapter.KEY_WSP,
                    PriceListDBAdapter.KEY_RSP,
                    PriceListDBAdapter.KEY_WSP
            };

            // the XML defined views which the data will be bound to
            to = new int[]{
                    R.id.barcode,
                    R.id.make,
                    R.id.itemdesc,
                    R.id.unit,
                    R.id.qty,
                    R.id.wsp,
                    R.id.rsp,
                    R.id.wsp2,
            };

            // create the adapter using the cursor pointing to the desired data
            //as well as the layout information
            dataAdapter = new SimpleCursorAdapter(
                    this, R.layout.pricelist_info,
                    cursor,
                    columns,
                    to,
                    0);

            dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                public Cursor runQuery(CharSequence constraint) {
                    try {
                        return dbHelper.fetchPriceListByItemDesc(constraint.toString());
                    } catch (Exception e) {
                        Toast.makeText(SearchProduct.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        return null;
                    }
                }
            });

            displayListView();
            handleIntent(getIntent());
        } catch (Exception e) {
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

    /*
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

        listView = (ListView) findViewById(R.id.listView);
        registerForContextMenu(listView);
        //listView = (SwipeMenuListView) findViewById(R.id.listView);
        // Assign adapter to ListView

        listView.setAdapter(dataAdapter);
        /*
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                // set item width
                openItem.setWidth(dp2px(90));
                // set item title
                openItem.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("wsp")));
                // set item title fontsize
                openItem.setTitleSize(18);
                // set item title font color
                openItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(openItem);
            }
        };

        listView.setMenuCreator(creator);
        listView.setCloseInterpolator(new BounceInterpolator());
        listView.setOpenInterpolator(new BounceInterpolator());
        */

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                                    int position, long id) {
                ((SwipeLayout) (listView.getChildAt(position - listView.getFirstVisiblePosition())))
                        .open(true);
                // Get the cursor, positioned to the corresponding row in the result set
                /*Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // Get the state's capital from this row in the database.
                String barcode =
                        cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                Toast.makeText(getApplicationContext(),
                        barcode, Toast.LENGTH_SHORT).show();
                        */

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
}
