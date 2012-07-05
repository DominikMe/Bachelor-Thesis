package edu.cmu.sei.dome.cloudlets.jmdns;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class JmDNSDiscoverer {
	
	public static void main(String[] args) {
		new JmDNSDiscoverer().startDiscovery();
	}
	

	JmDNS mdnsService;
	final static String TAG = "JmDNSDiscoverer";
	private JmDNSDiscovererThread discoverer;

	public JmDNSDiscoverer() {
		this.discoverer = new JmDNSDiscovererThread();
	}

	public void startDiscovery() {
		System.out.println("start cloudlet discovery");
		discoverer.start();
	}

	public void stopDiscovery() {
		discoverer.stopDiscovery();
		discoverer.interrupt();
	}

	private ServiceListener mdnsListener = new ServiceListener() {

		public void serviceAdded(ServiceEvent event) {
			System.out.println("service added");
			mdnsService.requestServiceInfo(event.getType(), event.getName());
		}

		public void serviceResolved(ServiceEvent event) {
			System.out.println("service resolved");
			ServiceInfo info = event.getInfo();
			System.out.println(String.format("%s (%s), running on %s:%s",
					info.getName(), info.getNiceTextString(), info.getDomain(),
					info.getPort()));
		}

		public void serviceRemoved(ServiceEvent event) {
			// TODO Auto-generated method stub
			System.out.println("service removed");
		}

	};

	private class JmDNSDiscovererThread extends Thread {

		@Override
		public void run() {
			try {
				mdnsService = JmDNS.create();
			} catch (IOException e) {
				System.out.println("NetworkError: Service discovery failed.");
			}
			mdnsService.addServiceListener("_http._tcp.local.", mdnsListener);
		}

		public void stopDiscovery() {
			mdnsService
					.removeServiceListener("_http._tcp.local.", mdnsListener);
			try {
				mdnsService.close();
			} catch (IOException e) {
				System.out.println("NetworkError: Service discovery failed.");
				e.printStackTrace();
			}
		}
	}
}
