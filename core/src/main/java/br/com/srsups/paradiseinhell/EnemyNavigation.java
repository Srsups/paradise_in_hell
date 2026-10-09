package br.com.srsups.paradiseinhell;

import java.util.Arrays;

/** Shared reverse breadth-first field plus swept body collision; no graphics/input. */
public final class EnemyNavigation {
    @FunctionalInterface public interface Collision { boolean solid(float x, float y); }
    public static class Body {
        public float x, y;
        private float waypointX, waypointY;
        private boolean hasWaypoint;
        public Body(float x, float y) { this.x = x; this.y = y; }
    }
    private static final int TILE = 16, RADIUS = 32, WIDTH = RADIUS * 2 + 1;
    private static final float HALF_BODY = 7;
    private final Collision collision;
    private final int[] distance = new int[WIDTH * WIDTH], queue = new int[WIDTH * WIDTH];
    private final boolean[] walkable = new boolean[WIDTH * WIDTH];
    private int left, bottom, goalX = Integer.MIN_VALUE, goalY = Integer.MIN_VALUE;
    private float targetX, targetY, refreshTimer;
    public EnemyNavigation(Collision collision) { this.collision = collision; }

    public void update(float delta, float targetX, float targetY) {
        this.targetX = targetX; this.targetY = targetY;
        int nextX = tile(targetX), nextY = tile(targetY);
        refreshTimer -= delta;
        if (goalX == nextX && goalY == nextY && refreshTimer > 0) return;
        goalX = nextX; goalY = nextY;
        left = goalX - RADIUS; bottom = goalY - RADIUS;
        refreshTimer = 0.3f;
        Arrays.fill(distance, -1);
        for (int y = 0; y < WIDTH; y++) for (int x = 0; x < WIDTH; x++) {
            walkable[y * WIDTH + x] = canOccupy(center(left + x), center(bottom + y));
        }
        int goal = index(goalX, goalY);
        if (!walkable[goal]) return;
        int head = 0, tail = 0;
        queue[tail++] = goal;
        distance[goal] = 0;
        while (head < tail) {
            int current = queue[head++], x = current % WIDTH, y = current / WIDTH;
            if (x > 0) tail = visit(current - 1, current, tail);
            if (x < WIDTH - 1) tail = visit(current + 1, current, tail);
            if (y > 0) tail = visit(current - WIDTH, current, tail);
            if (y < WIDTH - 1) tail = visit(current + WIDTH, current, tail);
        }
    }
    private int visit(int next, int current, int tail) {
        if (walkable[next] && distance[next] < 0) {
            distance[next] = distance[current] + 1;
            queue[tail++] = next;
        }
        return tail;
    }
    public boolean reachable(float x, float y) {
        int index = index(tile(x), tile(y));
        return index >= 0 && distance[index] >= 0;
    }
    public boolean canOccupy(float x, float y) {
        return !collision.solid(x - HALF_BODY, y - HALF_BODY)
            && !collision.solid(x + HALF_BODY, y - HALF_BODY)
            && !collision.solid(x - HALF_BODY, y + HALF_BODY)
            && !collision.solid(x + HALF_BODY, y + HALF_BODY);
    }
    public boolean clearLine(float x, float y, float nextX, float nextY) {
        return canTravel(x, y, nextX, nextY);
    }
    private boolean canTravel(float x, float y, float nextX, float nextY) {
        return clearRay(x - HALF_BODY, y - HALF_BODY, nextX - HALF_BODY, nextY - HALF_BODY)
            && clearRay(x + HALF_BODY, y - HALF_BODY, nextX + HALF_BODY, nextY - HALF_BODY)
            && clearRay(x - HALF_BODY, y + HALF_BODY, nextX - HALF_BODY, nextY + HALF_BODY)
            && clearRay(x + HALF_BODY, y + HALF_BODY, nextX + HALF_BODY, nextY + HALF_BODY);
    }
    private boolean clearRay(float x, float y, float nextX, float nextY) {
        int gridX = tile(x), gridY = tile(y), endX = tile(nextX), endY = tile(nextY);
        float dx = nextX - x, dy = nextY - y;
        int stepX = Float.compare(dx, 0), stepY = Float.compare(dy, 0);
        float incrementX = stepX == 0 ? Float.POSITIVE_INFINITY : TILE / Math.abs(dx);
        float incrementY = stepY == 0 ? Float.POSITIVE_INFINITY : TILE / Math.abs(dy);
        float crossingX = stepX == 0 ? Float.POSITIVE_INFINITY
            : ((gridX + (stepX > 0 ? 1 : 0)) * TILE - x) / dx;
        float crossingY = stepY == 0 ? Float.POSITIVE_INFINITY
            : ((gridY + (stepY > 0 ? 1 : 0)) * TILE - y) / dy;
        while (true) {
            if (collision.solid(center(gridX), center(gridY))) return false;
            if (gridX == endX && gridY == endY) return true;
            if (Math.abs(crossingX - crossingY) < 0.00001f) {
                if (collision.solid(center(gridX + stepX), center(gridY))
                        || collision.solid(center(gridX), center(gridY + stepY))) return false;
                gridX += stepX; gridY += stepY;
                crossingX += incrementX; crossingY += incrementY;
            } else if (crossingX < crossingY) {
                gridX += stepX; crossingX += incrementX;
            } else {
                gridY += stepY; crossingY += incrementY;
            }
        }
    }
    public void move(Body body, float speed, float delta) {
        if (delta <= 0) return;
        float x = body.x + 8, y = body.y + 8;
        float dx = targetX - x, dy = targetY - y;
        if (dx * dx + dy * dy < 0.01f) return;
        float destinationX, destinationY;
        if (Math.abs(dx) <= 512 && Math.abs(dy) <= 512 && canTravel(x, y, targetX, targetY)) {
            body.hasWaypoint = false;
            destinationX = targetX; destinationY = targetY;
        } else {
            if (body.hasWaypoint && squared(x - body.waypointX, y - body.waypointY) < 0.01f) body.hasWaypoint = false;
            if (!body.hasWaypoint) {
                int tileX = tile(x), tileY = tile(y), current = index(tileX, tileY);
                if (current < 0 || distance[current] < 0) return;
                float cellX = center(tileX), cellY = center(tileY);
                if (squared(x - cellX, y - cellY) > 0.01f) {
                    body.waypointX = cellX; body.waypointY = cellY;
                } else {
                    int next = current;
                    int localX = current % WIDTH, localY = current / WIDTH;
                    if (localX > 0) next = nearer(current - 1, next);
                    if (localX < WIDTH - 1) next = nearer(current + 1, next);
                    if (localY > 0) next = nearer(current - WIDTH, next);
                    if (localY < WIDTH - 1) next = nearer(current + WIDTH, next);
                    if (next == current) return;
                    body.waypointX = center(left + next % WIDTH);
                    body.waypointY = center(bottom + next / WIDTH);
                }
                body.hasWaypoint = true;
            }
            destinationX = body.waypointX; destinationY = body.waypointY;
        }
        dx = destinationX - x; dy = destinationY - y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.0001f) return;
        float step = Math.min(length, speed * delta);
        float nextX = x + dx / length * step, nextY = y + dy / length * step;
        if (canTravel(x, y, nextX, nextY)) {
            body.x = nextX - 8; body.y = nextY - 8;
        } else body.hasWaypoint = false;
    }
    private int nearer(int candidate, int current) {
        return distance[candidate] >= 0 && distance[candidate] < distance[current] ? candidate : current;
    }
    private int index(int x, int y) {
        x -= left; y -= bottom;
        return x < 0 || y < 0 || x >= WIDTH || y >= WIDTH ? -1 : y * WIDTH + x;
    }
    private static int tile(float coordinate) { return (int) Math.floor(coordinate / TILE); }
    private static float center(int tile) { return tile * TILE + TILE / 2f; }
    private static float squared(float x, float y) { return x * x + y * y; }
}
