package de.j4velin.systemappmover;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.List;

public class AppClickListener implements OnItemClickListener {

	private final AppPicker	ap;

	public AppClickListener(final AppPicker a) {
		ap = a;
	}

	public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {
		if (position >= ap.apps.size())
			return;

		if ("MOVED".equals(view.getTag())) {
			ap.activity.showErrorDialog("Please reboot before moving this app again");
			return;
		}

		ApplicationInfo tmp = ap.apps.get(position);
		boolean tmpAlreadySys = (tmp.flags & ApplicationInfo.FLAG_SYSTEM) == 1;

		// update necessary?
		if ((tmpAlreadySys && tmp.sourceDir.contains("/data/app/")) || (!tmpAlreadySys && tmp.sourceDir.contains(a_MoverActivity.SYSTEM_APP_FOLDER))) {
			try {
				tmp = ap.pm.getApplicationInfo(tmp.packageName, 0);
			}
			catch (NameNotFoundException e1) {
				ap.activity.showErrorDialog("App not found");
				if (BuildConfig.DEBUG)
					z_Logger.log(e1);
				return;
			}
		}

		final ApplicationInfo app = tmp;
		final String appName = (String) app.loadLabel(ap.pm);
		final boolean alreadySys = (app.flags & ApplicationInfo.FLAG_SYSTEM) == 1;

		if (BuildConfig.DEBUG)
			z_Logger.log("Trying to move " + appName + " - " + app.packageName);

		if (app.packageName.equals(ap.activity.getPackageName())) {
			ap.activity.showErrorDialog("Can not move myself");
			if (BuildConfig.DEBUG)
				z_Logger.log("Can not move myself");
			return;
		}

		if (alreadySys && app.sourceDir.contains("/data/app/")) {
			if (BuildConfig.DEBUG)
				z_Logger.log("Need to remove updates first");
			AlertDialog.Builder builder = new AlertDialog.Builder(ap.activity);
			builder.setTitle("Error").setMessage("Can not move " + appName + ": Remove installed updates first.").setPositiveButton("Remove updates", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, int id) {
					try {
						ap.activity.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName)));
						dialog.dismiss();
					}
					catch (Exception e) {
					}
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, int id) {
					try {
						dialog.dismiss();
					}
					catch (Exception e) {
					}
				}
			});
			builder.create().show();
			return;
		}
		else if (!alreadySys && tmp.sourceDir.contains(a_MoverActivity.SYSTEM_APP_FOLDER)) {
			ap.activity.showErrorDialog("Can not move " + appName + ": Undefined app status. You might need to reboot once.");
			if (BuildConfig.DEBUG)
				z_Logger.log("Undefined app status: IsSystem = " + alreadySys + " path = " + tmp.sourceDir);
			return;
		}

		if (!alreadySys && app.sourceDir.endsWith("pkg.apk")) {
			if (app.sourceDir.contains("asec")) {
				if (BuildConfig.DEBUG)
					z_Logger.log("Paid app? Path = " + app.sourceDir);
				ap.activity.showErrorDialog(appName + " seems to be a paid app and therefore can not be converted to system app due to limitations by the Android system");
			}
			else {
				if (BuildConfig.DEBUG)
					z_Logger.log("SD card? " + app.sourceDir);
				ap.activity.showErrorDialog(appName + " is currently installed on SD card. Please move to internal memory before moving to /system/app/");
			}
			return;
		}

		AlertDialog.Builder b = new AlertDialog.Builder(ap.activity);
		b.setMessage("Convert " + appName + " to " + (alreadySys ? "normal" : "system") + " app?");
		b.setPositiveButton(android.R.string.yes, new OnClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (RootTools.remount("/system", "rw")) {
					try {

						if (BuildConfig.DEBUG)
							z_Logger.log("process name: " + app.processName);

						ActivityManager activityManager = (ActivityManager) ap.activity.getSystemService(Context.ACTIVITY_SERVICE);
						List<RunningAppProcessInfo> runningProcInfo = activityManager.getRunningAppProcesses();
						String[] pkgList;
						for (RunningAppProcessInfo p : runningProcInfo) {
							pkgList = p.pkgList;
							for (String pkg : pkgList) {
								if (pkg.equals(app.processName)) {
									if (BuildConfig.DEBUG)
										z_Logger.log("killing: " + p.processName);
									RootTools.killProcess(p.processName);
									break;
								}
							}
						}

						if (BuildConfig.DEBUG)
							z_Logger.log("source: " + app.sourceDir);

						String mvcmd, newFile;
						List<String> output;
						if (!alreadySys) {
							if (app.sourceDir.endsWith("/pkg.apk")) {
								newFile = a_MoverActivity.SYSTEM_APP_FOLDER + app.packageName + "-asec.apk";
								if (!RootTools.remount("/mnt", "rw")) {
									if (BuildConfig.DEBUG)
										z_Logger.log("Can not remount /mnt");
									ap.activity.showErrorDialog("Can not remount /mnt/asec");
									return;
								}
								mvcmd = "busybox mv " + app.sourceDir + " " + newFile;
								if (BuildConfig.DEBUG)
									z_Logger.log("source ends with /pkg.apk -> paid app");
							}
							else {
								newFile = app.sourceDir.replace("/data/app/", a_MoverActivity.SYSTEM_APP_FOLDER);
								mvcmd = "busybox mv " + app.sourceDir + " " + a_MoverActivity.SYSTEM_APP_FOLDER;
							}
						}
						else {
							if (app.sourceDir.endsWith("/pkg.apk")) {
								newFile = "/data/app/" + app.packageName + ".apk";
								mvcmd = "busybox mv " + app.sourceDir + " " + newFile;
							}
							else {
								newFile = app.sourceDir.replace(a_MoverActivity.SYSTEM_APP_FOLDER, "/data/app/");
								mvcmd = "busybox mv " + app.sourceDir + " /data/app/";
							}
						}
						if (BuildConfig.DEBUG)
							z_Logger.log("command: " + mvcmd);

						output = RootTools.sendShell(mvcmd, 10000);

						if (output.size() > 1) {
							String error = "Error: ";
							for (String str : output) {
								if (str.length() > 1) {
									error += "\n" + str;
								}
							}
							if (BuildConfig.DEBUG)
								z_Logger.log(error);
							ap.activity.showErrorDialog(error);
						}
						else {

							File f = new File(newFile);

							for (int i = 0; f.length() < 1 && i < 20; i++) {
								Thread.sleep(100);
							}

							if (BuildConfig.DEBUG)
								z_Logger.log("file " + f.getAbsolutePath() + " size: " + f.length());

							if (f.length() > 1) {

								if (!alreadySys) {
									output = RootTools.sendShell("busybox chmod 644 " + newFile, 5000);
									if (BuildConfig.DEBUG) {
										for (String str : output) {
											z_Logger.log(str);
										}
									}
								}

								view.setVisibility(View.GONE);
								view.setTag("MOVED");
								AlertDialog.Builder b2 = new AlertDialog.Builder(ap.activity);
								b2.setMessage(appName + " successfully moved, you need to reboot your device.\nReboot now?");
								if (BuildConfig.DEBUG)
									z_Logger.log("successfully moved");
								b2.setPositiveButton(android.R.string.yes, new OnClickListener() {
									@Override
									public void onClick(final DialogInterface dialog, int which) {
										if (BuildConfig.DEBUG)
											z_Logger.log("reboot now");
										ap.activity.sendBroadcast(new Intent("de.j4velin.ACTION_SHUTDOWN"));
										try {
											dialog.dismiss();
										}
										catch (Exception e) {
										}
										try {
											RootTools.sendShell("am broadcast -a android.intent.action.ACTION_SHUTDOWN", 5000);
											try {
												Thread.sleep(1000);
											}
											catch (InterruptedException e) {
											}
											RootTools.sendShell("reboot", 5000);
										}
										catch (Exception e) {
										}
									}
								});
								b2.setNegativeButton(android.R.string.no, new OnClickListener() {
									@Override
									public void onClick(final DialogInterface dialog, int which) {
										if (BuildConfig.DEBUG)
											z_Logger.log("no reboot");
										try {
											dialog.dismiss();
										}
										catch (Exception e) {
										}
									}
								});
								b2.create().show();
							}
							else {
								ap.activity.showErrorDialog(appName + " could not be moved");
							}
						}
					}
					catch (Exception e) {
						ap.activity.showErrorDialog(e.getClass().getName() + " " + e.getMessage());
						e.printStackTrace();
						if (BuildConfig.DEBUG)
							z_Logger.log(e);
					}
					finally {
						RootTools.remount("/system", "ro");
						RootTools.remount("/mnt", "ro");
					}
				}
				else {
					if (BuildConfig.DEBUG)
						z_Logger.log("can not remount /system");
					ap.activity.showErrorDialog("Could not remount /system");
				}
			}
		});
		b.setNegativeButton(android.R.string.no, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					dialog.dismiss();
				}
				catch (Exception e) {
				}
			}
		});
		b.create().show();
	}
}
