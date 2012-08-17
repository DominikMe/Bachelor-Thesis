package edu.cmu.sei.dome.cloudlets.packagehandler.linux;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.packagehandler.Executor;

public class LinuxTerminalExecutor extends Executor {

	private static final String TERMINAL = "gnome-terminal";
	private static final String TERMINAL_EXECFLAG = "-x";
	protected File executable;
	protected File cwd;

	public LinuxTerminalExecutor(File cwd) {
		this.cwd = cwd;
	}

	@Override
	public Process start(String... args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.inheritIO();
		pb.directory(cwd);
		Log.println("CWD: " + pb.directory().getAbsolutePath() + "/");

		String[] cmd = new String[] { TERMINAL, TERMINAL_EXECFLAG };
		String[] cmd_args = Arrays.copyOf(cmd, cmd.length + args.length);
		System.arraycopy(args, 0, cmd_args, cmd.length, args.length);

		pb.command(cmd_args);
		return pb.start();
	}

}
