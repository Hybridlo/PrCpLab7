package lab7;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Controller {
    public Label labelStart;
    public AnchorPane mainPane;

    ImageView gunView;

    List<ImageView> duckViews = new ArrayList<>();
    boolean moveLeft = false, moveRight = false;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    public void startHover() {
        labelStart.setTextFill(new Color(1, 0, 0, 1));
    }

    public void startUnhover() {
        labelStart.setTextFill((new Color(0, 0, 0, 1)));
    }

    public void startGame() {
        mainPane.getChildren().remove(labelStart);

        initGame();
    }

    private void initGame() {
        Image gun = null;
        try {
            gun = new Image(getClass().getResource("images/gun.png").toURI().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        gunView = new ImageView(gun);

        gunView.setLayoutX(mainPane.getWidth() / 2);
        AnchorPane.setBottomAnchor(gunView, 0.0);

        mainPane.getChildren().add(gunView);

        initControls();

        duckGenerator();
    }

    private void initControls() {
        Scene scene = mainPane.getScene();

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:  moveLeft   = true; break;
                case RIGHT: moveRight  = true; break;
            }
        });

        scene.setOnKeyReleased(event -> {
            switch (event.getCode()) {
                case LEFT:  moveLeft   = false; break;
                case RIGHT: moveRight  = false; break;
                case SPACE: shoot(); break;
            }
        });

        scene.setOnMouseClicked(event -> {
            int clickCollisionDistance = 80;

            if (event.getButton() == MouseButton.PRIMARY) {
                for (ImageView duckView : duckViews) {
                    if (Math.pow(event.getSceneX() - duckView.getLayoutX(), 2)
                            + Math.pow(event.getSceneY() - duckView.getLayoutY(), 2)
                            < Math.pow(clickCollisionDistance, 2)
                            && !duckView.getImage().getUrl().contains("duckDead")) {            //within collision distance and not

                        try {       //set duck dead
                            duckView.setImage(new Image(getClass().getResource("images/duckDead.png").toURI().toString()));
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                        break;
                    }
                }
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            int gunSpeed = 3;

            @Override
            public void handle(long l) {
                int dx = 0;

                if (moveLeft)   dx -= gunSpeed;
                if (moveRight)  dx += gunSpeed;

                if (gunView.getLayoutX() + gunView.getImage().getWidth() + dx < scene.getWidth() && gunView.getLayoutX() + dx > 0) {
                    gunView.setLayoutX(gunView.getLayoutX() + dx);
                }
            }
        };
        timer.start();
    }

    private void shoot() {
        Image bullet = null;

        try {
            bullet = new Image(getClass().getResource("images/bullet.png").toURI().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        ImageView bulletView = new ImageView(bullet);
        bulletView.setLayoutX(gunView.getLayoutX() + (gunView.getImage().getWidth() / 2) - (bulletView.getImage().getWidth() / 2));
        bulletView.setLayoutY(gunView.getLayoutY() - bulletView.getImage().getHeight());

        mainPane.getChildren().add(bulletView);

        int bulletSpeed = 10;
        int collisionDistance = 50;

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                bulletView.setLayoutY(bulletView.getLayoutY() - bulletSpeed);

                if (bulletView.getLayoutY() + bulletView.getImage().getHeight() < 0) {
                    this.stop();
                    mainPane.getChildren().remove(bulletView);
                }

                lock.readLock().lock();     //with a RWLock

                for (ImageView duckView : duckViews) {
                    if (Math.pow(bulletView.getLayoutX() - duckView.getLayoutX(), 2)
                            + Math.pow(bulletView.getLayoutY() - duckView.getLayoutY(), 2)
                            < Math.pow(collisionDistance, 2)
                        && !duckView.getImage().getUrl().contains("duckDead")) {            //within collision distance and not dead
                        mainPane.getChildren().remove(bulletView);

                        try {       //set duck dead
                            duckView.setImage(new Image(getClass().getResource("images/duckDead.png").toURI().toString()));
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                        this.stop();
                        break;
                    }
                }

                lock.readLock().unlock();   //unlock
            }
        };
        timer.start();
    }

    private void duckGenerator() {
        AnimationTimer timer = new AnimationTimer() {
            long timeSinceLastDuck = 0;
            int duckSpeed = 1;
            Random random = new Random();
            long lastUpdate = System.nanoTime();
            long second = 1_000_000_000;

            @Override
            public void handle(long l) {
                timeSinceLastDuck += l - lastUpdate;

                if (timeSinceLastDuck / second > 1) {    //generate duck every second
                    timeSinceLastDuck = 0;

                    boolean leftSide = random.nextBoolean();    //true - left, false - right
                    int wiggleLength = 50;
                    int middleHeight = wiggleLength + random.nextInt(200);

                    Image duck = null;

                    try {
                        if (leftSide) {
                            duck = new Image(getClass().getResource("images/duckRight0.png").toURI().toString());
                        } else {
                            duck = new Image(getClass().getResource("images/duckLeft0.png").toURI().toString());
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }

                    ImageView duckView = new ImageView(duck);

                    duckView.setLayoutY(middleHeight);

                    if (leftSide) {
                        duckView.setLayoutX(-duckView.getImage().getWidth());
                    } else {
                        duckView.setLayoutX(mainPane.getWidth());
                    }

                    addDuckView(duckView);

                    AnimationTimer duckTimer = new AnimationTimer() {
                        int state = 0;
                        long timeSinceLastChangeState = 0;
                        boolean goingUp = true;
                        long lastUpdate = System.nanoTime();
                        boolean dead = false;

                        @Override
                        public void handle(long l) {
                            timeSinceLastChangeState += l - lastUpdate;

                            if (dead) {
                                if (timeSinceLastChangeState / second > 2) {               //with a delay
                                    removeDuckView(duckView);
                                    this.stop();
                                }

                                return;
                            }

                            if (duckView.getImage().getUrl().contains("duckDead")) {
                                timeSinceLastChangeState = 0;
                                dead = true;        //if duck killed - remove it

                                return;
                            }

                            if (timeSinceLastChangeState / second > 0.5) {   //change state every half a second
                                timeSinceLastChangeState = 0;
                                state = 1 - state;

                                try {
                                    if (leftSide) {
                                        duckView.setImage(new Image(getClass().getResource("images/duckRight" + state + ".png").toURI().toString()));
                                    } else {
                                        duckView.setImage(new Image(getClass().getResource("images/duckLeft" + state + ".png").toURI().toString()));
                                    }
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (leftSide) {         //going left or right
                                duckView.setLayoutX(duckView.getLayoutX() + duckSpeed);

                                if (duckView.getLayoutX() > mainPane.getWidth()) {
                                    removeDuckView(duckView);
                                    this.stop();
                                }
                            } else {
                                duckView.setLayoutX(duckView.getLayoutX() - duckSpeed);

                                if (duckView.getLayoutX() < -duckView.getImage().getWidth()) {
                                    removeDuckView(duckView);
                                    this.stop();
                                }
                            }

                            if (goingUp) {          //going up or down
                                duckView.setLayoutY(duckView.getLayoutY() - duckSpeed);

                                if (duckView.getLayoutY() < middleHeight - wiggleLength) {
                                    goingUp = false;
                                }
                            } else {
                                duckView.setLayoutY(duckView.getLayoutY() + duckSpeed);

                                if (duckView.getLayoutY() > middleHeight + wiggleLength) {
                                    goingUp = true;
                                }
                            }

                            lastUpdate = l;     //change inner last update time
                        }
                    };
                    duckTimer.start();
                }

                lastUpdate = l;     //change last update time
            }
        };
        timer.start();
    }

    private void addDuckView(ImageView duck) {
        lock.writeLock().lock();
        mainPane.getChildren().add(duck);   //with RWLock

        duckViews.add(duck);                //with RWLock
        lock.writeLock().unlock();
    }

    private void removeDuckView(ImageView duck) {
        lock.writeLock().lock();
        mainPane.getChildren().remove(duck);    //with RWLock

        duckViews.remove(duck);                 //with RWLock
        lock.writeLock().unlock();
    }
}
