package ch.almana.android.stechkarte.model;

import java.util.Calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import ch.almana.android.stechkarte.provider.DB;
import ch.almana.android.stechkarte.provider.DB.Holidays;

public class Holiday {

	public enum BorderType {
		allDay, halfDay, restOfDay, specifyed
	}

	private static final long DAY_IN_MILLIES = 24 * 60 * 60 * 1000;

	private long id = -1;
	private long start = -1;
	private BorderType startType = BorderType.allDay;
	private float startHours;
	private long end = -1;
	private BorderType endType = BorderType.allDay;
	private float endHours;
	private float days;
	private boolean isHoliday;
	private boolean isPaid;
	private boolean isYearly;
	private boolean yieldOvertime;
	private String comment;
	private long holidayType;


	public Holiday() {
		super();
	}

	public Holiday(Holiday holiday) {
		this();
		setId(holiday.getId());
		start = holiday.start;
		startType = holiday.startType;
		startHours = holiday.startHours;
		end = holiday.end;
		endType = holiday.endType;
		endHours = holiday.endHours;
		days = holiday.days;
		isHoliday = holiday.isHoliday;
		isPaid = holiday.isPaid;
		isYearly = holiday.isYearly;
		yieldOvertime = holiday.yieldOvertime;
		comment = holiday.comment;
		holidayType = holiday.holidayType;
	}

	public Holiday(Cursor c) {
		this();
		setId(c.getLong(DB.INDEX_ID));
		start = c.getLong(Holidays.INDEX_START);
		startType = BorderType.valueOf(c.getString(Holidays.INDEX_START_TYPE));
		startHours = c.getFloat(Holidays.INDEX_START_HOURS);
		end = c.getLong(Holidays.INDEX_END);
		endType = BorderType.valueOf(c.getString(Holidays.INDEX_END_TYPE));
		endHours = c.getFloat(Holidays.INDEX_END_HOURS);
		days = c.getFloat(Holidays.INDEX_DAYS);
		isHoliday = c.getInt(Holidays.INDEX_IS_HOLIDAY) == 1 ? true : false;
		isPaid = c.getInt(Holidays.INDEX_IS_PAID) == 1 ? true : false;
		isYearly = c.getInt(Holidays.INDEX_IS_YEARLY) == 1 ? true : false;
		yieldOvertime = c.getInt(Holidays.INDEX_YIELDS_OVERTIME) == 1 ? true : false;
		comment = c.getString(Holidays.INDEX_COMMENT);
		holidayType = c.getLong(Holidays.INDEX_TYPE);
	}

	public Holiday(Bundle instanceState) {
		this();
		readFromBundle(instanceState);
	}


	public ContentValues getValues() {
		ContentValues values = new ContentValues();
		if (getId() > -1) {
			values.put(DB.NAME_ID, getId());
		}
		values.put(Holidays.NAME_START, getStart());
		values.put(Holidays.NAME_START_TYPE, getStartType().toString());
		values.put(Holidays.NAME_START_HOURS, getStartHours());
		values.put(Holidays.NAME_END, getEnd());
		values.put(Holidays.NAME_END_TYPE, getEndType().toString());
		values.put(Holidays.NAME_END_HOURS, getEndHours());
		values.put(Holidays.NAME_DAYS, getDays());
		values.put(Holidays.NAME_IS_HOLIDAY, isHoliday ? 1 : 0);
		values.put(Holidays.NAME_IS_PAID, isPaid ? 1 : 0);
		values.put(Holidays.NAME_IS_YEARLY, isYearly ? 1 : 0);
		values.put(Holidays.NAME_YIELDS_OVERTIME, yieldOvertime ? 1 : 0);
		values.put(Holidays.NAME_COMMENT, getComment());
		values.put(Holidays.NAME_TYPE, getHolidayType());
		return values;
	}

	public void saveToBundle(Bundle bundle) {
		if (getId() > -1) {
			bundle.putLong(DB.NAME_ID, getId());
		} else {
			bundle.putLong(DB.NAME_ID, -1);
		}
		bundle.putLong(Holidays.NAME_START, getStart());
		bundle.putString(Holidays.NAME_START_TYPE, getStartType().toString());
		bundle.putFloat(Holidays.NAME_START_HOURS, getStartHours());
		bundle.putLong(Holidays.NAME_END, getEnd());
		bundle.putString(Holidays.NAME_END_TYPE, getEndType().toString());
		bundle.putFloat(Holidays.NAME_END_HOURS, getEndHours());
		bundle.putFloat(Holidays.NAME_DAYS, getDays());
		bundle.putInt(Holidays.NAME_IS_HOLIDAY, isHoliday ? 1 : 0);
		bundle.putInt(Holidays.NAME_IS_PAID, isPaid ? 1 : 0);
		bundle.putInt(Holidays.NAME_IS_YEARLY, isYearly ? 1 : 0);
		bundle.putInt(Holidays.NAME_YIELDS_OVERTIME, yieldOvertime ? 1 : 0);
		bundle.putString(Holidays.NAME_COMMENT, getComment());
		bundle.putLong(Holidays.NAME_TYPE, getHolidayType());
	}

