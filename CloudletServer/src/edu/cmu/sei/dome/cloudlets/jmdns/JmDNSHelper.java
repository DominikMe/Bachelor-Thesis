package edu.cmu.sei.dome.cloudlets.jmdns;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;

import edu.cmu.sei.dome.cloudlets.server.Commons;
import edu.cmu.sei.dome.cloudlets.log.Log;

public class JmDNSHelper {

	private JmDNSHelper() {
	}

	/**
	 * Registers the jmdns Service with all IPv4 addresses excluding localhost
	 * and loopback addresses.
	 * 
	 * @param name
	 * @param attributes
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public static void registerService(String name, String attributes)
			throws SocketException, UnknownHostException {
		Collection<Inet4Address> addresses = NetworkUtil
				.filterInet4Adresses(NetworkUtil.getRunningNetInterfaces());
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddress loopback = InetAddress.getLoopbackAddress();
		for (InetAddress address : addresses) {
			if (!localhost.equals(address) && !loopback.equals(address)) {
				Log.println(address);
				JmDNSRegistrar jmdns = new JmDNSRegistrar(name, address,
						Commons.PORT, attributes);
				jmdns.registerService();
			}
		}
	}

}
