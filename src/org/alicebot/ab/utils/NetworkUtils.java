package org.alicebot.ab.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

@Slf4j
public class NetworkUtils {

	public static String localIPAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ipAddress = inetAddress.getHostAddress().toString();
						int p = ipAddress.indexOf("%");
						if (p > 0)
							ipAddress = ipAddress.substring(0, p);
						// if (Properties.trace_mode) log.info("--> localIPAddress =
						// "+ipAddress);
						return ipAddress;
					}
				}
			}
		} catch (SocketException ex) {
			log.error(ex.getMessage(), ex);
		}
		return "127.0.0.1";
	}

	public static OkHttpClient client() {
		HttpLoggingInterceptor httpLogging = new HttpLoggingInterceptor();
		httpLogging.level(Level.BODY);
		// httpLogging.setLevel(Level.BASIC);
		return new OkHttpClient.Builder().addInterceptor(httpLogging).build();
	};

	public static String responseContent(String url) throws Exception {
//		HttpClient client = new DefaultHttpClient();
//		HttpGet request = new HttpGet();
//		request.setURI(new URI(url));
//		InputStream is = client.execute(request).getEntity().getContent();
//		BufferedReader inb = new BufferedReader(new InputStreamReader(is));
//		StringBuilder sb = new StringBuilder("");
//		String line;
//		String NL = System.getProperty("line.separator");
//		while ((line = inb.readLine()) != null) {
//			sb.append(line).append(NL);
//		}
//		inb.close();
//		return sb.toString();
		Request request = new Request.Builder().url(url).build();
		try (Response response = client().newCall(request).execute()) {
			return response.body().string();
		}
	}

	/*
	 * public static String responseContent(String url) throws Exception { String
	 * result="";
	 * 
	 * // Prepare a request object HttpGet httpget = new HttpGet();
	 * httpget.setURI(new URI(url));
	 * 
	 * HttpParams httpParameters = new BasicHttpParams(); // Set the timeout in
	 * milliseconds until a connection is established. // The default value is zero,
	 * that means the timeout is not used. int timeoutConnection = 3000;
	 * HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
	 * // Set the default socket timeout (SO_TIMEOUT) // in milliseconds which is
	 * the timeout for waiting for data. int timeoutSocket = 5000;
	 * HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
	 * 
	 * 
	 * HttpClient httpclient = new DefaultHttpClient(httpParameters);
	 * 
	 * 
	 * 
	 * // Execute the request HttpResponse response = httpclient.execute(httpget);
	 * 
	 * // Examine the response status log.info(response.getStatusLine());
	 * 
	 * // Get hold of the response entity HttpEntity entity = response.getEntity();
	 * 
	 * // If the response does not enclose an entity, there is no need // to worry
	 * about connection release if (entity != null) { InputStream is =
	 * entity.getContent(); try { BufferedReader inb = new BufferedReader(new
	 * InputStreamReader(is)); StringBuilder sb = new StringBuilder(""); String
	 * line; String NL = System.getProperty("line.separator"); while ((line =
	 * inb.readLine()) != null) { sb.append(line).append(NL); } inb.close(); result
	 * = sb.toString(); BufferedReader reader = new BufferedReader( new
	 * InputStreamReader(is)); // do something useful with the response
	 * log.info(reader.readLine());
	 * 
	 * } catch (IOException ex) {
	 * 
	 * // In case of an IOException the connection will be released // back to the
	 * connection manager automatically throw ex;
	 * 
	 * } catch (RuntimeException ex) {
	 * 
	 * // In case of an unexpected exception you may want to abort // the HTTP
	 * request in order to shut down the underlying // connection and release it
	 * back to the connection manager. httpget.abort(); throw ex;
	 * 
	 * } finally {
	 * 
	 * // Closing the input stream will trigger connection release is.close();
	 * 
	 * }
	 * 
	 * // When HttpClient instance is no longer needed, // shut down the connection
	 * manager to ensure // immediate deallocation of all system resources
	 * httpclient.getConnectionManager().shutdown(); }
	 * 
	 * return result; }
	 */
	public static String responseContentUri(URI uri) throws Exception {
//		HttpClient client = new DefaultHttpClient();
//		HttpPost request = new HttpPost();
//		request.setURI(uri);
//		InputStream is = client.execute(request).getEntity().getContent();
//		BufferedReader inb = new BufferedReader(new InputStreamReader(is));
//		StringBuilder sb = new StringBuilder("");
//		String line;
//		String NL = System.getProperty("line.separator");
//		while ((line = inb.readLine()) != null) {
//			sb.append(line).append(NL);
//		}
//		inb.close();
//		return sb.toString();
		String url = uri.toString();
		Request request = new Request.Builder().url(url).build();
		try (Response response = client().newCall(request).execute()) {
			return response.body().string();
		}
	}

	public static String spec(String host, String botid, String custid, String input) {
		// log.info("--> custid = "+custid);
		String spec = "";
		try {
			if (custid.equals("0")) // get custid on first transaction with Pandorabots
				spec = String.format("%s?botid=%s&input=%s", "http://" + host + "/pandora/talk-xml", botid, URLEncoder.encode(input, "UTF-8"));
			else
				spec = // re-use custid on each subsequent interaction
						String.format("%s?botid=%s&custid=%s&input=%s", "http://" + host + "/pandora/talk-xml", botid, custid, URLEncoder.encode(input, "UTF-8"));
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		log.info(spec);
		return spec;
	}

}
