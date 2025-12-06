package AlienMarauders.server;

public class ServerApp {
    public static void main(String[] args) {
        Server server = new Server();
        server.start(8888);  // use same port as your client
    }
}