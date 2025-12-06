package AlienMarauders.Menu.Chatmenu;

import AlienMarauders.Model;
import javafx.beans.property.*;

import java.util.List;

public class ChatModel {

    private final Model rootModel;

    // login / connection state
    private final StringProperty username   = new SimpleStringProperty("");
    private final StringProperty host       = new SimpleStringProperty("localhost");
    private final StringProperty port       = new SimpleStringProperty("8888");
    private final StringProperty loginError = new SimpleStringProperty("");
    private final BooleanProperty connected = new SimpleBooleanProperty(false);

    // message input
    private final StringProperty inputText  = new SimpleStringProperty("");

    public ChatModel(Model rootModel) {
        this.rootModel = rootModel;
    }

    // --- background (same as MainMenuModel) ---
    public String getBackgroundImage() {
        return rootModel.getBackgroundImage();
    }

    public ObjectProperty<String> backgroundImageProperty() {
        return rootModel.backgroundImageProperty();
    }

    // shared chat lists (from root model)
    public ListProperty<String> usersProperty() {
        return rootModel.chatUsersProperty();
    }

    public ListProperty<String> messagesProperty() {
        return rootModel.chatMessagesProperty();
    }

    public void addLine(String line) {
        messagesProperty().add(line);
    }

    public void setUsers(List<String> users) {
        usersProperty().setAll(users);
    }

    // login properties
    public StringProperty usernameProperty()   { return username; }
    public StringProperty hostProperty()       { return host; }
    public StringProperty portProperty()       { return port; }
    public StringProperty loginErrorProperty() { return loginError; }
    public BooleanProperty connectedProperty() { return connected; }

    public StringProperty inputTextProperty()  { return inputText; }

    public void clearChatLists() {
        messagesProperty().clear();
        usersProperty().clear();
    }

    public void resetLoginState() {
        loginError.set("");
        connected.set(false);
        inputText.set("");
    }
}
