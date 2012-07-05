package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import edu.cmu.sei.dome.cloudlets.fileprocessing.FileCompressor;

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

		System.out.println("doPost triggered");
		Part upload = req.getPart("file");
		String hash = Utils.readString(req.getPart("hash"));
		String name = Utils.readString(req.getPart("name"));

		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(
				String.format("You uploaded %s, %s bytes\n", upload.getName(),
						upload.getSize()));
		
		// client must now have requested server push by sending a GET request to /push
		PushServlet push = PushServlet.getInstance();
		
		// copy file to store
		File f = copyFileToStore(name, hash, upload);
		if (f == null) {
			push.error("The uploaded application must be either a .zip or .tar.gz archive.");
			return;
		}

		// Status messages should be async
		push.respond("Decompressing started.\n", true);
		if (Commons.MY_OS == OS.linux) {
			FileCompressor.untargz(f.getAbsolutePath());
		} else if (Commons.MY_OS == OS.windows) {
			FileCompressor.unzip(f.getAbsolutePath());
		}
		push.respond("Decompressing finished.\n", true);
		

		// try to execute file
		try {
			ExecUtil.execute(hash);
		} catch (UnsupportedFileTypeException e) {
			push.error(Commons.UnsupportedFileTypeException);
			return;
		} catch (WrongOSException e) {
			push.error(Commons.WrongOSException());
			return;
		} catch (InterruptedException e) {
			push.error(Commons.InterruptedException);
		}

		push.respond("Execution started.\n", false);
	}

	private File copyFileToStore(String name, String hash, Part upload)
			throws IOException {
		if (!archiveFilter.accept(null, name)) {
			return null;
		}
		return Utils.uploadFile(upload, name, Commons.STORE + "/" + hash + "/");

	}
}
