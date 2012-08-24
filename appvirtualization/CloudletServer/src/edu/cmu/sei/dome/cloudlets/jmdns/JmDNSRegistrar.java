package edu.cmu.sei.dome.cloudlets.jmdns;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import edu.cmu.sei.dome.cloudlets.constants.CloudletProperties;

public class JmDNSRegistrar {

	public final String name;
	public final InetAddress address;
	public final int port;
	public final String properties;

	private Thread service;

	public JmDNSRegistrar(String name, InetAddress address, int port,
			CloudletProperties properties) throws IOException {
		this.name = name;
		this.address = address;
		this.port = port;
		this.properties = properties.toString();

		this.service = new Thread(new JmDNSRegistrarTask());
	}

	public void registerService() {
		this.service.start();
	}

	public void unregisterService() {
		this.service.interrupt();
	}

	private class JmDNSRegistrarTask implements Runnable {
		public void run() {
			try {
				JmDNS mdnsServer = JmDNS.create(JmDNSRegistrar.this.address,
						JmDNSRegistrar.this.name);
				ServiceInfo serviceInfo = ServiceInfo.create(
						"_http._tcp.local.", JmDNSRegistrar.this.name,
						JmDNSRegistrar.this.port,
						JmDNSRegistrar.this.properties);
				mdnsServer.registerService(serviceInfo);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
