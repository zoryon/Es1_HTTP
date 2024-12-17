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

            // get first header
            method = request[0];
            resource = request[1];
            version = request[2];

            // get secondary headers
            do {
                header = in.readLine();
                System.out.println(header);
            } while (!header.isEmpty());

            // get file stream
            File file = getFile(resource);

            if (file == null) {
                sendResponse("301 Moved Permanently", null, null);
            } else {
                responseBody = getFileStream(file);

                // build & send response
                sendResponse("200 OK", responseBody, getContentType(file));
            }

            // closing resources
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFile(String resource) {
        String basePath = "htdocs/chartjs";
        String path = basePath + resource;
        File file = new File(path);
    
        // if resource ends with "/"
        if (resource.endsWith("/")) {

            // if directory exists, find "index.html"
            if (file.exists() && file.isDirectory()) {
                file = new File(file, "index.html");

                if (file.exists()) {
                    return file;
                }
            }
        } else {
            // if resource doesn't end with "/", look for a file with the same name
            if (!resource.contains(".")) {
                path += ".html";
            }

            file = new File(path);

            if (file.exists() && !file.isDirectory()) {
                return file;
            } else {
                return null;
            }
        }
    
        // if no file or directory is found, return null
        return null;
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

    public void sendResponse(String statusCode, byte[] responseBody, String contentType) throws IOException {
        // send header
        out.writeBytes("HTTP/1.1 " + statusCode + System.lineSeparator());

        int len = -1;
        switch (statusCode) {
            case "301 Moved Permanently":
                out.writeBytes("Location: " + resource + "/" + System.lineSeparator());
                contentType = "0";
                len = 0;
                break;
            case "200 OK":
                len = responseBody.length;
                break;
            default:
        }
        
        // send response
        out.writeBytes("Content-Type: " + contentType + System.lineSeparator());
        out.writeBytes("Content-Length: " + len + System.lineSeparator());
        out.writeBytes(System.lineSeparator());
        out.write(responseBody);
    }
}
