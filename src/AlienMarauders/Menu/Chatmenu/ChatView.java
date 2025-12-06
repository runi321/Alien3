package AlienMarauders.Menu.Chatmenu;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

public class ChatView {

    private final BorderPane root;

    private void bindBackground(BorderPane node, ChatModel model) {
        Runnable apply = () -> {
            String name = model.getBackgroundImage();
            var url = getClass().getResource("/AlienMarauders/Myndir/" + name);
            if (url != null) {
                node.setStyle(
                    "-fx-background-image: url('" + url.toExternalForm() + "');" +
                    "-fx-background-size: cover;"
                );
            } else {
                node.setStyle("-fx-background-color: #fcfcfcff;");
            }
        };
        model.backgroundImageProperty().addListener((obs, oldV, newV) -> apply.run());
        apply.run();
    }

    // UI fields

    public ChatView(ChatModel model,
                    Runnable onLogin,
                    Runnable onSend,
                    Runnable onBackFromChat,
                    Runnable onBackFromLogin) {

        root = new BorderPane();
        bindBackground(root, model); 
        

        // ---------------- LOGIN PANE ----------------
        TextField usernameField = new TextField();
        TextField hostField     = new TextField();
        TextField portField     = new TextField();

        usernameField.textProperty().bindBidirectional(model.usernameProperty());
        hostField.textProperty().bindBidirectional(model.hostProperty());
        portField.textProperty().bindBidirectional(model.portProperty());

        Label usernameLabel = new Label("Username");
        Label hostLabel     = new Label("Chat server address");
        Label portLabel     = new Label("Chat server port number");

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(model.loginErrorProperty());

        Button loginButton = new Button("Login");
        Button mainMenuButton = new Button("Main menu");

        loginButton.setOnAction(e -> onLogin.run());
        mainMenuButton.setOnAction(e -> onBackFromLogin.run());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(15);
        form.add(usernameLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(hostLabel,     0, 1);
        form.add(hostField,     1, 1);
        form.add(portLabel,     0, 2);
        form.add(portField,     1, 2);

        HBox buttons = new HBox(10, loginButton, errorLabel);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox loginPane = new VBox(15, form, buttons, mainMenuButton);
        loginPane.setPadding(new Insets(40));
        loginPane.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(loginPane, new Insets(0, 0, 0, 200));


        // ---------------- CHAT PANE ----------------
        ListView<String> messagesList = new ListView<>();
        ListView<String> usersList = new ListView<>();

        messagesList.itemsProperty().bind(model.messagesProperty());
        usersList.itemsProperty().bind(model.usersProperty());

        TextField inputField = new TextField();
        inputField.textProperty().bindBidirectional(model.inputTextProperty());

        Button sendButton = new Button("Send");
        Button backButton = new Button("Back");

        sendButton.setOnAction(e -> onSend.run());
        backButton.setOnAction(e -> onBackFromChat.run());
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSend.run();
        });

        BorderPane chatPane = new BorderPane();
        chatPane.setCenter(messagesList);
        chatPane.setRight(new VBox(new Label("Online"), usersList));
        chatPane.setBottom(new HBox(5, inputField, sendButton));
        chatPane.setTop(backButton);

        // auto-switch view:
        model.connectedProperty().addListener((obs, oldVal, isConnected) ->
            root.setCenter(isConnected ? chatPane : loginPane)
        );
        root.setCenter(model.connectedProperty().get() ? chatPane : loginPane);
    }

    public Region getRoot() {
        return root;
    }
}
