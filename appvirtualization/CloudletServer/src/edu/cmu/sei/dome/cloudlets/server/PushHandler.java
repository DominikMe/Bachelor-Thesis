package edu.cmu.sei.dome.cloudlets.server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;

import edu.cmu.sei.dome.cloudlets.log.Log;

/**
 * Uses the technique of long polling for doing Server Push.
 * 
 * @author Dome
 * 
 */
public class PushHandler {

	// maximum wait time for client response in milliseconds
	private static final int WAIT_TIMEOUT = 3000;
	private static final int SLEEP = 100;
	// maximum time for holding a client request
	private static final long CONTINUATION_TIMEOUT = 1000 * 60 * 12;

	private static final int CONTINUE = HttpServletResponse.SC_OK;
	private static final int ERROR = HttpServletResponse.SC_BAD_REQUEST;
	private static final int FINISH = HttpServletResponse.SC_GONE;

	private String appId;
	private Queue<Continuation> queue;

	public PushHandler(String appId) {
		this.appId = appId;
		queue = new LinkedList<Continuation>();
	}

	public void addRequest(Continuation continuation) {
		continuation.setTimeout(CONTINUATION_TIMEOUT);
		queue.add(continuation);
	}

	private void pushToClient(String message, boolean keepalive)
			throws IOException {
		Log.println(appId, "Respond: " + message);
		if (message == null || message.equals(""))
			return;
		Continuation continuation = waitForClientRequest();
		if (continuation == null)
			return;

		HttpServletResponse resp = (HttpServletResponse) continuation
				.getServletResponse();
		resp.setContentType("text/html");
		if (keepalive)
			// continues long polling
			resp.setStatus(CONTINUE);
		else
			// ends long polling
			resp.setStatus(FINISH);
		resp.getWriter().write(message);
		continuation.complete();
	}

	public void respond(String message) throws IOException {
		pushToClient(message, true);
	}

	public void finish(int port) throws IOException {
		pushToClient(String.format("port:%d", port), false);
	}

	public void error(String message) throws IOException {
		Log.println(appId, "Error: " + message);
		if (message == null || message.equals(""))
			return;
		Continuation continuation = waitForClientRequest();
		if (continuation == null)
			return;

		HttpServletResponse resp = (HttpServletResponse) continuation
				.getServletResponse();
		resp.setContentType("text/html");
		// ends push connection
		resp.setStatus(ERROR);
		resp.getWriter().write("ERROR: " + message);
		continuation.complete();
	}

	private Continuation waitForClientRequest() {
		long time = System.currentTimeMillis();
		while (queue.isEmpty()) {
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
			if (System.currentTimeMillis() - time > WAIT_TIMEOUT) {
				Log.println(appId, "Error: No response in queue");
				return null;
			}
		}
		Log.println(appId, "Response in queue");
		return queue.poll();
	}

	public void close() {
		queue = null;
		PushHandlerStore.close(this.appId);
	}
}
