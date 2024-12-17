package com.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLConnection;

public class HttpHandler extends Thread {
    BufferedReader in;
    DataOutputStream out;
    Socket socket;
    String header;
    String method;
    String resource;
    String version;
    String responseHeader;
    byte[] responseBody;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

            String firstLine = in.readLine();
            System.out.println(firstLine);
            String[] request = firstLine.split(" ");

            method = request[0];
            resource = request[1];
            version = request[2];

            do {
                header = in.readLine();
                System.out.println(header);
            } while (!header.isEmpty());
            System.out.println("Request ended");

            File file = getFile(resource);
            responseBody = getFileStream(file);

            out.writeBytes("HTTP/1.1 "+ responseHeader + System.lineSeparator());
            out.writeBytes("Content-Type: " + getContentType(file) + System.lineSeparator());
            out.writeBytes("Content-Length: " + responseBody.length + System.lineSeparator());
            out.writeBytes(System.lineSeparator());
            out.write(responseBody);

            // closing resources
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFile(String resource) {
        String basePath = "htdocs/chartjs";

        return new File(
            basePath +
            (resource.equals("/")
                ? "/index.html" 
                : resource
            )
        );
    }

    public byte[] getFileStream(File file) throws IOException {
        if (file == null || !file.exists() || file.isDirectory()) {
            return "<html><body><h1>404 Not Found</h1></body></html>".getBytes();
        }

        InputStream input = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1) {
            responseContent.write(buffer, 0, bytesRead);
        }

        input.close();
        return responseContent.toByteArray();
    }

    public String getContentType(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return "unknown";
        }

        return URLConnection.guessContentTypeFromName(file.getName());
    }
}
