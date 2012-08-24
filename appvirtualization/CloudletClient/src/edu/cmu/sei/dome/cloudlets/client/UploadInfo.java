package edu.cmu.sei.dome.cloudlets.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.JsonReader;
import android.util.JsonToken;

public class UploadInfo {
	public final String name;
	public final String checksum;
	public final String description;
	public final String type;
	public final String clientPackage;
	public final String serverArgs;
	public final long size;
	public int port;
	public final List<HashMap<String, Object>> cloudlets;

	public final File json;
	public File app;

	private final static String NAME = "name";
	private final static String DESCRIPTION = "description";
	private final static String CHECKSUM = "checksum";
	private final static String TYPE = "type";
	private final static String CLIENT_PKG = "package";
	private final static String SIZE = "size";
	private final static String PORT = "port";
	private final static String SERVER_ARGS = "server_args";
	private static final String CLOUDLET = "cloudlet";
	private static final String _MIN = "_min";
	private static final String _MAX = "_max";

	public UploadInfo(File json) throws IOException {
		this.cloudlets = new ArrayList<HashMap<String, Object>>();
		JsonReader jsonreader = new JsonReader(new FileReader(json));
		Map<String, String> entries = new HashMap<String, String>();
		jsonreader.beginObject();
		while (jsonreader.hasNext()) {
			String name = jsonreader.nextName();
			if (name.equals(CLOUDLET)) {
				jsonreader.beginArray();
				while (jsonreader.hasNext()) {
					jsonreader.beginObject();
					HashMap<String, Object> map = new HashMap<String, Object>();
					this.cloudlets.add(map);
					while (jsonreader.hasNext()) {
						name = jsonreader.nextName();
						JsonToken token = jsonreader.peek();
						if (token == JsonToken.NUMBER)
							map.put(name, jsonreader.nextLong());
						else
							map.put(name, jsonreader.nextString());
					}
					jsonreader.endObject();
				}
				jsonreader.endArray();
			} else {
				entries.put(name, jsonreader.nextString());
			}
		}
		jsonreader.endObject();

		this.name = entries.get(NAME);
		this.checksum = entries.get(CHECKSUM);
		this.description = entries.get(DESCRIPTION);
		this.type = entries.get(TYPE);
		this.clientPackage = entries.get(CLIENT_PKG);
		this.port = Integer.parseInt(entries.get(PORT));
		Object args = entries.get(SERVER_ARGS);
		this.serverArgs = args == null ? "" : (String) args;
		this.size = Long.parseLong(entries.get(SIZE));
		this.json = json;
	}

	/**
	 * @param props
	 * @return true if one of props matches one of the legitimate cloudlets,
	 *         false otherwise
	 */
	public boolean matches(Map<String, String> props) {
		String error = "Broken cloudlet requirements or properties, respectively. Can only compare min/max for numbers.";
		if(cloudlets.isEmpty()) {
			return true;
		}
		// iterate over legitimate cloudlets
		for (Map<String, Object> reqs : cloudlets) {
			boolean match = true;
			// iterate over required cloudlet properties
			for (Entry<String, Object> e : reqs.entrySet()) {
				if (e.getKey().endsWith(_MIN)) {
					String key = e.getKey().substring(0,
							e.getKey().lastIndexOf(_MIN));
					assert (e.getValue() instanceof Long) : error;
					String p = props.get(key);
					if (p == null || Long.parseLong(p) < (Long) e.getValue()) {
						match = false;
						break;
					}
				} else if (e.getKey().endsWith(_MAX)) {
					String key = e.getKey().substring(0,
							e.getKey().lastIndexOf(_MAX));
					assert (e.getValue() instanceof Long) : error;
					String p = props.get(key);
					if (p == null || Long.parseLong(p) > (Long) e.getValue()) {
						match = false;
						break;
					}
				} else {
					// must be equal
					String p = props.get(e.getKey());
					if (p == null || !e.getValue().equals(p)) {
						match = false;
						break;
					}
				}
			}
			if (match)
				return true;
		}
		// no cloudlet matches the given props
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s: %s\n", NAME, name));
		sb.append(String.format("%s: %s\n", CHECKSUM, checksum));
		sb.append(String.format("%s: %s\n", TYPE, type));
		sb.append(String.format("%s: %s\n", CLIENT_PKG, clientPackage));
		sb.append(String.format("%s: %s\n", PORT, port));
		sb.append(String.format("%s: %s\n", SIZE, size));
		for (HashMap<String, Object> j : cloudlets) {
			sb.append(CLOUDLET + ":\n");
			for (Entry<String, Object> e : j.entrySet()) {
				sb.append(String.format("%s: %s\n", e.getKey(), e.getValue()));
			}
		}
		return sb.toString();
	}
}