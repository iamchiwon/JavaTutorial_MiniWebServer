package com.kosa.product.webserver.uitl;

public class Logger {
	public static void d(String log) {
		System.out.println("["+Thread.currentThread().getName()+"] : "+log);
	}
}
