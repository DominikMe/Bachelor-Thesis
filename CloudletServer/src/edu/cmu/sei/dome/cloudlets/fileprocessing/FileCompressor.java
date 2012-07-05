package edu.cmu.sei.dome.cloudlets.fileprocessing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileCompressor {

	public static void main(String[] args) throws Exception {
		// String path = "";// "//media/sf_VirtualApp/";
		String path = "C:/Users/Dome/Programmieren/Studium/Bachelorarbeit/";
		// zip(path + "_2moped-cde", "testmoped-cde.zip");
		// System.out.println(new File(path).getParent());
		unzip(path + "testmoped-cde.zip");
	}

	public static void untargz(String archive) {
		// archive is absolute path
		try {
			String cmd = "tar -xzf " + archive + " -C " + new File(archive).getParent();
			System.out.println(cmd);
			Runtime.getRuntime().exec(cmd).waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void unzip(String archive) {
		try {
			final int BUFFER = 2048;
			byte data[] = new byte[BUFFER];
			String dest = new File(archive).getParent();
			FileInputStream fis = new FileInputStream(archive);
			CheckedInputStream checksum = new CheckedInputStream(fis,
					new Adler32());
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
					checksum));

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String entryPath = dest + File.separatorChar + entry.getName();
				entryPath = entryPath.replace('\\', '/');
				// System.out.println("Extracting: " + entryPath);
				if (entry.isDirectory()) {
					if (!new File(entryPath).mkdirs()) {
						System.out.println("Bööpp");
						break;
					}
				} else {
					// write the files to the disk
					int n;
					FileOutputStream fos = new FileOutputStream(entryPath);
					BufferedOutputStream out = new BufferedOutputStream(fos,
							BUFFER);
					while ((n = zis.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, n);
					}
					out.flush();
					out.close();
				}
			}
			zis.close();
			// System.out
			// .println("Checksum: " + checksum.getChecksum().getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// not working
	/*
	 * public static void zip(String file, String zipFile) { FileOutputStream
	 * dest; try { String zipPath = new File(file).getParent() + "/" + zipFile;
	 * zipPath = zipPath.replace('\\', '/'); dest = new
	 * FileOutputStream(zipPath); CheckedOutputStream checksum = new
	 * CheckedOutputStream(dest, new Adler32()); ZipOutputStream out = new
	 * ZipOutputStream(new BufferedOutputStream( checksum));
	 * zipRecursively(file, out); out.flush(); out.close(); System.out
	 * .println("checksum: " + checksum.getChecksum().getValue()); } catch
	 * (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e)
	 * { e.printStackTrace(); } }
	 * 
	 * private static void zipRecursively(String file, ZipOutputStream out)
	 * throws IOException { final int BUFFER = 2048; BufferedInputStream origin
	 * = null;
	 * 
	 * // out.setMethod(ZipOutputStream.DEFLATED); byte data[] = new
	 * byte[BUFFER]; // get a list of files from current directory File f = new
	 * File(file); if (f.isDirectory()) { String files[] = f.list(); for (int i
	 * = 0; i < files.length; i++) { System.out.println("Adding: " + files[i]);
	 * zipRecursively(f.getAbsolutePath() + "/" + files[i], out); } } else {
	 * origin = new BufferedInputStream(new FileInputStream(file), BUFFER);
	 * ZipEntry entry = new ZipEntry(file); out.putNextEntry(entry); int n;
	 * while ((n = origin.read(data, 0, BUFFER)) != -1) { out.write(data, 0, n);
	 * } origin.close(); } }
	 */
}
