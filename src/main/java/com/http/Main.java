package com.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        @SuppressWarnings("resource")
        ServerSocket ss = new ServerSocket(3000);

        while (true) {
            Socket s = ss.accept();

            HttpHandler handler = new HttpHandler(s);
            handler.start();
        }
    }
}