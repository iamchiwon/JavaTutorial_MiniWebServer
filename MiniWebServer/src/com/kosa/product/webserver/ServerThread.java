package com.kosa.product.webserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;

public class ServerThread extends Thread {

	private Socket client;
	private String rootPath;
	private HashMap<String, String> requestHeader;
	private HashMap<String, String> parameters;

	public ServerThread(Socket client) {
		this.client = client;
		rootPath = "";
	}

	public ServerThread(Socket client, String rootPath) {
		this.client = client;
		this.rootPath = rootPath;
	}

	public void setRootPath(String path) {
		rootPath = path;
	}

	@Override
	public void run() {
		try {
			DataInputStream in = new DataInputStream(client.getInputStream());
			DataOutputStream out = new DataOutputStream(client.getOutputStream());

			requestHeader = new HashMap<String, String>();
			parameters = new HashMap<String, String>();

			String reqMethod = in.readLine(); // "GET / HTTP/1.1"

			while (true) {
				String line = in.readLine();
				if (line.isEmpty())
					break;
				String[] header = line.split(":");
				requestHeader.put(header[0], header[1]);
			}

			String method = reqMethod.split(" ")[0]; // "GET"

			if (method.equals("POST")) {
				String lenStr = requestHeader.get("Content-Length");
				int len = Integer.parseInt(lenStr.trim());

				byte[] postQuery = new byte[len];
				in.read(postQuery);

				String params = new String(postQuery);
				parseParameters(params);
			}

			String filePath = reqMethod.split(" ")[1].substring(1);

			int qIndex = filePath.indexOf("?");
			if (qIndex != -1) {
				String params = filePath.substring(qIndex + 1);
				parseParameters(params);
				
				filePath = filePath.substring(0, qIndex);
			}

			if (filePath.equals(""))
				filePath = "index.html";
			String fullPath = rootPath + filePath;

			String contentType = getContentType(filePath);

			filePath = URLDecoder.decode(filePath, "utf-8");

			byte[] buffer = null;
			File f = new File(fullPath);
			if (!f.exists() && !existContentFile(filePath)) {
				String outputMessage = "";
				outputMessage += "<html>";
				outputMessage += "<h1>404: File not found.</h1>";
				outputMessage += "</html>";

				responseHeader(out, outputMessage.length());
				out.writeBytes(outputMessage);
				out.flush();

				client.close();

				return;
			}

			if (f.exists()) {
				buffer = readFile(f);
				if (fullPath.endsWith(".html")) {
					String original = new String(buffer);
					original = replaceParameter(original);
					buffer = original.getBytes();
				}
			} else {
				buffer = generateHtml(filePath);
			}

			responseHeader(out, contentType, buffer.length);
			out.write(buffer);
			out.flush();

			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseParameters(String params) throws UnsupportedEncodingException {
		String[] parsedParams = params.split("&");
		for (String s : parsedParams) {
			String[] param = s.split("=");
			String val = URLDecoder.decode(param[1], "utf-8");
			parameters.put(param[0], val);
		}
	}

	private byte[] generateHtml(String filename) throws IOException {
		filename = filename.replace("html", "cont"); // index.html -> index.cont
		String contentFilePath = rootPath + "content\\" + filename;

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(contentFilePath))));

		final int NONE = 100;
		final int COMMAND = 100;
		final int CONTENT = 200;

		int processType = NONE;
		String contentTitle = "";
		String content = "";
		String template = "";
		while (in.ready()) {
			String line = in.readLine();

			// % 로 시작하는지?
			if (line.startsWith("%")) {
				if (processType == CONTENT) {
					template = template.replaceAll(contentTitle, content);
					contentTitle = "";
					content = "";
				}

				processType = COMMAND;
			} else {
				processType = CONTENT;
			}

			if (processType == COMMAND) {
				if (line.equals("%TEMPLATE%")) {
					String templateName = in.readLine();
					String templatePath = rootPath + "template\\" + templateName + ".html";
					byte[] templateData = readFile(new File(templatePath));
					template = new String(templateData);
				} else if (line.equals("%TITLE%")) {
					String title = in.readLine();
					template = template.replaceAll(line, title);
				} else if (line.equals("%CONTENT%")) {
					contentTitle = line;
				}
			} else if (processType == CONTENT) {
				content += line;
			}
		}

		if (!contentTitle.isEmpty()) {
			template = template.replaceAll(contentTitle, content);
			contentTitle = "";
			content = "";
		}

		template = replaceParameter(template);

		in.close();

		return template.getBytes();
	}

	private String replaceParameter(String original) {
		// parameter replace
		for (String key : parameters.keySet()) {
			String rkey = "%" + key + "%";
			String value = parameters.get(key);
			original = original.replaceAll(rkey, value);
		}

		// constant replace
		original = original.replaceAll("%DATE%", new Date().toString());
		return original;
	}

	private boolean existContentFile(String filename) {
		filename = filename.replace("html", "cont"); // index.html -> index.cont
		String contentFilePath = rootPath + "content\\" + filename;
		return new File(contentFilePath).exists();
	}

	private String getContentType(String filePath) {
		String contentType = "Content-Type: ";
		if (filePath.endsWith(".html") || filePath.endsWith(".htm"))
			contentType += "text/html; charset=UTF-8";
		else if (filePath.endsWith(".png"))
			contentType += "image/png";
		else if (filePath.endsWith(".jpg"))
			contentType += "image/jpg";
		else if (filePath.endsWith(".gif"))
			contentType += "image/gif";
		else
			contentType += "";
		return contentType;
	}

	private byte[] readFile(File file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		int fileSize = fin.available(); // 파일크기
		byte[] buffer = new byte[fileSize];
		fin.read(buffer);
		fin.close();
		return buffer;
	}

	private void responseHeader(DataOutputStream out, int contentLength) throws IOException {
		out.writeBytes("HTTP/1.1 505 Not Support\n");
		out.writeBytes("Content-Type: text/html; charset=UTF-8\n");
		out.writeBytes("Content-Length: " + contentLength + "\n");
		out.writeBytes("Server: MiniWebServer\n");
		out.writeBytes("\n");
	}

	private void responseHeader(DataOutputStream out, String contentType, int contentLength) throws IOException {
		out.writeBytes("HTTP/1.1 505 Not Support\n");
		out.writeBytes("Content-Type: " + contentType + "\n");
		out.writeBytes("Content-Length: " + contentLength + "\n");
		out.writeBytes("Server: MiniWebServer\n");
		out.writeBytes("\n");
	}

}
