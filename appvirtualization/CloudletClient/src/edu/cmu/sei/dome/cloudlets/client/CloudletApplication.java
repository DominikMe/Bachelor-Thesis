package edu.cmu.sei.dome.cloudlets.client;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import android.app.Application;

public class CloudletApplication extends Application {

	public List<Cloudlet> servers;

	@Override
	public void onCreate() {
		super.onCreate();
		this.servers = new ArrayList<Cloudlet>();
	}

	public InetAddress getServerAddress(UploadInfo info) {
		for (Cloudlet c : servers) {
			if (info.matches(c.properties)) {
				return c.service.getInet4Addresses()[0];
			}
		}
		return null;
	}

	public int getServerPort(UploadInfo info) {
		for (Cloudlet c : servers) {
			if (info.matches(c.properties)) {
				return c.service.getPort();
			}
		}
		return -1;
	}

	public void addServer(Map<String, String> properties, ServiceInfo info) {
		servers.add(new Cloudlet(properties, info));
	}

	class Cloudlet {
		final ServiceInfo service;
		final Map<String, String> properties;

		public Cloudlet(Map<String, String> properties, ServiceInfo service) {
			this.properties = properties;
			this.service = service;
		}
	}
}
