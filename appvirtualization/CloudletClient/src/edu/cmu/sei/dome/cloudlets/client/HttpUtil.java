package edu.cmu.sei.dome.cloudlets.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class HttpUtil {

	private static final String TAG = "HttpUtil";
	public static final String PORT_KEY = "port";

	private HttpUtil() {
	}

	public static DefaultHttpClient getThreadSafeClient() {

		DefaultHttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();

		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);

		return client;
	}

	public static String getContent(HttpResponse response) {
		String content = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();
			String s;
			while ((s = reader.readLine()) != null) {
				sb.append(s);
				sb.append("\n");
			}
			reader.close();
			content = sb.toString();
			// remove last line feed
			content = content.substring(0,
					Math.max(0, content.lastIndexOf("\n")));
		} catch (IOException e) {
			Log.d(TAG, "Error in getContent(HttpResponse)!");
			e.printStackTrace();
		}
		return content;
	}

	public static String getIPAddressFromURL(String url) {
		String addr = url.replaceFirst("http://", "");
		addr = addr.replaceAll("/.*", "");
		addr = addr.replaceAll(":.*", "");
		return addr;
	}

	public static Map<String, String> parseFinalResponse(String content) {
		HashMap<String, String> map = new HashMap<String, String>();
		String[] entries = content.split(",");
		for (String e : entries) {
			String[] key_value = e.split(":");
			map.put(key_value[0], key_value[1]);
		}
		return map;
	}
}
