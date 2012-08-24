package edu.cmu.sei.dome.cloudlets.client;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

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
			Log.d(TAG, "Listen for Server PUSH");
			HttpGet get = new HttpGet(url);
			HttpResponse response = null;
			try {
				response = client.execute(get);
				String content = HttpUtil.getContent(response);
				if ((response != null) && (response.getEntity() != null))
					showResponse(content);

				// error message
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
					Log.d(TAG, "ERROR occured. Stop listening for Server PUSH");
					stopListening();
				}
				// no follow up - server finished 'connection'
				else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_GONE) {
					Log.d(TAG, "Stop listening for Server PUSH");
					stopListening();

					// start App Client

					// get top level domain out of entire url
					String addr = HttpUtil.getIPAddressFromURL(url);
					// get port from cloudlet message
					int port = Integer.parseInt(HttpUtil.parseFinalResponse(
							content).get(HttpUtil.PORT_KEY));
					this.cloudletClient.showToast("Started on port " + port);

					this.cloudletClient.startApp(this.info.clientPackage, addr,
							port);
					return;
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

	public void showResponse(String content) {
		if (content == null) {
			cloudletClient.error("IOException when reading HttpResponse!");
		} else
			cloudletClient.showToast(content);
	}
}
