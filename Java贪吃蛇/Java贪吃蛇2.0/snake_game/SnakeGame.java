package snake_game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SnakeGame extends Application {
    private static final int TILE_SIZE = 25;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final int ALL_TILES = (WIDTH * HEIGHT) / (TILE_SIZE * TILE_SIZE);

    private final int[] x = new int[ALL_TILES];
    private final int[] y = new int[ALL_TILES];

    private int bodyParts = 3;
    private int applesEaten;
    private int appleX;
    private int appleY;

    private char direction = 'R';
    private boolean running = false;
    private boolean paused = false;
    private Timeline timeline;
    private Random random;

    private Color snakeColor = Color.LIGHTGREEN; // 初始蛇的颜色
    private Color foodColor = Color.LIGHTPINK; // 初始食物的颜色

    private Label scoreLabel;
    private boolean showScores = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        random = new Random();

        BorderPane root = new BorderPane();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        HBox controls = new HBox(10);
        Button startPauseButton = new Button("开始/暂停游戏");
        startPauseButton.setOnAction(e -> {
            toggleStartPause(gc);
            canvas.requestFocus(); // 确保按钮点击后 Canvas 重新获得焦点
        });
        Button saveButton = new Button("存档");
        saveButton.setOnAction(e -> {
            saveGame();
            canvas.requestFocus(); // 确保按钮点击后 Canvas 重新获得焦点
        });
        Button loadButton = new Button("读档");
        loadButton.setOnAction(e -> {
            loadGame();
            canvas.requestFocus(); // 确保按钮点击后 Canvas 重新获得焦点
        });
        Button restartButton = new Button("重新开始");
        restartButton.setOnAction(e -> {
            startGame(gc);
            canvas.requestFocus(); // 确保按钮点击后 Canvas 重新获得焦点
        });
        Button showHideScoresButton = new Button("隐藏排行榜");
        showHideScoresButton.setOnAction(e -> {
            toggleShowScores(gc, showHideScoresButton);
            canvas.requestFocus(); // 确保按钮点击后 Canvas 重新获得焦点
        });

        Slider speedSlider = new Slider(50, 300, 140);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(50);
        speedSlider.setFocusTraversable(false); // 禁用 Slider 的焦点
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (timeline != null) {
                timeline.setRate(300 / newVal.doubleValue());
            }
        });

        controls.getChildren().addAll(startPauseButton, saveButton, loadButton, restartButton, showHideScoresButton, new Label("速度:"), speedSlider);
        root.setTop(controls);

        scoreLabel = new Label("得分: 0");
        root.setBottom(scoreLabel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case LEFT:
                    if (direction != 'R') direction = 'L';
                    break;
                case RIGHT:
                    if (direction != 'L') direction = 'R';
                    break;
                case UP:
                    if (direction != 'D') direction = 'U';
                    break;
                case DOWN:
                    if (direction != 'U') direction = 'D';
                    break;
                default:
                    break;
            }
            e.consume(); // 阻止事件传播
            canvas.requestFocus(); // 确保 Canvas 保持焦点
        });

        primaryStage.setTitle("贪吃蛇游戏");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 确保 Canvas 聚焦以接收键盘事件
        canvas.setFocusTraversable(true);
        canvas.requestFocus();

        startGame(gc);
    }

    private void toggleStartPause(GraphicsContext gc) {
        if (!running) {
            startGame(gc);
        } else {
            pauseGame();
        }
    }

    private void toggleShowScores(GraphicsContext gc, Button button) {
        showScores = !showScores;
        button.setText(showScores ? "隐藏排行榜" : "显示排行榜");
        draw(gc);
    }

    private void startGame(GraphicsContext gc) {
        if (!paused) {
            newApple();
            bodyParts = 3;
            applesEaten = 0;
            direction = 'R';
            for (int i = 0; i < bodyParts; i++) {
                x[i] = 0;
                y[i] = 0;
            }
        }
        running = true;
        paused = false;
        timeline = new Timeline(new KeyFrame(Duration.millis(300 / (300 / 140.0)), e -> {
            if (running && !paused) {
                move();
                checkApple();
                checkCollisions();
            }
            draw(gc);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pauseGame() {
        if (running) {
            paused = !paused;
            if (paused) {
                timeline.pause();
            } else {
                timeline.play();
            }
        }
    }

    private void newApple() {
        appleX = random.nextInt((int) (WIDTH / TILE_SIZE)) * TILE_SIZE;
        appleY = random.nextInt((int) (HEIGHT / TILE_SIZE)) * TILE_SIZE;
        foodColor = Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.5 + random.nextDouble() * 0.5); // 随机柔和食物颜色
    }

    private void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'U':
                y[0] = y[0] - TILE_SIZE;
                break;
            case 'D':
                y[0] = y[0] + TILE_SIZE;
                break;
            case 'L':
                x[0] = x[0] - TILE_SIZE;
                break;
            case 'R':
                x[0] = x[0] + TILE_SIZE;
                break;
        }
    }

    private void checkApple() {
        if ((x[0] == appleX) && (y[0] == appleY)) {
            bodyParts++;
            applesEaten++;
            newApple();
            snakeColor = Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.5 + random.nextDouble() * 0.5); // 随机柔和蛇的颜色
        }
    }

    private void checkCollisions() {
        for (int i = bodyParts; i > 0; i--) {
            if ((x[0] == x[i]) && (y[0] == y[i])) {
                running = false;
            }
        }

        if (x[0] < 0 || x[0] >= WIDTH || y[0] < 0 || y[0] >= HEIGHT) {
            running = false;
        }

        if (!running) {
            timeline.stop();
        }
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.LAVENDER); // 柔和的背景颜色
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        if (running) {
            gc.setFill(foodColor); // 使用随机食物颜色
            gc.fillOval(appleX, appleY, TILE_SIZE, TILE_SIZE);

            for (int i = 0; i < bodyParts; i++) {
                gc.setFill(snakeColor); // 使用随机蛇的颜色
                gc.fillRect(x[i], y[i], TILE_SIZE, TILE_SIZE);
            }

            scoreLabel.setText("得分: " + applesEaten);
        } else {
            gameOver(gc);
        }

        if (showScores) {
            drawScores(gc);
        }
    }

    private void gameOver(GraphicsContext gc) {
        saveScore();
        List<Integer> scores = loadScores();

        String message = "Game Over";
        gc.setFill(Color.PINK); // 柔和的红色
        gc.setFont(new javafx.scene.text.Font("Helvetica", 75));
        gc.fillText(message, (WIDTH - gc.getFont().getSize() * message.length()) / 2, HEIGHT / 2);

        gc.setFont(new javafx.scene.text.Font("Helvetica", 20));
        gc.fillText("得分: " + applesEaten, (WIDTH - gc.getFont().getSize() * ("得分: " + applesEaten).length()) / 2, HEIGHT / 2 + 50);

        gc.fillText("排名:", (WIDTH - gc.getFont().getSize() * "排名:".length()) / 2, HEIGHT / 2 + 100);
        for (int i = 0; i < Math.min(5, scores.size()); i++) {
            gc.fillText((i + 1) + ". " + scores.get(i), (WIDTH - gc.getFont().getSize() * ((i + 1) + ". " + scores.get(i)).length()) / 2, HEIGHT / 2 + 130 + i * 30);
        }
    }

    private void saveGame() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("savegame.dat"))) {
            out.writeObject(x);
            out.writeObject(y);
            out.writeInt(bodyParts);
            out.writeInt(applesEaten);
            out.writeChar(direction);
            out.writeBoolean(running);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGame() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("savegame.dat"))) {
            int[] savedX = (int[]) in.readObject();
            int[] savedY = (int[]) in.readObject();
            System.arraycopy(savedX, 0, x, 0, savedX.length);
            System.arraycopy(savedY, 0, y, 0, savedY.length);
            bodyParts = in.readInt();
            applesEaten = in.readInt();
            direction = in.readChar();
            running = in.readBoolean();
            timeline.play();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("scores.txt", true))) {
            writer.write(applesEaten + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> loadScores() {
        List<Integer> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("scores.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                scores.add(Integer.parseInt(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort(scores, Collections.reverseOrder());
        return scores;
    }

    private void drawScores(GraphicsContext gc) {
        List<Integer> scores = loadScores();
        gc.setFont(new javafx.scene.text.Font("Helvetica", 20));
        gc.fillText("排名:", 10, 50);
        for (int i = 0; i < Math.min(5, scores.size()); i++) {
            gc.fillText((i + 1) + ". " + scores.get(i), 10, 80 + i * 30);
        }
    }
}