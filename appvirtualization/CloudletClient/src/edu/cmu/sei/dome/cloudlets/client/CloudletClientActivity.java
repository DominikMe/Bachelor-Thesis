package edu.cmu.sei.dome.cloudlets.client;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CloudletClientActivity extends Activity implements
		OnItemClickListener {

	private static final String TAG = "CloudletClientActivity";
	static final String STORE = "sdcard/myCloudlets/apps/servers/";

	private CloudletApplication cloudlet;
	private Toast toast;
	private Uploader uploader = new Uploader(this);
	public static ProgressDialog progress;
	private ArrayAdapter<String> adapter;

	private static final FilenameFilter jsonFilter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return filename.endsWith(".json");
		}
	};
	private static final FilenameFilter archiveFilter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return filename.endsWith(".zip") || filename.endsWith(".tar.gz");
		}
	};

	ListView listApps;

	private final static Handler progressHandler = new Handler() {
		final String TAG = "ProgressHandler";

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Uploader.PROGRESS_UPDATE:
				double transferred = msg.getData().getDouble("transferred");
				progress.setProgress((int) (transferred * progress.getMax()));
				Log.d(TAG, "progress update " + transferred);
				if (!progress.isShowing())
					progress.show();
				if (progress.getProgress() == progress.getMax()) {
					progress.dismiss();
				}
				break;
			}
		};
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		cloudlet = (CloudletApplication) getApplication();

		Log.e(TAG,
				"Service started? "
						+ (startService(new Intent(this, JmDNSDiscoverer.class)) != null));

		toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
		progress = new ProgressDialog(CloudletClientActivity.this);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setCancelable(false);
		// progress.setMessage("Uploading " + appname + "...");

		listApps = (ListView) findViewById(R.id.listApps);
		adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.appTitle);
		listApps.setAdapter(adapter);
		listApps.setOnItemClickListener(this);
		updateAppList(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateAppList(adapter);
	}

	private void updateAppList(ArrayAdapter<String> adapter) {
		File store = new File(STORE);
		Log.d(TAG, store.getAbsolutePath());
		adapter.clear();
		File[] files = store.listFiles();
		Arrays.sort(files);
		for (File app : files) {
			if (app.isDirectory())
				adapter.add(app.getName());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void error(final String msg) {
		runOnUiThread(new Runnable() {

			public void run() {
				toast.setText(msg);
				toast.show();
				Log.d(TAG, "HttpResponse->Error: " + msg);
			}
		});
	}

	public void showToast(final String msg) {
		if (msg == null || msg.equals(""))
			return;
		runOnUiThread(new Runnable() {

			public void run() {
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setText(msg);
				toast.show();
				Log.d(TAG, "Toast: " + msg);
			}
		});
	}

	public void startApp(String pkg, String address, int port) {
		if (pkg.equals("")) {
			Log.d(TAG, "Start no application.");
			return;
		}
		Log.d(TAG, "Start application " + pkg + ", address:" + address + ":"
				+ port);
		Intent app = getPackageManager().getLaunchIntentForPackage(pkg);
		app.putExtra("address", address);
		app.putExtra("port", port);
		startActivity(app);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long row) {
		TextView item = (TextView) ((LinearLayout) view).getChildAt(0);
		String appname = (String) item.getText();
		Log.d(TAG, "You clicked " + item.getText());
		File appdir = new File(STORE + appname);
		if (!appdir.isDirectory())
			return;

		// analyze json
		File json = appdir.listFiles(jsonFilter)[0];
		Log.d(TAG, "Found " + json.getName());

		try {
			UploadInfo info = new UploadInfo(json);

			this.showToast(info.toString());

			// get upload file
			File app = appdir.listFiles(archiveFilter)[0];
			Log.d(TAG, "Found " + app.getName());
			info.app = app;

			Log.d(TAG, "Cloudlet requirements are " + info.cloudlets.toString()
					+ ".");
			InetAddress address = cloudlet.getServerAddress(info);
			int port = cloudlet.getServerPort(info);

			if (address == null || port == -1) {
				error(String.format("Could not find a suitable Cloudlet! (%s)",
						info.cloudlets));
				return;
			}

			deployApplication(info, address, port);

		} catch (IOException e) {
			e.printStackTrace();
			error("Could not read json!");
			return;
		}
	}

	private void deployApplication(UploadInfo info, InetAddress address,
			int port) {
		String url = "http:/" + address + ":" + port + "/apps/" + info.checksum;
		Log.d(TAG, "Send to " + url);
		uploader.postJSON(info, url);
	}

	public void uploadApplication(UploadInfo info, String url) {
		Log.d(TAG, "Application not cached. Upload it.");
		new EventListener(this, url, info).start();
		uploader.putFile(info, url, progressHandler);
		progress.setMessage("Uploading " + info.name + "...");
		progress.setProgress(0);
		progress.show();
	}
}