package edu.cmu.sei.dome.cloudlets.client;

import java.net.InetAddress;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jmdns.ServiceInfo;

import android.app.Application;

public class CloudletApplication extends Application {

	public LinkedBlockingQueue<ServiceInfo> linuxServers;
	public LinkedBlockingQueue<ServiceInfo> windowsServers;

	@Override
	public void onCreate() {
		super.onCreate();
		this.linuxServers = new LinkedBlockingQueue<ServiceInfo>();
		this.windowsServers = new LinkedBlockingQueue<ServiceInfo>();
	}

	public InetAddress getLinuxServerAddress() {
		if (linuxServers.isEmpty())
			return null;
		return linuxServers.peek().getInet4Addresses()[0];
	}
	
	public int getLinuxServerPort() {
		if (linuxServers.isEmpty())
			return -1;
		return linuxServers.peek().getPort();
	}
	
	public InetAddress getWindowsServerAddress() {
		if (windowsServers.isEmpty())
			return null;
		return windowsServers.peek().getInet4Addresses()[0];
	}
	
	public int getWindowsServerPort() {
		if (windowsServers.isEmpty())
			return -1;
		return windowsServers.peek().getPort();
	}
}
