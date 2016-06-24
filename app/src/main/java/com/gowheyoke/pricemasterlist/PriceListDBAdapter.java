package com.gowheyoke.pricemasterlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class PriceListDBAdapter {

    public static final String KEY_ROWID = "_id";
    public static final String KEY_BARCODE = "barcode";
    public static final String KEY_MAKE = "make";
    public static final String KEY_ITEMDESC = "itemdesc";
    public static final String KEY_UNIT = "unit";
    public static final String KEY_QTY = "qty";
    public static final String KEY_WSP = "wsp";
    public static final String KEY_RSP = "rsp";
    public static final String KEY_USERID = "userid";
    public static final String KEY_PWD = "pwd";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_ACCESS_LEVEL = "accesslevel";
    public static final String KEY_CONFIG_NAME = "name";
    public static final String KEY_CONFIG_VAL = "value";
    public static final String KEY_ACTIVE = "active";

    private static final String TAG = "PriceListDbAdapter";
    private static final String DATABASE_NAME = "Product";
    private static final String SQLITE_PRODUCT = "priceList";
    private static final String SQLITE_USER = "users";
    private static final String SQLITE_CONFIG = "configdata";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_PRODUCT_CREATE =
            "CREATE TABLE if not exists " + SQLITE_PRODUCT + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_BARCODE + "," +
                    KEY_MAKE + "," +
                    KEY_ITEMDESC + "," +
                    KEY_UNIT + "," +
                    KEY_QTY + "," +
                    KEY_WSP + "," +
                    KEY_RSP + ");";
    private static final String TABLE_USERS_CREATE =
            "CREATE TABLE if not exists " + SQLITE_USER + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_EMAIL + " NOT NULL UNIQUE," +
                    KEY_USERID + " NOT NULL UNIQUE," +
                    KEY_PWD + " NOT NULL," +
                    KEY_ACCESS_LEVEL + " NOT NULL," +
                    KEY_ACTIVE + " INT NOT NULL);";
    private static final String TABLE_CONFIG_CREATE =
            "CREATE TABLE if not exists " + SQLITE_CONFIG + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_CONFIG_NAME + " NOT NULL UNIQUE," +
                    KEY_CONFIG_VAL + " NOT NULL);";
    private final Context mCtx;
    private SQLiteDatabase mDb;

    public PriceListDBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public PriceListDBAdapter open() throws SQLException {
        DatabaseHelper mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public SQLiteStatement initInsert() {
        String sql = "Insert into " + SQLITE_PRODUCT + " (" +
                KEY_BARCODE + "," +
                KEY_MAKE + "," +
                KEY_ITEMDESC + "," +
                KEY_UNIT + "," +
                KEY_QTY + "," +
                KEY_WSP + "," +
                KEY_RSP + ") values(?,?,?,?,?,?,?);";
        SQLiteStatement insert = mDb.compileStatement(sql);
        return insert;
    }

    public void close() {
        if (mDb != null) {
            mDb.close();
        }
    }

    public long createPriceList(String barcode, String make, String itemdesc,
                                String unit, String qty, String wsp, String rsp) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_BARCODE, barcode);
        initialValues.put(KEY_MAKE, make);
        initialValues.put(KEY_ITEMDESC, itemdesc);
        initialValues.put(KEY_UNIT, unit);
        initialValues.put(KEY_QTY, qty);
        initialValues.put(KEY_WSP, wsp);
        initialValues.put(KEY_RSP, rsp);

        return mDb.insert(SQLITE_PRODUCT, null, initialValues);
    }

    public int updateUser(String userid, String pwd) {

        pwd = getPassEncypt(pwd.trim());
        ContentValues values = new ContentValues();
        values.put(KEY_PWD, pwd);

        // updating row
        return mDb.update(SQLITE_USER, values, KEY_USERID + " like ?",
                new String[]{userid.trim()});
    }

    public long createUser(String email, String userid, String pwd, String accesslevel, Integer active) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_EMAIL, email);
        initialValues.put(KEY_USERID, userid);
        initialValues.put(KEY_PWD, getPassEncypt(pwd.trim()));
        initialValues.put(KEY_ACCESS_LEVEL, accesslevel);
        initialValues.put(KEY_ACTIVE, active);

        return mDb.insert(SQLITE_USER, null, initialValues);
    }

    public long createConfig(String name, String value) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_CONFIG_NAME, name);
        initialValues.put(KEY_CONFIG_VAL, value);

        return mDb.insert(SQLITE_CONFIG, null, initialValues);
    }

    public boolean deleteAllPriceList() {

        int doneDelete;
        doneDelete = mDb.delete(SQLITE_PRODUCT, null, null);
        Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;
    }

    public boolean deleteAllUser() {

        int doneDelete;
        doneDelete = mDb.delete(SQLITE_USER, null, null);
        Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;
    }

    public int PriceListCount() {

        int recordCount;
        recordCount = fetchAllPriceList().getCount();
        Log.w(TAG, Integer.toString(recordCount));
        return recordCount;

    }
