package AlienMarauders.Game.formation;

import AlienMarauders.Game.Gameview;
import AlienMarauders.Game.entities.Enemy;
import javafx.scene.image.Image;

import java.util.ArrayList;

public class GridFormation implements Formation {

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final Image spriteSheet;

    public GridFormation(Image spriteSheet) {
        this.spriteSheet = spriteSheet;
    }

    @Override
    public void createEnemies() {
        enemies.clear();

        int rows = 3;
        int cols = 4;

        double startX   = 60;
        double startY   = 60;
        double spacingX = 80;
        double spacingY = 60;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = startX + c * spacingX;
                double y = startY + r * spacingY;
                enemies.add(new Enemy(x, y, 40, 40, spriteSheet, 2));
            }
        }
    }

    @Override
    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }
}
