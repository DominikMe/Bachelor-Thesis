package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.constants.CloudletProperties;
import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;

public class PackageInfo {
	public final String name;
	public final String description;
	public final String checksum;
	public final String type;
	public final String clientPackage;
	public final String serverArgs;
	public final long size;
	public final int port;
	public final HashMap<String, Object>[] cloudlets;

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
	public static final FilenameFilter JSON_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".json"));
		}
	};

	@SuppressWarnings("unchecked")
	private PackageInfo(InputStream json) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(json));
		Map<String, Object> j = (HashMap<String, Object>) JSON.parse(r);
		r.close();

		this.name = (String) j.get(NAME);
		this.description = (String) j.get(DESCRIPTION);
		this.checksum = (String) j.get(CHECKSUM);
		this.type = (String) j.get(TYPE);
		this.clientPackage = (String) j.get(CLIENT_PKG);
		Object args = j.get(SERVER_ARGS);
		this.serverArgs = args == null ? "" : (String) args;
		this.port = (int) (0 + (Long) j.get(PORT));
		this.size = (Long) j.get(SIZE);

		Object c = j.get(CLOUDLET);
		Object[] cs = c == null ? new Object[] {} : (Object[]) c;
		this.cloudlets = new HashMap[cs.length];
		for (int i = 0; i < cs.length; i++) {
			this.cloudlets[i] = (HashMap<String, Object>) cs[i];
		}
	}

	/**
	 * Parses the application json file and returns the PackageInfo.
	 * 
	 * @param appId
	 * @return
	 * @throws PackageNotFoundException
	 */
	public static PackageInfo getPackageInfo(String appId)
			throws PackageNotFoundException {
		File pkg = new File(Commons.STORE + appId);
		if (!pkg.isDirectory())
			throw new PackageNotFoundException();
		File[] fs = pkg.listFiles(PackageInfo.JSON_FILTER);
		if (fs.length == 0)
			throw new PackageNotFoundException();
		File json = fs[0];

		try {
			return new PackageInfo(new FileInputStream(json));
		} catch (Exception e) {
			e.printStackTrace();
			throw new PackageNotFoundException();
		}
	}

	public static PackageInfo getPackageInfo(InputStream json)
			throws IOException {
		return new PackageInfo(json);
	}

	/**
	 * @param props
	 * @return true if one of props matches one of the legitimate cloudlets,
	 *         false otherwise
	 */
	public boolean matches(CloudletProperties props) {
		String error = "Broken cloudlet requirements or properties, respectively. Can only compare min/max for numbers.";
		if (cloudlets.length == 0) {
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
					assert (e.getValue() instanceof Long && props.get(key) instanceof Long) : error;
					Object p = props.get(key);
					if (p == null || (Long) p < (Long) e.getValue()) {
						match = false;
						break;
					}
				} else if (e.getKey().endsWith(_MAX)) {
					String key = e.getKey().substring(0,
							e.getKey().lastIndexOf(_MAX));
					assert (e.getValue() instanceof Long && props.get(key) instanceof Long) : error;
					Object p = props.get(key);
					if (p == null || (Long) p > (Long) e.getValue()) {
						match = false;
						break;
					}
				} else {
					// must be equal
					Object p = props.get(e.getKey());
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
