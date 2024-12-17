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

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());

            parseRequest();
            handleRequest();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    private void parseRequest() throws IOException {
        String firstLine = in.readLine();
        if (firstLine == null || firstLine.isEmpty()) {
            throw new IOException("Empty HTTP request");
        }

        System.out.println("Request: " + firstLine);
        String[] requestParts = firstLine.split(" ");
        if (requestParts.length != 3) {
            throw new IOException("Malformed HTTP request's header");
        }

        method = requestParts[0];
        resource = requestParts[1];
        version = requestParts[2];

        // consume all headers
        do {
            header = in.readLine();
            System.out.println(header);
        } while (!header.isEmpty());
    }

    private void handleRequest() {
        File file = getFile(resource);

        if (file == null) {
            // if the file does not exist and doesn't end with "/", send 401
            if (!resource.endsWith("/")) {
                sendErrorResponse("404 Not Found", "Resource not found.");
            }
        } else {
            serveFile(file);
        }
    }

    private File getFile(String resourcePath) {
        String basePath = "htdocs/chartjs";
        File file = new File(basePath + resourcePath);

        // if resource is a directory but lacks a trailing "/"
        if (file.exists() && file.isDirectory() && !resourcePath.endsWith("/")) {
            sendRedirectResponse(resourcePath + "/");
            return null; // Stop further processing
        }

        // if directory find the relative index
        if (resourcePath.endsWith("/")) {
            file = new File(file, "index.html");
            if (file.exists()) return file;
        }

        // if no extension, search for a file with any extension
        if (!resourcePath.contains(".")) {
            File parentDir = file.getParentFile();
            String baseName = file.getName();
            if (parentDir != null && parentDir.exists()) {
                File[] matchingFiles = parentDir.listFiles((dir, name) -> name.startsWith(baseName + "."));
                if (matchingFiles != null && matchingFiles.length > 0) {
                    return matchingFiles[0];
                }
            }
    
            File indexFile = new File(file, "index.html");
            if (indexFile.exists() && indexFile.isFile()) {
                return indexFile;
            }
        }

        // return the file directly if it exists
        if (file.exists() && file.isFile()) {
            return file;
        }

        return null;
    }

    private void serveFile(File file) {
        try {
            byte[] fileContent = getFileStream(file);
            String contentType = getContentType(file);
            sendResponse("200 OK", fileContent, contentType);
        } catch (IOException e) {
            sendErrorResponse("500 Internal Server Error", "Failed to read the resource.");
        }
    }

    private byte[] getFileStream(File file) throws IOException {
        ByteArrayOutputStream responseContent = new ByteArrayOutputStream();
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                responseContent.write(buffer, 0, bytesRead);
            }
        }
        return responseContent.toByteArray();
    }

    private String getContentType(File file) {
        return URLConnection.guessContentTypeFromName(file.getName());
    }

    private void sendResponse(String statusCode, byte[] responseBody, String contentType) {
        try {
            out.writeBytes("HTTP/1.1 " + statusCode + "\r\n");
            out.writeBytes("Content-Type: " + contentType + "\r\n");
            out.writeBytes("Content-Length: " + responseBody.length + "\r\n");
            out.writeBytes("\r\n");
            out.write(responseBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendErrorResponse(String statusCode, String message) {
        try {
            String errorHtml = "<html><body><h1>" + statusCode + "</h1><p>" + message + "</p></body></html>";
            byte[] responseBody = errorHtml.getBytes();
            sendResponse(statusCode, responseBody, "text/html");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRedirectResponse(String newLocation) {
        try {
            out.writeBytes("HTTP/1.1 301 Moved Permanently\r\n");
            out.writeBytes("Location: " + newLocation + "\r\n");
            out.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
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