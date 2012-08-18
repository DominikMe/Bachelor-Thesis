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

import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;

public class PackageInfo {
	public final String name;
	public final String checksum;
	public final String os;
	public final String type;
	public final String client_package;
	public final long size;
	public final int port;

	private final static String NAME = "name";
	private final static String CHECKSUM = "checksum";
	private final static String OS = "os";
	private final static String TYPE = "type";
	private final static String CLIENT_PKG = "package";
	private final static String SIZE = "size";
	private final static String PORT = "port";
	public static final FilenameFilter JSON_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".json"));
		}
	};

	private PackageInfo(InputStream json) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(json));
		@SuppressWarnings("unchecked")
		Map<String, Object> j = (HashMap<String, Object>) JSON.parse(r);
		r.close();

		this.name = (String) j.get(NAME);
		this.checksum = (String) j.get(CHECKSUM);
		this.os = (String) j.get(OS);
		this.type = (String) j.get(TYPE);
		this.client_package = (String) j.get(CLIENT_PKG);
		this.port = (int)(0 + (Long) j.get(PORT));
		this.size = (Long) j.get(SIZE);
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

	public static PackageInfo getPackageInfo(InputStream json) throws IOException {
		return new PackageInfo(json);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s: %s\n", NAME, name));
		sb.append(String.format("%s: %s\n", CHECKSUM, checksum));
		sb.append(String.format("%s: %s\n", OS, os));
		sb.append(String.format("%s: %s\n", TYPE, type));
		sb.append(String.format("%s: %s\n", CLIENT_PKG, client_package));
		sb.append(String.format("%s: %s\n", PORT, port));
		sb.append(String.format("%s: %s\n", SIZE, size));
		return sb.toString();
	}

}
