package ch.almana.android.stechkarte.view;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.ViewBinder;
import ch.almana.android.stechkarte.R;
import ch.almana.android.stechkarte.log.Logger;
import ch.almana.android.stechkarte.model.Day;
import ch.almana.android.stechkarte.model.DayAccess;
import ch.almana.android.stechkarte.model.Timestamp;
import ch.almana.android.stechkarte.model.TimestampAccess;
import ch.almana.android.stechkarte.provider.db.DB;
import ch.almana.android.stechkarte.provider.db.DB.Timestamps;
import ch.almana.android.stechkarte.utils.Formater;

public class DayEditor extends ListActivity {

	public static final int MENU_ITEM_DELETE = 100;
	public static final int MENU_ITEM_EDIT = MENU_ITEM_DELETE + 1;

	private Day day;
	private Day origDay;
	private TextView dayRefTextView;
	private EditText holiday;
	private EditText holidayLeft;
	private EditText overtime;
	private EditText hoursTarget;
	private TextView hoursWorked;
	private CheckBox fixed;
	private ListView timestamps;
	private SimpleCursorAdapter adapter;
	private boolean overtimeAction;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.day_editor);

		Intent intent = getIntent();
		String action = intent.getAction();
		if (savedInstanceState != null) {
			Log.w(Logger.LOG_TAG, "Reading day information from savedInstanceState");
			if (day != null) {
				day.readFromBundle(savedInstanceState);
			} else {
				day = new Day(savedInstanceState);
			}
		} else if (Intent.ACTION_INSERT.equals(action)) {
			day = new Day(20091101); // FIXME get today
		} else if (Intent.ACTION_EDIT.equals(action)) {
			Cursor c = managedQuery(intent.getData(), DB.Days.DEFAULT_PROJECTION, null, null, null);
			if (c.moveToFirst()) {
				day = new Day(c);
			}
			c.close();
		}

		origDay = new Day(day);
		dayRefTextView = (TextView) findViewById(R.id.TextViewDayRef);
		holiday = (EditText) findViewById(R.id.EditTextHoliday);
		holidayLeft = (EditText) findViewById(R.id.EditTextHolidaysLeft);
		overtime = (EditText) findViewById(R.id.EditTextOvertime);
		hoursTarget = (EditText) findViewById(R.id.EditTextHoursTarget);
		hoursWorked = (TextView) findViewById(R.id.TextViewHoursWorkedDayEditor);
		fixed = (CheckBox) findViewById(R.id.CheckBoxFixed);

		timestamps = getListView();
		overtimeAction = true;
		overtime.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (overtimeAction) {
					overtimeAction = false;
					fixed.setChecked(true);
					Toast.makeText(DayEditor.this,
							"Overtime changed setting day to " + getText(R.string.CheckBoxFixed), Toast.LENGTH_SHORT)
							.show();
				}
				return false;
			}
		});

		timestamps.setOnCreateContextMenuListener(this);
		long dayRef = day.getDayRef();
		String selection = null;
		if (dayRef > 0) {
			selection = DB.Days.NAME_DAYREF + "=" + dayRef;
		}

		Cursor cursor = TimestampAccess.getInstance().query(selection);
		// Used to map notes entries from the database to views
		adapter = new SimpleCursorAdapter(this, R.layout.timestamplist_item, cursor, new String[] {
				Timestamps.NAME_TIMESTAMP, Timestamps.NAME_TIMESTAMP_TYPE }, new int[] { R.id.TextViewTimestamp,
				R.id.TextViewTimestampType });
		adapter.setViewBinder(new ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (cursor == null) {
					return false;
				}
				if (columnIndex == Timestamps.INDEX_TIMESTAMP) {
					TextView ts = (TextView) view.findViewById(R.id.TextViewTimestamp);
					long time = cursor.getLong(Timestamps.INDEX_TIMESTAMP);
					ts.setText(Timestamp.timestampToString(time));
				} else if (columnIndex == Timestamps.INDEX_TIMESTAMP_TYPE) {
					String txt = "unknown";
					int type = cursor.getInt(Timestamps.INDEX_TIMESTAMP_TYPE);
					if (type == Timestamp.TYPE_IN) {
						txt = " IN";
					} else if (type == Timestamp.TYPE_OUT) {
						txt = " OUT";
					}
					((TextView) view.findViewById(R.id.TextViewTimestampType)).setText(txt);
				}
				return true;
			}
		});
		timestamps.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateFields();
		// timestamps.setAdapter(adapter);
	}

	private void updateFields() {
		dayRefTextView.setText(day.getDayString());
		holiday.setText(day.getHolyday() + "");
		holidayLeft.setText(day.getHolydayLeft() + "");
		overtime.setText(Formater.formatHourMinFromHours(day.getOvertime()));
		fixed.setChecked(day.isFixed());
		hoursTarget.setText(Formater.formatHourMinFromHours(day.getHoursTarget()));
		hoursWorked.setText(Formater.formatHourMinFromHours(day.getHoursWorked()));
	}

	private void updateModel() {
		try {
			day.setHolyday(Float.parseFloat(holiday.getText().toString()));
		} catch (NumberFormatException e) {
			Toast.makeText(getApplicationContext(), "Cannot parse number " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		try {
			day.setHolydayLeft(Float.parseFloat(holidayLeft.getText().toString()));
		} catch (NumberFormatException e) {
			Toast.makeText(getApplicationContext(), "Cannot parse number " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		try {
			day.setOvertime(Formater.getHoursFromHoursMin(overtime.getText().toString()));
		} catch (NumberFormatException e) {
			Toast.makeText(getApplicationContext(), "Cannot parse number " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		try {
			day.setHoursTarget(Formater.getHoursFromHoursMin(hoursTarget.getText().toString()));
		} catch (NumberFormatException e) {
			Toast.makeText(getApplicationContext(), "Cannot parse number " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		day.setFixed(fixed.isChecked());
	}

	@Override
	protected void onPause() {
		super.onPause();
		updateModel();
		if (origDay.equals(day)) {
			return;
		}
		String action = getIntent().getAction();
		if (Intent.ACTION_INSERT.equals(action)) {
			DayAccess access = DayAccess.getInstance();
			access.insert(day);
		} else if (Intent.ACTION_EDIT.equals(action)) {
			DayAccess access = DayAccess.getInstance();
			access.update(day);
		}
		DayAccess.getInstance().recalculate(this, day);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		day.saveToBundle(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (day != null) {
			day.readFromBundle(savedInstanceState);
		} else {
			day = new Day(savedInstanceState);
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		getMenuInflater().inflate(R.menu.dayeditor_context, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.dayeditor_option, menu);
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(Logger.LOG_TAG, "bad menuInfo", e);
			return false;
		}

		Uri tsUri = ContentUris.withAppendedId(Timestamps.CONTENT_URI, info.id);
		switch (item.getItemId()) {
		case R.id.itemDayDeleteTimestamp:
			getContentResolver().delete(tsUri, null, null);
			return true;
		case R.id.itemDayEditTimestamp:
			startActivity(new Intent(Intent.ACTION_EDIT, tsUri));
			return true;
		case R.id.itemDayInsertTimestamp:
			insertNewTimestamp();
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.itemDayInsertTimestamp:
			insertNewTimestamp();
			return true;
		}
		return true;
	}

	private void insertNewTimestamp() {
		startActivity(new Intent(Intent.ACTION_INSERT, Timestamps.CONTENT_URI));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(DB.Timestamps.CONTENT_URI, id);
		startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}

}
