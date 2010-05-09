package ch.almana.android.stechkarte.utils;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import ch.almana.android.stechkarte.model.DayAccess;
import ch.almana.android.stechkarte.model.Timestamp;

public class RebuildDaysTask extends AsyncTask<Timestamp, Object, Object> {
	
	private ProgressDialog progressDialog;
	
	public RebuildDaysTask() {
		super();
	}
	
	public RebuildDaysTask(ProgressDialog progressDialog) {
		this.progressDialog = progressDialog;
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog.setMessage("Rebuilding days...");
		progressDialog.show();
		super.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		progressDialog.dismiss();
	}
	
	@Override
	protected Object doInBackground(Timestamp... timestamps) {
		DayAccess.getInstance().recalculateDayFromTimestamp(null);
		return null;
	}
	
}