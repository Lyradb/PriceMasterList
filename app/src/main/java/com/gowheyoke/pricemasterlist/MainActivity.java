package com.gowheyoke.pricemasterlist;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String PREF_LOG = "loginses";
    public static final String PREF_USERID = "userid";
    public static final String PREF_PWD = "pwd";
    public static final String PREF_URI = "host_uri";
    public static final String PREF_LOGOUT = "is_logout";
    public static final String PREF_LOADING_DATA = "is_loading_data";
    public static final String PREF_IMPORT_FILE = "file_path";
    public static final String ACTION_CLOSE = "com.gowheyoke.pricemasterlist.ACTION_CLOSE";
    public static final String ACTION_LOGIN = "com.gowheyoke.pricemasterlist.ACTION_LOGIN";
    public static final String ACTION_ADMIN = "com.gowheyoke.pricemasterlist.ACTION_ADMIN";
    public static final String INTERSTITIALAD_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final Integer CREDIT_EXCHANGE = 10;
    public static final Integer CREDIT_POINTS = 5;
    public static String deviceId;
    public String host_uri;
    SharedPreferences sharedpreferences;
    View.OnClickListener attemptLogin;
    View.OnClickListener fabOnClickListner;
    DialogInterface.OnClickListener syncOnClickListner;
    private FirstReceiver firstReceiver;
    private PriceListDBAdapter dbHelper;
    private Runnable r;
    private Runnable backR;
    private Handler hRecCount;
    private Runnable rRecCount;
    private Handler backHandler;
    private Handler logOutHandle;
    private Runnable logOutRun;
    private ArrayList<String[]> list;
    private Integer nExit = 0;
    private InterstitialAd mInterstitialAd;
    private Handler adHandle;
    private Runnable adRun;
    private Handler adTHandle;
    private Runnable adTRun;
    private ProgressDialog progress;
    private Firebase ref;
    //private Menu mnuTopMenuActionBar_;

    //getting unique id for device
    //String device_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    private Boolean doneTimer = false;
    private Integer credit_earned = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Firebase.setAndroidContext(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(INTERSTITIALAD_ID);
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                credit_earned += CREDIT_POINTS;
            }

            @Override
            public void onAdClosed() {
                try {
                    adTHandle.removeCallbacks(adTRun);
                    requestNewInterstitial();
                    Log.d("DB", "doneTimer: " + doneTimer);
                    adCheckTimer();

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        requestNewInterstitial();

        //For Ad button to show
        adHandle = new Handler();
        adRun = new Runnable() {
            @Override
            public void run() {
                Button btnShowAd = (Button) findViewById(R.id.btnShowAd);
                if (mInterstitialAd.isLoaded()) {
                    btnShowAd.setVisibility(View.VISIBLE);
                } else {
                    btnShowAd.setVisibility(View.GONE);
                    requestNewInterstitial();
                }
                adHandle.postDelayed(adRun, 4000);
            }
        };

        adHandle.postDelayed(adRun, 2000);

        adTHandle = new Handler();
        adTRun = new Runnable() {
            @Override
            public void run() {
                doneTimer = true;
            }
        };

        //For Ad button to show
        logOutHandle = new Handler();
        logOutRun = new Runnable() {
            @Override
            public void run() {

                try {
                    sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
                    Boolean isLogout = sharedpreferences.getBoolean(PREF_LOGOUT, true);
                    String userId = sharedpreferences.getString(PREF_USERID, null);

                    if (!isLogout) {
                        Button btnUserAccess = (Button) findViewById(R.id.btn_userAccess);
                        Button btnimportCSV = (Button) findViewById(R.id.btn_importCSV);
                        TextView viewAdmin = (TextView) findViewById(R.id.textViewAdmin);

                        if (dbHelper.isAdmin(userId)) {
                            btnUserAccess.setVisibility(View.VISIBLE);
                            btnimportCSV.setVisibility(View.VISIBLE);
                            viewAdmin.setVisibility(View.VISIBLE);
                        }
                    } else {
                        logOutHandle.postDelayed(logOutRun, 1000);
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        };

        //logOutHandle.postDelayed(logOutRun, 1000);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            MenuItem logout = (MenuItem) findViewById(R.id.action_logout);
            logout.setVisible(false);
        }
        try {
            sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(PREF_LOGOUT, true);
            editor.putBoolean(PREF_LOADING_DATA, false);
            editor.apply();
            host_uri = sharedpreferences.getString(PREF_URI, null);

            dbHelper = new PriceListDBAdapter(this);
            dbHelper.open();
            dbHelper.initConfig();
            displayAdCredit();
            countRecords();
            IntentFilter filter = new IntentFilter(ACTION_CLOSE);
            firstReceiver = new FirstReceiver();
            registerReceiver(firstReceiver, filter);
            if (dbHelper.UserCount() < 1) {
                IntentFilter newfilter = new IntentFilter(ACTION_LOGIN);
                registerReceiver(firstReceiver, newfilter);
                Intent newLogin = new Intent(getApplicationContext(), CreateLoginActivity.class);
                newLogin.putExtra("rowID", 0);
                startActivityForResult(newLogin, 5);
            } else {
                IntentFilter newfilter = new IntentFilter(ACTION_ADMIN);
                registerReceiver(firstReceiver, newfilter);
                Intent login = new Intent(getApplicationContext(), Login.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, login, Intent.FILL_IN_ACTION);
                pendingIntent.send();
                //startActivityForResult(login,5);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.toString().trim(), Toast.LENGTH_SHORT).show();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            fab.show();
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm_logout(view);
            }
        });

        syncOnClickListner = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButt) {
                //Creating firebase object
                ref = new Firebase(Config.FIREBASE_URL + "data");
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Cursor cursor = dbHelper.fetchAllPriceList();
                                    cursor.moveToFirst();
                                    int i = 0;
                                    while (cursor.moveToNext()) {
                                        i++;
                                        Product product = new Product();
                                        //Adding values
                                        Log.d("_xxxbarcode_", "onOptionsItemSelected: " + cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)));
                                        Log.d("_xxxmake_", "onOptionsItemSelected: " + cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_MAKE)));
