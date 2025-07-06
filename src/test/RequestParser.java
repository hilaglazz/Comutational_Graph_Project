package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {

    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {  
        System.out.println("in parseRequest");
        String httpCommand = reader.readLine();
        System.out.println("httpCommand: " + httpCommand);
        if (httpCommand == null || httpCommand.isEmpty()) {
            // add a throw or be sure to handle the null case outside
            return null; // End of stream
        }

        String[] commandParts = httpCommand.split(" ");
        if (commandParts.length < 2) {
            throw new IOException("Invalid HTTP request line: " + httpCommand);
        }
        String method = commandParts[0];
        //extract the URI
        // if the URI contains a query string, split it into segments and parameters
        String uri = commandParts[1];
        System.out.println("uri: " + uri);
        String[] uriSegments;
        Map<String, String> parameters = new HashMap<>();
        if (uri.contains("?")) {
            String[] splitUri = uri.split("\\?");
            uriSegments = splitUri[0].split("/");
            uriSegments = Arrays.copyOfRange(uriSegments, 1, uriSegments.length);
            String[] pairs = splitUri[1].split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    parameters.put(keyValue[0], keyValue[1]);
                }
            }
        }
        else {
            uriSegments = uri.split("/");
        }

        // extract all further headers
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }
        // additional parameters will be added to the parameters map if exists
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            //String[] keyValue = line.split("=", 2);
            String[] parseHeaderParam = line.split(": ");
            if (parseHeaderParam.length == 2) {
                String[] paramParts = parseHeaderParam[1].split("; ");
                for (String paramPart : paramParts) {
                    String[] keyValue = paramPart.split("=");
                    if (keyValue.length == 2) {
                        parameters.put(keyValue[0], keyValue[1]);
                    }
                    else{
                        parameters.put(paramPart, paramPart);
                    }
                }
            }
        }
        // check for Content-Length header and read the content if it exists
        int contentLength = headers.containsKey("Content-Length") ? Integer.parseInt(headers.get("Content-Length")) : 0;
        byte[] content = null;
        // read the number of characters specified in the Content-Length header
        if (contentLength > 0) {
            char[] contentBuffer = new char[contentLength];
            reader.read(contentBuffer, 0, contentLength);
            content = new String(contentBuffer).getBytes();
            System.out.println("content: " + new String(content));
        }
        else{
            // read the content from the request body until an empty line is encountered
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                byte[] lineBytes = (line + "\n").getBytes();
                if (content == null) {
                    content = lineBytes;
                } else {
                    byte[] newContent = new byte[content.length + lineBytes.length];
                    System.arraycopy(content, 0, newContent, 0, content.length);
                    System.arraycopy(lineBytes, 0, newContent, content.length, lineBytes.length);
                    content = newContent;
                }
            }
        }
        System.out.println("exit parseRequest");
        // create and return the RequestInfo object
        return new RequestInfo(method, uri, uriSegments, parameters, content);
    }
	
	// RequestInfo given internal class
    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        public RequestInfo(String httpCommand, String uri, String[] uriSegments, Map<String, String> parameters, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        public String getHttpCommand() {
            return httpCommand;
        }

        public String getUri() {
            return uri;
        }

        public String[] getUriSegments() {
            return uriSegments;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
