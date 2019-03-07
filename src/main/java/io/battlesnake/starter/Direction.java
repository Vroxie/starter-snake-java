package io.battlesnake.starter;

public enum Direction {
    LEFT("left", new Vector(-1, 0)),
    RIGHT("right", new Vector(1, 0)),
    UP("up", new Vector(0, -1)),
    DOWN("down", new Vector(0, 1));

    public final String stringDirection;
    public final Vector vec;

    Direction(String stringDirection, Vector vec) {
        this.stringDirection = stringDirection;
        this.vec = vec;
    }
}
