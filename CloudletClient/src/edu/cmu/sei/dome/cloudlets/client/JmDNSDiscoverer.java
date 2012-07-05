package edu.cmu.sei.dome.cloudlets.client;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.IBinder;
import android.util.Log;
import edu.cmu.sei.dome.cloudlets.client.CloudletApplication;
import edu.cmu.sei.dome.cloudlets.client.R;

public class JmDNSDiscoverer extends Service {

	final static String TAG = "JmDNSDiscoverer";
	private JmDNS jmdns;
	private JmDNSDiscovererThread discoverer;
	private MulticastLock lock;
	private CloudletApplication app;

	@Override
	public void onCreate() {
		super.onCreate();
		app = (CloudletApplication) getApplication();
		Log.d(TAG, "create cloudlet discovery service");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.discoverer = new JmDNSDiscovererThread();
		Log.d(TAG, "start cloudlet discovery");
		setUpMulticast();
		discoverer.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		discoverer.stopDiscovery();
		discoverer.interrupt();
		if (lock != null) {
			lock.release();
		}
	}

	// needed for JmDNS Discovery
	private void setUpMulticast() {
		WifiManager wifi = (WifiManager) app
				.getSystemService(Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("CloudletDiscovery");
		lock.setReferenceCounted(true);
		lock.acquire();
		Log.d(TAG, "MulticastLock acquired.");
	}

	private ServiceListener mdnsListener = new ServiceListener() {

		public void serviceAdded(ServiceEvent event) {
			Log.d(TAG, "service added");
			jmdns.requestServiceInfo(event.getType(), event.getName());
		}

		public void serviceResolved(ServiceEvent event) {
			Log.d(TAG, "service resolved");
			ServiceInfo info = event.getInfo();
			Log.d(TAG, String.format("%s (%s), running on %s:%s",
					info.getName(), info.getNiceTextString(),
					info.getInet4Addresses()[0].getHostAddress(),
					info.getPort()));

			// jmdns makes the names unique ! -> "CloudletServer (2)"
			if (!info.getName().contains(getString(R.string.jmdns_name))) {
				return;
			}
			Log.d(TAG, "attributes: " + info.getNiceTextString());

			String[] attr = info.getNiceTextString().split(",");
			for (String a : attr) {
				a = a.trim();
				String[] key_value = a.split("=");
				if (key_value[0].equals("os")) {
					Log.d(TAG, "service runs on " + key_value[1]);
					try {
						if (key_value[1].equals(getString(R.string.linux))) {
							app.linuxServers.put(info);
						} else if (key_value[1]
								.equals(getString(R.string.windows))) {
							app.windowsServers.put(info);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}

		public void serviceRemoved(ServiceEvent event) {
			// TODO Auto-generated method stub
			Log.d(TAG, "service removed");
			// handle lost service connection
		}

	};

	private class JmDNSDiscovererThread extends Thread {

		// private static final long DELAY = 1000;

		@Override
		public void run() {
			try {
				Log.d(TAG, "discovery thread started");
				jmdns = JmDNS.create();
			} catch (IOException e) {
				Log.e("NetworkError", "Service discovery failed.");
			}
			jmdns.addServiceListener(getString(R.string.jmdns_type),
					mdnsListener);
			// while (app.linuxServers.isEmpty() ||
			// app.windowsServers.isEmpty()) {
			// // Namen siehe oben ! (wirklich?)
			// jmdns.requestServiceInfo(getString(R.string.jmdns_type),
			// getString(R.string.jmdns_name));
			// Log.d(TAG, "RequestService ended.");
			// try {
			// Thread.sleep(DELAY);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
		}

		public void stopDiscovery() {
			jmdns.removeServiceListener(getString(R.string.jmdns_type),
					mdnsListener);
			try {
				jmdns.close();
			} catch (IOException e) {
				Log.e("NetworkError", "Service discovery could not be stopped.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
