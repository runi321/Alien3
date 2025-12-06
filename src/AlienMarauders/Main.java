package AlienMarauders;



import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

@Override
public void start(Stage primaryStage) {
    Model model = new Model();
    View view = new View();
    Controller controller = new Controller(model, view);

    primaryStage.setTitle("Alien Marauders");
    primaryStage.setScene(view.getScene());
    primaryStage.show();

    controller.showMainMenu();
}

    public static void main(String[] args) {
    launch(args);

}
}