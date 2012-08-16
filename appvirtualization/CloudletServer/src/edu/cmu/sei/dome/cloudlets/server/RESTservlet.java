package edu.cmu.sei.dome.cloudlets.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.fileprocessing.Utils;
import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.log.TimeLog;
import edu.cmu.sei.dome.cloudlets.log.TimeLogStore;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageHandler;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;

public class RESTservlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final FilenameFilter archiveFilter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".zip") || filename.endsWith(".tar.gz"));
		}
	};

	/**
	 * Return status of an application, REST-GET
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
		String name = Utils.readString(req.getPart("name"));
		long size = Long.parseLong(Utils.readString(req.getPart("size")));

		Log.println(appId, "FileServlet.doPost has been triggered.");
		TimeLog timeLog = TimeLogStore.getTimeLog(appId);
		timeLog.stamp("Application upload started.");

		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(
				String.format("You uploaded %s, %s bytes\n", upload.getName(),
						upload.getSize()));

		// client must now already have requested server push by sending a GET
		// request
		PushHandler push = PushHandlerStore.getPushHandler(appId);

		// copy file to store
		File f = copyFileToStore(name, appId, upload);
		if (f == null) {
			push.error("The uploaded application must be either a .zip or .tar.gz archive.");
			return;
		}
		timeLog.stamp("Application upload finished.");

		// check md5hash
		try {
			String checkhash = Utils.md5hash(f);
			if (!appId.equals(checkhash)) {
				push.error(Commons.ChecksumException);
				// backtrack: delete application folder
				Utils.deleteRecursively(new File(Commons.STORE + appId));
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
			Utils.deleteRecursively(new File(Commons.STORE + appId));
			return;
		}
		timeLog.stamp("File size verified.");

		PackageHandler pkgHandler = new PackageHandler(Commons.MY_OS);

		// decompress
		push.respond("Decompress\n", true);
		try {
			pkgHandler.decompress(appId);
		} catch (PackageNotFoundException e1) {
			push.error(Commons.PackageNotFoundException);
			e1.printStackTrace();
		}
		timeLog.stamp("Application decompressed.");

		// try to execute file
		try {
			pkgHandler.execute(appId).execute(new String[] {});
		} catch (UnsupportedFileTypeException e) {
			push.error(Commons.UnsupportedFileTypeException);
			e.printStackTrace();
		} catch (PackageNotFoundException e) {
			push.error(Commons.PackageNotFoundException);
			e.printStackTrace();
		} catch (WrongOSException e) {
			push.error(Commons.WrongOSException());
			e.printStackTrace();
		}
		timeLog.stamp("Application executed.");
		String time = new SimpleDateFormat("yyyy-MM-dd HH-mm ")
				.format(new Date());
		timeLog.writeToFile(Commons.LOG + time + name + ".txt");
		timeLog.close();

		push.respond("Execute\n", false);
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

		TimeLog timeLog = TimeLogStore.getTimeLog(appId);
		timeLog.stamp("JSON upload started.");
		Part json_upload = req.getPart("json");
		BufferedReader r = new BufferedReader(new InputStreamReader(
				json_upload.getInputStream()));

		// parse json file
		@SuppressWarnings("unchecked")
		Map<String, Object> j = (HashMap<String, Object>) JSON.parse(r);

		// DEBUG
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, Object>> it = j.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> e = (Entry<String, Object>) it.next();
			sb.append(e.getKey() + ": " + e.getValue() + "\n");
		}
		timeLog.stamp("JSON has been parsed.");

		// copy json to store
		File dir = new File(Commons.STORE + j.get(Commons.JSON_CHECKSUM));
		Utils.deleteRecursively(dir);

		dir.mkdir();
		Utils.writeInputStreamToFile(json_upload.getInputStream(),
				dir.getAbsolutePath() + "/" + j.get(Commons.JSON_NAME)
						+ ".json");

		timeLog.stamp("JSON saved to disk.");

		Log.println(appId, "Received json:");
		Log.println(appId, sb.toString());

		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(sb.toString());

	}

	private File copyFileToStore(String name, String hash, Part upload)
			throws IOException {
		if (!archiveFilter.accept(null, name)) {
			return null;
		}
		return Utils.uploadFile(upload, name, Commons.STORE + "/" + hash + "/");
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

		Utils.deleteRecursively(new File(Commons.STORE + appId));
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write("Application " + appId + " has been deleted.");
	}

}
