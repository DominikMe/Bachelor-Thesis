package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import edu.cmu.sei.dome.cloudlets.fileprocessing.Utils;
import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.log.TimeLog;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageHandler;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;

;

public class FileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final FilenameFilter archiveFilter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".zip") || filename.endsWith(".tar.gz"));
		}
	};

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("Hello to the fileServlet GET page!");
		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write("Hello to the fileServlet GET page!");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		Log.println("FileServlet.doPost has been triggered.");
		TimeLog.stamp("Application upload started.");
		Part upload = req.getPart("file");
		String hash = Utils.readString(req.getPart("hash"));
		String name = Utils.readString(req.getPart("name"));
		long size = Long.parseLong(Utils.readString(req.getPart("size")));

		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(
				String.format("You uploaded %s, %s bytes\n", upload.getName(),
						upload.getSize()));

		// client must now have requested server push by sending a GET request
		// to /push
		PushServlet push = PushServlet.getInstance();

		// copy file to store
		File f = copyFileToStore(name, hash, upload);
		if (f == null) {
			push.error("The uploaded application must be either a .zip or .tar.gz archive.");
			return;
		}
		TimeLog.stamp("Application upload finished.");

		// check md5hash
		try {
			String checkhash = Utils.md5hash(f);
			if (!hash.equals(checkhash)) {
				push.error(Commons.ChecksumException);
				// backtrack: delete application folder
				Utils.deleteRecursively(new File(Commons.STORE + hash));
				return;
			}
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			push.error(Commons.GeneralException);
			return;
		}
		TimeLog.stamp("File checksum verified.");

		// check file size
		if (size != f.length()) {
			push.error(Commons.FilesizeException);
			// backtrack: delete application folder
			Utils.deleteRecursively(new File(Commons.STORE + hash));
			return;
		}
		TimeLog.stamp("File size verified.");

		PackageHandler pkgHandler = new PackageHandler(Commons.MY_OS);

		// decompress
		push.respond("Decompress\n", true);
		try {
			pkgHandler.decompress(hash);
		} catch (PackageNotFoundException e1) {
			push.error(Commons.PackageNotFoundException);
			e1.printStackTrace();
		}
		TimeLog.stamp("Application decompressed.");

		// try to execute file
		try {
			pkgHandler.execute(hash);
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
		TimeLog.stamp("Application executed.");
		String time = new SimpleDateFormat("yyyy-MM-dd HH-mm ")
				.format(new Date());
		TimeLog.writeToFile(Commons.LOG + time + name + ".txt");
		TimeLog.reset();

		push.respond("Execute\n", false);
	}

	private File copyFileToStore(String name, String hash, Part upload)
			throws IOException {
		if (!archiveFilter.accept(null, name)) {
			return null;
		}
		return Utils.uploadFile(upload, name, Commons.STORE + "/" + hash + "/");

	}
}
