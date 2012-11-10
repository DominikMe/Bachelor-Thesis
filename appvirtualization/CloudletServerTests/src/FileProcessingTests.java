import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import junit.framework.Assert;

import org.junit.Test;

public class FileProcessingTests {

	private static Runnable md5 = new Runnable() {

		@Override
		public void run() {
			try {
				File file = new File("SpeechRecognition_cameyo.zip");
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] data = new byte[8192];
				DigestInputStream is = new DigestInputStream(
						new BufferedInputStream(new FileInputStream(file)), md);
				int n;
				do {
					n = is.read(data);
				} while (n > 0);
				is.close();
//				System.out.println(new BigInteger(1, md.digest()).toString(md
//						.getDigestLength()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private static Runnable copy = new Runnable() {

		@Override
		public void run() {
			String original = "SpeechRecognition_cameyo.zip";
			String kopie = new File("SpeechRecognition_cameyo_copy.zip")
					.getAbsolutePath();
			InputStream in;
			try {
				in = new FileInputStream(original);
				Files.copy(in, FileSystems.getDefault().getPath(kopie),
						StandardCopyOption.REPLACE_EXISTING);
				in.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private static long measure(Runnable r) {
		long start = System.currentTimeMillis();
		r.run();
		long end = System.currentTimeMillis();
		return end - start;
	}

	private static long[] measureN(Runnable r, int n) {
		long[] values = new long[n];
		for (int i = 0; i < n; i++) {
			values[i] = measure(r);
			System.out.println(values[i] + " ms");
		}
		return values;
	}

	private static double variance(long[] values) {
		long mean = mean(values);

		long var = 0;
		for (long i : values) {
			var += (i - mean) * (i - mean);
		}
		var /= (values.length - 1);
		return Math.sqrt(var);
	}

	private static long sum(long[] values) {
		long sum = 0;
		for (long i : values) {
			sum += i;
		}
		return sum;
	}

	private static long mean(long[] values) {
		return sum(values) / values.length;
	}

	private static long min(long[] values) {
		long min = Long.MAX_VALUE;
		for (long i : values) {
			Math.min(min, i);
		}
		return min;
	}

	private static long max(long[] values) {
		long max = Long.MIN_VALUE;
		for (long i : values) {
			Math.max(max, i);
		}
		return max;
	}

	private static double spread(long[] values) {
		long min = min(values);
		long max = max(values);
		long mean = mean(values);
		return (double) (max - min) / mean;
	}

	@Test
	public void testCopy() {
		long[] values = measureN(copy, 10);
		double spread = spread(values);
		System.out.println("Spread " + spread);
		Assert.assertTrue(spread < 0.2);
	}

	@Test
	public void testMD5() {
		long[] values = measureN(md5, 10);
		double spread = spread(values);
		System.out.println("Spread " + spread);
		Assert.assertTrue(spread < 0.2);
	}

	public static void main(String[] args) {
		FileProcessingTests tester = new FileProcessingTests();
		System.out.println("50 x COPY");
		tester.testCopy();
		System.out.println("50 x MD5");
		tester.testMD5();
	}
}
