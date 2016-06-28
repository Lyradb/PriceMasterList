package com.gowheyoke.pricemasterlist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class Login extends AppCompatActivity {

    EditText userid;
    EditText pwd;
    String val_userid;
    String val_pwd;
    SharedPreferences sharedpreferences;
    private PriceListDBAdapter dbHelper;
    private int nExit = 0;
    private Handler handler;
    private Runnable r;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");

        AdView mAdView = (AdView) findViewById(R.id.adView);
//        mAdView.setAdSize(AdSize.BANNER);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("ED123B22C4C2AB888AB1234D51E43446")
                .build();
        mAdView.loadAd(adRequest);

        EditText pwd = (EditText) findViewById(R.id.pwd);
        pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    click_login(getCurrentFocus());
                    return true;
                }
                return false;
            }
        });

        dbHelper = new PriceListDBAdapter(this);
        dbHelper.open();

        handler = new Handler();

        r = new Runnable() {
            public void run() {
                nExit = 0;
            }
        };

        userid = (EditText) findViewById(R.id.userid);
        userid.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(pwd, InputMethodManager.SHOW_IMPLICIT);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        nExit++;
        if (nExit > 1) {
            sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
            Boolean isLoadingData = sharedpreferences.getBoolean(MainActivity.PREF_LOADING_DATA, false);
            if (!isLoadingData) {
                Intent myIntent = new Intent(MainActivity.ACTION_CLOSE);
                sendBroadcast(myIntent);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.clear();
                editor.apply();
                finish();
            } else {
                Toast.makeText(this, "Loading Data on progress (See notification)." +
                        "\nExit is not allowed at the moment.\nPlease try again later.", Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, "Press BACK again to Close.", Toast.LENGTH_SHORT).show();
        handler.postDelayed(r, 2000);
    }

    public void click_login(View view) {
        userid = (EditText) findViewById(R.id.userid);
        pwd = (EditText) findViewById(R.id.pwd);

        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
        builder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });

        val_userid = userid.getText().toString().trim();
        val_pwd = pwd.getText().toString().trim();

        if (val_userid == null || val_userid.length() < 1) {
            builder.setMessage("USER ID must not be blank");
            alert = builder.create();
            alert.show();
            Toast.makeText(this, "USER ID must not be blank", Toast.LENGTH_SHORT).show();
            userid.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(userid, InputMethodManager.SHOW_IMPLICIT);
        } else {
            if (val_pwd == null || val_pwd.length() < 1) {
                builder.setMessage("PASSWORD must not be blank");
                alert = builder.create();
                alert.show();
                Toast.makeText(this, "PASSWORD must not be blank", Toast.LENGTH_SHORT).show();
                pwd.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(pwd, InputMethodManager.SHOW_IMPLICIT);
            } else {
                try {
                    if (dbHelper.validateUser(val_userid, val_pwd)) {
                        sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(MainActivity.PREF_USERID, val_userid)
                                .putString(MainActivity.PREF_PWD, val_pwd)
                                .putBoolean(MainActivity.PREF_LOGOUT, false)
                                .apply();
                        Intent myIntent = new Intent(MainActivity.ACTION_ADMIN);
                        sendBroadcast(myIntent);
                        finish();
                    } else {
                        builder.setMessage("Invalid USER ID or PASSWORD.");
                        alert = builder.create();
                        alert.show();
                        Toast.makeText(this, "Invalid USER ID or PASSWORD.", Toast.LENGTH_SHORT).show();
                        userid.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(userid, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
