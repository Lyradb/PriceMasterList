package com.gowheyoke.pricemasterlist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class CreateLoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private Handler backHandler;
    private Runnable backR;
    private int nExit = 0;
    private PriceListDBAdapter dbHelper = new PriceListDBAdapter(this);
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUserIdView;
    private EditText mPasswordView;
    private EditText mVerifyPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Spinner mAccessLevel;
    private CheckBox mUserActive;
    //private Button btnRegister = (Button) findViewById(R.id.email_sign_in_button);
    //private Button btnSave = (Button) findViewById(R.id.action_save_button);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_login);
        dbHelper = new PriceListDBAdapter(this);
        int editUserId = getIntent().getExtras().getInt("rowID");
        Toast.makeText(CreateLoginActivity.this, "ID " + editUserId, Toast.LENGTH_SHORT).show();

        dbHelper.open();
        try {
            if (dbHelper.UserCount() < 1) {
                setTitle("Create ADMIN Sign-In");
            } else {

                //btnRegister.setVisibility(View.GONE);
                //btnSave.setVisibility(View.VISIBLE);
                Log.e("CreateLOG", "onCreate: VISIBILITY");
                if (editUserId > 0) {
                    setTitle("Edit User");
                    if (editUserId > 0) {
                        Cursor cursor;
                        cursor = dbHelper.fetchUserAccessByID(editUserId);
                        String email = cursor.getString(cursor.getColumnIndexOrThrow(PriceListDBAdapter.KEY_EMAIL));
                        String UserId = cursor.getString(cursor.getColumnIndexOrThrow(PriceListDBAdapter.KEY_USERID));
                        mPasswordView.setVisibility(View.GONE);
                        mVerifyPasswordView.setVisibility(View.GONE);
                        mAccessLevel.setSelection(cursor.getString(cursor.getColumnIndexOrThrow(PriceListDBAdapter.KEY_ACCESS_LEVEL)) == "ADMIN" ? 1 : 2);
                        mUserActive.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(PriceListDBAdapter.KEY_ACTIVE)) > 0);
                    }

                } else
                    setTitle("New User");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                LinearLayout activelevel = (LinearLayout) findViewById(R.id.activelevel);
                activelevel.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CreateLOG", "onCreate: " + e.toString().trim());
            Toast.makeText(CreateLoginActivity.this, e.toString().trim(), Toast.LENGTH_SHORT).show();
        }

        //To exit for BackPressed 2 times
        backHandler = new Handler();
        backR = new Runnable() {
            @Override
            public void run() {
                nExit = 0;
            }
        };

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mUserIdView = (AutoCompleteTextView) findViewById(R.id.userid);
        mPasswordView = (EditText) findViewById(R.id.password);
        mVerifyPasswordView = (EditText) findViewById(R.id.verifypassword);
        mAccessLevel = (Spinner) findViewById(R.id.userlevel);
        mUserActive = (CheckBox) findViewById(R.id.useractive);
        mVerifyPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mUserIdView.setError(null);
        mVerifyPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String userid = mUserIdView.getText().toString();
        String password = mPasswordView.getText().toString().trim();
        String verifypassword = mVerifyPasswordView.getText().toString().trim();
        String accesslevel = mAccessLevel.getSelectedItem().toString().trim();
        Boolean useractive = mUserActive.isChecked();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(verifypassword)) {
            mVerifyPasswordView.setError(getString(R.string.error_field_required));
            focusView = mVerifyPasswordView;
            cancel = true;
        } else {
            if (!isPasswordValid(password, verifypassword)) {
                mVerifyPasswordView.setError(getString(R.string.error_matching_password));
                focusView = mVerifyPasswordView;
                cancel = true;
            }
        }

        if (TextUtils.isEmpty(userid)) {
            mUserIdView.setError(getString(R.string.error_field_required));
            focusView = mUserIdView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, userid, password, verifypassword, accesslevel, useractive);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(CharSequence email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password, String verifypassword) {
        return password.equals(verifypassword);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(CreateLoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//       connect Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "CreateLogin Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.gowheyoke.pricemasterlist/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
//    }

//    @Override
//    public void onStop() {
//        super.onStop();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "CreateLogin Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.gowheyoke.pricemasterlist/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
//        client.disconnect();
//    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */


    public void clickCancel(View view) {
        Intent myIntent;
        if (dbHelper.UserCount() < 1) {
            myIntent = new Intent(MainActivity.ACTION_CLOSE);
        } else {
            myIntent = new Intent(UserAccess.ACTION_REFRESH);
        }
        sendBroadcast(myIntent);
        finish();

    }

    @Override
    public void onBackPressed() {
        nExit++;
        if (nExit > 1) {
            Intent myIntent;
            if (dbHelper.UserCount() > 0) {
                myIntent = new Intent(UserAccess.ACTION_REFRESH);
            } else {
                myIntent = new Intent(MainActivity.ACTION_CLOSE);
            }
            sendBroadcast(myIntent);
            finish();
        } else
            Toast.makeText(this, "Press BACK again to Close.", Toast.LENGTH_SHORT).show();
        backHandler.postDelayed(backR, 2000);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return this.onOptionsItemSelected(item);
        }
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mUserID;
        private final String mPassword;
        private final String mVerifyPassword;
        private final String mAccessLevel;
        private final Integer mUserActive;

        UserLoginTask(String email, String userid, String password, String verifypassword, String accesslevel, Boolean useractive) {
            mEmail = email;
            mUserID = userid;
            mPassword = password;
            mVerifyPassword = verifypassword;
            mAccessLevel = accesslevel;
            mUserActive = useractive ? 1 : 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            // Simulate network access.
            try {
                /*
                Log.d("FireBase", "doInBackground: "+Config.FIREBASE_URL);
                Firebase ref = new Firebase(Config.FIREBASE_URL);
                Log.d("FireBase", "doInBackground: Users");
                Users user = new Users();
                Log.d("FireBase", "doInBackground: get device id");
                user.setDeviceId("1234567890");
                Log.d("FireBase", "doInBackground: "+mEmail);
                user.setEmail(mEmail);
                Log.d("FireBase", "doInBackground: "+mUserID);
                user.setUserId(mUserID);
                ref.child("Users").setValue(user);
                */
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i("xxxxx", "doInBackground:" + mAccessLevel);
            dbHelper.createUser(mEmail, mUserID, mPassword, mAccessLevel, mUserActive);
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                if (dbHelper.UserCount() <= 1) {
                    Intent myIntent = new Intent(MainActivity.ACTION_LOGIN);
                    sendBroadcast(myIntent);
                } else {
                    Intent myIntent = new Intent(UserAccess.ACTION_REFRESH);
                    sendBroadcast(myIntent);
                }
                finish();
            } else {
                mVerifyPasswordView.setError(getString(R.string.action_sign_in_short));
                mVerifyPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

