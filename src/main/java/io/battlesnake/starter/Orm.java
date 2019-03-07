package io.battlesnake.starter;

import java.util.ArrayList;
import java.util.List;

public class Orm {
    private List<Vector> parts = new ArrayList<>();

    public void addPart(Vector part) {
        parts.add(part);
    }

    public Vector getHead() {
        return parts.get(0);
    }

    public boolean intersects(Vector p) {
        for (Vector part : parts) {
            if (part.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