/*
    public boolean deleteUserAdmin() {

        int doneDelete ;
        doneDelete = mDb.delete(SQLITE_USER,KEY_USERID+" like 'admin'", null);
        Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;
    }
    */

    public int adCreditCount() {

        Cursor mCursor = mDb.query(SQLITE_CONFIG, new String[]{KEY_ROWID,
                        KEY_CONFIG_NAME, KEY_CONFIG_VAL},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public void TransCounter() {
        String name = "TransCounter";
        String pname = "Credit Count";
        Cursor mCursor = getConfig(name);

        if (mCursor == null) {
            createConfig(name, "1");
            Log.d("xDBx", name + 1);
        } else {
            int id_trans = Integer.parseInt(mCursor.getString(mCursor.getColumnIndexOrThrow(KEY_ROWID)));
            Log.d("xDBx", "id_trans " + id_trans);
            int val_trans = Integer.parseInt(mCursor.getString(mCursor.getColumnIndexOrThrow(KEY_CONFIG_VAL)));
            Log.d("xDBx", "val_trans " + val_trans);
            val_trans += 1;
            if (val_trans == MainActivity.CREDIT_EXCHANGE) {
                Cursor pCursor = getConfig(pname);

                int id_credit = Integer.parseInt(pCursor.getString(pCursor.getColumnIndexOrThrow(KEY_ROWID)));
                Log.d("xDBx", "id_credit " + id_credit);
                int val_credit = Integer.parseInt(pCursor.getString(pCursor.getColumnIndexOrThrow(KEY_CONFIG_VAL)));
                Log.d("xDBx", "val_credit " + val_credit);

                val_credit -= 1;
                ContentValues values_credit = new ContentValues();
                values_credit.put(KEY_CONFIG_NAME, pname);
                values_credit.put(KEY_CONFIG_VAL, val_credit);

                // updating row
                mDb.update(SQLITE_CONFIG, values_credit, KEY_ROWID + " = ?",
                        new String[]{String.valueOf(id_credit)});
                val_trans = 0;
            }

            ContentValues values_trans = new ContentValues();
            values_trans.put(KEY_CONFIG_NAME, name);
            values_trans.put(KEY_CONFIG_VAL, val_trans);

            // updating row
            mDb.update(SQLITE_CONFIG, values_trans, KEY_ROWID + " = ?",
                    new String[]{String.valueOf(id_trans)});
        }
    }

    public void addCredit(int val) {
        String pname = "Credit Count";
        Log.d("xDBx", pname);
        Cursor pCursor = getConfig(pname);

        if (pCursor != null) {
            int id_credit = Integer.parseInt(pCursor.getString(pCursor.getColumnIndexOrThrow(KEY_ROWID)));
            Log.d("xDBx", "id_credit " + id_credit);
            int val_credit = Integer.parseInt(pCursor.getString(pCursor.getColumnIndexOrThrow(KEY_CONFIG_VAL)));
            Log.d("xDBx", "val_credit " + val_credit);

            val_credit += val;
            ContentValues values_credit = new ContentValues();
            values_credit.put(KEY_CONFIG_NAME, pname);
            values_credit.put(KEY_CONFIG_VAL, val_credit);

            // updating row
            mDb.update(SQLITE_CONFIG, values_credit, KEY_ROWID + " = ?",
                    new String[]{String.valueOf(id_credit)});
        }
    }

    public Cursor getConfig(String config_name) {

        Cursor mCursor = mDb.query(SQLITE_CONFIG, new String[]{KEY_ROWID,
                        KEY_CONFIG_NAME, KEY_CONFIG_VAL},
                KEY_CONFIG_NAME + " LIKE '" + config_name + "'", null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
            Log.d("xDBx", "getConfig: " + config_name);
        }
        return mCursor;
    }

    public String ConfigValue(String config_name) {
        Cursor mCursor = getConfig(config_name);

        if (mCursor != null)
            mCursor.moveToFirst();

        return mCursor.getString(mCursor.getColumnIndexOrThrow(KEY_CONFIG_VAL));
    }

    public int UserCount() {
        int recordCount;
        Cursor mCursor = mDb.query(SQLITE_USER, new String[]{KEY_ROWID,
                        KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        recordCount = mCursor.getCount();
        Log.w(TAG, Integer.toString(recordCount));
        return recordCount;
    }

    public boolean validateUser(String userid, String pwd) {
        Cursor mCursor;
        if (userid.length() > 0 && pwd.length() > 0) {
            pwd = getPassEncypt(pwd.trim());
            mCursor = mDb.query(true, SQLITE_USER, new String[]{KEY_ROWID,
                            KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE},
                    "Upper(" + KEY_USERID + ") like upper('" + userid + "') AND " + KEY_PWD + " like '" + pwd + "' AND " + KEY_ACTIVE + " LIKE 1", null,
                    null, null, null, null);

            return mCursor.getCount() > 0;
        } else
            return false;
    }

    public Boolean isAdmin(String userid) {
        Cursor mCursor;

        mCursor = mDb.query(true, SQLITE_USER, new String[]{KEY_ROWID,
                        KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE},
                "Upper(" + KEY_USERID + ") like upper('" + userid + "') AND Upper(" + KEY_ACCESS_LEVEL + ") like 'ADMIN' AND " + KEY_ACTIVE + " LIKE 1", null,
                null, null, null, null);

        if (mCursor != null)
            mCursor.moveToFirst();

        return mCursor.getCount() > 0 ? true : false;
    }

    public Cursor fetchPriceListByItemDesc(String inputText) throws SQLException {
        Log.w(TAG, inputText);
        Cursor mCursor;
        if (inputText == null || inputText.length() == 0) {
            mCursor = mDb.query(SQLITE_PRODUCT, new String[]{KEY_ROWID,
                            KEY_BARCODE, KEY_MAKE, KEY_ITEMDESC, KEY_UNIT, KEY_QTY, KEY_WSP, KEY_RSP},
                    null, null, null, null, null);

        } else {

            String[] arr = inputText.split(" ");
            String where_statement = "";
            for (int i = 0; i < arr.length; i++) {
                where_statement = where_statement + KEY_BARCODE + " || ' ' || " + KEY_MAKE + " || ' ' || " + KEY_ITEMDESC + " || ' ' || " + KEY_UNIT + " like '%" + arr[i] + "%'" + ((i + 1) < arr.length ? " and " : "");
            }

            mCursor = mDb.query(true, SQLITE_PRODUCT, new String[]{KEY_ROWID,
                            KEY_BARCODE, KEY_MAKE, KEY_ITEMDESC, KEY_UNIT, KEY_QTY, KEY_WSP, KEY_RSP},
                    where_statement, null,
                    null, null, null, null);
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public Cursor fetchUserAccessByItem(String inputText) throws SQLException {
        Log.w(TAG, inputText);
        Cursor mCursor;
        if (inputText == null || inputText.length() == 0) {
            mCursor = mDb.query(SQLITE_USER, new String[]{KEY_ROWID,
                            KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE, "(CASE WHEN " + KEY_ACTIVE + " like 1 THEN 'ACTIVE' ELSE 'INACTIVE' END) as active_text"},
                    null, null, null, null, null);

        } else {

            String[] arr = inputText.split(" ");
            String where_statement = "";
            for (int i = 0; i < arr.length; i++) {
                where_statement = where_statement + KEY_USERID + " || ' ' || " + KEY_EMAIL + " || ' ' || " + KEY_ACCESS_LEVEL + " || ' ' || (CASE WHEN " + KEY_ACTIVE + " like 1 THEN 'ACTIVE' ELSE 'INACTIVE' END) like '%" + arr[i] + "%'" + ((i + 1) < arr.length ? " and " : "");
            }

            mCursor = mDb.query(true, SQLITE_USER, new String[]{KEY_ROWID,
                            KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE, "(CASE WHEN " + KEY_ACTIVE + " like 1 THEN 'ACTIVE' ELSE 'INACTIVE' END) as active_text"},
                    where_statement, null,
                    null, null, null, null);
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public Cursor fetchAllPriceList() {

        Cursor mCursor = mDb.query(SQLITE_PRODUCT, new String[]{KEY_ROWID,
                        KEY_BARCODE, KEY_MAKE, KEY_ITEMDESC, KEY_UNIT, KEY_QTY, KEY_WSP, KEY_RSP},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public Cursor fetchAllUserAccess() {

        Cursor mCursor = mDb.query(SQLITE_USER, new String[]{KEY_ROWID,
                        KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE, "(CASE WHEN " + KEY_ACTIVE + " like 1 THEN 'ACTIVE' ELSE 'INACTIVE' END) as active_text"},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public void beginTransaction() {
        mDb.beginTransaction();
    }

    public void endTransaction() {
        mDb.endTransaction();
    }

    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    public Cursor fetchUserAccessByID(Integer rowId) {

        Cursor mCursor = mDb.query(SQLITE_USER, new String[]{KEY_ROWID,
                        KEY_EMAIL, KEY_USERID, KEY_PWD, KEY_ACCESS_LEVEL, KEY_ACTIVE},
                KEY_ROWID + " like rowId", null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public String getPassEncypt(String pass) {
        String pwd = "";
        for (int i = 0; i < pass.trim().length(); i++) {
            int n = (int) pass.trim().charAt(i);
            char c = (char) (((n * 3) + 1) - i);
            pwd = pwd + c;
        }
        return pwd;
    }

    /*
    public void insertSomePriceList() {

        createPriceList("1","HONDA","STAR","PC","25","75000.00","90000");
        createPriceList("2","KAWASAKI","FURY","PC","45","65000.00","7000.00");
        createPriceList("3","YAMAHA","MEGA","PC","65","85000.00","150000.00");
        createPriceList("4","BAJAJ","CLIPPER","PC","50","30000.00","45000.00");
        createPriceList("5","CHINA","BIKE","PC","4","15000.00","25000.00");
        createPriceList("6","MOTORSTAR","EAGLE","PC","4","35000.00","45000.00");
        createPriceList("7","FOTON","ACER","PC","12","25000","50000.00");

    }
    */

    public void initConfig() {

        Cursor mCursor = mDb.query(SQLITE_CONFIG, new String[]{KEY_ROWID,
                        KEY_CONFIG_NAME, KEY_CONFIG_VAL},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        if (mCursor.getCount() <= 0 || mCursor == null) {
            //initialize
            createConfig("Credit Count", "5000");
            createConfig("TransCounter", "0");
            Log.i("xDBx", "DB initial value is created");
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.w(TAG, TABLE_PRODUCT_CREATE);
            db.execSQL(TABLE_PRODUCT_CREATE);
            Log.w(TAG, TABLE_USERS_CREATE);
            db.execSQL(TABLE_USERS_CREATE);
            Log.w(TAG, TABLE_CONFIG_CREATE);
            db.execSQL(TABLE_CONFIG_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SQLITE_PRODUCT);
            db.execSQL("DROP TABLE IF EXISTS " + SQLITE_USER);
            onCreate(db);
        }
    }

    /*
    public void insertAdminUser(){
        createUser("admin",getPassEncypt("admin"));
    }

    /*
    public void deleteAdminUser(){
        deleteUserAdmin();
    }
    */

}
