package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        // Allowed gap sizes
        final int MAX_GAP_WITH_BOUNDARY = 100;
        final int MAX_GAP_WITH_OTHERSHIPS = 50;
        final int MAX_GAP_WITH_HARMFUL_OBJECTS = 15;

        if (!gameState.getGameObjects().isEmpty()) {
            // Game Tick Payload
            var gameObjects = gameState.getGameObjects();
            var playerGameObjects = gameState.getPlayerGameObjects();
            var worldObjects = gameState.getWorld();

            // List of objects
            var playerList = gameState.getPlayerGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.PLAYER)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            var foodList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            var gasCloudsList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            var asteroidsList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());

            int currentSize = this.bot.getSize();

            double distanceToBoundary = getDistanceBetween(this.bot, worldObjects);

            // nearest object
            double nearestShips = getDistanceBetween(this.bot, playerList.get(0)) + currentSize;
            double nearestGasCloud = getDistanceBetween(this.bot, gasCloudsList.get(0)) + currentSize;
            double nearestAsteroid = getDistanceBetween(this.bot, asteroidsList.get(0)) + currentSize;

            // Default move
            playerAction.action = PlayerActions.FORWARD;

            if (distanceToBoundary <= MAX_GAP_WITH_BOUNDARY) {
                // move towards center
                playerAction.heading = getHeadingBetween();
            }
            else {
                if (nearestShips <= MAX_GAP_WITH_OTHERSHIPS) {
                    int sizeDiff = playerList.get(0).getSize() - currentSize;

                    if (sizeDiff > 20) {
                        // move to opposite direction
                        playerAction.heading = (-1) * getHeadingBetween(playerList.get(0));
                    }
                    else if (sizeDiff < 20 && sizeDiff >= 0) {
                        // farming if 0 <= sizeDiff < 20
                        playerAction.heading = getHeadingBetween(foodList.get(0));
                    }
                    else {
                        // pursue small ship
                        playerAction.heading = getHeadingBetween(playerList.get(0));
                    }
                }
                else if (nearestGasCloud <= MAX_GAP_WITH_HARMFUL_OBJECTS || nearestAsteroid <= MAX_GAP_WITH_HARMFUL_OBJECTS) {
                    if (nearestGasCloud > nearestAsteroid) {
                        playerAction.heading = (-1) * getHeadingBetween(asteroidsList.get(0));
                    } else {
                        playerAction.heading = (-1) * getHeadingBetween(gasCloudsList.get(0));
                    }
                }
                else {
                    if (getDistanceBetween(foodList.get(0), gasCloudsList.get(0)) > 20 && getDistanceBetween(foodList.get(0), asteroidsList.get(0)) > 20) {
                        playerAction.heading = getHeadingBetween(foodList.get(0));
                    }
                }
            }
        }

        this.playerAction = playerAction;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    /**
     * Get distance relative to boundary
     * @param bot this bot
     * @param world map
     * @return distance from boundary
     * */
    private double getDistanceBetween(GameObject bot, World world) {
        var botPosition = Math.sqrt(bot.getPosition().x * bot.getPosition().x + bot.getPosition().y * bot.getPosition().y);
        return world.getRadius() - botPosition;
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    /**
     * Get degrees to origin
     * @return direction to origin
     * */
    private int getHeadingBetween() {
        var direction = toDegrees(Math.atan2(-bot.getPosition().y,
                -bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }


}
