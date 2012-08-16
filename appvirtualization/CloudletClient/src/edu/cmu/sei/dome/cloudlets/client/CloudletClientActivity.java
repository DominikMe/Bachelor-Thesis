package edu.cmu.sei.dome.cloudlets.client;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.sei.dome.cloudlets.client.Uploader.UploadInfo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
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
	private static ProgressDialog progress;
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
		for (File app : store.listFiles()) {
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
			JsonReader jsonreader = new JsonReader(new FileReader(json));
			Map<String, String> entries = new HashMap<String, String>();
			jsonreader.beginObject();
			while (jsonreader.hasNext()) {
				entries.put(jsonreader.nextName(), jsonreader.nextString());
			}
			jsonreader.endObject();

			UploadInfo info = uploader.new UploadInfo();
			info.name = entries.get("name");
			info.checksum = entries.get("checksum");
			info.os = entries.get("os");
			info.type = entries.get("type");
			info.client_pkg = entries.get("package");
			info.port = Integer.parseInt(entries.get("port"));
			info.size = Long.parseLong(entries.get("size"));
			info.json = json;

			String msg = String.format(
					"Name: %s\nSize: %d\nChecksum: %s\nOS: %s\nType: %s\n",
					info.name, info.size, info.checksum, info.os, info.type);
			this.showToast(msg);

			// get upload file
			File app = appdir.listFiles(archiveFilter)[0];
			Log.d(TAG, "Found " + app.getName());
			info.app = app;

			Log.d(TAG, "Required OS is " + info.os + ".");
			if (info.os.toLowerCase().equals(getString(R.string.linux))) {
				InetAddress address = cloudlet.getLinuxServerAddress();
				int port = cloudlet.getLinuxServerPort();

				if (address == null || port == -1) {
					error("Could not find a Linux Cloudlet!");
					return;
				}

				upload_JSON_APP(info, address, port);
			} else if (info.os.toLowerCase()
					.equals(getString(R.string.windows))) {
				InetAddress address = cloudlet.getWindowsServerAddress();
				int port = cloudlet.getWindowsServerPort();

				if (address == null || port == -1) {
					error("Could not find a Windows Cloudlet!");
					return;
				}

				upload_JSON_APP(info, address, port);
			}

		} catch (IOException e) {
			e.printStackTrace();
			error("Could not read json!");
			return;
		}
	}

	private void upload_JSON_APP(UploadInfo info, InetAddress address, int port) {
		String url = "http:/" + address + ":" + port + "/apps/" + info.checksum;
		Log.d(TAG, "Send to " + url);
		uploader.postJSON(info.json, url);
		progress.setMessage("Uploading " + info.name + "...");
		progress.show();
		new EventListener(this, url, info).start();
		uploader.putFile(info.app, info.size, url,
				progressHandler);
	}
}