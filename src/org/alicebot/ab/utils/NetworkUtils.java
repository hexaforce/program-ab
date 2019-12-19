package org.alicebot.ab.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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
						if (p > 0) {
							ipAddress = ipAddress.substring(0, p);
						}
						return ipAddress;
					}
				}
			}
		} catch (SocketException ex) {
			log.error(ex.getMessage(), ex);
		}
		return "127.0.0.1";
	}

	private static OkHttpClient client() {
		HttpLoggingInterceptor httpLogging = new HttpLoggingInterceptor();
		httpLogging.level(Level.BODY);
		// httpLogging.setLevel(Level.BASIC);
		return new OkHttpClient.Builder().addInterceptor(httpLogging).build();
	};

	public static String responseContent(String url) throws Exception {
		Request request = new Request.Builder().url(url).build();
		try (Response response = client().newCall(request).execute()) {
			return response.body().string();
		}
	}

	public static String spec(String host, String botid, String custid, String input) {
		String spec = "";
		try {
			if (custid.equals("0"))
				spec = String.format("%s?botid=%s&input=%s", "http://" + host + "/pandora/talk-xml", botid, URLEncoder.encode(input, "UTF-8"));
			else
				spec = String.format("%s?botid=%s&custid=%s&input=%s", "http://" + host + "/pandora/talk-xml", botid, custid, URLEncoder.encode(input, "UTF-8"));
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		log.info(spec);
		return spec;
	}

}
