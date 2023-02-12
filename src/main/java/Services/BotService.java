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
    final int MAX_GAP_WITH_BOUNDARY = 40;
    final int MAX_GAP_WITH_OTHER_SHIPS = 350;
    final int MAX_GAP_WITH_HARMFUL_OBJECTS = 10;

    private static boolean supernovaAvail = false;
    private static int teleporterCount = 0;
    private static boolean firedTeleport = false;
    private boolean aggresiveMode = false;
    private static boolean hasActiveTeleporter = false;

//    private boolean used_afterburn = false;

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
            Random random = new Random();
        if (!gameState.getGameObjects().isEmpty()) {
            if (gameState.world.currentTick % 100 == 0 && gameState.world.currentTick != 0) {
                teleporterCount++;
            }
            System.out.println("\n\n============================");
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
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            var asteroidsList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.ASTEROIDFIELD)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());


            int currentSize = this.bot.getSize();

            double distanceToBoundary = getDistanceBetween(this.bot, worldObjects) - currentSize;

            // nearest object
            // TODO: add more objects from ObjectTypes
            double nearestShips = getDistanceBetween(this.bot, playerList.get(1)) - currentSize - playerList.get(1).getSize();
            double nearestGasCloud = getDistanceBetween(this.bot, gasCloudsList.get(0)) - currentSize - gasCloudsList.get(0).getSize();
            double nearestAsteroid = getDistanceBetween(this.bot, asteroidsList.get(0)) - currentSize - asteroidsList.get(0).getSize();
            forward();


            int totalHarmfulObjects = countSurroundHarmfulObjects(nearestGasCloud, nearestAsteroid);

            boolean nearBoundary = distanceToBoundary <= MAX_GAP_WITH_BOUNDARY;
            if (nearestShips <= MAX_GAP_WITH_OTHER_SHIPS) {
                // near other ships
                if (!nearBoundary) { // if not near boundary
                    switch (totalHarmfulObjects) {
                        case 0: // if no gas cloud & asteroid
                            if (currentSize - playerList.get(1).getSize() > 3) {
                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                attack();
                            } else {
                                if (nearestShips > 150) {
                                    farming();
                                    stopAfterburner();
                                } else {
                                    System.out.println("Escaping from other");
                                    stopAfterburner();
                                    if (nearestShips > 0.5) {
                                        playerAction.heading = (-1) * (180 - getHeadingBetween(playerList.get(1))) + random.nextInt(4) + 10;
                                        if (nearestShips < 60) {
                                            if (nearestShips < 40) {
                                                System.out.println("Attacking Big Ship");
                                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                                fireTorpedoes();
                                            } else {
                                                startAfterburner();
                                            }
                                        } else {
                                            stopAfterburner();
                                        }
                                    }
                                }
                            }
                            break;
                        case 1: // if there are 1 (whether gas cloud or asteroid)
                            GameObject nearestObstacle = nearestGasCloud < nearestAsteroid ? gasCloudsList.get(0) : asteroidsList.get(0);
                            // default: keep pursuing if size still greater
                            // TODO : handling for gas clouds
                            if (currentSize - playerList.get(1).getSize() > 3) {
                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                attack();
                            } else {
                                // if size smaller
                                stopAfterburner();
                                if (nearestShips > 150) { // if other ships not too near -> avoid obstacle
                                    playerAction.heading = getHeadingBetween(nearestObstacle) * (-1) + random.nextInt(4) + 6;
                                } else {
                                    System.out.println("Escaping from other + 1 obstacles");
                                    if (nearestShips > 0.5) { // if other ships near -> avoid player & obstacle
                                        playerAction.heading = ((getHeadingBetween(playerList.get(1)) + getHeadingBetween(nearestObstacle)) / (-2)) % 360 + random.nextInt(4) + 3;
                                        if (nearestShips < 60) {
                                            if (nearestShips < 40) {
                                                System.out.println("Attacking Big Ship");
                                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                                fireTorpedoes();
                                            } else {
                                                startAfterburner();
                                            }
                                        } else {
                                            stopAfterburner();
                                        }
                                    }
                                }
                            }
                            break;
                        case 2: // if there are gas cloud & asteroid + other ship approach
                            if (currentSize - playerList.get(1).getSize() > 3) {
                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                attack();
                            } else {
                                if (nearestShips < 40) {
                                    System.out.println("Attacking Big Ship");
                                    playerAction.heading = getHeadingBetween(playerList.get(1));
                                    fireTorpedoes();
                                } else {
                                    System.out.println("Escaping from other + 2 obstacles");
                                    stopAfterburner();
                                    playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween(playerList.get(1))) / (-3) + random.nextInt(4) + 3) % 360;
                                }
                            }
                            break;
                    }
                    // TODO : conditional if approach by 2 or more ships
                }
                else { // near boundary + near other ship
                    switch (totalHarmfulObjects) {
                        case 0: // if no gas clouds & asteroid but near boundary & other ship
                            if (currentSize - playerList.get(1).getSize() > 3) {
                                // attack if still safe gap from boundary
                                if (distanceToBoundary > 10) {
                                    playerAction.heading = getHeadingBetween(playerList.get(1));
                                    attack();
                                } else { // move to center if gap < 10
                                    System.out.println("Escaping from boundary, but greater size");
                                    moveToCenter();
                                    stopAfterburner();
                                }
                            } else { // if smaller
                                if (nearestShips > 150) { // if far from other ship + near boundary -> move to center
                                    moveToCenter();
                                } else { // if other ship approach
                                    if (nearestShips > 0.5) {
                                        if (nearestShips < 40) {
                                            System.out.println("Attacking Big Ship");
                                            playerAction.heading = getHeadingBetween(playerList.get(1));
                                            fireTorpedoes();
                                        } else {
                                            System.out.println("Escaping from other + boundary");
                                            playerAction.heading = getHeadingBetween(playerList.get(1)) < 15
                                                    ? getHeadingBetween(playerList.get(1)) < 0
                                                    ? ((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / (-2) + 15)
                                                    : ((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / (-2) - 15)
                                                    : ((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / (-2) + random.nextInt(4) + 3) % 360;
                                        }
                                    }
                                }
                                stopAfterburner();
                            }
                            break;
                        case 1: // if there are 1 (whether gas cloud or asteroid) + near boundary & other ship
                            GameObject nearestObstacle = nearestGasCloud < nearestAsteroid ? gasCloudsList.get(0) : asteroidsList.get(0);
                            if (currentSize - playerList.get(1).getSize() > 3) {
                                if (distanceToBoundary > 10) {
                                    playerAction.heading = getHeadingBetween(playerList.get(1));
                                    attack();
                                } else {
                                    System.out.println("Escaping from boundary + 1 obstacle, but greater size");
                                    stopAfterburner();
                                    if (Math.abs(getHeadingBetween() - getHeadingBetween(nearestObstacle)) > 30) {
                                        playerAction.heading = (getHeadingBetween() + getHeadingBetween(nearestObstacle)) / 2 + random.nextInt(4) + 3;
                                    } else {
                                        playerAction.heading += 15;
                                    }
                                }
                            } else {
                                if (nearestShips > 150) {
                                    System.out.println("Escaping from boundary + 1 obstacle");
                                    if (Math.abs(getHeadingBetween() - getHeadingBetween(nearestObstacle)) > 30) {
                                        playerAction.heading = (getHeadingBetween() + getHeadingBetween(nearestObstacle)) / 2 + random.nextInt(4) + 3;
                                    } else {
                                        playerAction.heading += 15;
                                    }
                                } else {
                                    if (nearestShips > 0.5) {
                                        if (nearestShips < 40) {
                                            System.out.println("Attacking Big Ship");
                                            playerAction.heading = getHeadingBetween(playerList.get(1));
                                            fireTorpedoes();
                                        } else {
                                            System.out.println("Escaping from other + boundary + 1 obstacle");
                                            playerAction.heading = (getHeadingBetween(playerList.get(1)) * (-1) + getHeadingBetween() + getHeadingBetween(nearestObstacle) + random.nextInt(4) + 3) % 360;
                                        }
                                    }
                                }
                                stopAfterburner();
                            }
                            break;
                        case 2: // if there are gas cloud & asteroid + other ship + boundary
                            if (nearestShips < 40) {
                                System.out.println("Attacking Big Ship");
                                playerAction.heading = getHeadingBetween(playerList.get(1));
                                fireTorpedoes();
                            } else {
                                System.out.println("Escaping from other + boundary + 2 obstacles");
                                playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween() + getHeadingBetween(playerList.get(0))) / (-4) + random.nextInt(4) + 3) % 360;
                            }
                            stopAfterburner();
                            break;
                    }
                }
            }
            // if no ships around
            else {
                if (!nearBoundary) {
                    switch (totalHarmfulObjects) {
                        case 0:
                            if (nearestGasCloud - getDistanceBetween(this.bot, foodList.get(0)) - currentSize > MAX_GAP_WITH_HARMFUL_OBJECTS / 2 || nearestAsteroid - getDistanceBetween(this.bot, foodList.get(0)) - currentSize > MAX_GAP_WITH_HARMFUL_OBJECTS / 2){
                                farming();
                            } else {
                                moveToCenter();
                            }
                            break;
                        case 1:
                            System.out.println("Escaping from 1 obstacle");
                            playerAction.heading = nearestGasCloud > nearestAsteroid
                                    ? (getHeadingBetween(asteroidsList.get(0)) + random.nextInt(4) + 3) + 180
                                    : (getHeadingBetween(gasCloudsList.get(0)) + random.nextInt(4) + 3) + 180;
                            break;
                        case 2:
                            System.out.println("Escaping from 2 obstacles");
                            if (getDistanceBetween(asteroidsList.get(0), gasCloudsList.get(0)) - asteroidsList.get(0).getSize() - gasCloudsList.get(0).getSize() > currentSize + 15) {
                                playerAction.heading = (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / 2 + random.nextInt(4) + 3;
                            } else {
                                playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / -2 + random.nextInt(4) + 3) % 360;
                            }
                            break;
                    }
                }
                else {
                    switch (totalHarmfulObjects) {
                        case 0:
                            System.out.println("Escaping from boundary");
                            // move to center
                            moveToCenter();
                            break;
                        case 1:
                            System.out.println("Escaping from boundary + 1 obstacle");
                            playerAction.heading = nearestGasCloud > nearestAsteroid
                                    ? (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween() + 360) / 3
                                    : (getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween() + 360) / 3;
                            break;
                        case 2:
                            System.out.println("Escaping from boundary + 2 obstacle");
                            playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween()) / (-3) + random.nextInt(4) + 3) % 360;
                            break;
                    }
                }
                stopAfterburner();
            }

