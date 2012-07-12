package edu.cmu.sei.dome.cloudlets.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.log.TimeLog;

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
				FileReader freader = new FileReader(f);
				@SuppressWarnings("unchecked")
				Map<String, Object> j = (HashMap<String, Object>) JSON
						.parse(freader);
				freader.close();
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
			if (f.isDirectory()) {// f.getName().endsWith("." + type) ||
									// f.isDirectory()) {
				exec(f, type);
				break;
			}
			// else json is wrong
		}
	}

	private static void exec(File f, String type) throws IOException,
			UnsupportedFileTypeException, InterruptedException {
		Log.println("Try to execute " + f.getName() + ".");
		if (type.equals(Commons.FILETYPE_JAR)) {
			Log.println("Run JAR.");
			runJAR(f);
		} else if (Commons.MY_OS == OS.windows) {
			if (type.equals(Commons.FILETYPE_EXE)) {
				Log.println("Run EXE.");
				runEXE(f);
			}
		} else if (Commons.MY_OS == OS.linux) {
			if (type.equals(Commons.FILETYPE_CDE)) {
				Log.println("Run CDE.");
				runCDE(f);
			} else if (type.equals(Commons.FILETYPE_REMOTE_INSTALL)) {
				Log.println("Remote install.");
				remoteInstall_Run(f);
			}
		} else
			throw new UnsupportedFileTypeException();
	}

	private static void runJAR(File jarpkg) throws IOException {
		if (jarpkg.isDirectory()) {
			for (File f : jarpkg.listFiles()) {
				if (f.getName().endsWith("." + Commons.FILETYPE_JAR)) {
					Log.println("Execute JAR: " + f.getAbsolutePath());
					f.setExecutable(true, false);
					// Runtime.getRuntime().exec(
					// "java -jar " + f.getAbsolutePath());
					if (Commons.MY_OS == OS.linux)
						runInLinuxTerminal(jarpkg,
								"java -jar " + f.getAbsolutePath());
					else if (Commons.MY_OS == OS.windows)
						runInWindowsTerminal(jarpkg,
								"java -jar " + f.getAbsolutePath());
					return;
				}
			}
		}
	}

	private static void runEXE(File exepkg) throws IOException {
		if (exepkg.isDirectory()) {
			for (File f : exepkg.listFiles()) {
				if (f.getName().endsWith("." + Commons.FILETYPE_EXE)) {
					f.setExecutable(true, false);
					Log.println("Execute EXE: " + f.getAbsolutePath());
					runInWindowsTerminal(exepkg, f.getAbsolutePath());
					return;
				}
			}
		}
	}

	private static void runCDE(File cdepkg) throws IOException,
			InterruptedException {
		if (cdepkg.isDirectory()) {
			for (String f : cdepkg.list()) {
				Log.println("CDE? " + f);
				if (f.endsWith("." + Commons.FILETYPE_CDE)) {
					Log.println("Execute CDE: " + f + ".");
					File cde = new File(cdepkg.getAbsolutePath() + "/" + f);
					cde.setExecutable(true, false);

					runInLinuxTerminal(cdepkg, "./" + cde.getName());
					return;
				}
			}
		}
	}

	private static Process runInLinuxTerminal(File cwd, String command)
			throws IOException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(cwd);
		Log.println(pb.directory().getAbsolutePath());
		String[] _cmd = command.split(" ");
		String[] cmd = new String[2 + _cmd.length];
		cmd[0] = TERMINAL;
		cmd[1] = TERMINAL_EXECFLAG;
		for (int i = 2; i < cmd.length; i++) {
			cmd[i] = _cmd[i - 2];
		}
		pb.command(cmd);
		return pb.start();
	}

	private static Process runInWindowsTerminal(File cwd, String command)
			throws IOException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(cwd);
		Log.println(pb.directory().getAbsolutePath());
		String[] _cmd = command.split(" ");
		String[] cmd = new String[3 + _cmd.length];
		cmd[0] = "cmd";
		cmd[1] = "/c";
		cmd[2] = "start";
		for (int i = 3; i < cmd.length; i++) {
			cmd[i] = _cmd[i - 3];
		}
		pb.command(cmd);
		return pb.start();
	}

	private static void remoteInstall_Run(File pkg) throws IOException {
		if (pkg.isDirectory()) {
			File setup = new File(pkg.getAbsolutePath() + "/setup");
			if (setup.isFile() && setup.canExecute()) {
				try {
					TimeLog.stamp("Start installation.");
					Log.println("Remote install: " + setup.getAbsolutePath());
					runInLinuxTerminal(pkg, setup.getAbsolutePath())
							.waitFor();
					TimeLog.stamp("Installation finished.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			for (File f : pkg.listFiles()) {
				if (!f.isDirectory() && f.canExecute()
						&& !f.getName().contains("install")) {
					Log.println("Run Binary: " + f.getAbsolutePath());
					TimeLog.stamp("Run Binary.");
					runInLinuxTerminal(pkg, f.getAbsolutePath());
					return;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		runInWindowsTerminal(
				new File(
						"C:/Users/Dome/Programmieren/Studium/Bachelorarbeit/SEI/SEIcloudlets/Cloudlet/CloudletServer/uploads/1234/Release"),
				"FaceRecognitionServer.exe");
	}

	private static boolean isValidFileType(String type) {
		if (Commons.MY_OS == OS.windows) {
			return Commons.windowsTypes.contains(type);
		} else {
			return Commons.linuxTypes.contains(type);
		}
	}

}