//                                product.setBarcode(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)).toString().trim());
                                        product.setMake(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_MAKE)).toString().trim());
                                        product.setItemDesc(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_ITEMDESC)).toString().trim());
                                        product.setUnit(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_UNIT)).toString().trim());
                                        product.setQty(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_QTY)).toString().trim());
                                        product.setWsp(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_WSP)).toString().trim());
                                        product.setRsp(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_RSP)).toString().trim());

                                        //Storing values to firebase
                                        ref.child("products").child(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)).toString().trim()).setValue(product);
                                        progress.setProgress(i + 1);

                                    }
                                    progress.dismiss();
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), e.toString().trim(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                );
            }
        };

        fabOnClickListner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout(v);
                try {
                    IntentFilter filter = new IntentFilter(ACTION_CLOSE);
                    IntentFilter adminFilter = new IntentFilter(ACTION_ADMIN);
                    firstReceiver = new FirstReceiver();
                    registerReceiver(firstReceiver, filter);
                    registerReceiver(firstReceiver, adminFilter);
                    Intent login = new Intent(getApplicationContext(), Login.class);
                    //startActivityForResult(login,5);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, login, Intent.FILL_IN_ACTION);
                    pendingIntent.send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        //To exit for BackPressed 2 times
        backHandler = new Handler();
        backR = new Runnable() {
            @Override
            public void run() {
                nExit = 0;
            }
        };

        //Logout due to inactivity
        Handler handler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                try {
                    sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
                    Boolean isLogOut = sharedpreferences.getBoolean(PREF_LOGOUT, true);
                    if (!isLogOut) {
                        logout(getCurrentFocus());
                        IntentFilter adminFilter = new IntentFilter(ACTION_ADMIN);
                        firstReceiver = new FirstReceiver();
                        registerReceiver(firstReceiver, adminFilter);
                        Intent intent = new Intent(getApplicationContext(), Login.class);
                        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, Intent.FILL_IN_ACTION);
                        pi.send();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Get Number of Records
        hRecCount = new Handler();
        rRecCount = new Runnable() {
            @Override
            public void run() {
                countRecords();
                displayAdCredit();
                //invalidateOptionsMenu();
                hRecCount.postDelayed(rRecCount, 4000);
            }
        };

        hRecCount.postDelayed(rRecCount, 4000);
        /*
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) this);
        */
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /*
    public void onPause() {
        super.onPause();
        handler.postDelayed(r, 180000); // 3 minutes delay
    }

    public void onResume() {
        super.onResume();
        handler.removeCallbacks(r);
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //mnuTopMenuActionBar_ = menu;
        return true;
    }
    /*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
        String user = sharedpreferences.getString(PREF_USERID, null);
        MenuItem mAdminAcces = menu.findItem(R.id.action_admin_access);
        mAdminAcces.setVisible(dbHelper.getAccesslevel(user) == "Admin");
        return true;
    }
    */

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    public void openAd(View view) {
        if (mInterstitialAd.isLoaded()) {
            adTHandle.removeCallbacks(adTRun);
            adTHandle.postDelayed(adTRun, 10000);
            mInterstitialAd.show();
        } else {
            Button btnShowAd = (Button) findViewById(R.id.btnShowAd);
            btnShowAd.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, "Ad is not loaded\nCheck your internet.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            progress = ProgressDialog.show(file_import.this,
//                "Checking/Caching File", s.toString() + " file...Please wait, it may take long.", true);
//            progress.setCancelable(true);
//            progress.show();
            progress = new ProgressDialog(MainActivity.this);
            progress.setCancelable(true);
            progress.setMessage("Ready to Sync ...");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setButton(DialogInterface.BUTTON_POSITIVE, "Sync NOW", syncOnClickListner);
            progress.setProgress(0);
            progress.setMax(dbHelper.PriceListCount());
            progress.show();
//            progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(false);
//            try {
//                if (host_uri != null && host_uri.length() > 0) {
//                    if (isInternetAvailable(host_uri)) {
//                        getData();
//                    } else {
//                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                        builder.setMessage("Sorry,\nYou are disconnected.\nPlease Retry again later.")
//                                .setCancelable(false)
//                                .setPositiveButton("OK",
//                                        new DialogInterface.OnClickListener() {
//                                            public void onClick(DialogInterface dialog, int id) {
//                                                //do things
//                                            }
//                                        });
//                        AlertDialog alert = builder.create();
//                        alert.show();
//                    }
//                }
//            } catch (Exception e) {
//                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
//            }
        }

        if (id == R.id.action_host) {
            final EditText input = new EditText(MainActivity.this);
            input.setText(host_uri);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Setup your host")
                    .setView(input)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String uri = input.getText().toString().trim();
                            if (!isInternetAvailable(uri)) {
                                Toast.makeText(getBaseContext(), "Invalid host", Toast.LENGTH_SHORT).show();
                            } else {
                                sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(MainActivity.PREF_URI, uri);
                                editor.apply();
                                host_uri = uri;
                            }
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if (id == R.id.action_credit_rule) {
            TextView creditRule = new TextView(MainActivity.this);
            creditRule.setText(getText(R.string.credit_info));
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(creditRule)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //do things
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if (id == R.id.action_change_pwd) {
            //startActivity(new Intent(getApplicationContext(), CreateLoginActivity.class));

            sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
            String user = sharedpreferences.getString(PREF_USERID, null);

            LayoutInflater ll = getLayoutInflater();
            View sv = ll.inflate(R.layout.change_password, null);
            final EditText mUserID = (EditText) sv.findViewById(R.id.userid);
            mUserID.setText(user);
            mUserID.setEnabled(false);

            final EditText mPwd = (EditText) sv.findViewById(R.id.password);
            mPwd.requestFocus();
            final EditText mNewPwd = (EditText) sv.findViewById(R.id.newpassword);
            final EditText mVerifyNewPwd = (EditText) sv.findViewById(R.id.verifynewpassword);
            mVerifyNewPwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.id.login || id == EditorInfo.IME_NULL) {
                        attemptLogin.onClick(textView);
                        return true;
                    }
                    return false;
                }
            });

            final AlertDialog alert;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(sv)
                    .setTitle("Change Password")
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    })
                    .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            }
                    );
            alert = builder.create();
            alert.show();

            attemptLogin = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Boolean cancel = false;
                        View focusView = null;
                        String userid = mUserID.getText().toString().trim();
                        String pwd = mPwd.getText().toString().trim();
                        String newPwd = mNewPwd.getText().toString().trim();
                        String verifyNewPwd = mVerifyNewPwd.getText().toString().trim();

                        if (!newPwd.equals(verifyNewPwd)) {
                            mVerifyNewPwd.setError("New Password does not Match.");
                            focusView = mVerifyNewPwd;
                            cancel = true;
                        }

                        if (!dbHelper.validateUser(userid, pwd)) {
                            mPwd.setError("Current Password is invalid.");
                            focusView = mPwd;
                            cancel = true;
                        }

                        if (dbHelper.validateUser(userid, pwd) && pwd.equals(newPwd)) {
                            mNewPwd.setError("New Password must not be same to current password.");
                            focusView = mNewPwd;
                            cancel = true;
                        }

                        if (verifyNewPwd.length() < 1) {
                            mVerifyNewPwd.setError("Confirm New Password is blank.");
                            focusView = mVerifyNewPwd;
                            cancel = true;
                        }

                        if (newPwd.length() < 1) {
                            mNewPwd.setError("New Password is blank.");
                            focusView = mNewPwd;
                            cancel = true;
                        }

                        if (pwd.length() < 1) {
                            mPwd.setError("Password is blank.");
                            focusView = mPwd;
                            cancel = true;
                        }

                        if (cancel) {
                            focusView.requestFocus();
                        } else {
                            dbHelper.updateUser(userid, verifyNewPwd);
                            Toast.makeText(MainActivity.this, "Password is Change.", Toast.LENGTH_SHORT).show();
                            alert.dismiss();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    attemptLogin.onClick(v);
                }
            });

        }

        if (id == R.id.action_import_product) {
            startActivityForResult(new Intent(getApplicationContext(), file_import.class), 5);
        }

        if (id == R.id.action_logout) {
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                fab.callOnClick();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        nExit++;
        if (nExit > 1) {
            sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
            Boolean isLoadingData = sharedpreferences.getBoolean(PREF_LOADING_DATA, false);
            if (!isLoadingData) {
                //handler.removeCallbacks(r);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.clear();
                editor.apply();
                finish();
            } else {
                Toast.makeText(this, "Loading Data on progress (See notification).\n" +
                        "Exit is not allowed at the moment.\nPlease try again later.", Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, "Press BACK again to Close.", Toast.LENGTH_SHORT).show();
        backHandler.postDelayed(backR, 2000);
    }

    public void clickSearch(View view) {
        startActivityForResult(new Intent(this, SearchProduct.class), 5);
    }

    public void clickUserAccess(View view) {
        startActivityForResult(new Intent(getApplicationContext(), UserAccess.class), 5);
    }

    public void clickImport(View view) {
        startActivityForResult(new Intent(getApplicationContext(), file_import.class), 5);
    }

    public void clickCloudImport(View view) {
        Log.d("_xxError_", "clickCloudImport: " + 1);
        Log.d("_xxError_", "clickCloudImport: " + 4);
        progress = new ProgressDialog(view.getContext());
        progress.setCancelable(true);
        progress.setTitle("Importing From Cloud");
        progress.setMessage("Checking Data... Please wait.");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        progress.setProgress(0);
        progress.setMax(100);
        progress.show();
        progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(false);
//
//        Log.d("_xxError_", "clickCloudImport: "+2);
//
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d("_xxError_", "clickCloudImport: " + 3);
                        dbHelper = new PriceListDBAdapter(MainActivity.this);
                        dbHelper.open();
                        Cursor cursor = dbHelper.fetchUserAccessByID(1);
                        String emailId = cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_EMAIL));
                        Firebase ref = new Firebase(Config.FIREBASE_URL);
                        Query queryRef = ref.child("web").child("data").child("email").child(emailId.replace(".", "")).orderByChild("product");
                        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.getChildrenCount() > 0) {
                                    progress.dismiss();
                                    progress = new ProgressDialog(MainActivity.this);
                                    progress.setCancelable(true);
                                    progress.setTitle("Importing From Cloud");
                                    progress.setMessage("Checking Data... Please wait.");
                                    progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    progress.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    progress.setProgress(0);
                                    progress.setMax(100);
                                    progress.show();
                                    progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(false);
                                    Boolean first = true;
                                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                        Log.d("_xxSsnapshot_", "onDataChange: " + postSnapshot.getChildrenCount());
                                        Log.d("_xxSnapshot_", "onDataChange: " + postSnapshot.getValue());
                                        final DataSnapshot childSnapshot = postSnapshot;
                                        if (first) {
                                            progress.setMax((int) childSnapshot.getChildrenCount());
                                            first = false;
                                        }
                                        new Thread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Integer x = 0;
                                                        for (DataSnapshot data : childSnapshot.getChildren()) {
                                                            Log.d("_xxsnapshot_", "onDataChange: " + data.getKey() + data.getValue());
                                                            progress.setMessage("Importing..." + data.getKey());
                                                            String barcode = data.child(PriceListDBAdapter.KEY_BARCODE).getValue().toString();
                                                            String make = data.child(PriceListDBAdapter.KEY_MAKE).getValue().toString();
                                                            String item_desc = data.child(PriceListDBAdapter.KEY_ITEMDESC).getValue().toString();
                                                            String unit = data.child(PriceListDBAdapter.KEY_UNIT).getValue().toString();
                                                            String qty = data.child(PriceListDBAdapter.KEY_QTY).getValue().toString();
                                                            String wsp = data.child(PriceListDBAdapter.KEY_WSP).getValue().toString();
                                                            String rsp = data.child(PriceListDBAdapter.KEY_RSP).getValue().toString();

                                                            Cursor cursor = dbHelper.fetchPriceListByItemDesc(data.getKey());
                                                            if (cursor.getCount() > 0) { //record exists
                                                                dbHelper.updateProduct(barcode, make, item_desc, unit, qty, wsp, rsp);
                                                            } else { //record not found
                                                                dbHelper.createPriceList(barcode, make, item_desc, unit, qty, wsp, rsp);
                                                            }
                                                            progress.setProgress(++x);
                                                        }
                                                    }
                                                }
                                        ).start();
                                    }
                                    progress.setMessage("Completed.");
                                } else {
                                    progress.setMessage("No update available.");
                                }
                                Log.d("_xxError_", "clickCloudImport: " + 6);
                                progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(true);
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Log.d("_xxError_", "clickCloudImport: " + firebaseError.toString());
                            }
                        });
                    }
                }
        ).start();
    }

    public void clickCloudExport(View view) {
        Log.d("_xxxx_", "clickCloudExport: " + 0);
        if (isInternetAvailable(Config.FIREBASE_URL)) {
            Log.d("_xxxx_", "clickCloudExport: " + 1);

            progress = new ProgressDialog(view.getContext());
            progress.setCancelable(true);
            progress.setTitle("Exporting to Cloud");
            Log.d("_xxxx_", "clickCloudExport: " + 3);
            progress.setMessage("In Progress..");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            progress.setProgress(0);
            dbHelper = new PriceListDBAdapter(this);
            dbHelper.open();
            progress.setMax(dbHelper.PriceListCount());
            progress.show();
            progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(false);

            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.d("_xxxx_", "clickCloudExport: " + 4);
                            Cursor cursor = dbHelper.fetchUserAccessByID(1);
                            String emailId = cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_EMAIL));
                            Firebase ref = new Firebase(Config.FIREBASE_URL);
                            Log.d("_xxxx_", "clickCloudExport: " + cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_EMAIL)));

                            try {
                                cursor = dbHelper.fetchAllPriceList();
                                Log.d("_xxxx_", "clickCloudExport: " + 5 + cursor.getCount());
                                cursor.moveToFirst();
                                int y = (cursor.getCount() / 1000) + ((cursor.getCount() % 1000) > 0 ? 1 : 0);
                                int start = 0;
                                for (int x = 1; x <= y; x++) {
                                    int end = 1000 * x;
                                    for (int i = start; i < end; i++) {
//                                    for (int i = 0; i <= cursor.getCount(); i++) {
                                        Log.d("_xxxx_", "clickCloudExport: " + 6 + i);
                                        Map<String, String> product = new HashMap<String, String>();
                                        product.put(PriceListDBAdapter.KEY_BARCODE, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)));
                                        product.put(PriceListDBAdapter.KEY_MAKE, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_MAKE)));
                                        product.put(PriceListDBAdapter.KEY_ITEMDESC, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_ITEMDESC)));
                                        product.put(PriceListDBAdapter.KEY_UNIT, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_UNIT)));
                                        product.put(PriceListDBAdapter.KEY_QTY, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_QTY)));
                                        product.put(PriceListDBAdapter.KEY_WSP, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_WSP)));
                                        product.put(PriceListDBAdapter.KEY_RSP, cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_RSP)));
                                        ref.child("web").child("data").child("email").child(emailId.replace(".", "")).child("products").child(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE))).setValue(product);
                                        cursor.moveToNext();
                                        if (i == (cursor.getCount() - 1)) {
//                                                mBuilder.setSound(uri);
                                            break;
                                        }
                                    }
                                    progress.setProgress(end);
                                    start = end;
                                }
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Error: " + e.toString(), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                            Log.d("_xxxx_", "clickCloudExport: " + 7);
                            progress.setMessage("Completed");
                            progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    }
            ).start();
        }
    }

    public void clickDelete(View view) {
        Toast.makeText(this, "You clicked DELETE.", Toast.LENGTH_LONG).show();
    }

    public void clickUpdate(View view) {
        Toast.makeText(this, "You clicked UPDATE DATA.", Toast.LENGTH_LONG).show();
    }

    public void countRecords() {
        int recordCount = dbHelper.PriceListCount();
        TextView textViewRecordCount = (TextView) findViewById(R.id.textViewRecordCount);
        if (recordCount > 0)
            textViewRecordCount.setText(recordCount + " records");
        else
            textViewRecordCount.setText("No Record Available");
    }

    public void displayAdCredit() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd hhmmss");
        dateFormatter.setLenient(false);
        Date today = new Date();
        String s = dateFormatter.format(today);

        String val1 = dbHelper.ConfigValue("TransCounter");
        String val = dbHelper.ConfigValue("Credit Count");

        if (isInternetAvailable(Config.FIREBASE_URL)) {
            Firebase ref = new Firebase(Config.FIREBASE_URL + "web/data/DeviceInfo");
            Map<String, Object> deviceInfo = new HashMap<String, Object>();
            deviceInfo.put("Email", MainActivity.deviceId);
            deviceInfo.put("TransDate", s);
            deviceInfo.put("TransCounter", val1);
            deviceInfo.put("Credit Count", val);
            ref.child("devices").child(MainActivity.deviceId).updateChildren(deviceInfo);
        }

        TextView textViewRecordCount = (TextView) findViewById(R.id.availCredit);
        if (Integer.valueOf(val) > 0)
            textViewRecordCount.setText("Available Credit: " + val + "\nSee Credit Rule");
        else
            textViewRecordCount.setText("No Credit Available");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(firstReceiver);
    }

    public void adCheckTimer() {
        Log.d("DB", "AdCheckTimer");
        if (doneTimer) {
            dbHelper.addCredit(credit_earned);
            displayAdCredit();
            Toast.makeText(MainActivity.this, "Congratulations!\nYou just earned " + credit_earned + " CREDITS.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Sorry, No CREDIT earned.\nYou did not finish watching.", Toast.LENGTH_SHORT).show();
        }
        credit_earned = 0;
        doneTimer = false;
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Main Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://com.gowheyoke.pricemasterlist/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        //handler.removeCallbacks(r);
//        //backHandler.removeCallbacks(backR);
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Main Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://com.gowheyoke.pricemasterlist/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
//        client.disconnect();
//    }

    public void confirm_logout(View view) {
        sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
        String user = sharedpreferences.getString(PREF_USERID, null);

        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("CONFIRM LOG-OUT").setMessage("Are you sure, Log-out user " + user + "?")
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                })
                .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout(getCurrentFocus());
                        try {
                            TextView viewAdmin = (TextView) findViewById(R.id.textViewAdmin);
                            Button btnUserAccess = (Button) findViewById(R.id.btn_userAccess);
                            Button btnImportCSV = (Button) findViewById(R.id.btn_importCSV);
                            Button btnCloudExport = (Button) findViewById(R.id.btnCloudExport);
                            viewAdmin.setVisibility(View.GONE);
                            btnUserAccess.setVisibility(View.GONE);
                            btnImportCSV.setVisibility(View.GONE);
                            btnCloudExport.setVisibility(View.GONE);
                            IntentFilter filter = new IntentFilter(ACTION_CLOSE);
                            IntentFilter adminFilter = new IntentFilter(ACTION_ADMIN);
                            firstReceiver = new FirstReceiver();
                            registerReceiver(firstReceiver, filter);
                            registerReceiver(firstReceiver, adminFilter);
                            Intent login = new Intent(getApplicationContext(), Login.class);
                            //startActivityForResult(login,5);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, login, Intent.FILL_IN_ACTION);
                            pendingIntent.send();
                            //logOutHandle.postDelayed(logOutRun, 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        alert = builder.create();
        alert.show();

        /*
        Snackbar.make(view, "Log-out user " + user + "?", Snackbar.LENGTH_LONG)
                .setAction("CONFIRM", fabOnClickListner)
                .setActionTextColor(Color.RED)
                .show();
        */
    }
