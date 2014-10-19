package org.badwebapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AddHandler implements HttpHandler {

	public static Map<String, List<String>> splitQuery(URI url)
			throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = url.getQuery().split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(
					pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder
					.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

	private static final String hash(String str) throws Exception {
		byte[] bytesOfMessage = str.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] thedigest = md.digest(bytesOfMessage);
		return Base64.getEncoder().encodeToString(thedigest);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {

		try {
			Map<String, List<String>> arguments = splitQuery(exchange
					.getRequestURI());

			if (!arguments.containsKey("username")) {
				print(exchange,
						"<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>username is not specified</p></body></html>");
				return;
			}
			if (!arguments.containsKey("result")) {
				print(exchange,
						"<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>result is not specified</p></body></html>");
				return;
			}
			if (!arguments.containsKey("md5")) {
				print(exchange,
						"<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>md5 is not specified</p></body></html>");
				return;
			}

			String username = arguments.get("username").get(0);
			Integer result = Integer.valueOf(arguments.get("result").get(0));
			String md5 = arguments.get("md5").get(0);

			String toCheck = hash(username + ":" + result);
			if (!toCheck.toLowerCase().equals(md5.toLowerCase())) {
				print(exchange,
						"<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>Incorrect MD5. Expected "
								+ toCheck
								+ ", received "
								+ md5
								+ "</p></body></html>");
				return;
			}

			if (result > 1000) {
				byte[] response = ("<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>Result ("
						+ result + ") is more than 1000</p></body></html>")
						.toString().getBytes("utf-8");
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
						response.length);
				exchange.getResponseBody().write(response);
				exchange.close();
				return;
			}

			try (Connection conn = DriverManager.getConnection(Start.dbURL)) {
				try (PreparedStatement ps = conn
						.prepareStatement("INSERT INTO results (username, result) VALUES('"
								+ username + "', " + result + ")")) {
					ps.execute();
				}
			}

			byte[] response = "<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>Result added</p></body></html>"
					.toString().getBytes("utf-8");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
					response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		} catch (Exception exc) {
			byte[] response = exc.toString().getBytes("utf-8");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR,
					response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		}

	}

	private void print(HttpExchange exchange, String message)
			throws UnsupportedEncodingException, IOException {
		byte[] response = message.toString().getBytes("utf-8");
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
