package AlienMarauders.Game;

import AlienMarauders.Controller;
import AlienMarauders.Model;
import AlienMarauders.Game.entities.Enemy;
import AlienMarauders.Game.entities.Player;
import AlienMarauders.Game.entities.PlayerShot;
import AlienMarauders.Game.formation.Formation;
import AlienMarauders.Game.formation.GridFormation;
import AlienMarauders.Game.formation.RowFormation;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import AlienMarauders.Game.movement.MovementStrategy;
import AlienMarauders.Game.movement.NoMovementStrategy;
import AlienMarauders.Game.movement.MoveDownStrategy;
import AlienMarauders.Game.movement.ZigZagMovementStrategy;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import javafx.scene.layout.Region;
import java.util.List;

public class GameController {

    private final Model model;
    private final Gameview view;
    private final Controller rootController;
    private boolean gameOver = false;

    // Game state
    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<PlayerShot> shots = new ArrayList<>();
    private final Score score = new Score();

    private MovementStrategy movementStrategy;
    private double speedMultiplier = 1.0;

    // Concurrency: one worker thread for collision checks
    private final ExecutorService executor =
        Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );


    private AnimationTimer gameLoop;
    private boolean firstFrame = true;

    public GameController(Model model, Controller rootController) {
        this.model = model;
        this.rootController = rootController;
        this.view = new Gameview(model); 

        setupGame();
        attachHandlers();
    }

    public Region getView() {
    return view.getRoot();   // Gameview must have getRoot()
}
    /* ---------------------- setup ---------------------- */

    private void setupGame() {

        gameOver = false; 

        enemies.clear();
        shots.clear();
        score.resetScore();
        speedMultiplier = 1.0;

        // Create player
        Image playerImage = new Image(
                getClass().getResource("/AlienMarauders/Myndir/player.png").toExternalForm());

        player = new Player(
                Gameview.WIDTH / 2.0 - 25,
                Gameview.HEIGHT - 80,
                50, 50,
                playerImage
        );

        updateSpeedMultiplierFromSettings();


        // First wave of enemies
            spawnEnemies();
            chooseMovementStrategy();

        // Keyboard input
        initializeKeyBindings(view.getCanvas(), player);

        // Game loop
        gameLoop = new AnimationTimer() {
            long lastNanoTime = 0;

            @Override
            public void handle(long now) {
                if (firstFrame) {
                    lastNanoTime = now;
                    firstFrame = false;
                    return;
                }
                long elapsedMs = (now - lastNanoTime) / 1_000_000;
                lastNanoTime = now;

                double dt = elapsedMs;

                // 1) Move entities
                player.move(dt);
                movementStrategy.moveEnemies(enemies, dt);
                for (PlayerShot shot : shots) {
                    shot.move(dt);
                }

                // mark off-screen shots as dead
                for (PlayerShot shot : shots) {
                    if (shot.getPositionY() + shot.getHeight() < 0) {
                        shot.kill();
                    }
                }

                                // 2) Collision checks (concurrent inside doCollisionChecks)
                CollisionResult r = doCollisionChecks();

                // 3) Apply collision result on FX thread
                if (r.playerHit || r.enemyAtBottom) {
                    gameOver = true;
                    stopGame();
                }

                for (int i : r.enemyIndicesToKill) {
                    if (i >= 0 && i < enemies.size()) {
                        enemies.get(i).kill();
                    }
                }
                for (int j : r.shotIndicesToKill) {
                    if (j >= 0 && j < shots.size()) {
                        shots.get(j).kill();
                    }
                }

                if (r.scoreDelta != 0) {
                    score.updateScore(r.scoreDelta);
                }


                enemies.removeIf(e -> !e.isAlive());
                shots.removeIf(s -> !s.isAlive());


                if (enemies.isEmpty()) {
                    speedMultiplier *= 1.15;

                    // keep same pattern, just faster:
                    if (movementStrategy != null) {
                        movementStrategy.setSpeedMultiplier(speedMultiplier);
                    }
                    
                    chooseMovementStrategy();
                    shots.clear();
                    spawnEnemies();
                }

                // 5) Render
                var gc = view.getGraphicsContext();
                gc.clearRect(0, 0, Gameview.WIDTH, Gameview.HEIGHT);

                player.render(gc);
                for (Enemy e : enemies) e.render(gc);
                for (PlayerShot s : shots) s.render(gc);
                score.render(gc);

                if (gameOver) gc.fillText("GAME OVER", 280, 300);
            }
        };
    }

    /* ----------------- enemies & strategy ----------------- */

    private void spawnEnemies() {
        enemies.clear();

        Image enemySheet = new Image(
                getClass().getResource("/AlienMarauders/Myndir/greenmonster.png").toExternalForm());

        // choose a formation implementation
        Formation formation;
        Random rand = new Random();
        if (rand.nextBoolean()) {
            formation = new RowFormation(enemySheet);
        } else {
            formation = new GridFormation(enemySheet);
        }

        formation.createEnemies();
        enemies.addAll(formation.getEnemies());
    }



    private void chooseMovementStrategy() {
    Random rand = new Random();
    int choice = rand.nextInt(3);
    switch (choice) {
        case 0 -> movementStrategy = new NoMovementStrategy();
        case 1 -> movementStrategy = new MoveDownStrategy();
        default -> movementStrategy = new ZigZagMovementStrategy();
    }
    updateSpeedMultiplierFromSettings(); 
}


    
    // runs on FX thread but uses worker threads internally via executor
    private CollisionResult doCollisionChecks() {

        // if nothing to collide, skip
        if (enemies.isEmpty() && shots.isEmpty()) {
            return new CollisionResult();
        }

        // decide how many parallel tasks we want
        int numTasks = Math.min(
                Runtime.getRuntime().availableProcessors(),
                Math.max(1, shots.size())
        );

        List<Callable<CollisionResult>> tasks = new ArrayList<>();

        int chunkSize = (int) Math.ceil(shots.size() / (double) numTasks);

        // --- tasks for shots vs enemies (chunked by shot index) ---
        for (int t = 0; t < numTasks; t++) {
            int from = t * chunkSize;
            int to   = Math.min(shots.size(), from + chunkSize);
            if (from >= to) break;

            tasks.add(() -> {
                CollisionResult r = new CollisionResult();

                for (int s = from; s < to; s++) {
                    PlayerShot shot = shots.get(s);
                    if (!shot.isAlive()) continue;

                    for (int e = 0; e < enemies.size(); e++) {
                        Enemy enemy = enemies.get(e);
                        if (!enemy.isAlive()) continue;

                        if (CollisionDetection.Aabb(shot, enemy)) {
                            r.enemyIndicesToKill.add(e);
                            r.shotIndicesToKill.add(s);
                            r.scoreDelta += 10;
                        }
                    }
                }
                return r;
            });
        }

        // --- one extra task for player vs enemies + enemyAtBottom ---
        tasks.add(() -> {
            CollisionResult r = new CollisionResult();
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (!enemy.isAlive()) continue;

                if (CollisionDetection.Aabb(player, enemy)) {
                    r.playerHit = true;
                }
                if (enemy.getPositionY() + enemy.getHeight() >= Gameview.HEIGHT) {
                    r.enemyAtBottom = true;
                }
            }
            return r;
        });

        try {
            // runs tasks in parallel, BUT this call itself is blocking
            List<Future<CollisionResult>> futures = executor.invokeAll(tasks);

            // merge all partial results
            CollisionResult result = new CollisionResult();
            for (Future<CollisionResult> f : futures) {
                CollisionResult r = f.get();
                if (r.playerHit)     result.playerHit = true;
                if (r.enemyAtBottom) result.enemyAtBottom = true;
                result.scoreDelta += r.scoreDelta;
                result.enemyIndicesToKill.addAll(r.enemyIndicesToKill);
                result.shotIndicesToKill.addAll(r.shotIndicesToKill);
            }
            return result;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return new CollisionResult();
        }
    }



    private void updateSpeedMultiplierFromSettings() {
        switch (model.getDifficulty()) {
            case EASY   -> speedMultiplier = 0.7;
            case MEDIUM -> speedMultiplier = 1.0;
            case HARD   -> speedMultiplier = 1.4;
        }
        if (movementStrategy != null) {
            movementStrategy.setSpeedMultiplier(speedMultiplier);
        }
    }

    /* ----------------- input & buttons ----------------- */

    private void initializeKeyBindings(Canvas canvas, Player player) {
        canvas.setFocusTraversable(true);

        canvas.setOnKeyPressed(evt -> {
            switch (evt.getCode()) {
                case A, LEFT  -> player.movingLeft(true);
                case D, RIGHT -> player.movingRight(true);
                case W, UP    -> player.movingUp(true);
                case S, DOWN  -> player.movingDown(true);
                case SPACE    -> shoot();
                case ESCAPE   -> {
                    stopGame();
                    rootController.showMainMenu();
                }
                default -> { }
            }
        });

        canvas.setOnKeyReleased(evt -> {
            switch (evt.getCode()) {
                case A, LEFT  -> player.movingLeft(false);
                case D, RIGHT -> player.movingRight(false);
                case W, UP    -> player.movingUp(false);
                case S, DOWN  -> player.movingDown(false);
                default -> { }
            }
        });
    }

    private void shoot() {
        double x = player.getPositionX() + player.getWidth() / 2.0 - 2;
        double y = player.getPositionY() - 12;
        shots.add(new PlayerShot(x, y));
    }

    private void attachHandlers() {
        view.getBackBtn().setOnAction(e -> {
            stopGame();
            rootController.showMainMenu();
        });
    }

    /* ----------------- lifecycle ----------------- */

    public void startGame() {
        stopGame();

        setupGame();
        
        firstFrame = true;
        gameLoop.start();
        view.getCanvas().requestFocus();
    }


    public void stopGame() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    // call when app closes to clean up the thread (not used yet)
    public void shutdownExecutor() {
        executor.shutdownNow();
    }
}
