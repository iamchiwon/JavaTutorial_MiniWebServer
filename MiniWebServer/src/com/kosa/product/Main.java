package com.kosa.product;

import java.io.IOException;

import com.kosa.product.webserver.MiniWebServer;

public class Main {

	public static void main(String[] args) throws IOException {
		MiniWebServer server = new MiniWebServer();
		server.setPort(80);
		server.setRootPath("C:\\dev\\WEBROOT\\");
		server.serverStart();
	}

}
