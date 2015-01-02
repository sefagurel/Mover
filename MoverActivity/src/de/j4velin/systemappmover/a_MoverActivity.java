package de.j4velin.systemappmover;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.stericson.RootTools.RootTools;

public class a_MoverActivity extends Activity {

	final static String	SYSTEM_APP_FOLDER	= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ? "/system/priv-app/" : "/system/app/";

	void showErrorDialog(final String text) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Error").setMessage(text).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, int id) {
				try {
					dialog.dismiss();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		builder.create().show();
	}

	void showWarningDialog() {
		final Dialog dialog1 = new Dialog(this);
		dialog1.setTitle("Warning");
		dialog1.setCancelable(false);
		dialog1.setContentView(R.layout.warningdialog);

		final CheckBox c = (CheckBox) dialog1.findViewById(R.id.c);
		final Button b = (Button) dialog1.findViewById(R.id.b);

		c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				b.setText(checked ? android.R.string.ok : android.R.string.cancel);
			}
		});

		b.setText(android.R.string.cancel);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (c.isChecked()) {
					getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("warningRead", true).commit();
					dialog1.dismiss();
				}
				else {
					dialog1.dismiss();
					finish();
				}
			}
		});

		dialog1.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		RootTools.debugMode = false;
		checkForRoot();
	}

	private void checkForRoot() {

		final ProgressDialog progress = ProgressDialog.show(this, "", "Waiting for root access", true);
		progress.show();
		final TextView error = (TextView) findViewById(R.id.error);
		final Handler h = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!RootTools.isRootAvailable()) {
					if (progress == null || !progress.isShowing())
						return;
					progress.cancel();
					h.post(new Runnable() {

						@Override
						public void run() {
							error.setText("Your device seems not to be rooted!\nThis app requires root access and does not work without.\n\nClick [here] to uninstall.");
							// ask user to delete app on non-rooted devices
							error.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:de.j4velin.systemappmover")));
								}
							});
						}
					});
					return;
				}
				final boolean root = RootTools.isAccessGiven();
				if (progress == null || !progress.isShowing())
					return;
				progress.cancel();
				h.post(new Runnable() {

					@Override
					public void run() {

						if (root) {
							((CheckBox) findViewById(R.id.root)).setChecked(true);
						}
						else {
							error.setText("No root access granted - click here to recheck");
							error.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									checkForRoot();
								}
							});
							return;
						}

						if (RootTools.isBusyboxAvailable()) {
							CheckBox busyBox = (CheckBox) findViewById(R.id.busybox);
							busyBox.setChecked(true);
							busyBox.setText("BusyBox " + RootTools.getBusyBoxVersion());
							if (root) {
								new AppPicker(a_MoverActivity.this).execute();
								if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("warningRead", false)) {
									showWarningDialog();
								}
							}
						}
						else {
							error.setText("No busybox found!\nClick here to download");
							error.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									RootTools.offerBusyBox(a_MoverActivity.this);
									finish();
								}
							});

							return;
						}

						error.setText("Use at your own risk! I won't take responsibility for damages on your device! Make a backup first!");

					}
				});
			}
		}).start();

	}

}