package edu.cmu.sei.dome.cloudlets.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.util.ajax.JSON;

public class JSONServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
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
		
		// copy json to store
		File dir = new File(Commons.STORE + j.get(Commons.JSON_CHECKSUM));
		Utils.deleteRecursively(dir);
		
		dir.mkdir();
		Utils.writeInputStreamToFile(json_upload.getInputStream(),
				dir.getAbsolutePath() + "/" + j.get(Commons.JSON_NAME) + ".json");
		
		Log.println("Received json:");
		Log.println(sb.toString());

		resp.setContentType("text/html");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write(sb.toString());
	}
}
