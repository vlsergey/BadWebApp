package org.badwebapp;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class Start {

	public static String dbURL = "jdbc:derby:myDB;create=true";

	public static String tableName = "results";

	public static void main(String[] args) throws Exception {

		try {
			try (Connection conn = DriverManager.getConnection(Start.dbURL)) {
				try (PreparedStatement ps = conn
						.prepareStatement("CREATE TABLE results (username VARCHAR(256), result INTEGER)")) {
					ps.execute();
				}
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.setExecutor(Executors.newCachedThreadPool());

		server.createContext("/add", new AddHandler());
		server.createContext("/show", new ShowHandler());

		server.start();
	}

}
