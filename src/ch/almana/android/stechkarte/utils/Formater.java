package ch.almana.android.stechkarte.utils;

import java.text.DecimalFormat;

import android.util.Log;
import ch.almana.android.stechkarte.log.Logger;

public class Formater {

	private static final DecimalFormat df = new DecimalFormat("#.##");

	public static CharSequence formatHourMinFromHours(float hours) {
		// return df.format(hours);
		// floor(-3.5) == -4
		boolean neg = false;
		if (hours < 0) {
			hours = -hours;
			neg = true;
		}
		int h = (int) Math.floor(hours);
		int m = Math.round((hours - h) * 60); // rather: /100 * 60 * 100
		String sign = neg ? "-" : "";
		return sign + h + ":" + String.format("%02d", m);
	}

	public static float getHoursFromHoursMin(String hourMin) {
		// try {
		// return Float.parseFloat(hourMin);
		// } catch (Exception e) {
		// Log.e(Logger.LOG_TAG, "Cannot parse " + hourMin + " als float", e);
		// return 0f;
		// }
		float hours = 0f;
		int sepPos = hourMin.indexOf(':');
		if (sepPos < 0) {
			sepPos = hourMin.indexOf('.');
		}
		if (sepPos > -1) {
			String h = hourMin.substring(0, sepPos);
			String m = hourMin.substring(sepPos + 1);
			try {
				hours = Float.parseFloat(h);
			} catch (Exception e) {
				Log.e(Logger.LOG_TAG, "Cannot parse " + h + " als float", e);
			}
			try {
				// FIXME check for -0:50
				float min = Float.parseFloat(m);
				min /= 60f;
				if (hours < 0) {
					hours -= min;
				} else {
					hours += min;
				}
			} catch (Exception e) {
				Log.e(Logger.LOG_TAG, "Cannot parse " + m + " als float", e);
			}
		}
		return hours;
	}

}
