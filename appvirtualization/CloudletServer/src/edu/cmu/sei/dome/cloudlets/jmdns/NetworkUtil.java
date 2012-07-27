package edu.cmu.sei.dome.cloudlets.jmdns;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtil {

	public static Collection<NetworkInterface> getRunningNetInterfaces()
			throws SocketException {
		List<NetworkInterface> netintfs = new ArrayList<NetworkInterface>();
		Enumeration<NetworkInterface> intfs = NetworkInterface
				.getNetworkInterfaces();
		while (intfs.hasMoreElements()) {
			NetworkInterface intf = intfs.nextElement();
			if (intf.isUp() && intf.supportsMulticast() && !intf.isVirtual()) {
				netintfs.add(intf);
			}
		}
		return netintfs;
	}

	public static Collection<Inet4Address> filterInet4Adresses(
			Collection<NetworkInterface> intfs) {
		ArrayList<Inet4Address> addresses = new ArrayList<Inet4Address>();
		for (NetworkInterface intf : intfs) {
			Enumeration<InetAddress> addrs = intf.getInetAddresses();
			while (addrs.hasMoreElements()) {
				try {
					InetAddress address = InetAddress.getByName(addrs
							.nextElement().getHostAddress());
					if (address instanceof Inet4Address)
						addresses.add((Inet4Address) address);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		return addresses;
	}

}
