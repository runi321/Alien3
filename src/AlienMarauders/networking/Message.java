package AlienMarauders.networking;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {

    private final MessageType type;
    private final String sender;
    private final String text;
    private final List<String> users;

    public Message(MessageType type, String sender, String text, List<String> users) {
        this.type = type;
        this.sender = sender;
        this.text = text;
        this.users = users;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public List<String> getUsers() {
        return users;
    }
}