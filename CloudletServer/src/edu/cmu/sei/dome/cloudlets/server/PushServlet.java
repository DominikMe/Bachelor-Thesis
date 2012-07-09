package edu.cmu.sei.dome.cloudlets.server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import edu.cmu.sei.dome.cloudlets.log.Log;

/**
 * Uses the technique of long polling for doing Server Push.
 * 
 * @author Dome
 * 
 */
public class PushServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// maximum wait time for client response in milliseconds
	private static final int WAIT_TIMEOUT = 3000;
	private static final int SLEEP = 100;
	// maximum time for holding a client request
	private static final long CONTINUATION_TIMEOUT = 1000 * 60 * 12;

	private static final String TAG = "PushServlet";

	private static PushServlet instance = null;

	private Queue<Continuation> queue = new LinkedList<Continuation>();

	private PushServlet() {

	}

	public static synchronized PushServlet getInstance() {
		if (instance == null) {
			instance = new PushServlet();
		}
		return instance;
	}

	@Override
	protected synchronized void doGet(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		Continuation continuation = (Continuation) ContinuationSupport.getContinuation(req);
		continuation.setTimeout(CONTINUATION_TIMEOUT);
		continuation.suspend(resp);
		queue.add(continuation);
	}

	public void respond(String message, boolean keepalive) throws IOException {
		Log.println("[" + TAG + "] Respond: " + message);
		if (message == null || message.equals(""))
			return;
		Continuation continuation = waitForClient();
		if(continuation == null)
			return;

		HttpServletResponse resp = (HttpServletResponse) continuation.getServletResponse();
		resp.setContentType("text/html");
		if (keepalive)
			// continues push connection
			resp.setStatus(HttpServletResponse.SC_OK);
		else
			// ends push connection
			resp.setStatus(HttpServletResponse.SC_GONE);
		resp.getWriter().write(message);
		continuation.complete();
	}

	public void error(String message) throws IOException {
		Log.println("[" + TAG + "] Error: " + message);
		if (message == null || message.equals(""))
			return;
		Continuation continuation = waitForClient();
		if(continuation == null)
			return;

		HttpServletResponse resp = (HttpServletResponse) continuation.getServletResponse();
		resp.setContentType("text/html");
		// ends push connection
		resp.setStatus(HttpServletResponse.SC_GONE);
		resp.getWriter().write("ERROR: " + message);
		continuation.complete();
	}

	private Continuation waitForClient() {
		long time = System.currentTimeMillis();
		while (queue.isEmpty()) {
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
			if (System.currentTimeMillis() - time > WAIT_TIMEOUT) {
				Log.println("[" + TAG + "] Error: No response in queue");
				return null;
			}
		}
		Log.println("[" + TAG + "] Response in queue");
		return queue.poll();
	}

}
