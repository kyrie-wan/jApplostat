package com.bmape.applostat.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author martinoswald
 */
public class UrlConnection {

	public static InputStream openConnectionCheckRedirects(URLConnection conn) throws IOException {
		boolean redir;
		int redirects = 0;
		InputStream in = null;
		do {
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).setInstanceFollowRedirects(false);
			}
			// We want to open the input stream before getting headers
			// because getHeaderField() et al swallow IOExceptions.
			in = conn.getInputStream();
			redir = false;
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection http = (HttpURLConnection) conn;
				int stat = http.getResponseCode();
				if (stat >= 300 && stat <= 307 && stat != 306 && stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
					URL base = http.getURL();
					String loc = http.getHeaderField("Location");
					URL target = null;
					if (loc != null) {
						target = new URL(base, loc);
					}
					http.disconnect();
					// Redirection should be allowed only for HTTP and HTTPS
					// and should be limited to 5 redirections at most.
					if (target == null || !(target.getProtocol().equals("http") || target.getProtocol().equals("https")) || redirects >= 5) {
						throw new SecurityException("illegal URL redirect");
					}
					redir = true;
					conn = target.openConnection();
					redirects++;
				}
			}
		} while (redir);
		return in;
	}

}
