package edu.cmu.sei.dome.cloudlets.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.fileprocessing.FileUtils;
import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.log.TimeLog;
import edu.cmu.sei.dome.cloudlets.log.TimeLogStore;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageHandler;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageInfo;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.InvalidCloudletException;

public class RESTservlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final FilenameFilter ARCHIVE_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".zip") || filename.endsWith(".tar.gz"));
		}
	};

	/**
	 * Return status of an application. REST-GET
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 *      , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// get application ID, strip first character, i.e. slash
		String appId = req.getPathInfo().substring(1);
		Continuation continuation = (Continuation) ContinuationSupport
				.getContinuation(req);
		continuation.suspend(resp);
		PushHandler push = PushHandlerStore.getPushHandler(appId);
		push.addRequest(continuation);
	}

	/**
	 * Upload an application to the cloudlet. REST-PUT
	 * 
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// get application ID, strip first character, i.e. slash
		String appId = req.getPathInfo().substring(1);

		Part upload = req.getPart("file");
		String name = readString(req.getPart("name"));
		long size = Long.parseLong(readString(req.getPart("size")));

		Log.println(appId, "FileServlet.doPost has been triggered.");
		TimeLog timeLog = TimeLogStore.getTimeLog(appId);
		timeLog.stamp("Application upload started.");


		// client must now already have requested server push by sending a GET
		// request
		PushHandler push = PushHandlerStore.getPushHandler(appId);

		// copy file to store
		if (!ARCHIVE_FILTER.accept(null, name)) {
			push.error("The uploaded application must be either a .zip or .tar.gz archive.");
			return;
		}
		File f = FileUtils.saveUpload(upload, name, appId);
		timeLog.stamp("Application upload finished.");

		// check md5hash
		try {
			String checkhash = FileUtils.md5hash(f);
			if (!appId.equals(checkhash)) {
				push.error(Commons.ChecksumException);
				// backtrack: delete application folder
				FileUtils.deleteRecursively(new File(Commons.STORE + appId));
				return;
			}
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			push.error(Commons.GeneralException);
			return;
		}
		timeLog.stamp("File checksum verified.");

		// check file size
		if (size != f.length()) {
			push.error(Commons.FilesizeException);
			// backtrack: delete application folder
			FileUtils.deleteRecursively(new File(Commons.STORE + appId));
			return;
		}
		timeLog.stamp("File size verified.");

		PackageHandler pkgHandler = PackageHandler.getInstance(Commons.MY_OS);
		PackageInfo info = null;

		// decompress
		push.respond("Decompress\n");
		try {
			pkgHandler.decompress(appId);
		} catch (PackageNotFoundException e1) {
			push.error(Commons.PackageNotFoundException);
			e1.printStackTrace();
		}
		timeLog.stamp("Application decompressed.");

		// try to execute file
		try {
			// start application with arguments from packageinfo
			info = PackageInfo.getPackageInfo(appId);
			pkgHandler.execute(appId).start(info.serverArgs.split(" "));
		} catch (UnsupportedFileTypeException e) {
			push.error(Commons.UnsupportedFileTypeException);
			e.printStackTrace();
		} catch (PackageNotFoundException e) {
			push.error(Commons.PackageNotFoundException);
			e.printStackTrace();
		} catch (InvalidCloudletException e) {
			push.error(Commons.InvalidCloudletException());
			e.printStackTrace();
		}
		timeLog.stamp("Application executed.");
		String time = new SimpleDateFormat("yyyy-MM-dd HH-mm ")
				.format(new Date());
		timeLog.writeToFile(Commons.LOG + time + name + ".txt");
		timeLog.close();

		push.respond("Execute\n");

		// STUB
		// Just returns the port from the package info. Some advanced logic on
		// what port to run the application and then send this port back to the
		// mobile client would be great.
		push.finish(info.port);
		push.close();
	}

	/**
	 * Post application metadata to the cloudlet. REST-POST
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// get application ID, strip first character, i.e. slash
		String appId = req.getPathInfo().substring(1);

		resp.setContentType("text/html");

		// APPLICATION IS CACHED
		if (new File(Commons.STORE + appId).isDirectory()) {
			PackageHandler pkgHandler = PackageHandler
					.getInstance(Commons.MY_OS);
			try {
				PackageInfo info = PackageInfo.getPackageInfo(appId);
				pkgHandler.execute(appId).start(info.serverArgs.split(" "));
				// tell client to use existing package
				Log.println(appId, "Execute cached application.");
				resp.setStatus(HttpServletResponse.SC_GONE);
				resp.getWriter().write(String.format("port:%d", info.port));

				return;

			} catch (Exception e) {
				e.printStackTrace();
				// let client upload and overwrite existing malfunctioning
				// package
				Log.println(appId, "Cached application is broken.");
			}
		}

		// NEW APPLICATION - NOT CACHED
		Log.println(appId, "Application not cached, wait for upload.");

		TimeLog timeLog = TimeLogStore.getTimeLog(appId);
		timeLog.stamp("JSON upload started.");

		// parse json file
		Part jsonUpload = req.getPart("json");
		PackageInfo info = PackageInfo.getPackageInfo(jsonUpload
				.getInputStream());
		timeLog.stamp("JSON has been parsed.");

		// copy json to store
		File dir = new File(Commons.STORE + appId);
		FileUtils.deleteRecursively(dir);

		dir.mkdir();
		FileUtils.saveUpload(jsonUpload, info.name + ".json", ""
				+ info.checksum);

		timeLog.stamp("JSON saved to disk.");

		Log.println(appId, "Received json:");
		Log.println(appId, info);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(info.toString());

	}

	/**
	 * Delete application from the cloudlet. REST-DELETE
	 * 
	 * @see javax.servlet.http.HttpServlet#doDelete(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// get application ID, strip first character, i.e. slash
		String appId = req.getPathInfo().substring(1);

		FileUtils.deleteRecursively(new File(Commons.STORE + appId));
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write("Application " + appId + " has been deleted.");
	}

	private String readString(Part name) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				name.getInputStream()));
		String filename = r.readLine();
		r.close();
		return filename;
	}

}
