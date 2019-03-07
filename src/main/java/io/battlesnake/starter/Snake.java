package io.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port != null) {
            LOG.info("Found system provided port: {}", port);
        } else {
            LOG.info("Using default port: {}", port);
            port = "8080";
        }
        port(Integer.parseInt(port));
        get("/", (req, res) -> "Battlesnake documentation can be found at " + 
            "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>.");
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/ping", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the ping request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/ping")) {
                    snakeResponse = ping();
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }
                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));
                return snakeResponse;
            } catch (Exception e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @return an empty response.
         */
        public Map<String, String> ping() {
            return EMPTY;
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        public Map<String, String> start(JsonNode startRequest) {
            Map<String, String> response = new HashMap<>();
            response.put("color", "#ff00ff");
            return response;
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        public Map<String, String> move(JsonNode moveRequest) {
            List<Food> foods = new ArrayList<>();
            World world = new World(moveRequest.get("width").asInt(), moveRequest.get("height").asInt());

            JsonNode food = moveRequest.get("food");
            for(JsonNode a : food.get("data")){
                foods.add(new Food(a.get("x").asInt(), a.get("y").asInt()));
            }

            JsonNode snakes = moveRequest.get("snakes");
            for(JsonNode a : snakes.get("data")) {
                world.addSnake(decodeSnake(a));
            }

            Orm ourSnake = decodeSnake(moveRequest.get("you"));
            world.addSnake(ourSnake);

            Vector head = ourSnake.getHead();

            Food clostestFood = null;
            double prevClosestDist = Double.MAX_VALUE;
            for (Food f : foods) {
                int dx = head.getX() - f.getX();
                int dy = head.getY() - f.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (prevClosestDist > distance) {
                    clostestFood = f;
                    prevClosestDist = distance;
                }
            }

            List<Direction> availableDirections = new ArrayList<>();
            availableDirections.add(Direction.DOWN);
            availableDirections.add(Direction.UP);
            availableDirections.add(Direction.LEFT);
            availableDirections.add(Direction.RIGHT);

            if (head.getX() == 0) {
                availableDirections.remove(Direction.LEFT);
            }
            if (head.getX() == world.getWidth() - 1) {
                availableDirections.remove(Direction.RIGHT);
            }
            if (head.getY() == 0) {
                availableDirections.remove(Direction.UP);
            }
            if (head.getY() == world.getHeight() - 1) {
                availableDirections.remove(Direction.DOWN);
            }

            List <Direction> avaiblecopy = new ArrayList<>(availableDirections);
            for (Direction dir : avaiblecopy) {

                boolean foodInDirection = false;
                if (clostestFood != null) {
                    int dx = (int)Math.signum(clostestFood.getX() - head.getX());
                    int dy = (int)Math.signum(clostestFood.getY() - head.getY());

                    if (dir.equals(Direction.LEFT) && dx == -1 ||
                            dir.equals(Direction.RIGHT) && dx == 1 ||
                            dir.equals(Direction.UP) && dy == -1 ||
                            dir.equals(Direction.DOWN) && dy == 1) {
                        foodInDirection = true;
                    }
                }


                Vector checkifnotObs = new Vector((head.getX() + dir.vec.getX()),(head.getY()) + dir.vec.getY());
                boolean obstacleInDirection = world.isObstacle(checkifnotObs);

                if (obstacleInDirection || (clostestFood != null && !foodInDirection)) {
                    availableDirections.remove(dir);
                }

            }

            Direction finalDirection = availableDirections.get(new Random().nextInt(availableDirections.size()));

            Map<String, String> response = new HashMap<>();

            response.put("move", finalDirection.stringDirection);
            return response;
        }

        private Orm decodeSnake(JsonNode snakeJson) {
            Orm orm = new Orm();
            for (JsonNode part : snakeJson.get("body").get("data")) {
                orm.addPart(new Vector(part.get("x").asInt(), part.get("y").asInt()));
            }
            return orm;
        }


        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            Map<String, String> response = new HashMap<>();
            return response;
        }
    }

}
