package AlienMarauders.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class ChatClient {

    private final String username;
    private final String host;
    private final int port;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final Consumer<String> onChatLine;
    private final Consumer<List<String>> onUsers;
    private final Consumer<String> onSystem;

    public ChatClient(String username,
                      String host,
                      int port,
                      Consumer<String> onChatLine,
                      Consumer<List<String>> onUsers,
                      Consumer<String> onSystem) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.onChatLine = onChatLine;
        this.onUsers = onUsers;
        this.onSystem = onSystem;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());

        send(new Message(MessageType.LOGIN, username, null, null));

        Thread t = new Thread(this::listenLoop);
        t.setDaemon(true);
        t.start();
    }

    private void listenLoop() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                switch (msg.getType()) {
                    case CHAT:
                        if (onChatLine != null) {
                            onChatLine.accept(msg.getSender() + ": " + msg.getText());
                        }
                        break;
                    case SYSTEM:
                        if (onSystem != null) {
                            onSystem.accept(msg.getText());
                        }
                        break;
                    case USER_LIST:
                        if (onUsers != null && msg.getUsers() != null) {
                            onUsers.accept(msg.getUsers());
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (onSystem != null) {
                onSystem.accept("Disconnected from server.");
            }
        }
    }

    public void sendChat(String text) {
        send(new Message(MessageType.CHAT, username, text, null));
    }

    public void disconnect() {
        send(new Message(MessageType.LOGOUT, username, null, null));
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            if (onSystem != null) {
                onSystem.accept("Failed to send message: " + e.getMessage());
            }
        }
    }
}
