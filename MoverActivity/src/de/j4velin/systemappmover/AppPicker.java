package de.j4velin.systemappmover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ListView;

/**
 * Class to list all installed app.
 * <p/>
 * The actual moving happens in the AppClickListener class when an item is clicked.
 */
public class AppPicker extends AsyncTask<Void, Void, Void> {

	List<Drawable>			icons;
	List<ApplicationInfo>	apps;
	PackageManager			pm;
	private ProgressDialog	progress;
	final a_MoverActivity	activity;

	public AppPicker(final a_MoverActivity a) {
		activity = a;
	}

	@Override
	protected void onPreExecute() {
		pm = activity.getPackageManager();
		progress = ProgressDialog.show(activity, "", "Loading apps", true);
	}

	@Override
	protected void onPostExecute(Void a) {
		try {
			progress.cancel();
		}
		catch (IllegalArgumentException e) {
			if (BuildConfig.DEBUG)
				z_Logger.log(e);
		}
		if (apps == null || apps.isEmpty()) {
			activity.showErrorDialog("Error loadings apps!");
		}
		else {
			ListView liste = (ListView) activity.findViewById(R.id.apps);
			liste.setAdapter(new z_EfficientAdapter(activity, this));
			liste.setOnItemClickListener(new AppClickListener(this));
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		// load all apps and their icons, sort them alphabetical
		apps = pm.getInstalledApplications(0);
		try {
			Collections.sort(apps, new Comparator<ApplicationInfo>() {
				public int compare(final ApplicationInfo app1, final ApplicationInfo app2) {
					try {
						return app1.loadLabel(pm).toString().toLowerCase().compareTo(app2.loadLabel(pm).toString().toLowerCase());
					}
					catch (Exception e) {
						e.printStackTrace();
						return 0;
					}
				}
			});
		}
		catch (IllegalArgumentException iae) {
		}

		icons = new ArrayList<Drawable>(apps.size());
		try {
			for (int i = 0; i < apps.size(); i++) {
				icons.add(apps.get(i).loadIcon(pm));
			}
		}
		catch (OutOfMemoryError oom) {
		}
		return null;
	}

}