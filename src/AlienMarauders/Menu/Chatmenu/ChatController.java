package AlienMarauders.Menu.Chatmenu;

import AlienMarauders.Controller;
import AlienMarauders.Model;
import AlienMarauders.networking.ChatClient;
import javafx.application.Platform;
import javafx.scene.layout.Region;

import java.util.List;

public class ChatController {

    private final ChatModel model;
    private final ChatView view;
    private final Controller rootController;

    private ChatClient client;

    public ChatController(Model rootModel, Controller rootController) {
        this.model = new ChatModel(rootModel);
        this.rootController = rootController;

        this.view = new ChatView(
                this.model,
                this::handleLogin,           // Login button pressed
                this::sendMessage,           // Send pressed / Enter
                this::leaveChat,             // Back from chat
                () -> rootController.showMainMenu() // Back from login to main menu
        );
    }

    // ----- LOGIN -----

    private void handleLogin() {
        String username = model.usernameProperty().get().trim();
        String host     = model.hostProperty().get().trim();
        String portText = model.portProperty().get().trim();

        if (username.isEmpty() || host.isEmpty() || portText.isEmpty()) {
            model.loginErrorProperty().set("Failed login");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            model.loginErrorProperty().set("Failed login");
            return;
        }

        try {
            model.clearChatLists();
            model.loginErrorProperty().set("");
            client = new ChatClient(
                    username,
                    host,
                    port,
                    this::onChatLine,
                    this::onUsers,
                    this::onSystem
            );
            client.connect();
            model.connectedProperty().set(true);
        } catch (Exception e) {
            model.loginErrorProperty().set("Failed login");
            model.connectedProperty().set(false);
        }
    }

    // ----- from ChatClient -----

    private void onChatLine(String line) {
        Platform.runLater(() -> model.addLine(line));
    }

    private void onUsers(List<String> users) {
        Platform.runLater(() -> model.setUsers(users));
    }

    private void onSystem(String text) {
        Platform.runLater(() -> model.addLine("[SYSTEM] " + text));
    }

    // ----- from ChatView -----

    private void sendMessage() {
        String text = model.inputTextProperty().get().trim();
        if (text.isEmpty() || client == null) return;

        client.sendChat(text);
        model.inputTextProperty().set("");
    }

    private void leaveChat() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        model.connectedProperty().set(false);
        model.clearChatLists();
        model.resetLoginState();
        rootController.showMainMenu();
    }

    public Region getView() {
        return view.getRoot();
    }
}
