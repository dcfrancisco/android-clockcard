package ch.almana.android.stechkarte.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import ch.almana.android.stechkarte.log.Logger;
import ch.almana.android.stechkarte.provider.DB;
import ch.almana.android.stechkarte.provider.DB.Days;
import ch.almana.android.stechkarte.provider.DB.Months;
import ch.almana.android.stechkarte.utils.IProgressWrapper;

public class MonthAccess implements IModelAccess {
	private static final String LOG_TAG = Logger.TAG;

	private static SimpleDateFormat monthRefDateFormat = new SimpleDateFormat("yyyyMM");

	private final Context context;

	private boolean doNotRecalculate;

	private static MonthAccess instance;
	public static final float HOURS_IN_MILLIES = 1000f * 60f * 60f;

	public static void initInstance(Context context) {
		instance = new MonthAccess(context);
	}

	public static MonthAccess getInstance() {
		return instance;
	}

	public MonthAccess(Context context) {
		super();
		this.context = context.getApplicationContext();
	}

	public Context getContext() {
		return context;
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = getContext().getContentResolver().delete(uri, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		Uri ret = getContext().getContentResolver().insert(uri, initialValues);
		getContext().getContentResolver().notifyChange(ret, null);
		return ret;
	}

	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = getContext().getContentResolver().update(uri, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	public void insert(Month month) {
		Uri uri = insert(Months.CONTENT_URI, month.getValues());
		long id = ContentUris.parseId(uri);
		if (id > 0) {
			month.setId(id);
		}
	}

	public boolean hasMothRef(long monthref) {
		Cursor c = null;
		try {
			c = query(Months.CONTENT_URI, Months.PROJECTTION_MONTHREF, Months.NAME_MONTHREF + "=" + monthref, null, Months.DEFAULT_SORTORDER);
			return c.moveToFirst();
		} finally {
			if (c != null) {
				c.close();
				c = null;
			}

		}
	}

	@Override
	public Cursor query(String selection) {
		return query(selection, Months.DEFAULT_SORTORDER);
	}

	public Cursor query(String selection, String sortOrder) {
		return query(Months.CONTENT_URI, Months.DEFAULT_PROJECTION, selection, null, sortOrder);
	}

	public Month getOrCreateMonth(long dayref) {
		Month m;
		Cursor c = null;
		try {
			c = query(Months.NAME_MONTHREF + "=" + dayref);
			if (c.moveToFirst()) {
				m = new Month(c);
			} else {
				m = new Month(dayref);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return m;
	}

	public void insertOrUpdate(Month month) {
		if (month.getId() > -1) {
			update(month);
		} else {
			insert(month);
		}
	}

	public void update(Month month) {
		update(Months.CONTENT_URI, month.getValues(), DB.NAME_ID + "=" + month.getId(), null);
	}

	private Month getMonthBefore(Month currentMonth) {
		Month m = null;
		Cursor c = query(Months.NAME_MONTHREF + "<" + currentMonth.getMonthRef(), Months.DEFAULT_SORTORDER);
		if (c.moveToFirst()) {
			m = new Month(c);
		}
		c.close();
		return m;
	}

	public void recalculate(long monthRef) {
		if (doNotRecalculate) {
			return;
		}
		if (monthRef < 1) {
			return;
		}
		Month m = getOrCreateMonth(monthRef);
		recalculate(m);
	}

	public void recalculate(Month month) {
		if (doNotRecalculate) {
			return;
		}
		month.setLastUpdated(System.currentTimeMillis());

		long monthRef = month.getMonthRef();
		Month previousMonth = getMonthBefore(month);
		if (previousMonth == null) {
			previousMonth = new Month(0);
		}
		Log.i(LOG_TAG, "Recalculating " + monthRef + " with prev month " + previousMonth.getMonthRef());
		float worked = 0;
		float target = 0;
		float holiday = 0;

		Cursor c = month.getDays();
		boolean error = false;
		Day day = null;
		while (c.moveToNext()) {
			day = new Day(c);
			worked = worked + day.getHoursWorked();
			target = target + day.getHoursTarget();
			holiday = holiday + day.getHolyday();
			error = error ? error : day.isError();

		}
		if (!c.isClosed()) {
			c.close();
		}

		month.setError(error);
		month.setHoursWorked(worked);
		month.setHoursTarget(target);
		month.setHolyday(holiday);
		if (day != null) {
			month.setOvertime(day.getOvertime());
			month.setHolydayLeft(day.getHolydayLeft());
		}

		// weiter hier
		//
		// float hoursTargetDefault =
		// Settings.getInstance().getHoursTarget(month.getDayRef());
		// float hoursTarget = month.getHoursTarget();
		// if (hoursTarget == hoursTargetDefault || hoursTarget < 0f) {
		// float hoursTargetReal = hoursTargetDefault - month.getHolyday() *
		// hoursTargetDefault;
		// month.setHoursTarget(hoursTargetReal);
		// }
		// float hoursWorked = worked / HOURS_IN_MILLIES;
		// float overtime = hoursWorked - month.getHoursTarget();
		// Log.i(LOG_TAG, "Total hours worked: " + hoursWorked +
		// " yields overtime: " + overtime);
		// month.setHoursWorked(hoursWorked);
		// if (!month.isFixed()) {
		//
		// month.setOvertime(getTotalOvertime(month, previousMonth, overtime));
		// month.setHolydayLeft(previousMonth.getHolydayLeft() -
		// month.getHolyday());
		// }
		Log.w(Logger.TAG, "Rebuild month: " + monthRef + " w:" + worked + " target " + target);
		month.setLastUpdated(System.currentTimeMillis());
		insertOrUpdate(month);
	}

	public static long getMonthRefFromDayRef(long dayRef) {
		long monthRef = dayRef / 100;
		Log.d(LOG_TAG, "dayRef " + dayRef + " -> monthRef " + monthRef);
		return monthRef;
	}

	public static long getMonthRefFromTimestamp(long timestamp) {
		return getMonthRefFromDate(new Date(timestamp));
	}

	public static long getMonthRefFromDate(Date date) {
		String timeString = monthRefDateFormat.format(date);
		return Long.parseLong(timeString);
	}

	// public static long getTimestampFromMonthRef(long monthref) throws
	// ParseException {
	// try {
	// return monthRefDateFormat.parse(monthref + "").getTime();
	// } catch (ParseException e) {
	// Log.w(LOG_TAG, "Cannot parse " + monthref + " as monthref", e);
	// return 0;
	// }
	// }

	public Month getOldestUpdatedMonth(long since) {
		Cursor cursor = query(Months.NAME_LAST_UPDATED + " > " + since, Months.NAME_MONTHREF + " ASC");
		if (cursor.moveToFirst()) {
			return new Month(cursor);
		} else {
			return null;
		}
	}

	public void recalculateMonthFromTimestamp(Timestamp timestamp, IProgressWrapper progressWrapper) {
		// TODO Auto-generated method stub
		// FIXME dfydfasd
	}

	public void setDoNotRecalculate(boolean b) {
		this.doNotRecalculate = b;
	}

	public void rebuildFromDayRef(long dayref) {

		SortedSet<Long> monthRefs = new TreeSet<Long>();
		String selection = null;
		selection = DB.Days.NAME_DAYREF + ">=" + dayref;

		DayAccess dayAccess = DayAccess.getInstance();
		Cursor c = dayAccess.query(selection, Days.REVERSE_SORTORDER);

		while (c.moveToNext()) {
			Day d = new Day(c);
			long monthref = MonthAccess.getMonthRefFromDayRef(d.getDayRef());
			if (monthRefs.add(monthref)) {
				Log.i(LOG_TAG, "Added month " + monthref + " for recalculation");
			}
		}

		for (Iterator<Long> iterator = monthRefs.iterator(); iterator.hasNext();) {
			Long monthRef = iterator.next();
			recalculate(monthRef);
		}
	}
}
