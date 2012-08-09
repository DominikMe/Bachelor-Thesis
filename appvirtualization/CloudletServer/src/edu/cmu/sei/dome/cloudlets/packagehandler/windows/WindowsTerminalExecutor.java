package edu.cmu.sei.dome.cloudlets.packagehandler.windows;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.packagehandler.Executor;

public class WindowsTerminalExecutor extends Executor {

	private static final String TERMINAL = "cmd";
	private static final String TERMINAL_FLAG = "/c";
	private static final String TERMINAL_EXECFLAG = "start";
	protected File executable;
	protected File cwd;

	public WindowsTerminalExecutor(File cwd) {
		this.cwd = cwd;
	}

	@Override
	public Process execute(String... args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(cwd);
		Log.println("CWD: " + pb.directory().getAbsolutePath());

		String[] cmd = new String[] { TERMINAL, TERMINAL_FLAG,
				TERMINAL_EXECFLAG };
		String[] cmd_args = Arrays.copyOf(cmd, cmd.length + args.length);
		System.arraycopy(args, 0, cmd_args, cmd.length, args.length);

		pb.command(cmd_args);
		return pb.start();
	}
}
