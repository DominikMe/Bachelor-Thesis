import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VirtualizationHelper {

	private static final String PROMPT = ">> ";

	private static final String HELP = "help";
	private static final String EXIT = "exit";
	private static final String MD5 = "md5";
	private static final String SIZE = "size";
	private static final String CREATE = "create";

	private static final String unknownCommand() {
		return "Unknown command. Type \'" + HELP
				+ "\' for further information.";
	}

	public static void main(String[] args) throws Exception {
		String cmd = "";
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		do {
			System.out.print(PROMPT);
			String[] input = in.readLine().split(" ");
			cmd = input[0];

			if (cmd.equals(HELP)) {
				System.out.println("Type one of the following commands:");
				System.out.println(MD5 + " <file>\t Prints the md5 checksum.");
				System.out.println(SIZE
						+ " <file>\t Prints the file size in bytes.");
				System.out.println(CREATE
						+ " <file>\t Creates a file at this path.");
				System.out.println(EXIT + " \t Exit the program.");
			} else if (cmd.equals(EXIT)) {
				return;
			} else if (input.length > 1 && cmd.equals(MD5)) {
				System.out.println(md5hash(new File(input[1])));
			} else if (input.length > 1 && cmd.equals(SIZE)) {
				System.out.println(new File(input[1]).length());
			} else if (input.length > 1 && cmd.equals(CREATE)) {
				System.out.println(new File(input[1]).createNewFile());
			} else {
				System.out.println(unknownCommand());
			}
		} while (!cmd.equals(EXIT));

	}

	public static String md5hash(File file) throws NoSuchAlgorithmException,
			IOException {
		if (!file.isFile())
			throw new IllegalArgumentException();
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] data = new byte[8192];
		DigestInputStream is = new DigestInputStream(new FileInputStream(file),
				md);
		int n;
		do {
			n = is.read(data);
		} while (n > 0);
		is.close();
		return new BigInteger(1, md.digest()).toString(md.getDigestLength());
	}
}
