package ch.almana.android.stechkarte.model;

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import ch.almana.android.stechkarte.provider.DB;
import ch.almana.android.stechkarte.provider.IAccess;
import ch.almana.android.stechkarte.provider.StechkarteTimestampProvider;
import ch.almana.android.stechkarte.provider.DB.Timestamps;

public class TimestampAccess implements IAccess {

	private class AlertDialogHandler implements OnClickListener {

		private static final int ACTION_ADD_INVERTED = 0;
		private static final int ACTION_ADD = 1;
		private static final int ACTION_ADDLAST_ADD = 2;
		private static final int ACTION_CANCAL = 3;
		private static final int ACTION_MAX = 4;

		private Timestamp timestamp;
		private CharSequence[] actions;
		private Context context;

		public AlertDialogHandler(Context context, Timestamp timestamp) {
			this.timestamp = timestamp;
			this.context = context;
			actions = new String[ACTION_MAX];
			String invTsTAsString = Timestamp.getTimestampTypeAsString(context, Timestamp
					.invertTimestampType(timestamp));
			String tstAsString = Timestamp.getTimestampTypeAsString(context, timestamp);
			actions[ACTION_ADD_INVERTED] = "Add " + invTsTAsString;
			actions[ACTION_ADD] = "Add " + tstAsString;
			actions[ACTION_ADDLAST_ADD] = "Add last " + invTsTAsString + " and add " + tstAsString;
			actions[ACTION_CANCAL] = "Cancel";
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case ACTION_ADD_INVERTED:
				timestamp.setTimestampType(Timestamp.invertTimestampType(timestamp));
				insert(timestamp);
				break;
			case ACTION_ADD:
				insert(timestamp);
				break;
			case ACTION_CANCAL:
				Toast.makeText(context, "Canceled action", Toast.LENGTH_SHORT).show();
				break;

			case ACTION_ADDLAST_ADD:
				insert(timestamp);
				Intent i = new Intent(Intent.ACTION_INSERT, Timestamps.CONTENT_URI);
				i.putExtra(Timestamps.COL_NAME_TIMESTAMP_TYPE, Timestamp.invertTimestampType(timestamp));
				context.startActivity(i);
				break;

			default:
				Toast.makeText(context, "Action not implemented.", Toast.LENGTH_SHORT).show();
				break;
			}

		}

