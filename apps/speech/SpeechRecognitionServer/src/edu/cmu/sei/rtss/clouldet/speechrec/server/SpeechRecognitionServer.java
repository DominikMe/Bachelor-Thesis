/**
---------------------
Copyright 2012 Carnegie Mellon University

This material is based upon work funded and supported by the Department of Defense under Contract No. 
FA8721-05-C-0003 with Carnegie Mellon University for the operation of the Software Engineering Institute, 
a federally funded research and development center.

Any opinions, findings and conclusions or recommendations expressed in this material are those of the 
author(s) and do not necessarily reflect the views of the United States Department of Defense.

NO WARRANTY
THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN “AS-IS” 
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY 
MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, 
OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF 
ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

This material contains SEI Proprietary Information and may not be disclosed outside of the SEI without 
the written consent of the Director’s Office and completion of the Disclosure of Information process.
------------
**/

/** Last changed: $LastChangedDate:$
 * Last changed by: $Author:$
 * @version $Revision:$* 
 */

package edu.cmu.sei.rtss.clouldet.speechrec.server;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * 
 * @author ssimanta
 * 
 */
public class SpeechRecognitionServer {

	public static final int DEFAULT_SERVER_PORT = 10191;

	public static void main(String[] args) throws Exception {

		int serverPort = DEFAULT_SERVER_PORT;
		if (args == null || args.length == 0) {
			System.out.println("Using default server port "
					+ DEFAULT_SERVER_PORT);
		} else if (args.length == 1)
			try {
				serverPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				System.err.println("Input port " + serverPort
						+ "is invalid. Using default port "
						+ DEFAULT_SERVER_PORT);

			}

		SpeechRecognitionServer server = new SpeechRecognitionServer();
		server.run(serverPort);
	}

	public void run(final int port) {
		ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

		ServerBootstrap bootstrap = new ServerBootstrap(factory);

		bootstrap.setPipelineFactory(new SpeechServerPipelineFactory());

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.bufferFactory",
				new HeapChannelBufferFactory(ByteOrder.BIG_ENDIAN));

		bootstrap.bind(new InetSocketAddress(port));
		System.out.println("SpeechRecognitionServer V5.0 listening on " + port
				+ " ...");
	}

}
