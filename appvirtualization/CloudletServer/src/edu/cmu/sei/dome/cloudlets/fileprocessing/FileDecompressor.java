package edu.cmu.sei.dome.cloudlets.fileprocessing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FileDecompressor {
	
	private FileDecompressor() {
		
	}
	
	public static final FilenameFilter ZIP_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".zip"));
		}
	};
	public static final FilenameFilter TARGZ_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".tar.gz"));
		}
	};

	public static void untargz(String archive) {
		// archive is absolute path
		try {
			String cmd = "tar -xzf " + archive + " -C "
					+ new File(archive).getParent();
			System.out.println(cmd);
			Runtime.getRuntime().exec(cmd).waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void unzip(String archive) {
		ZipInputStream zis = null;
		try {
			final int BUFFER = 2048;
			byte [] data = new byte[BUFFER];
			String dest = new File(archive).getParent();
			FileInputStream fis = new FileInputStream(archive);
			CheckedInputStream checksum = new CheckedInputStream(fis,
					new Adler32());
			zis = new ZipInputStream(new BufferedInputStream(checksum));

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String entryPath = dest + File.separatorChar + entry.getName();
				entryPath = entryPath.replace('\\', '/');
				// System.out.println("Extracting: " + entryPath);
				if (entry.isDirectory()) {
					new File(entryPath).mkdirs();
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

}
