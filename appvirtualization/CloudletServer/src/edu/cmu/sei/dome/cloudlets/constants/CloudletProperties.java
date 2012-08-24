package edu.cmu.sei.dome.cloudlets.constants;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.packagehandler.PackageInfo;

public class CloudletProperties {

	private final static String DELIMITER = ",";
	private final static String DEF = ":";

	private final Map<String, Object> props;

	public CloudletProperties(Map<String, Object> props) {
		this.props = props;
	}

	public static CloudletProperties getCloudletProperties(String jsonFile) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(jsonFile)));
		@SuppressWarnings("unchecked")
		Map<String, Object> props = (HashMap<String, Object>) JSON.parse(r);
		r.close();

		return new CloudletProperties(props);
	}

	public Object get(String key) {
		return props.get(key);
	}

	public boolean matches(PackageInfo info) {
		return info.matches(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> e : props.entrySet()) {
			sb.append(e.getKey());
			sb.append(DEF);
			sb.append(e.getValue());
			sb.append(DELIMITER);
		}
		String properties = sb.toString();
		properties = properties.substring(0,
				properties.length() - DELIMITER.length());
		return properties;
	}

}
