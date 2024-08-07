package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Abstract class to represent message

enum State {
    START,
    HEADER
}

public class Message {
    // Class for TCP messages
    String SEP = "\r\n\r\n";
    List<String> header;
    String body, message;

    public Message(List<String> header, String body) {
        this.header = header;
        this.body = body;
        header.add(Integer.toString(this.body.length()));
        this.assembleMessage();
    }

    public Message(BufferedReader reader) {
        header = new ArrayList<>();
        State state = State.HEADER;
        String line;
        int size = 0;
        Boolean readHeader = false;
        while (true) {
            if (readHeader) {
                break;
            }
            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                this.header = new ArrayList<>();
                this.body = "";
                this.message = "";
                return;
            }
            switch (state) {
                case START:
                    if (line.equals("")) {
                    } else {
                        state = State.HEADER;
                        header.add(line);
                    }
                    break;
                case HEADER:
                    if (line.equals("")) {
                        size = Integer.valueOf(header.get(header.size() - 1));
                        readHeader = true;
                    } else {
                        header.add(line);
                    }
                    break;
            }
        }
        if (size == 0) {
            this.body = "";
        } else {
            char[] body = new char[size];
            try {
                reader.read(body, 0, size);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.body = String.valueOf(body);
        }
        assembleMessage();
    }

    private String readMessage(String message) {
        return "";
    }

    private void assembleMessage() {
        String headerString = String.join("\r\n", header);
        String message = headerString + SEP;
        if (!body.equals("")) {
            message += body;
        }
        this.message = message;
    }

    public List<String> getHeaders() {
        return header;
    }

    public void setHeader(Integer index, String newHeader) {
        if (index < 0 || index > header.size() - 1 - 1) {
            return;
        } else {
            header.set(index, newHeader);
            this.assembleMessage();
        }
    }

    public int getBodySize() {
        return Integer.valueOf(header.get(header.size() - 1));
    }

    public String getBody() {
        return body;
    }

    public String getMessage() {
        return message;
    }
}