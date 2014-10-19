package org.badwebapp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ShowHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder
					.append("<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body><p>Current results:</p><ol>");

			try (Connection conn = DriverManager.getConnection(Start.dbURL)) {
				try (PreparedStatement ps = conn
						.prepareStatement(" SELECT * FROM ( SELECT ROW_NUMBER() OVER() AS rownum, username, MAX(result) AS result "
								+ "FROM results GROUP BY username ORDER BY result DESC ) src WHERE rownum <= 50")) {
					try (ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							stringBuilder.append("<li>"
									+ rs.getString("username") + " => "
									+ rs.getInt("result") + "</li>");
						}
					}
				}
			}
			stringBuilder.append("</ol></body></html>");

			byte[] response = stringBuilder.toString().getBytes("utf-8");
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
					response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		} catch (Exception exc) {
			exc.printStackTrace();
			throw new IOException(exc.toString());
		}
	}
}
