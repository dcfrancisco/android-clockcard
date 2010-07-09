package ch.almana.android.stechkarte.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import android.widget.Toast;
import ch.almana.android.stechkarte.R;
import ch.almana.android.stechkarte.log.Logger;
import ch.almana.android.stechkarte.model.DayAccess;
import ch.almana.android.stechkarte.view.BuyFullVersion;

public class Settings extends SettingsBase {

	private static float hoursTargetDefault = 8.4f;
	private static final long SECONDS_IN_MILLIES = 1000;
	private static final int MIN_LICENSE_VERSION = 1;

	public static void initInstance(Context ctx) {
		if (instance == null) {
			instance = new Settings(ctx);
		}
	}

	private Settings(Context ctx) {
		super(ctx);
		// FIXME remove since its only for update 1.0 -> 1.0.1
		String key = context.getResources().getString(R.string.prefKeyMinTimestampDiff);
		SharedPreferences preferences = getPreferences();
		if (preferences.contains(key)) {
			String keyNew = context.getResources().getString(R.string.prefKeyMinTimestampDiffInSecs);
			Editor editor = preferences.edit();
			int minutes = Integer.parseInt(preferences.getString(key, "1")) * 60;
			editor.putString(keyNew, minutes + "");
			editor.remove(key);
			editor.commit();
		}
		// END remove since its only for update 1.0 -> 1.0.1
		// FIXME remove since its only for update 1.2.2 -> 1.2.3
		String value = getCsvSeparator();
		if (value != null && value.contains("\t")) {
			value = value.replace("\t", "\\t");
			key = context.getResources().getString(R.string.prefKeyCsvFieldSeparator);
			Editor editor = preferences.edit();
			editor.putString(key, value);
			editor.commit();
		}
		// END remove since its only for update 1.0 -> 1.0.1
	}

	static final SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEE");

	public float getHoursTarget(long dayref) {
		float hoursTarget = -1;
		try {
			long timestamp = DayAccess.timestampFromDayRef(dayref);
			String prefKey = context.getString(R.string.prefKeyWorkHoursWeekBase);
			prefKey = prefKey + weekdayFormat.format(timestamp);
			hoursTarget = Float.parseFloat(getPreferences().getString(prefKey, "-1"));
		} catch (Exception e) {
			Log.w(Logger.LOG_TAG, "Error parsing setting hours per weekday", e);
			hoursTarget = -1;
		}
		if (hoursTarget < 0) {
			try {
				hoursTarget = getPrefAsFloat(R.string.prefKeyHoursPerDay, R.string.prefHoursPerDayDefault);
			} catch (Exception e) {
				Log.w(Logger.LOG_TAG, "Error parsing setting hours per day", e);
				hoursTarget = hoursTargetDefault;
			}
		}

		return hoursTarget;
	}

	private boolean checkLicense() {
		String packageName = "ch.almana.android.stechkarteLicense";
		// ComponentName componentName = new ComponentName(context,
		// packageName);
		try {
			PackageManager pm = context.getPackageManager();
			Signature[] mySignatures = context.getApplicationContext().getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			List<PackageInfo> preferredPackages = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
			for (Iterator<PackageInfo> iterator = preferredPackages.iterator(); iterator.hasNext();) {
				PackageInfo packageInfo = iterator.next();
				// Log.d(Logger.LOG_TAG, "Package: " + packageInfo.packageName);
				if (packageName.equals(packageInfo.packageName)) {
					Log.d(Logger.LOG_TAG, "Found package: " + packageInfo.packageName);
					if (packageInfo.versionCode >= MIN_LICENSE_VERSION) {
						Signature[] signatures = packageInfo.signatures;

						if (Arrays.equals(signatures, mySignatures)) {
							Log.i(Logger.LOG_TAG, "Found valid license");
							return true;
						} else {
							Toast.makeText(context, "Wrong license signature.", Toast.LENGTH_LONG).show();
						}

					} else {
						Toast.makeText(context, "License version to low, please update.", Toast.LENGTH_LONG).show();
						BuyFullVersion.startClockCardInstall(context);
					}
				}
			}

			// PackageInfo packageInfo = pm.getPackageInfo(packageName,
			// PackageManager.GET_SIGNATURES);

		} catch (Exception e) {
			Log.d(Logger.LOG_TAG, "Exception while looking for " + packageName + " as license", e);
		}
		Log.i(Logger.LOG_TAG, "License " + packageName + " not found");
		return false;
	}

	public boolean isPayVersion() {
		if (checkLicense()) {
			return true;
		}
		return false;
	}

	public boolean isBetaVersion() {
		String lic = getPrefAsString(R.string.prefKeyLicence, R.string.prefLicenceDefault);
		if ("sonnenscheinInBasel".equals(lic)) {
			return true;
		}
		return false;
	}

	public long getMinTimestampDiff() {
		try {
			long diff = getPrefAsLong(R.string.prefKeyMinTimestampDiffInSecs, R.string.prefMinTimestampDiffDefault);
			return diff * SECONDS_IN_MILLIES;
		} catch (Exception e) {
			return SECONDS_IN_MILLIES;
		}
	}

	public String getEmailAddress() {
		try {
			return getPrefAsString(R.string.prefKeyEmailAddress, R.string.prefEmailAddressDefault);
		} catch (Exception e) {
			return "";
		}

	}

	public String getCsvSeparator() {
		try {
			return getPrefAsString(R.string.prefKeyCsvFieldSeparator, R.string.prefCsvFieldSeparatorDefault);
		} catch (Exception e) {
			return context.getString(R.string.prefCsvFieldSeparatorDefault);
		}
	}

	public DecimalFormat getCsvDecimalFormat() {
		DecimalFormat df = null;
		try {
			String format = getPrefAsString(R.string.prefKeyDecimalFormat, R.string.prefDecimalFormatDefault);
			if (format != null && !format.trim().equals("")) {
				df = new DecimalFormat(format);
			}
		} catch (Exception e) {
			Log.e(Logger.LOG_TAG, "Error getting decimalformat from perf", e);
			// df = new DecimalFormat();
			// // context
			// // .getString(R.string.prefDecimalFormatDefault));
			// df.setMaximumFractionDigits(2);
		}
		if (df == null) {
			df = (DecimalFormat) NumberFormat.getNumberInstance();
		}
		return df;
	}

	public boolean isEmailExportEnabled() {
		return isPayVersion();
	}

	public boolean isBackupEnabled() {
		return isBetaVersion();
	}

	public boolean hasBetaFeatures() {
		return true;
	}

	public void setLastDaysRebuild(long currentTimeMillis) {
		Editor editor = getPreferences().edit();
		editor.putLong(RebuildDaysTask.PREF_KEY_LAST_UPDATE, currentTimeMillis);
		editor.commit();
	}

	public long getLastDaysRebuild() {
		return getPreferences().getLong(RebuildDaysTask.PREF_KEY_LAST_UPDATE, 0);
	}

	public boolean isNightshiftEnabled() {
		return getPreferences().getBoolean(context.getString(R.string.prefKeyNightshift), false);
	}
}
