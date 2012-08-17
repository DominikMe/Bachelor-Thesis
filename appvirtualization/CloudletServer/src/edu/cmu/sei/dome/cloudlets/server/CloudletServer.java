package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.constants.OS;
import edu.cmu.sei.dome.cloudlets.jmdns.JmDNSRegistrar;
import edu.cmu.sei.dome.cloudlets.log.Log;

public class CloudletServer {

	public static void main(String[] args) throws Exception {
		String osname = System.getProperty("os.name");
		Log.println("Running on " + osname);

		if (osname.toLowerCase().contains(OS.linux.name())) {
			Commons.MY_OS = OS.linux;
		} else if (osname.toLowerCase().contains(OS.windows.name())) {
			Commons.MY_OS = OS.windows;
		} else {
			throw new IllegalArgumentException();
		}

		Log.println("Detected a " + Commons.MY_OS + " system.");

		// make directories if they do not exist
		File store = new File(Commons.STORE);
		if (!store.isDirectory())
			store.mkdirs();
		File log = new File(Commons.LOG);
		if (!log.isDirectory())
			log.mkdirs();

		Server server = new Server(Commons.PORT);

		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		context.addServlet(new ServletHolder(new RESTservlet()), "/apps/*");

		// register cloudlet server
		// JmDNSHelper.registerService(Commons.NAME, Commons.getAttributes());
		InetAddress address = null;
		if (args.length > 0) {
			String intfName = args[0];
			final NetworkInterface intf = NetworkInterface.getByName(intfName);

			if (intf != null && intf.isUp() && intf.supportsMulticast()) {
				address = getInet4Address(intf);
			}
		}
		if (address == null)
			address = InetAddress.getLocalHost();
		Log.println(address.getHostAddress());
		JmDNSRegistrar jmdns = new JmDNSRegistrar(Commons.NAME, address,
				Commons.PORT, Commons.getAttributes());
		jmdns.registerService();

		server.start();
		server.join();
	}

	private static InetAddress getInet4Address(final NetworkInterface intf)
			throws UnknownHostException {
		Enumeration<InetAddress> addrs = intf.getInetAddresses();
		while (addrs.hasMoreElements()) {
			InetAddress ad = addrs.nextElement();
			if (ad instanceof Inet4Address)
				return ad;
		}
		return null;
	}
	
}
