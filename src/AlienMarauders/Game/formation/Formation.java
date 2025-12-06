package AlienMarauders.Game.formation;
import AlienMarauders.Game.formation.Formation;

import java.util.ArrayList;

import AlienMarauders.Game.entities.Enemy;

public interface Formation {
    public void createEnemies();
    public ArrayList<Enemy> getEnemies();
}

