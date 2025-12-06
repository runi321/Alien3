package AlienMarauders.server;

import AlienMarauders.networking.Message;
import AlienMarauders.networking.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public void start(int port) {
        System.out.println("Starting chat server on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected from " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // called by ClientHandler when a message arrives
    public synchronized void handleMessage(ClientHandler from, Message msg) {
        if (msg == null) return;

        switch (msg.getType()) {
            case LOGIN:
                from.setUsername(msg.getSender());
                System.out.println(from.getUsername() + " logged in");
                broadcastSystem(from.getUsername() + " joined the chat");
                broadcastUserList();
                break;

            case CHAT:
                broadcast(msg);
                break;

            case LOGOUT:
                removeClient(from);
                break;

            default:
                break;
        }
    }

    // called by ClientHandler when it disconnects
    public void removeClient(ClientHandler handler) {
        if (clients.remove(handler)) {
            if (handler.getUsername() != null) {
                broadcastSystem(handler.getUsername() + " left the chat");
            }
            broadcastUserList();
        }
    }

    public void broadcast(Message msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    public void broadcastSystem(String text) {
        Message msg = new Message(MessageType.SYSTEM, "SERVER", text, null);
        broadcast(msg);
    }

    public void broadcastUserList() {
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.getUsername() != null) {
                names.add(c.getUsername());
            }
        }
        Message msg = new Message(MessageType.USER_LIST, "SERVER", null, names);
        broadcast(msg);
    }
}