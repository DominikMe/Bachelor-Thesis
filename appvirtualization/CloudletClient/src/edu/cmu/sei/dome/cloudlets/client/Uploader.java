package edu.cmu.sei.dome.cloudlets.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Uploader {

	private HttpClient client;
	private CloudletClientActivity cloudletClient;
	static final String TAG = "Uploader";

	static final int PROGRESS_UPDATE = 1;
	static final double PROGRESS_FIDELITY = 0.05;

	public Uploader(CloudletClientActivity cloudletClient) {
		this.client = HttpUtil.getThreadSafeClient();
		this.cloudletClient = cloudletClient;
	}

	/**
	 * REST-POST: Post application metadata.
	 * 
	 * @param f
	 * @param url
	 */
	public void postJSON(final UploadInfo info, final String url) {
		new AsyncTask<String, Integer, HttpResponse>() {

			@Override
			protected HttpResponse doInBackground(String... params) {
				HttpPost post = new HttpPost(url);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody file = new FileBody(info.json);
				mpEntity.addPart("json", file);
				post.setEntity(mpEntity);

				Log.d(TAG, "upload " + info.json.getName() + " to " + url);
				HttpResponse response = null;
				try {
					response = client.execute(post);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Could not reach " + url + "!");
					cloudletClient.error("Could not reach " + url + "!");
					return null;
				}
				return response;
			}

			protected void onPostExecute(HttpResponse result) {
				if (result != null) {
					String content = HttpUtil.getContent(result);
					cloudletClient.showToast(content);
					int status = result.getStatusLine().getStatusCode(); 
					if (status == HttpStatus.SC_GONE) {
						String address = HttpUtil.getIPAddressFromURL(url);
						int port = Integer.parseInt(HttpUtil
								.parseFinalResponse(content).get(
										HttpUtil.PORT_KEY));
						cloudletClient
								.showToast("Application already deployed.\nStarted on port "
										+ port);
						cloudletClient.startApp(info.client_pkg, address, port);
					} else if (status == HttpStatus.SC_OK){
						cloudletClient.uploadApplication(info, url);
					}
				}
			};

		}.execute();

	}

	/**
	 * REST-PUT: Upload application.
	 * 
	 * @param f
	 * @param size
	 * @param url
	 * @param progressHandler
	 */
	public void putFile(final UploadInfo info, final String url,
			final Handler progressHandler) {
		new AsyncTask<String, Integer, HttpResponse>() {

			@Override
			protected HttpResponse doInBackground(String... params) {

				HttpPut put = new HttpPut(url);
				MultipartEntity mpEntity = new MultipartEntity();
				ContentBody file = new ProgressFileBody(info.app,
						progressHandler);
				Log.d(TAG, "upload " + info.app.getAbsolutePath() + " to "
						+ url);
				try {
					ContentBody filename = new StringBody(info.app.getName());
					ContentBody length = new StringBody("" + info.size);
					mpEntity.addPart("file", file);
					mpEntity.addPart("name", filename);
					mpEntity.addPart("size", length);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return null;
				}
				put.setEntity(mpEntity);

				HttpResponse response = null;
				try {
					response = client.execute(put);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					cloudletClient.error("Could not reach " + url + "!");
					return null;
				}
				return response;
			}

			protected void onPostExecute(HttpResponse result) {
				if (result != null)
					cloudletClient.showToast(HttpUtil.getContent(result));
			};

		}.execute();

	}

	class ProgressFileBody extends FileBody {

		private final File file;
		private final Handler handler;

		public ProgressFileBody(final File file, final Handler listener) {
			super(file);
			this.file = file;
			this.handler = listener;
		}

		@Override
		public void writeTo(OutputStream out) throws IOException {
			if (out == null) {
				throw new IllegalArgumentException(
						"Output stream may not be null");
			}
			updateProgress(0.);
			InputStream in = new FileInputStream(this.file);
			try {
				byte[] tmp = new byte[4096];
				long count = 0;
				double last = 0;
				long total = this.file.length();
				int l;
				while ((l = in.read(tmp)) != -1) {
					out.write(tmp, 0, l);
					count += l;

					double p = (double) count / total;
					if ((p - last > PROGRESS_FIDELITY) || p == 1) {
						last = p;
						updateProgress(p);
					}
				}
				out.flush();
			} finally {
				in.close();
			}
		}

		private void updateProgress(double p) {
			Message msg = handler.obtainMessage(Uploader.PROGRESS_UPDATE);
			Bundle bundle = new Bundle();
			bundle.putDouble("transferred", p);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}

	class UploadInfo {
		String name;
		String checksum;
		String os;
		String type;
		String client_pkg;
		long size;
		File json;
		File app;
		int port;

		public UploadInfo() {
		}
	}
}
