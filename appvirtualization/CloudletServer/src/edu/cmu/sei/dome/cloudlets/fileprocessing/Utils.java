package edu.cmu.sei.dome.cloudlets.fileprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.Part;

/**
 * @author Dome
 * 
 */
public final class Utils {

	private Utils() {
	}

	public static void main(String[] args) throws Exception {
		// System.out
		// .println(Utils
		// .md5hash(new File(
		// "C:/Users/Dome/Programmieren/Studium/Bachelorarbeit/SEI/SEIcloudlets/Cloudlet/appvirtualization/Face Recognition/FaceRec.zip")));
		System.out
				.println(md5hash(new File(
						"C:/Users/Dome/Programmieren/Studium/Bachelorarbeit/SEI/SEIcloudlets/Cloudlet/remoteinstall/apps/RemoteInstall Object Recognition/moped_12.04_remote_install.tar.gz")));
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

	public static File uploadFile(Part upload, String filename, String location)
			throws IOException {
		String path = location + filename;
		Utils.writeInputStreamToFile(upload.getInputStream(), path);
		File f = new File(path);
		return f;
	}

	public static String readString(Part name) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				name.getInputStream()));
		String filename = r.readLine();
		r.close();
		return filename;
	}

	/**
	 * @param in
	 * @param file
	 *            gets overwritten if already exists
	 * @throws IOException
	 */
	public static void writeInputStreamToFile(InputStream in, String file)
			throws IOException {
		Files.copy(in, FileSystems.getDefault().getPath(file),
				StandardCopyOption.REPLACE_EXISTING);
		in.close();

		// BufferedInputStream bin = new BufferedInputStream(in);
		// BufferedOutputStream fout = new BufferedOutputStream(
		// new FileOutputStream(file));
		// byte[] data = new byte[8192];
		// int n = 0;
		// while (n >= 0) {
		// n = bin.read(data);
		// if (n > 0) {
		// fout.write(data);
		// }
		// }
		// fout.close();
		// bin.close();
	}

	public static boolean deleteRecursively(File file) {
		if (!file.exists()) {
			return false;
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteRecursively(f);
			}
		}
		file.delete();
		return true;
	}

}