//            System.out.println("Ships-1: " + nearestShips);
//            System.out.println("Ships-2: " + nearestShips2);
//            System.out.println("GasClouds: " + nearestGasCloud);
//            System.out.println("Current Size: " + currentSize);
//            System.out.println("Opponent Size: " + playerList.get(1).getSize());
//            System.out.println("Distance to boundary: " + distanceToBoundary);
//            System.out.println("Speed: " + this.bot.getSpeed());
//            System.out.println("Heading: " + this.bot.currentHeading % 360);
            System.out.println("Tick: " + gameState.world.currentTick);
            System.out.println("============================\n\n");

            if (this.bot.getSize() <= 10) {
                stopAfterburner();
            }
        }

        this.playerAction = playerAction;
    }

    private int countSurroundHarmfulObjects(double nearestGasCloud, double nearestAsteroid) {
        if (nearestGasCloud <= MAX_GAP_WITH_HARMFUL_OBJECTS || nearestAsteroid <= MAX_GAP_WITH_HARMFUL_OBJECTS) {
            if (Math.abs(nearestGasCloud - nearestAsteroid) > 40) {
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

    private void farming() {
        System.out.println("Farming");
        var foodList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        var supFoodList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERFOOD)
                .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        var supernovaList = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.SUPERNOVAPICKUP)
                .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        double nearestFood = getDistanceBetween(this.bot, foodList.get(0)) - this.bot.getSize();
        double nearestSupFood = getDistanceBetween(this.bot, supFoodList.get(0)) - this.bot.getSize();
        double distanceToSupernova = 0;

        if (supernovaList.size() != 0) {
            distanceToSupernova = getDistanceBetween(this.bot, supernovaList.get(0)) - this.bot.getSize();
            if (distanceToSupernova < 50) {
                if (distanceToSupernova <= 0) {
                    supernovaAvail = true;
                    System.out.println("PICKED UP SUPERNOVA AAAAAAAAAA-------------");
                } else {
                    this.playerAction.heading = getHeadingBetween(supernovaList.get(0));
                }
            } else {
                this.playerAction.heading = nearestSupFood <= nearestFood
                        ? getHeadingBetween(supFoodList.get(0))
                        : getHeadingBetween(foodList.get(0));
            }
        }

    }

    private void moveToCenter() {
        System.out.println("Moving to Center");
        this.playerAction.heading = getHeadingBetween();
    }

    private void attack() {
        var playerList = gameState.getPlayerGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.PLAYER)
                .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                .collect(Collectors.toList());

        var selfTeleporter = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
                .sorted(Comparator.comparing(item -> getHeadingBetween(item) < 10))
                .collect(Collectors.toList());

        double nearestShips = getDistanceBetween(this.bot, playerList.get(1)) - this.bot.getSize() - playerList.get(1).getSize();

        if (playerList.size() <= 2) {
            this.aggresiveMode = true;
        }

        if (this.aggresiveMode) {
            System.out.println("Aggressive Attacking");
            if (this.firedTeleport) {
                if (getDistanceBetween(selfTeleporter.get(0), playerList.get(1)) < 5) {
                    teleport();
                } else {
                    this.playerAction.heading = getHeadingBetween(playerList.get(1));
                }
                stopAfterburner();
            } else {
                System.out.println("Active teleporter: " + hasActiveTeleporter);
                if (this.bot.getSize() > playerList.get(1).getSize() + 25 && teleporterCount > 0 && !hasActiveTeleporter) {
                    fireTeleporter();
                    stopAfterburner();
                }
                else if (hasActiveTeleporter) {
                    this.playerAction.heading = getHeadingBetween(playerList.get(1));
                    stopAfterburner();
                }
                else {
                    if (this.supernovaAvail) {
                        if (nearestShips > 150) {
                            this.playerAction.action = PlayerActions.FIRESUPERNOVA;
                            this.supernovaAvail = false;
                            System.out.println("Fired Supernova URAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        } else {
                            fireTorpedoes();
                        }
                        stopAfterburner();
                    } else {
                        if (nearestShips < 150) {
                            fireTorpedoes();
                        } else {
                            if (this.bot.getSize() > playerList.get(1).getSize() + 20) {
                                startAfterburner();
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Normal Attacking");
            if (this.supernovaAvail) {
                if (nearestShips > 150) {
                    this.playerAction.action = PlayerActions.FIRESUPERNOVA;
                    this.supernovaAvail = false;
                    System.out.println("Fired Supernova URAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                } else {
                    fireTorpedoes();
                }
            } else {
                if (nearestShips < 150) {
                    fireTorpedoes();
                } else {
                    if (this.bot.getSize() > playerList.get(1).getSize() + 20) {
                        startAfterburner();
                    }
                }
            }
        }
    }

    private void fireTorpedoes() {
        System.out.println("Firing torpedoes");
        if (this.bot.getSize() >= 15) {
            this.playerAction.action = PlayerActions.FIRETORPEDOES;
        }
    }

    private void fireTeleporter() {
        System.out.println("Firing teleporter");
        this.playerAction.action = PlayerActions.FIRETELEPORT;
        this.teleporterCount--;
        this.firedTeleport = true;
        this.hasActiveTeleporter = true;
    }

    private void teleport() {
        System.out.println("Teleporting");
        this.playerAction.action = PlayerActions.TELEPORT;
        this.firedTeleport = false;
        this.hasActiveTeleporter = false;
    }

    private void stopAfterburner() {
        if (this.bot.effects % 2 != 0) {
            System.out.println("Stopping Afterburner");
            this.playerAction.action = PlayerActions.STOPAFTERBURNER;
        }
    }

    private void startAfterburner() {
        System.out.println("Start Afterburner");
        this.playerAction.action = PlayerActions.STARTAFTERBURNER;
    }

    private void forward() {
        System.out.println("Moving forward");
        this.playerAction.action = PlayerActions.FORWARD;
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
