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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Lattice;
import edu.cmu.sphinx.result.LatticeOptimizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * 
 * @author ssimanta
 * 
 */
public class SpeechRecognitionServerHandler extends SimpleChannelHandler {

	private Logger logger = Logger.getLogger(SpeechRecognitionServer.class
			.getName());

	private URL audioURL;
	private URL configURL;

	private File audioFile;

	public static final String FILE_PREFIX = "audio";
	public static final String FILE_SUFFIX = ".wav";
	public static final String CONFIG_FILE_PATH = "./config/config.xml";

	private ConfigurationManager configurationManager;
	private Recognizer recognizer;
	private AudioFileDataSource dataSource;
	private FileOutputStream fileOutputStream;

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		super.channelConnected(ctx, e);
		logger.info("Client channel connected ... ");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

		// we know the buffer contains only the contents of the audio file
		try {
			audioFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
			long start = System.currentTimeMillis();
			fileOutputStream = new FileOutputStream(audioFile);
			int payloadSize = buffer.array().length;

			// while (buffer.readable()) {
			fileOutputStream.write(buffer.readBytes(payloadSize).array());
			// }
			fileOutputStream.flush();
			long end = System.currentTimeMillis();
			long tempFileTime = (end - start);

			logger.log(Level.INFO, "Created a temp audio file of size "
					+ audioFile.length() + " at " + audioFile.getPath()
					+ " in " + tempFileTime + " ms.");
			start = System.currentTimeMillis();
			String output = getSpeechOutput(audioFile);
			end = System.currentTimeMillis();
			logger.log(Level.INFO, "Time required to process speech file "
					+ (end - start) + " ms.");
			long time = (end - start);
			Channel channel = e.getChannel();
			// clean up the file.
			fileOutputStream.close();
			audioFile.delete();
			channel.write(output + "[file_create: " + tempFileTime
					+ "ms , processing: " + time + " ms. ]");

		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		e.getCause().printStackTrace();

		Channel ch = e.getChannel();
		ch.close();
	}

	private String getSpeechOutput(final File tempAudioFile) throws IOException {

		audioURL = new File(tempAudioFile.getAbsolutePath()).toURI().toURL();
		logger.log(Level.INFO, "audio url:" + audioURL);

		/** IMP: configurationManager, recognizer and dataSource can be used across multiple 
		 *  Netty Handler calls because they are class variables of the Handler class. */
		if (configurationManager == null) {
			logger.log(Level.INFO, "config url: " + configURL);
			configURL = new URL("file:" + CONFIG_FILE_PATH);
			configurationManager = new ConfigurationManager(configURL);
		}
		
		/** allocate() is a very expensive operation. Do it only if and when required. 
		 *  IMP: I'm not sure that the same recognizer can be reused for multiple interactions. */
		if (recognizer == null) {
			recognizer = (Recognizer) configurationManager.lookup("recognizer");
			recognizer.allocate();
		}

		if (dataSource == null) {
			dataSource = (AudioFileDataSource) configurationManager
					.lookup("audioFileDataSource");
		}

		dataSource.setAudioFile(audioURL, null);

		boolean done = false;

		StringBuffer outputBuffer = new StringBuffer();
		while (!done) {
			Result result = recognizer.recognize();
			if (result != null) {
				Lattice lattice = new Lattice(result);
				LatticeOptimizer optimizer = new LatticeOptimizer(lattice);
				optimizer.optimize();
				String resultText = result.getBestResultNoFiller();
				// logger.log(Level.INFO, "[ Best Match ]: " + resultText);
				outputBuffer.append(resultText).append(" ");
			} else {
				done = true;
			}
		}


		return outputBuffer.toString();
	}
}