	public void readFromBundle(Bundle bundle) {
		setId(bundle.getLong(DB.NAME_ID));
		start = bundle.getLong(Holidays.NAME_START);
		startType = BorderType.valueOf(bundle.getString(Holidays.NAME_START_TYPE));
		startHours = bundle.getFloat(Holidays.NAME_START_HOURS);
		end = bundle.getLong(Holidays.NAME_END);
		endType = BorderType.valueOf(bundle.getString(Holidays.NAME_END_TYPE));
		endHours = bundle.getFloat(Holidays.NAME_END_HOURS);
		days = bundle.getFloat(Holidays.NAME_DAYS);
		isHoliday = bundle.getInt(Holidays.NAME_IS_HOLIDAY) == 1 ? true : false;
		isPaid = bundle.getInt(Holidays.NAME_IS_PAID) == 1 ? true : false;
		isYearly = bundle.getInt(Holidays.NAME_IS_YEARLY) == 1 ? true : false;
		yieldOvertime = bundle.getInt(Holidays.NAME_YIELDS_OVERTIME) == 1 ? true : false;
		comment = bundle.getString(Holidays.NAME_COMMENT);
		holidayType = bundle.getLong(Holidays.NAME_TYPE);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + Float.floatToIntBits(days);
		result = prime * result + (int) (end ^ (end >>> 32));
		result = prime * result + Float.floatToIntBits(endHours);
		result = prime * result + ((endType == null) ? 0 : endType.hashCode());
		result = prime * result + (int) (holidayType ^ (holidayType >>> 32));
		result = prime * result + (isHoliday ? 1231 : 1237);
		result = prime * result + (isPaid ? 1231 : 1237);
		result = prime * result + (isYearly ? 1231 : 1237);
		result = prime * result + (int) (start ^ (start >>> 32));
		result = prime * result + Float.floatToIntBits(startHours);
		result = prime * result + ((startType == null) ? 0 : startType.hashCode());
		result = prime * result + (yieldOvertime ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Holiday other = (Holiday) obj;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (Float.floatToIntBits(days) != Float.floatToIntBits(other.days))
			return false;
		if (end != other.end)
			return false;
		if (Float.floatToIntBits(endHours) != Float.floatToIntBits(other.endHours))
			return false;
		if (endType != other.endType)
			return false;
		if (holidayType != other.holidayType)
			return false;
		if (isHoliday != other.isHoliday)
			return false;
		if (isPaid != other.isPaid)
			return false;
		if (isYearly != other.isYearly)
			return false;
		if (start != other.start)
			return false;
		if (Float.floatToIntBits(startHours) != Float.floatToIntBits(other.startHours))
			return false;
		if (startType != other.startType)
			return false;
		if (yieldOvertime != other.yieldOvertime)
			return false;
		return true;
	}

	public long getStart() {
		return start;
	}

	public Calendar getStartAsCalendar() {
		if (start < 0) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(start);
		return cal;
	}

	public void setStart(long start) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(start);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		this.start = cal.getTimeInMillis();
		if (end < 0) {
			setEnd(start);
		}
		updateNumHolidayDays();
	}

	public BorderType getStartType() {
		return startType;
	}

	public void setStartType(BorderType startType) {
		this.startType = startType;
		updateNumHolidayDays();
	}

	public float getStartHours() {
		return startHours;
	}

	public void setStartHours(float startHours) {
		this.startHours = startHours;
	}

	public long getEnd() {
		return end;
	}

	public Calendar getEndAsCalendar() {
		if (end < 0) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(end);
		return cal;
	}

	public void setEnd(long end) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(end);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		this.end = cal.getTimeInMillis();
		if (start < 0) {
			setStart(end);
		}
		updateNumHolidayDays();
	}

	public BorderType getEndType() {
		return endType;
	}

	public void setEndType(BorderType endType) {
		this.endType = endType;
		updateNumHolidayDays();
	}

	public float getEndHours() {
		return endHours;
	}

	public void setEndHours(float endHours) {
		this.endHours = endHours;
	}

	public boolean isHoliday() {
		return isHoliday;
	}

	public void setHoliday(boolean isHoliday) {
		this.isHoliday = isHoliday;
	}

	public boolean isPaid() {
		return isPaid;
	}

	public void setPaid(boolean isPaid) {
		this.isPaid = isPaid;
	}

	public boolean isYearly() {
		return isYearly;
	}

	public void setYearly(boolean isYearly) {
		this.isYearly = isYearly;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isYieldOvertime() {
		return yieldOvertime;
	}

	public void setYieldOvertime(boolean yieldOvertime) {
		this.yieldOvertime = yieldOvertime;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public float getDays() {
		return days;
	}


	public void setDays(float days) {
		this.days = days;
		updateStartEndFromDays();
	}

	private void updateStartEndFromDays() {
		//		final int inMillies = Math.round(days * DAY_IN_MILLIES) - 1000;
		//		if (start > 0) {
		//			end = start + inMillies;
		//		} else if (end > 0) {
		//			start = end - inMillies;
		//		}
	}

	private void updateNumHolidayDays() {
		if (start > 0 && end > 0) {
			Calendar startCal = getStartAsCalendar();
			Calendar endCal = getEndAsCalendar();
			days = 0;
			while (startCal.getTimeInMillis() < endCal.getTimeInMillis()) {
				if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
					days++;
				}
				startCal.add(Calendar.DAY_OF_YEAR, 1);
			}
			if (startType == BorderType.halfDay) {
				days -= .5;
			}
			if (endType == BorderType.halfDay) {
				days -= .5;
			}
			//			if (days == 0) {
			//				days = .5f;
			//			}
		} else {
			updateStartEndFromDays();
		}
	}

	public long getHolidayType() {
		return holidayType;
	}

	public void setHolidayType(long holidayType) {
		this.holidayType = holidayType;
	}

}
