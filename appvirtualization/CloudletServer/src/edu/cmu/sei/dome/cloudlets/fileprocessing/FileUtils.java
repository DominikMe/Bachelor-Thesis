package edu.cmu.sei.dome.cloudlets.fileprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.Part;

import edu.cmu.sei.dome.cloudlets.constants.Commons;

/**
 * @author Dome
 * 
 */
public final class FileUtils {

	private FileUtils() {
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

	public static synchronized File saveUpload(Part upload, String filename, String appId)
			throws IOException {
		String path = Commons.STORE + "/" + appId + "/" + filename;
		InputStream in = upload.getInputStream();
		Files.copy(in, FileSystems.getDefault().getPath(path),
				StandardCopyOption.REPLACE_EXISTING);
		in.close();
		File f = new File(path);
		return f;
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
