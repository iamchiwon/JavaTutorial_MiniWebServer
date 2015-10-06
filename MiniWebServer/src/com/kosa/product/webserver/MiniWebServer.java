package com.kosa.product.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.kosa.product.webserver.uitl.Logger;

public class MiniWebServer {

	private int port;
	private String rootPath;

	public void setPort(int port) {
		this.port = port;
		rootPath = "";
	}

	public void setRootPath(String path) {
		rootPath = path;
	}

	public void serverStart() throws IOException {
		ServerSocket server = new ServerSocket(port);

		Logger.d("Server Started.");
		Logger.d("Waiting...");

		while (true) {
			Socket client = server.accept();
			Logger.d("client connected from " + client.getInetAddress().getHostAddress());

			new ServerThread(client, rootPath).start();
		}
	}

}
