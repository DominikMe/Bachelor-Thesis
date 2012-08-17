import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VirtualizationHelper {

	private static final String HELP = "help";
	private static final String MD5 = "md5";
	private static final String SIZE = "size";

	private static final String unknownCommand() {
		return "Unknown command. Type \'" + HELP + "\' for further information.";
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println(unknownCommand());
			return;
		}
		String cmd = args[0];
		if (cmd.equals(HELP)) {
			System.out.println("Type one of the following commands:");
			System.out.println(MD5 + " <file>\t Prints the md5 checksum.");
			System.out.println(SIZE
					+ " <file>\t Prints the file size in bytes.");
		} else if (args.length > 1 && cmd.equals(MD5)) {
			System.out.println(md5hash(new File(args[1])));
		} else if (args.length > 1 && cmd.equals(SIZE)) {
			System.out.println(new File(args[1]).length());
		} else {
			System.out.println(unknownCommand());
		}
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
