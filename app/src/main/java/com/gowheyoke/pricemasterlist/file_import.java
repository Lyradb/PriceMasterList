package com.gowheyoke.pricemasterlist;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;

public class file_import extends AppCompatActivity {

    SharedPreferences sharedpreferences;
    private EditText edtFilePath;
    private PriceListDBAdapter dbHelper;
    private List<String[]> list;
    private String[] next;
    private String fname;
    private Boolean firstRun = false;
    private Handler adHandle;
    private Runnable adRun;
    private Handler hRecCount;
    private Runnable rRecCount;
    private InterstitialAd mInterstitialAd;
    private Handler adTHandle;
    private Runnable adTRun;
    private Boolean doneTimer = false;
    private Integer credit_earned = 0;
    private int notifyID = 1;
    private int numMessages = 0;
    private ProgressDialog progress, progress_complete;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_import);
//        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6342826475120276~4024844544");

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(MainActivity.INTERSTITIALAD_ID);
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                credit_earned += MainActivity.CREDIT_POINTS;
            }

            @Override
            public void onAdClosed() {
                try {
                    adTHandle.removeCallbacks(adTRun);
                    requestNewInterstitial();
                    Log.d("DB", "doneTimer: " + doneTimer);
                    adCheckTimer();

                } catch (Exception e) {
                    Toast.makeText(file_import.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        requestNewInterstitial();

        dbHelper = new PriceListDBAdapter(this);
        dbHelper.open();

        displayAdCredit();

        adTHandle = new Handler();
        adTRun = new Runnable() {
            @Override
            public void run() {
                doneTimer = true;
            }
        };

        // Get Number of Records
        hRecCount = new Handler();
        rRecCount = new Runnable() {
            @Override
            public void run() {
                displayAdCredit();
                hRecCount.postDelayed(rRecCount, 4000);
            }
        };

        hRecCount.postDelayed(rRecCount, 4000);

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

        RadioGroup rproduct = (RadioGroup) findViewById(R.id.radio_select);
        rproduct.check(R.id.radioProduct);
        setTitle("Import File");
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public void clickOpenFile(View view) {
        try {
            firstRun = true;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath());
            intent.setDataAndType(uri, "text/csv");
            startActivityForResult(Intent.createChooser(intent, "Select CSV File"), 1);
        } catch (Exception e) {
            Toast.makeText(this, e.toString().trim(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            next = new String[0];
            list = new ArrayList<String[]>();
            Uri uri_res = data.getData();
            fname = getRealPathFromURI(uri_res).trim();
            if (requestCode == 1 && resultCode == RESULT_OK) {
                String ext = fname.substring(fname.lastIndexOf(".") + 1, fname.length());
                Log.d("_debug_", "onActivityResult: " + ext);
                if (!ext.toLowerCase().trim().equals("csv")) {
                    Toast.makeText(file_import.this, "File is not CSV format.", Toast.LENGTH_SHORT).show();
                } else {
                    File f = new File(fname);
                    String s = "Processing: ";
                    long Filesize = f.length() / 1024;
                    if (Filesize >= 1024)
                        s += Filesize / 1024 + " Mb";
                    else
                        s += Filesize + " Kb";
                    final ProgressDialog progresRing = ProgressDialog.show(file_import.this,
                            "Checking/Caching File", s.toString() + " file...Please wait, it may take long.", true);
                    progresRing.setCancelable(true);
                    progresRing.show();
                    Log.d("_clickOk_", "clickOk");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d("_clickOk_", "Try");
                                Log.d("_clickOk_", "Filename: " + fname);
                                FileReader file = new FileReader(fname);
                                CSVReader reader = new CSVReader(file);
                                int ctr = 0;
                                while ((next = reader.readNext()) != null) {
                                    if (next.length != 7) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(file_import.this);
                                        AlertDialog alert;
                                        builder.setMessage("Validation Error:\nYour source of CSV PRODUCT DATA\n has invalid column count.")
                                                .setCancelable(false)
                                                .setPositiveButton("CANCEL", null);
                                        alert = builder.create();
                                        alert.show();
                                        next = null;
                                    }
                                    list.add(next);
                                    Log.d("_clickOk_", "Next: " + next[0]);
                                    ctr++;
                                    if ((ctr % 500) == 0) {
                                        Log.d("_clickOk_", "Thread: " + next[0]);
                                        Thread.sleep(100);
                                    }
                                }
                            } catch (FileNotFoundException e) {
                                Toast.makeText(file_import.this, "The file is missing!", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(file_import.this, "The file is invalid or may be corrupted!", Toast.LENGTH_SHORT).show();
                            } catch (InterruptedException e) {
                                Toast.makeText(file_import.this, "The file is invalid or may be corrupted!", Toast.LENGTH_SHORT).show();
                            }
                            progresRing.dismiss();
                        }
                    }).start();
                    firstRun = false;
                }
            }

            edtFilePath = (EditText) findViewById(R.id.import_path);
            Button btnImport = (Button) findViewById(R.id.btnImportNow);
            if (edtFilePath.getText().toString().trim().length() > 0 || fname.trim().length() > 0) {
                Cursor curCredit = dbHelper.getConfig("Credit Count");
                int currentCredit = curCredit.getInt(curCredit.getColumnIndexOrThrow("value"));
                if ((currentCredit * MainActivity.CREDIT_EXCHANGE) < list.size()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(file_import.this);
                    AlertDialog alert;
                    builder.setTitle("Insufficient Credit!")
                            .setMessage("You need " +
                                    Math.abs((list.size() - (currentCredit * MainActivity.CREDIT_EXCHANGE)) * MainActivity.CREDIT_EXCHANGE) +
                                    " Credits.\nClick the \"Watch Add Now!\" to replenish.")
                            .setCancelable(false)
                            .setPositiveButton("CANCEL", null);
                    alert = builder.create();
                    alert.show();
                    btnImport.setEnabled(false);
                } else
                    btnImport.setEnabled(true);
            } else
                btnImport.setEnabled(false);
            if (fname.trim().length() > 0)
                edtFilePath.setText(fname);
        } catch (NullPointerException e) {
            Toast.makeText(file_import.this, "No selected file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(file_import.this, "Error Found: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("ED123B22C4C2AB888AB1234D51E43446")
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
            Toast.makeText(file_import.this, "Ad is not loaded\nCheck your internet.", Toast.LENGTH_SHORT).show();
        }
    }

    public void clickClose(View view) {
        finish();
    }

    public void clickOk(View view) {

        if (list.get(0).length != 7) {
            AlertDialog.Builder builder = new AlertDialog.Builder(file_import.this);
            AlertDialog alert;
            builder.setMessage("Validation Error:\nYour source of CSV PRODUCT DATA\n has invalid column count.")
                    .setCancelable(false)
                    .setPositiveButton("CANCEL", null);
            alert = builder.create();
            alert.show();
        } else {
//            Toast.makeText(getBaseContext(), "See progress at notification.", Toast.LENGTH_LONG).show();
//            final Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            Intent intent = new Intent(this, MainActivity.class);
//            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FILL_IN_ACTION);
//            final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
//            mBuilder.setContentTitle("Product Data Update")
//                    .setContentText("Update is in progress")
//                    .setOngoing(true)
//                    .setSmallIcon(R.drawable.price_masterlist)
//                    .setContentIntent(pendingIntent);

            progress_complete = new ProgressDialog(view.getContext());
            progress_complete.setCancelable(true);
            progress_complete.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress_complete.setMessage("File Import Completed.");
            progress_complete.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            progress_complete.setProgress(0);
            progress_complete.setMax(list.size());
            progress_complete.show();

            progress = new ProgressDialog(view.getContext());
            progress.setCancelable(true);
            progress.setMessage("Importing File ...");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //doing Nothing
                }
            });
            progress.setProgress(0);
            progress.setMax(list.size());
            progress.show();
            progress.getButton(ProgressDialog.BUTTON_POSITIVE).setEnabled(false);
            dbHelper = new PriceListDBAdapter(this);
            dbHelper.open();
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sharedpreferences = getSharedPreferences(MainActivity.PREF_LOG, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putBoolean(MainActivity.PREF_LOADING_DATA, true);
                            editor.apply();
                            CheckBox delAll = (CheckBox) findViewById(R.id.checkDelData);
                            RadioButton rproduct = (RadioButton) findViewById(R.id.radioProduct);
                            if (rproduct.isChecked()) {
                                if (delAll.isChecked())
                                    dbHelper.deleteAllPriceList();
                                if (list.size() <= 1000) {
                                    for (int i = 0; i < list.size(); i++) {
//                                        mBuilder.setProgress(list.size(), i, false);
//                                        if (i == (list.size() - 1))
//                                            mBuilder.setSound(uri);
                                        String barcode = list.get(i)[0];
                                        String make = list.get(i)[1];
                                        String item_desc = list.get(i)[2];
                                        String unit = list.get(i)[3];
                                        String qty = list.get(i)[4];
                                        String wprice = list.get(i)[5];
                                        String rprice = list.get(i)[6];
                                        dbHelper.createPriceList(barcode, make, item_desc, unit, qty, wprice, rprice);
                                        dbHelper.TransCounter();
                                        progress_complete.setProgress(i + 1);
                                        progress.setProgress(i + 1);
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            Toast.makeText(file_import.this, "Somethings Wrong with the delay", Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
//                                        mNotifyManager.notify(0, mBuilder.build());
                                    }
                                } else {
                                    int y = (list.size() / 1000) + ((list.size() % 1000) > 0 ? 1 : 0);
                                    int start = 0;
                                    for (int x = 1; x <= y; x++) {
                                        int end = 1000 * x;
                                        dbHelper.beginTransaction();
                                        SQLiteStatement insert = dbHelper.initInsert();
                                        for (int i = start; i < end; i++) {
//                                            mBuilder.setProgress(list.size(), i, false);
                                            insert.bindString(1, list.get(i)[0]);
                                            insert.bindString(2, list.get(i)[1]);
                                            insert.bindString(3, list.get(i)[2]);
                                            insert.bindString(4, list.get(i)[3]);
                                            insert.bindString(5, list.get(i)[4]);
                                            insert.bindString(6, list.get(i)[5]);
                                            insert.bindString(7, list.get(i)[6]);
                                            insert.execute();
                                            dbHelper.TransCounter();
//                                            mNotifyManager.notify(0, mBuilder.build());
                                            if (i == (list.size() - 1)) {
//                                                mBuilder.setSound(uri);
                                                break;
                                            }
                                        }
                                        dbHelper.setTransactionSuccessful();
                                        dbHelper.endTransaction();
                                        progress_complete.setProgress(end);
                                        progress.setProgress(end);
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            Toast.makeText(file_import.this, "Somethings Wrong with the delay", Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
                                        start = end;
                                    }
                                }
                            }
                            // When the loop is finished, updates the notification
//                            mNotifyManager.notify(0, mBuilder.build());
//                            mNotifyManager.cancel(0);

                            /*
                            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            NotificationManager cNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            NotificationCompat.Builder cBuilder = new NotificationCompat.Builder(file_import.this);
                            cBuilder.setContentTitle("Product Data Update")
                                    .setContentText("Update is in completed")
                                    .setSound(uri)
                                    .setNumber(++numMessages);
                            cNotifyManager.notify(notifyID, mBuilder.build());
                            */
                            editor.putBoolean(MainActivity.PREF_LOADING_DATA, false);
                            editor.apply();
                            progress.dismiss();
//                            progress_complete.show();
//                            finish();
                        }
                    }

// Starts the thread by calling the run() method in its Runnable
            ).start();
