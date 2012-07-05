package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

public class ExecUtil {
	private static final String TERMINAL = "gnome-terminal";
	private static final String TERMINAL_EXECFLAG = "-x";

	public static void execute(String hash) throws FileNotFoundException,
			IOException, UnsupportedFileTypeException, WrongOSException,
			InterruptedException {
		File dir = new File(Commons.STORE + "/" + hash);
		if (!dir.isDirectory())
			throw new IllegalArgumentException();
		File[] files = dir.listFiles();
		String type = null;
		// analyse json
		for (File f : files) {
			if (f.getName().endsWith(".json")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> j = (HashMap<String, Object>) JSON
						.parse(new FileReader(f));
				String os = (String) j.get(Commons.JSON_OS);
				Log.println("Compare OS: " + os + " vs "
						+ Commons.MY_OS.toString());
				if (!os.toLowerCase().equals(Commons.MY_OS.toString())) {
					throw new WrongOSException();
				}
				type = (String) j.get(Commons.JSON_TYPE);
				Log.println("Filetype is " + type + ".");
				if (!isValidFileType(type))
					throw new UnsupportedFileTypeException();
				break;
			}
		}
		// analyse and run executable
		for (File f : files) {
			if (f.getName().endsWith("." + type) || f.isDirectory()) {
				exec(f, type);
				break;
			}
			// else json is wrong
		}
	}

	private static void exec(File f, String type) throws IOException,
			UnsupportedFileTypeException, InterruptedException {
		Log.println("Try to execute " + f.getName() + ".");
		if (Commons.MY_OS == OS.windows) {
			if (type.equals(Commons.FILETYPE_EXE)
					&& f.getName().endsWith("." + Commons.FILETYPE_EXE)) {
				Log.println("Run EXE.");
				f.setExecutable(true, false);
				Log.println("Execute EXE: " + f.getAbsolutePath());
				Runtime.getRuntime().exec(f.getAbsolutePath());
			}
		} else if (Commons.MY_OS == OS.linux) {
			if (type.equals(Commons.FILETYPE_CDE)) {
				Log.println("Run CDE.");
				execCDE(f);
			}
		} else
			throw new UnsupportedFileTypeException();
	}

	private static void execCDE(File cdepkg) throws IOException,
			InterruptedException {
		if (cdepkg.isDirectory()) {
			for (String f : cdepkg.list()) {
				Log.println("CDE? " + f);
				if (f.endsWith("." + Commons.FILETYPE_CDE)) {
					Log.println("Execute CDE: " + f + ".");
					File cde = new File(cdepkg.getAbsolutePath() + "/" + f);
					cde.setExecutable(true, false);

					ProcessBuilder pb = new ProcessBuilder();
					pb.directory(cdepkg);
					Log.println(pb.directory().getAbsolutePath());
					String[] cmd = new String[] { TERMINAL, TERMINAL_EXECFLAG,
							"./" + cde.getName() };
					pb.command(cmd);
					pb.start();
					return;
				}
			}
		}
	}

	private static boolean isValidFileType(String type) {
		if (Commons.MY_OS == OS.windows) {
			return Commons.windowsTypes.contains(type);
		} else {
			return Commons.linuxTypes.contains(type);
		}
	}

}
