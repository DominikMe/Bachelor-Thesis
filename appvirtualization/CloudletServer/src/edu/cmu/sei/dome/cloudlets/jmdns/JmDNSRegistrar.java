package edu.cmu.sei.dome.cloudlets.jmdns;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

class JmDNSRegistrar {

	public final String name;
	public final InetAddress address;
	public final int port;
	public final String description;

	private Thread service;

	public static void main(String[] args) throws IOException {
		new JmDNSRegistrar("Bonjour", InetAddress.getLocalHost(), 8080, "Ja hallo erstmal.")
				.registerService();
	}

	public JmDNSRegistrar(String name, InetAddress address, int port, String description) {
		this.name = name;
		this.address = address;
		this.port = port;
		this.description = description;

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
				ServiceInfo service = ServiceInfo.create("_http._tcp.local.",
						JmDNSRegistrar.this.name, JmDNSRegistrar.this.port,
						JmDNSRegistrar.this.description);
				mdnsServer.registerService(service);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
