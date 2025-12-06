package AlienMarauders;

import AlienMarauders.Game.GameController;
import AlienMarauders.Menu.Chatmenu.ChatController;
import AlienMarauders.Menu.MainMenu.MainMenuController;
import AlienMarauders.Menu.Settingsmenu.SettingsController;

public class Controller {

    private final Model model;
    private final View view;

    private final MainMenuController mainMenuController;
    private final SettingsController settingsController;
    private final ChatController chatController;
    private final GameController gameController;

    public Controller(Model model, View view) {
        this.model = model;
        this.view = view;

        mainMenuController = new MainMenuController(model, this);
        settingsController = new SettingsController(model, this);
        chatController     = new ChatController(model, this);
        gameController     = new GameController(model, this); 
    }

    public void showMainMenu() {
        view.show(mainMenuController.getView());
    }

    public void showSettings() {
        view.show(settingsController.getView());
    }

    public void showChat() {
        view.show(chatController.getView());
    }

    public void showGame() {
        view.show(gameController.getView());
        gameController.startGame();
    }

    public void exitApplication() {
        // make sure the game loop and executor stop
        gameController.stopGame();
        gameController.shutdownExecutor();

        javafx.application.Platform.exit();
    }
}