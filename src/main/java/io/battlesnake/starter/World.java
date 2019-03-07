package io.battlesnake.starter;

import java.util.ArrayList;
import java.util.List;

public class World {
    private final int width;
    private final int height;

    private final List<Orm> snakes = new ArrayList<>();

    public World(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void addSnake(Orm snake) {
        snakes.add(snake);
    }

    public boolean isObstacle(Vector p) {
        for (Orm snake : snakes) {
            if (snake.intersects(p)) {
                return true;
            }
        }

        return false;
    }
}