		public CharSequence[] getActions() {
			return actions;
		}

	}

	private static final long MIN_TIMESTAMP_DIFF = 1000 * 60;

	private Context context;

	private static TimestampAccess instance;

	public static TimestampAccess getInstance(Context context) {
		if (instance == null) {
			instance = new TimestampAccess(context);
		}
		return instance;
	}

	private static final String LOG_TAG = "Timestamps";
	private static final int DATABASE_VERSION = 1;

	private static HashMap<String, String> sTimestampProjectionMap;

	private static final int TIMESTAMP = 1;
	private static final int TIMESTAMP_ID = 2;

	private static final UriMatcher sUriMatcher;

	private static final String DATABASE_CREATE = "create table " + DB.Timestamps.TABLE_NAME + " ("
			+ Timestamps.COL_NAME_ID + " integer primary key, " + Timestamps.COL_NAME_TIMESTAMP_TYPE + " int,"
			+ Timestamps.COL_NAME_TIMESTAMP + " long);";

	private DBHelper mOpenHelper;

	private class DBHelper extends SQLiteOpenHelper {

		public DBHelper(Context context) {
			super(context, DB.DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			Log.i(LOG_TAG, "Created table " + DB.Timestamps.TABLE_NAME);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TIMESTAMP:
			count = db.delete(DB.Timestamps.TABLE_NAME, selection, selectionArgs);
			break;

		case TIMESTAMP_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(DB.Timestamps.TABLE_NAME, Timestamps.COL_NAME_ID + "=" + noteId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private Context getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IDataAccessObject#getType(android
	 * .net.Uri)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IAccess#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case TIMESTAMP:
			return DB.Timestamps.CONTENT_TYPE;

		case TIMESTAMP_ID:
			return DB.Timestamps.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IDataAccessObject#insert(android
	 * .net.Uri, android.content.ContentValues)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IAccess#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		if (sUriMatcher.match(uri) != TIMESTAMP) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(DB.Timestamps.TABLE_NAME, null, values);
		if (rowId > 0) {
			Uri noteUri = ContentUris.withAppendedId(Timestamps.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.almana.android.stechkarte.provider.IDataAccessObject#onCreate()
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.almana.android.stechkarte.provider.IAccess#onCreate()
	 */
	@Override
	public boolean onCreate() {
		mOpenHelper = new DBHelper(getContext());
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IDataAccessObject#query(android
	 * .net.Uri, java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.almana.android.stechkarte.provider.IAccess#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(DB.Timestamps.TABLE_NAME);
		qb.setProjectionMap(sTimestampProjectionMap);
		switch (sUriMatcher.match(uri)) {
		case TIMESTAMP:
			break;

		case TIMESTAMP_ID:
			qb.appendWhere(Timestamps.COL_NAME_ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = DB.Timestamps.DEFAUL_SORTORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IDataAccessObject#update(android
	 * .net.Uri, android.content.ContentValues, java.lang.String,
	 * java.lang.String[])
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.almana.android.stechkarte.provider.IAccess#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TIMESTAMP:
			count = db.update(DB.Timestamps.TABLE_NAME, values, selection, selectionArgs);
			break;

		case TIMESTAMP_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.update(DB.Timestamps.TABLE_NAME, values, Timestamps.COL_NAME_ID + "=" + noteId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	public TimestampAccess(Context context) {
		super();
		this.context = context;
		mOpenHelper = new DBHelper(context);
	}

	public void addOutNow() {
		addOut(System.currentTimeMillis());
	}

	public void addOut(long time) {
		processInOutAdd(new Timestamp(time, Timestamp.TYPE_OUT));
	}

	public void addInNow() {
		addIn(System.currentTimeMillis());
	}

	public void addIn(long time) {
		processInOutAdd(new Timestamp(time, Timestamp.TYPE_IN));
	}

	private void processInOutAdd(Timestamp timestamp) {
		Timestamp lastTs = getLastTimestamp();
		if (lastTs != null) {
			if (Math.abs(timestamp.getTimestamp() - lastTs.getTimestamp()) < MIN_TIMESTAMP_DIFF) {
				String tsTime = Timestamp.formatTime(timestamp);
				String ltsTime = Timestamp.formatTime(lastTs);
				Toast.makeText(
						context,
						"Difference betwenn current and last timestamp is too small!\n" + tsTime + "\n" + ltsTime
								+ "\nIgnoring timestamp.", Toast.LENGTH_LONG).show();
				return;
			}
			if (timestamp.getTimestampType() == lastTs.getTimestampType()) {

				Builder alert = new AlertDialog.Builder(context);
				alert.setTitle("Same timestamp types: " + Timestamp.getTimestampTypeAsString(context, timestamp));
				AlertDialogHandler alertDiaHandler = new TimestampAccess.AlertDialogHandler(context, timestamp);
				alert.setItems(alertDiaHandler.getActions(), alertDiaHandler);
				alert.create().show();
				return;
			}
		}
		insert(timestamp);
	}

	public void insert(Timestamp timestamp) {
		Toast.makeText(context,
				Timestamp.getTimestampTypeAsString(context, timestamp) + ": " + Timestamp.formatTime(timestamp),
				Toast.LENGTH_LONG).show();
		insert(Timestamps.CONTENT_URI, timestamp.getValues());
	}

	public void update(Timestamp timestamp) {
		update(Timestamps.CONTENT_URI, timestamp.getValues(),
				Timestamps.COL_NAME_ID + "=" + timestamp.getId(), null);
	}

	public Cursor query(String selection, String[] selectionArgs) {
		return query(Timestamps.CONTENT_URI, Timestamps.DEFAULT_PROJECTION, selection,
				selectionArgs, DB.Timestamps.DEFAUL_SORTORDER);
	}

	private Timestamp getLastTimestamp() {
		Cursor cursor = query(null, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			return new Timestamp(cursor);
		}
		return null;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(StechkarteTimestampProvider.AUTHORITY, Timestamps.CONTENT_ITEM_NAME, TIMESTAMP);
		sUriMatcher.addURI(StechkarteTimestampProvider.AUTHORITY, Timestamps.CONTENT_ITEM_NAME + "/#", TIMESTAMP_ID);

		sTimestampProjectionMap = new HashMap<String, String>();
		for (String col : Timestamps.colNames) {
			sTimestampProjectionMap.put(col, col);
		}
	}
}
