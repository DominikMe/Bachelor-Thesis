package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;

public final class PackageInfo {

	public final String os;
	public final String type;

	public static final FilenameFilter JSON_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".json"));
		}
	};

	private PackageInfo(String os, String type) {
		this.os = os;
		this.type = type;
	}

	public static PackageInfo getPackageInfo(String pkgId)
			throws PackageNotFoundException {
		File pkg = new File(Commons.STORE + pkgId);
		if (!pkg.isDirectory())
			throw new PackageNotFoundException();
		File[] fs = pkg.listFiles(PackageInfo.JSON_FILTER);
		if (fs.length == 0)
			throw new PackageNotFoundException();
		File json = fs[0];

		// analyse json
		FileReader freader;
		try {
			freader = new FileReader(json);
			@SuppressWarnings("unchecked")
			Map<String, Object> j = (HashMap<String, Object>) JSON
					.parse(freader);
			freader.close();
			String os = (String) j.get(Commons.JSON_OS);
			String type = (String) j.get(Commons.JSON_TYPE);

			return new PackageInfo(os, type);
		} catch (Exception e) {
			e.printStackTrace();
			throw new PackageNotFoundException();
		}
	}
}
