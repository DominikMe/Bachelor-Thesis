package edu.cmu.sei.dome.cloudlets.client;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;
import edu.cmu.sei.dome.cloudlets.client.Uploader.UploadInfo;

public class EventListener extends Thread {

	private final HttpClient client;
	private final CloudletClientActivity cloudletClient;
	private final String url;
	private final UploadInfo info;
	private boolean running = true;
	static final String TAG = "EventListener";

	static final int PROGRESS_UPDATE = 1;
	static final double PROGRESS_FIDELITY = 0.05;

	public EventListener(final CloudletClientActivity cloudletClient,
			final String url, final UploadInfo info) {
		this.client = HttpUtil.getThreadSafeClient();
		this.cloudletClient = cloudletClient;
		this.url = url;
		this.info = info;
	}

	@Override
	public void run() {
		while (running) {
			HttpGet get = new HttpGet(url);
			HttpResponse response = null;
			try {
				response = client.execute(get);
				if ((response != null) && (response.getEntity() != null))
					showResponse(response);

				// no follow up - server finished 'connection'
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_GONE) {
					Log.d(TAG, "Stop listening for Server PUSH");
					stopListening();
					// start App Client
					CloudletApplication cloudlet = (CloudletApplication) cloudletClient
							.getApplication();

					InetAddress address = null;
					if (info.os
							.equals(cloudletClient.getString(R.string.linux)))
						address = cloudlet.getLinuxServerAddress();
					else if (info.os.equals(cloudletClient
							.getString(R.string.windows)))
						address = cloudlet.getWindowsServerAddress();
					this.cloudletClient.startApp(this.info.client_pkg,
							address.getHostAddress(), info.port);
					break;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				cloudletClient.error("Could not reach " + url + "!");
			}
		}
	}

	public void stopListening() {
		this.running = false;
	}

	public void showResponse(final HttpResponse response) {
		String content = HttpUtil.getContent(response);
		if (content == null) {
			cloudletClient.error("IOException when reading HtppResponse!");
		} else
			cloudletClient.showToast(content);
	}
}