//            try {
//                Object sbservice = getSystemService("statusbar");
//                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
//                Method showsb;
//                if (Build.VERSION.SDK_INT >= 17) {
//                    showsb = statusbarManager.getMethod("expandNotificationsPanel");
//                } else {
//                    showsb = statusbarManager.getMethod("expand");
//                }
//                showsb.invoke(sbservice);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            finish();
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public void displayAdCredit() {
        String val = dbHelper.ConfigValue("Credit Count");
        TextView textViewRecordCount = (TextView) findViewById(R.id.availCredit);
        if (Integer.valueOf(val) > 0)
            textViewRecordCount.setText("Available Credit: " + val + "\nSee Credit Rule");
        else
            textViewRecordCount.setText("No Credit Available");
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
//                "file_import Page", // TODO: Define a title for the content shown.
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
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "file_import Page", // TODO: Define a title for the content shown.
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

    public void adCheckTimer() {
        Log.d("DB", "AdCheckTimer");
        if (doneTimer) {
            dbHelper.addCredit(credit_earned);
            displayAdCredit();
            Toast.makeText(file_import.this, "Congratulations!\nYou just earned " + credit_earned + " CREDITS.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(file_import.this, "Sorry, No CREDIT earned.\nYou did not finish watching.", Toast.LENGTH_SHORT).show();
        }
        credit_earned = 0;
        doneTimer = false;
    }
}
