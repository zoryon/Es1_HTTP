package com.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLConnection;
import com.http.lib.FileResult;
import com.http.lib.FileStatus;

public class HttpHandler extends Thread {
    BufferedReader in;
    DataOutputStream out;
    Socket socket;
    String header;
    String method;
    String resource;
    String version;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

            parseRequest();
            processRequest();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    /** step 1: parse / analyse / get / fetch the HTTP request */
    private void parseRequest() throws IOException {
        String requestLine  = in.readLine();
        if (requestLine  == null || requestLine .isEmpty()) {
            throw new IOException("Empty HTTP request's header");
        }

        System.out.println("Request: " + requestLine );

        String[] parts  = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IOException("Malformed HTTP request's header");
        }

        method = parts[0];
        resource = parts[1];
        version = parts[2];

        // consume all headers (as of now they are unnecessary)
        do {
            header = in.readLine();
            System.out.println(header);
        } while (!header.isEmpty());
    }

    /** step 2: process the HTTP request and send a response */
    private void processRequest() {
        FileResult result = determineFile(resource);

        switch (result.getStatus()) {
            case DIRECTORY_NO_SLASH:
                sendRedirect(resource + "/");
                break;
            case FILE_NOT_FOUND:
                sendError("404 Not Found", "The requested resource was not found.");
                break;
            case FILE_FOUND:
                handleFileRequest(result.getFile());
                break;
        }
    }

    private FileResult determineFile(String path) {
        File file = new File("htdocs/chartjs" + path);

        // if resource is a directory but lacks a trailing "/" (doesn't end with "/")
        if (file.exists() && file.isDirectory() && !path.endsWith("/")) {
            /* 
             * this resource doesn't end with a "/",
             * BUT a directory was found with name + trailing "/" --> should send a 301
             */
            return new FileResult(null, FileStatus.DIRECTORY_NO_SLASH);
        }

        // if a directory, find the relative index file
        if (path.endsWith("/")) {
            file = new File(file, "index.html");
            if (file.exists()) return new FileResult(file, FileStatus.FILE_FOUND);
        }

        // if no extension, search for a file with same name but with any extension
        if (!path.contains(".")) {
            File parent = file.getParentFile();
            if (parent != null && parent.exists()) {
                final File searchFile = file; // i got an error saying file should have been final
                File[] matches = parent.listFiles((dir, name) -> name.startsWith(searchFile.getName() + ".")); // filter with callback function
                if (matches != null && matches.length > 0) {
                    return new FileResult(matches[0], FileStatus.FILE_FOUND);
                }
            }
        }

        // return the file directly if it exists
        return file.exists() && file.isFile()
            ? new FileResult(file, FileStatus.FILE_FOUND)
            : new FileResult(null, FileStatus.FILE_NOT_FOUND);
    }

    private void handleFileRequest(File file) {
        try {
            byte[] content = readFile(file);
            String contentType = determineContentType(file);
            sendResponse("200 OK", content, contentType);
        } catch (IOException e) {
            sendError("500 Internal Server Error", "Failed to read the requested resource.");
        }
    }

    private void sendResponse(String statusCode, byte[] body, String contentType) {
        try {
            out.writeBytes("HTTP/1.1 " + statusCode + "\r\n");
            if (contentType != null) out.writeBytes("Content-Type: " + contentType + "\r\n");
            if (body != null) out.writeBytes("Content-Length: " + body.length + "\r\n");
            out.writeBytes("\r\n");
            if (body != null) out.write(body);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRedirect(String newLocation) {
        try {
            out.writeBytes("HTTP/1.1 301 Moved Permanently\r\n");
            out.writeBytes("Location: " + newLocation + "\r\n");
            out.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendError(String statusCode, String message) {
        String body = "<html><body><h1>" + statusCode + "</h1><p>" + message + "</p></body></html>";
        sendResponse(statusCode, body.getBytes(), "text/html");
    }

    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];

        int bytesRead;
        while ((bytesRead = fis.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        fis.close();
        return buffer.toByteArray();
    }

    private String determineContentType(File file) {
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        return contentType != null ? contentType : "unknown";
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}