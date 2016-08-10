package utils;

import java.io.*;

public class FileUtils {

	public static boolean copyfile(File source, String dest) {
		boolean back = true;
		try {
			File f2 = new File(dest);
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(f2);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			back = false;
		}
		return back;
	}
}