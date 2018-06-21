package ec.gob.firmadigital.servicio.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class FileUtil {
	public static String getExtension(byte[] data) {
		String extension = "";
		try(InputStream is = new ByteArrayInputStream(data)) {
			extension = URLConnection.guessContentTypeFromStream(is);
		} catch (IOException e) {
			extension = "pdf";
		}
		return extension;
	}
}
