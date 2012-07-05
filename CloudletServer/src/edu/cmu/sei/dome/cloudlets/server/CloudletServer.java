package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import edu.cmu.sei.dome.cloudlets.jmdns.JmDNSHelper;

public class CloudletServer {

	public static void main(String[] args) throws Exception {
		String osname = System.getProperty("os.name");
		Log.println("Running on " + osname);

		if (osname.toLowerCase().contains(OS.linux.name())) {
			Commons.MY_OS = OS.linux;
		} else if (osname.toLowerCase().contains(OS.windows.name())) {
			Commons.MY_OS = OS.windows;
		} else
			throw new IllegalArgumentException();

		Log.println("Detected a " + Commons.MY_OS + " system.");

		File store = new File(Commons.STORE);
		if (!store.isDirectory())
			store.mkdirs();

		Server server = new Server(Commons.PORT);

		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		
		context.addServlet(new ServletHolder(new FileUploadServlet()), "/file");
		context.addServlet(new ServletHolder(new JSONServlet()), "/json");
		context.addServlet(new ServletHolder(PushServlet.getInstance()), "/push");

		// register cloudlet server
		JmDNSHelper.registerService(Commons.NAME, Commons.getAttributes());

		server.start();
		server.join();
	}
}