//
//    public void syncOnClickListner() {
//        //Creating firebase object
//        ref = new Firebase(Config.FIREBASE_URL + "data");
//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Cursor cursor = dbHelper.fetchAllPriceList();
//                            cursor.moveToFirst();
//                            int i = 0;
//                            while (cursor.moveToNext()) {
//                                i++;
//                                Product product = new Product();
//                                //Adding values
//                                Log.d("_xxxbarcode_", "onOptionsItemSelected: " + cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)));
//                                Log.d("_xxxmake_", "onOptionsItemSelected: " + cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_MAKE)));
////                                product.setBarcode(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)).toString().trim());
//                                product.setMake(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_MAKE)).toString().trim());
//                                product.setItemDesc(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_ITEMDESC)).toString().trim());
//                                product.setUnit(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_UNIT)).toString().trim());
//                                product.setQty(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_QTY)).toString().trim());
//                                product.setWsp(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_WSP)).toString().trim());
//                                product.setRsp(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_RSP)).toString().trim());
//
//                                //Storing values to firebase
//                                ref.child("products").child(cursor.getString(cursor.getColumnIndex(PriceListDBAdapter.KEY_BARCODE)).toString().trim()).setValue(product);
//                                progress.setProgress(i + 1);
//
//                            }
//                            progress.dismiss();
//                        } catch (Exception e) {
//                            Toast.makeText(getApplicationContext(), e.toString().trim(), Toast.LENGTH_LONG).show();
//                        }
//                    }
//                }
//        );
//
//        //Value event listener for realtime data update
//        ref.addValueEventListener(new ValueEventListener() {
//
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
//                    //Getting the data from snapshot
//                    Product product = postSnapshot.getValue(Product.class);
//                }
//            }
//
//            @Override
//            public void onCancelled(FirebaseError firebaseError) {
//                System.out.println("The read failed: " + firebaseError.getMessage());
//            }
//        });
//    }

    public void logout(View view) {
        SharedPreferences sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(PREF_USERID, null);
        editor.putString(PREF_PWD, null);
        editor.putBoolean(PREF_LOGOUT, true);
        editor.apply();
    }

    private void getData() {
        String url = "http://" + host_uri + Config.URL_GET_PRODUCT_LIST;
        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (updateData(response)) {
                    processList();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public void processList() {
        if (list.get(0).length != 7) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            AlertDialog alert;
            builder.setMessage("Validation Error:\nYour source of CSV PRODUCT DATA\n has invalid column count.")
                    .setCancelable(false)
                    .setPositiveButton("CANCEL", null);
            alert = builder.create();
            alert.show();
        } else {
            Toast.makeText(getBaseContext(), "See progress at notification.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FILL_IN_ACTION);
            final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setContentTitle("Product Data Update")
                    .setContentText("Update is in progress")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.price_masterlist)
                    .setContentIntent(pendingIntent);
            dbHelper = new PriceListDBAdapter(this);
            dbHelper.open();
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            CheckBox delAll = (CheckBox) findViewById(R.id.checkDelData);
                            RadioButton rproduct = (RadioButton) findViewById(R.id.radioProduct);
                            if (rproduct.isChecked()) {
                                if (delAll.isChecked())
                                    dbHelper.deleteAllPriceList();
                                for (int i = 0; i < list.size(); i++) {
                                    mBuilder.setProgress(list.size(), i, false);
                                    String barcode = list.get(i)[0];
                                    String make = list.get(i)[1];
                                    String item_desc = list.get(i)[2];
                                    String unit = list.get(i)[3];
                                    String qty = list.get(i)[4];
                                    String wprice = list.get(i)[5];
                                    String rprice = list.get(i)[6];
                                    dbHelper.createPriceList(barcode, make, item_desc, unit, qty, wprice, rprice);
                                    mNotifyManager.notify(0, mBuilder.build());
                                }
                            } else {

                            }
                            // When the loop is finished, updates the notification
                            mBuilder.setContentText("Update is Complete")
                                    // Removes the progress bar
                                    .setProgress(0, 0, false);
                            mNotifyManager.notify(0, mBuilder.build());
                            mNotifyManager.cancel(0);
                        }
                    }

// Starts the thread by calling the run() method in its Runnable
            ).start();
            try {
                Object sbservice = getSystemService("statusbar");
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method showsb;
                if (Build.VERSION.SDK_INT >= 17) {
                    showsb = statusbarManager.getMethod("expandNotificationsPanel");
                } else {
                    showsb = statusbarManager.getMethod("expand");
                }
                showsb.invoke(sbservice);
            } catch (Exception e) {
                e.printStackTrace();
            }

            finish();
        }
    }

    private Boolean updateData(String response) {
        Boolean ret = false;
        JSONObject jsonObject;
        try {
            list = new ArrayList<String[]>();
            jsonObject = new JSONObject(response);
            JSONArray result = jsonObject.getJSONArray(Config.TAG_JSON_ARRAY);

            String[][] arrjo = {};
            for (int i = 0; i < result.length(); i++) {
                JSONObject jo = result.getJSONObject(i);
                arrjo[i][0] = jo.getString(Config.TAG_BARCODE);
                arrjo[i][1] = jo.getString(Config.TAG_MAKE);
                arrjo[i][2] = jo.getString(Config.TAG_ITEM_DESC);
                arrjo[i][3] = jo.getString(Config.TAG_UNIT);
                arrjo[i][4] = jo.getString(Config.TAG_QTY);
                arrjo[i][5] = jo.getString(Config.TAG_WPRICE);
                arrjo[i][6] = jo.getString(Config.TAG_RPRICE);
                list.add(arrjo[i]);
                //dbHelper.createPriceList(barcode,make,item_desc,unit,qty,wprice,rprice);
            }
            ret = true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean isInternetAvailable(String uri) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        /*
        try {
            InetAddress ipAddr = InetAddress.getByName(uri.trim()); //You can replace it with your name

            return ipAddr.toString().trim().length()>0;

        } catch (Exception e) {
            return false;
        }
        */
    }

    class FirstReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("FirstReceiver", "FirstReceiver");
            if (intent.getAction().equals(ACTION_CLOSE)) {
                MainActivity.this.finish();
            }
            if (intent.getAction().equals(ACTION_ADMIN)) {
                try {
                    sharedpreferences = getSharedPreferences(PREF_LOG, Context.MODE_PRIVATE);
                    String userId = sharedpreferences.getString(PREF_USERID, null);

                    Button btnUserAccess = (Button) findViewById(R.id.btn_userAccess);
                    Button btnimportCSV = (Button) findViewById(R.id.btn_importCSV);
                    Button btnCloudExport = (Button) findViewById(R.id.btnCloudExport);
                    TextView viewAdmin = (TextView) findViewById(R.id.textViewAdmin);
                    if (dbHelper.isAdmin(userId)) {
                        btnUserAccess.setVisibility(View.VISIBLE);
                        btnimportCSV.setVisibility(View.VISIBLE);
                        btnCloudExport.setVisibility(View.VISIBLE);
                        viewAdmin.setVisibility(View.VISIBLE);
                    } else {
                        btnUserAccess.setVisibility(View.INVISIBLE);
                        btnimportCSV.setVisibility(View.INVISIBLE);
                        btnCloudExport.setVisibility(View.INVISIBLE);
                        viewAdmin.setVisibility(View.INVISIBLE);
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            if (intent.getAction().equals(ACTION_LOGIN)) {
                try {
                    Toast.makeText(MainActivity.this, "Congratulation!.\nYou are now a registered user.", Toast.LENGTH_SHORT).show();
                    IntentFilter filter = new IntentFilter(ACTION_CLOSE);
                    IntentFilter adminFilter = new IntentFilter(ACTION_ADMIN);
                    firstReceiver = new FirstReceiver();
                    registerReceiver(firstReceiver, filter);
                    registerReceiver(firstReceiver, adminFilter);
                    Intent login = new Intent(getApplicationContext(), Login.class);
                    //startActivityForResult(login,5);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, login, Intent.FILL_IN_ACTION);
                    pendingIntent.send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


