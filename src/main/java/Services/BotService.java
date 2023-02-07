package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    // Allowed gap sizes
    final int MAX_GAP_WITH_BOUNDARY = 25;
    final int MAX_GAP_WITH_OTHER_SHIPS = 50;
    final int MAX_GAP_WITH_HARMFUL_OBJECTS = 5;
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

            double distanceToBoundary = getDistanceBetween(this.bot, worldObjects) - currentSize;

            // nearest object
            // TODO: add more objects from ObjectTypes
            double nearestShips = getDistanceBetween(this.bot, playerList.get(1)) - currentSize - playerList.get(1).getSize();
            double nearestGasCloud = getDistanceBetween(this.bot, gasCloudsList.get(0)) - currentSize - gasCloudsList.get(0).getSize();
            double nearestAsteroid = getDistanceBetween(this.bot, asteroidsList.get(0)) - currentSize - asteroidsList.get(0).getSize();

            // Default move
            playerAction.action = PlayerActions.FORWARD;

            System.out.println("Ships: " + nearestShips);
            System.out.println("GasClouds: " + nearestGasCloud);
            int totalHarmfulObjects = countSurroundHarmfulObjects(nearestGasCloud, nearestAsteroid);

            boolean nearBoundary = distanceToBoundary <= MAX_GAP_WITH_BOUNDARY;
            if (nearestShips <= MAX_GAP_WITH_OTHER_SHIPS) {
                // Check
                if (!nearBoundary) { // if not near boundary
                    switch (totalHarmfulObjects) {
                        case 0:
                            playerAction.heading = currentSize - playerList.get(1).getSize() > 25
                                    ? getHeadingBetween(playerList.get(1))
                                    : getHeadingBetween(playerList.get(1)) + 150;
                            if (currentSize - playerList.get(1).getSize() > 50 && nearestShips <= 5) {
                                playerAction.action = PlayerActions.START_AFTERBURNER;
                            }
                            else {
                                playerAction.action = PlayerActions.STOP_AFTERBURNER;
                            }
                            break;
                        case 1:
                            playerAction.heading = nearestGasCloud > nearestAsteroid
                                    ? (Math.abs(getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(playerList.get(1))) / -2) + 15
                                    : (Math.abs(getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween(playerList.get(1))) / -2) + 15;
                            break;
                        case 2:
                            playerAction.heading = (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / 2 + getHeadingBetween(playerList.get(1)) * (-1) + 6;
                            break;
                        default:
                            playerAction.heading = getHeadingBetween(foodList.get(0));
                    }
                }
                else { // near boundary
                    switch (totalHarmfulObjects) {
                        case 0:
                            playerAction.heading = currentSize - playerList.get(1).getSize() > 25
                                    ? getHeadingBetween(playerList.get(1))
                                    : getHeadingBetween(playerList.get(1)) + 150;
                            break;
                        case 1:
                            playerAction.heading = nearestGasCloud > nearestAsteroid
                                    ? (Math.abs(getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / -3) + 15
                                    : (Math.abs(getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / -3) + 15;
                            break;
                        case 2:
                            playerAction.heading = (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween()) / 3 + getHeadingBetween(playerList.get(1)) * (-1) + 6;
                            break;
                        default:
                            playerAction.heading = getHeadingBetween(foodList.get(0));
                    }
                }
//                playerAction.action = PlayerActions.STOP;
            }
            // if no ships around
            else {
                if (!nearBoundary) {
                    switch (totalHarmfulObjects) {
                        case 0:
                            // farming
                            if (getDistanceBetween(this.bot, foodList.get(0)) - currentSize < nearestGasCloud + 5 || getDistanceBetween(this.bot, foodList.get(0)) - currentSize < nearestAsteroid + 5){
//                                playerAction.action = PlayerActions.STOP;
                                playerAction.heading = getHeadingBetween(foodList.get(0)) + 12;
                            } else {
                                playerAction.heading = getHeadingBetween(foodList.get(0));
                            }
                            break;
                        case 1:
                            playerAction.heading = nearestGasCloud > nearestAsteroid
                                    ? getHeadingBetween(asteroidsList.get(0)) * (-1) + 6
                                    : getHeadingBetween(gasCloudsList.get(0)) * (-1) + 6;
                            break;
                        case 2:
                            playerAction.heading = (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / -2 + 6;
                            break;
                    }
                }
                else {
                    playerAction.heading = getHeadingBetween();
                }
            }


        }

        this.playerAction = playerAction;
    }

    private int countSurroundHarmfulObjects(double nearestGasCloud, double nearestAsteroid) {
        if (nearestGasCloud <= MAX_GAP_WITH_HARMFUL_OBJECTS || nearestAsteroid <= MAX_GAP_WITH_HARMFUL_OBJECTS) {
            if (Math.abs(nearestGasCloud - nearestAsteroid) > 20) {
                return 1;
            }
            else {
                return 2;
            }
        }
        else {
            return 0;
        }
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
