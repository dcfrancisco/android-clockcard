package ch.almana.android.stechkarte.view.activity;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.almana.android.stechkarte.R;
import ch.almana.android.stechkarte.log.Logger;
import ch.almana.android.stechkarte.model.Day;
import ch.almana.android.stechkarte.model.DayAccess;
import ch.almana.android.stechkarte.model.Timestamp;
import ch.almana.android.stechkarte.model.calc.RebuildDays;
import ch.almana.android.stechkarte.provider.DB;
import ch.almana.android.stechkarte.provider.DB.Timestamps;
import ch.almana.android.stechkarte.utils.DeleteDayDialog;
import ch.almana.android.stechkarte.utils.DialogCallback;
import ch.almana.android.stechkarte.utils.Formater;
import ch.almana.android.stechkarte.utils.Settings;

public class DayEditor extends FragmentActivity implements DialogCallback {

	private static final int DIA_DATE_SELECT = 0;
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
	private TextView overtimeCur;
	private EditText comment;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.day_editor);
		if (Settings.getInstance().hasHoloTheme()) {
			getActionBar().setSubtitle(R.string.dayEditorTitle);
		}else {
			setTitle(getString(R.string.app_name)+": "+getString(R.string.dayEditorTitle));
		}

		dayRefTextView = (TextView) findViewById(R.id.TextViewDayRef);
		holiday = (EditText) findViewById(R.id.EditTextHoliday);
		holidayLeft = (EditText) findViewById(R.id.EditTextHolidaysLeft);
		overtimeCur = (TextView) findViewById(R.id.tvOvertimeCur);
		overtime = (EditText) findViewById(R.id.EditTextOvertime);
		hoursTarget = (EditText) findViewById(R.id.EditTextHoursTarget);
		hoursWorked = (TextView) findViewById(R.id.TextViewHoursWorkedDayEditor);
		fixed = (CheckBox) findViewById(R.id.CheckBoxFixed);
		comment = (EditText) findViewById(R.id.etComment);
		timestamps = (ListView) findViewById(android.R.id.list);

		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (savedInstanceState != null) {
			Log.w(Logger.TAG, "Reading day information from savedInstanceState");
			if (day != null) {
				day.readFromBundle(savedInstanceState);
			} else {
				day = new Day(savedInstanceState);
			}
		} else if (Intent.ACTION_INSERT.equals(action)) {
			day = new Day(DayAccess.getNextFreeDayref(System.currentTimeMillis()));
			TextView dayInfo = (TextView) findViewById(R.id.TextViewDayEditorDateInfo);
			dayInfo.setText("Tap to change date");
			dayRefTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDialog(DIA_DATE_SELECT);
				}
			});
			dayInfo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDialog(DIA_DATE_SELECT);
				}
			});
		} else if (Intent.ACTION_EDIT.equals(action)) {
			CursorLoader cursorLoader = new CursorLoader(this, intent.getData(), DB.Days.DEFAULT_PROJECTION, null, null, null);
			Cursor c = cursorLoader.loadInBackground();
			if (c.moveToFirst()) {
				day = new Day(c);
			}
			c.close();
		}

		if (day == null) {
			day = new Day();
		}

		origDay = new Day(day);

		overtimeAction = true;
		overtime.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (overtimeAction) {
					overtimeAction = false;
					fixed.setChecked(true);
					Toast.makeText(DayEditor.this, "Overtime changed setting day to " + getText(R.string.CheckBoxFixed), Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});

		timestamps.setOnCreateContextMenuListener(this);
		timestamps.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				Uri uri = ContentUris.withAppendedId(DB.Timestamps.CONTENT_URI, id);
				startActivity(new Intent(Intent.ACTION_EDIT, uri));
			}
		});



		adapter = new SimpleCursorAdapter(this, R.layout.timestamplist_item, null,
				new String[] { Timestamps.NAME_TIMESTAMP, Timestamps.NAME_TIMESTAMP_TYPE }, new int[] {
						R.id.TextViewTimestamp, R.id.TextViewTimestampType },0);
		adapter.setViewBinder(new ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (cursor == null) {
					return false;
				}
				if (columnIndex == Timestamps.INDEX_TIMESTAMP) {
					TextView ts = (TextView) view.findViewById(R.id.TextViewTimestamp);
					long time = cursor.getLong(Timestamps.INDEX_TIMESTAMP);
					ts.setText(Timestamp.timestampToString(time, false));
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
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIA_DATE_SELECT:
			OnDateSetListener callBack = new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					day.setYear(year);
					day.setMonth(monthOfYear);
					day.setDay(dayOfMonth);
					updateView();
				}
			};
			return new DatePickerDialog(this, callBack, day.getYear(), day.getMonth(), day.getDay());

		default:
			return super.onCreateDialog(id);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		updateView();
	}

	private void updateView() {
		long dayRef = day.getDayRef();
		String selection = null;
		if (dayRef > 0) {
			selection = DB.Days.NAME_DAYREF + "=" + dayRef;
		}
		CursorLoader cursorLoader = new CursorLoader(this, Timestamps.CONTENT_URI, Timestamps.DEFAULT_PROJECTION, selection, null, Timestamps.DEFAUL_SORTORDER);
		Cursor cursor = cursorLoader.loadInBackground();
		adapter.swapCursor(cursor);
		dayRefTextView.setText(day.getDayString());
		holiday.setText(day.getHolyday() + "");
		holidayLeft.setText(day.getHolydayLeft() + "");
		overtime.setText(Formater.formatHourMinFromHours(day.getOvertime()));
		overtimeCur.setText(Formater.formatHourMinFromHours(day.getDayOvertime()));
		fixed.setChecked(day.isFixed());
		hoursTarget.setText(Formater.formatHourMinFromHours(day.getHoursTarget()));
		hoursWorked.setText(Formater.formatHourMinFromHours(day.getHoursWorked()));
		comment.setText(day.getComment());
		// if (!day.isFixed()) {
		// // change work time on day change
		// hoursTarget.setText(Formater.formatHourMinFromHours(Settings.getInstance().getHoursTarget(day.getDayRef())));
		// }

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
		day.setComment(comment.getText().toString());
	}

	@Override
	protected void onPause() {
		super.onPause();
		updateModel();
		String action = getIntent().getAction();
		if (origDay.equals(day) && !Intent.ACTION_INSERT.equals(action)) {
			return;
		}
		try {
			RebuildDays rebuild = RebuildDays.create(this);
			rebuild.recalculateDay(day);
			DayAccess.getInstance().insertOrUpdate(day);
		} catch (Exception e) {
			Log.e(Logger.TAG, "Cannot save day", e);
			Toast.makeText(this, "Error saving day.", Toast.LENGTH_LONG).show();
		}
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
			Log.e(Logger.TAG, "bad menuInfo", e);
			return false;
		}

		Uri tsUri = ContentUris.withAppendedId(Timestamps.CONTENT_URI, info.id);
		switch (item.getItemId()) {
		case R.id.itemDayDeleteTimestamp:
			getContentResolver().delete(tsUri, null, null);
			updateView();
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
		case R.id.itemDeleteDay:
			long id = day.getId();
			if (id == -1) {
				return true;
			}
			DeleteDayDialog deleteDayDialog = new DeleteDayDialog(this, id);
			deleteDayDialog.setTitle("Delete Day...");
			deleteDayDialog.show();
			return true;
		}
		return true;
	}

	private void insertNewTimestamp() {
		Intent intent = new Intent(Intent.ACTION_INSERT, Timestamps.CONTENT_URI);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, day.getYear());
		cal.set(Calendar.MONTH, day.getMonth());
		cal.set(Calendar.DAY_OF_MONTH, day.getDay());
		intent.putExtra(Timestamps.NAME_TIMESTAMP, cal.getTimeInMillis());
		startActivity(intent);
	}

	@Override
	public void finished(boolean success) {
		if (success) {
			finish();
		}
	}

	@Override
	public Context getContext() {
		return this;
	}

}
