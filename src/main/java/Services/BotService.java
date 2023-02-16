package Services;
import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;
public class BotService {
    private GameObject bot;

    private PlayerAction playerAction;

    private GameState gameState;

    final int MAX_GAP_WITH_BOUNDARY = 40;

    final int MAX_GAP_WITH_OTHER_SHIPS = 350;

    final int MAX_GAP_WITH_HARMFUL_OBJECTS = 30;

    private static boolean supernovaAvail = false;

    private static int teleporterCount = 0;

    private static boolean firedTeleport = false;

    private boolean aggresiveMode = false;

    private static boolean hasActiveTeleporter = false;

    private static int torpedoHit = 0;

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
        if (!this.gameState.getGameObjects().isEmpty()) {
            System.out.println("\n\n============================");
            int INT_MAX = Integer.MAX_VALUE;
            List<GameObject> gameObjects = this.gameState.getGameObjects();
            List<GameObject> playerGameObjects = this.gameState.getPlayerGameObjects();
            World worldObjects = this.gameState.getWorld();
            List<GameObject> playerList = (List<GameObject>)this.gameState.getPlayerGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.PLAYER)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
            List<GameObject> foodList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.FOOD)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
            List<GameObject> gasCloudsList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.GASCLOUD)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
            List<GameObject> asteroidsList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.ASTEROIDFIELD)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
            List<GameObject> torpedosList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.TORPEDOSALVO)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
            int currentSize = this.bot.getSize();
            double distanceToBoundary = getDistanceBetween(this.bot, worldObjects) - currentSize;
            double nearestShips = (playerList.size() > 1) ? (getDistanceBetween(this.bot, playerList.get(1)) - currentSize - ((GameObject)playerList.get(1)).getSize()) : 2.147483647E9D;
            double nearestGasCloud = (gasCloudsList.size() > 0) ? (getDistanceBetween(this.bot, gasCloudsList.get(0)) - currentSize - ((GameObject)gasCloudsList.get(0)).getSize()) : 2.147483647E9D;
            double nearestAsteroid = (asteroidsList.size() > 0) ? (getDistanceBetween(this.bot, asteroidsList.get(0)) - currentSize - ((GameObject)asteroidsList.get(0)).getSize()) : 2.147483647E9D;
            forward();
            int totalHarmfulObjects = countSurroundHarmfulObjects(nearestGasCloud, nearestAsteroid);
            torpedoDefense(torpedosList);
            System.out.println("Torpedo hit: " + torpedoHit);
            System.out.println("Teleport count: " + teleporterCount);
            boolean nearBoundary = (distanceToBoundary <= 40.0D);
            if (torpedoHit == 0)
                if (nearestShips <= 350.0D) {
                    if (!nearBoundary) {
                        GameObject nearestObstacle;
                        switch (totalHarmfulObjects) {
                            case 0:
                                if (currentSize - ((GameObject)playerList.get(1)).getSize() > 5) {
                                    playerAction = attack();
                                    break;
                                }
                                if (nearestShips > 200.0D) {
                                    farming();
                                    stopAfterburner();
                                    break;
                                }
                                System.out.println("Escaping from other");
                                stopAfterburner();
                                if (nearestShips > 0.5D) {
                                    playerAction.heading = getHeadingBetween(playerList.get(1)) + random.nextInt(4) + 90;
                                    if (nearestShips < 120.0D) {
                                        if (nearestShips < 80.0D) {
                                            System.out.println("Attacking Big Ship");
                                            playerAction.heading = getHeadingBetween(playerList.get(1));
                                            if (this.bot.getSize() >= 30)
                                                playerAction.action = PlayerActions.FIRETORPEDOES;
                                            break;
                                        }
                                        startAfterburner();
                                        break;
                                    }
                                    stopAfterburner();
                                }
                                break;
                            case 1:
                                nearestObstacle = (nearestGasCloud < nearestAsteroid) ? gasCloudsList.get(0) : asteroidsList.get(0);
                                if (currentSize - ((GameObject)playerList.get(1)).getSize() > 3) {
                                    playerAction = attack();
                                    break;
                                }
                                stopAfterburner();
                                if (nearestShips > 150.0D) {
                                    playerAction.heading = getHeadingBetween(nearestObstacle) * -1 + random.nextInt(4) + 6;
                                    break;
                                }
                                System.out.println("Escaping from other + 1 obstacles");
                                if (nearestShips > 0.5D) {
                                    playerAction.heading = (getHeadingBetween(playerList.get(1)) + getHeadingBetween(nearestObstacle)) / -2 % 360 + random.nextInt(4) + 3;
                                    if (nearestShips < 120.0D) {
                                        if (nearestShips < 80.0D) {
                                            System.out.println("Attacking Big Ship");
                                            playerAction.heading = getHeadingBetween(playerList.get(1));
                                            if (this.bot.getSize() >= 30)
                                                playerAction.action = PlayerActions.FIRETORPEDOES;
                                            break;
                                        }
                                        startAfterburner();
                                        break;
                                    }
                                    stopAfterburner();
                                }
                                break;
                            case 2:
                                if (currentSize - ((GameObject)playerList.get(1)).getSize() > 3) {
                                    playerAction = attack();
                                    break;
                                }
                                if (nearestShips < 80.0D) {
                                    System.out.println("Attacking Big Ship");
                                    playerAction.heading = getHeadingBetween(playerList.get(1));
                                    if (this.bot.getSize() >= 30)
                                        playerAction.action = PlayerActions.FIRETORPEDOES;
                                    break;
                                }
                                System.out.println("Escaping from other + 2 obstacles");
                                stopAfterburner();
                                playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween(playerList.get(1))) / -3 + random.nextInt(4) + 3) % 360;
                                break;
                        }
                    } else {
                        GameObject nearestObstacle;
                        switch (totalHarmfulObjects) {
                            case 0:
                                if (currentSize - ((GameObject)playerList.get(1)).getSize() > 3) {
                                    if (distanceToBoundary > 10.0D) {
                                        playerAction = attack();
                                        break;
                                    }
                                    System.out.println("Escaping from boundary, but greater size");
                                    moveToCenter();
                                    stopAfterburner();
                                    break;
                                }
                                if (nearestShips > 150.0D) {
                                    moveToCenter();
                                } else if (nearestShips > 0.5D) {
                                    if (nearestShips < 80.0D) {
                                        System.out.println("Attacking Big Ship");
                                        playerAction.heading = getHeadingBetween(playerList.get(1));
                                        if (this.bot.getSize() >= 30)
                                            playerAction.action = PlayerActions.FIRETORPEDOES;
                                    } else {
                                        System.out.println("Escaping from other + boundary");
                                        playerAction.heading = (getHeadingBetween((GameObject)playerList.get(1)) < 15) ? ((getHeadingBetween(playerList.get(1)) < 0) ? ((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / -2 + 15) : ((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / -2 - 15)) : (((getHeadingBetween(playerList.get(1)) + getHeadingBetween()) / -2 + random.nextInt(4) + 3) % 360);
                                    }
                                }
                                stopAfterburner();
                                break;
                            case 1:
                                nearestObstacle = (nearestGasCloud < nearestAsteroid) ? gasCloudsList.get(0) : asteroidsList.get(0);
                                if (currentSize - ((GameObject)playerList.get(1)).getSize() > 3) {
                                    if (distanceToBoundary > 30.0D) {
                                        playerAction = attack();
                                        break;
                                    }
                                    System.out.println("Escaping from boundary + 1 obstacle, but greater size");
                                    stopAfterburner();
                                    if (Math.abs(getHeadingBetween() - getHeadingBetween(nearestObstacle)) > 30) {
                                        playerAction.heading = (getHeadingBetween() + getHeadingBetween(nearestObstacle)) / 2 + random.nextInt(4) + 90;
                                        break;
                                    }
                                    playerAction.heading += 15;
                                    break;
                                }
                                if (nearestShips > 150.0D) {
                                    System.out.println("Escaping from boundary + 1 obstacle");
                                    if (Math.abs(getHeadingBetween() - getHeadingBetween(nearestObstacle)) > 30) {
                                        playerAction.heading = (getHeadingBetween() + getHeadingBetween(nearestObstacle)) / 2 + random.nextInt(4) + 3;
                                    } else {
                                        playerAction.heading += 15;
                                    }
                                } else if (nearestShips > 0.5D) {
                                    if (nearestShips < 80.0D) {
                                        System.out.println("Attacking Big Ship");
                                        playerAction.heading = getHeadingBetween(playerList.get(1));
                                        if (this.bot.getSize() >= 30)
                                            playerAction.action = PlayerActions.FIRETORPEDOES;
                                    } else {
                                        System.out.println("Escaping from other + boundary + 1 obstacle");
                                        playerAction.heading = (getHeadingBetween(playerList.get(1)) * -1 + getHeadingBetween() + getHeadingBetween(nearestObstacle) + random.nextInt(4) + 3) % 360;
                                    }
                                }
                                stopAfterburner();
                                break;
                            case 2:
                                if (nearestShips < 80.0D) {
                                    System.out.println("Attacking Big Ship");
                                    playerAction.heading = getHeadingBetween(playerList.get(1));
                                    if (this.bot.getSize() >= 30)
                                        playerAction.action = PlayerActions.FIRETORPEDOES;
                                } else {
                                    System.out.println("Escaping from other + boundary + 2 obstacles");
                                    playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween() + getHeadingBetween(playerList.get(0))) / -4 + random.nextInt(4) + 3) % 360;
                                }
                                stopAfterburner();
                                break;
                        }
                    }
                } else {
                    if (!nearBoundary) {
                        switch (totalHarmfulObjects) {
                            case 0:
                                if (nearestGasCloud - getDistanceBetween(this.bot, foodList.get(0)) - currentSize > 15.0D || nearestAsteroid - getDistanceBetween(this.bot, foodList.get(0)) - currentSize > 15.0D) {
                                    farming();
                                    break;
                                }
                                moveToCenter();
                                break;
                            case 1:
                                System.out.println("Escaping from 1 obstacle");
                                playerAction

                                        .heading = (nearestGasCloud > nearestAsteroid) ? (getHeadingBetween(asteroidsList.get(0)) + random.nextInt(4) + 3 + 180) : (getHeadingBetween(gasCloudsList.get(0)) + random.nextInt(4) + 3 + 180);
                                break;
                            case 2:
                                System.out.println("Escaping from 2 obstacles");
                                if (getDistanceBetween(asteroidsList.get(0), gasCloudsList.get(0)) - ((GameObject)asteroidsList.get(0)).getSize() - ((GameObject)gasCloudsList.get(0)).getSize() > (currentSize + 15)) {
                                    playerAction.heading = (getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / 2 + random.nextInt(4) + 3;
                                    break;
                                }
                                playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0))) / -2 + random.nextInt(4) + 3) % 360;
                                break;
                        }
                    } else {
                        switch (totalHarmfulObjects) {
                            case 0:
                                System.out.println("Escaping from boundary");
                                moveToCenter();
                                break;
                            case 1:
                                System.out.println("Escaping from boundary + 1 obstacle");
                                playerAction.heading = (nearestGasCloud > nearestAsteroid) ? ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween() + 360) / 3) : ((getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween() + 360) / 3);
                                break;
                            case 2:
                                System.out.println("Escaping from boundary + 2 obstacle");
                                playerAction.heading = ((getHeadingBetween(asteroidsList.get(0)) + getHeadingBetween(gasCloudsList.get(0)) + getHeadingBetween()) / -3 + random.nextInt(4) + 3) % 360;
                                break;
                        }
                    }
                    stopAfterburner();
                }
            System.out.println("Current Size: " + currentSize);
            System.out.println("Tick: " + this.gameState.world.currentTick);
            System.out.println("============================\n\n");
            if (this.bot.getSize() <= 10)
                stopAfterburner();
        } else if (!this.gameState.getPlayerGameObjects().isEmpty()) {
            var torpedosList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDOSALVO)
                    .sorted(Comparator.comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            torpedoDefense(torpedosList);

            if (torpedoHit == 0){
                playerAction = attack();
            }

            if (this.bot.getSize() <= 10) {
                if (this.bot.effects % 2 != 0) {
                    System.out.println("Stopping Afterburner");
                    this.playerAction.action = PlayerActions.STOPAFTERBURNER;
                }
            }
        }
        this.playerAction = playerAction;
    }

    private int countSurroundHarmfulObjects(double nearestGasCloud, double nearestAsteroid) {
        if (nearestGasCloud <= 30.0D || nearestAsteroid <= 30.0D) {
            if (Math.abs(nearestGasCloud - nearestAsteroid) > 40.0D)
                return 1;
            return 2;
        }
        return 0;
    }

    private void farming() {
        System.out.println("Farming");
        List<GameObject> foodList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.FOOD)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
        List<GameObject> supFoodList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.SUPERFOOD)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
        List<GameObject> supernovaList = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.SUPERNOVAPICKUP)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
        double nearestFood = getDistanceBetween(this.bot, foodList.get(0)) - this.bot.getSize();
        double nearestSupFood = getDistanceBetween(this.bot, supFoodList.get(0)) - this.bot.getSize();
        double distanceToSupernova = 0.0D;
        if (supernovaList.size() != 0) {
            distanceToSupernova = getDistanceBetween(this.bot, supernovaList.get(0)) - this.bot.getSize();
            if (distanceToSupernova < 50.0D) {
                if (distanceToSupernova <= 0.0D) {
                    supernovaAvail = true;
                    System.out.println("PICKED UP SUPERNOVA AAAAAAAAAA-------------");
                } else {
                    this.playerAction.heading = getHeadingBetween(supernovaList.get(0));
                }
            } else {
                this.playerAction.heading = (nearestSupFood <= nearestFood) ? getHeadingBetween(supFoodList.get(0)) : getHeadingBetween(foodList.get(0));
            }
        } else {
            this.playerAction.heading = (nearestSupFood <= nearestFood) ? getHeadingBetween(supFoodList.get(0)) : getHeadingBetween(foodList.get(0));
        }
    }

    private void moveToCenter() {
        System.out.println("Moving to Center");
        this.playerAction.heading = getHeadingBetween();
    }

    private PlayerAction attack() {
        PlayerAction playerAction = new PlayerAction();
        List<GameObject> playerList = (List<GameObject>)this.gameState.getPlayerGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.PLAYER)).sorted(Comparator.comparing(item -> Double.valueOf(getDistanceBetween(this.bot, item)))).collect(Collectors.toList());
        List<GameObject> selfTeleporter = (List<GameObject>)this.gameState.getGameObjects().stream().filter(item -> (item.getGameObjectType() == ObjectTypes.TELEPORTER)).sorted(Comparator.comparing(item -> Boolean.valueOf((getHeadingBetween(item) < 10)))).collect(Collectors.toList());
        double nearestShips = getDistanceBetween(this.bot, playerList.get(1)) - this.bot.getSize() - ((GameObject)playerList.get(1)).getSize();
        playerAction.heading = getHeadingBetween(playerList.get(1));
        playerAction.action = PlayerActions.FORWARD;
        if (playerList.size() <= 2)
            this.aggresiveMode = true;
        if (this.aggresiveMode) {
            System.out.println("Aggressive Attacking");
            if (firedTeleport) {
                if (selfTeleporter.size() != 0 && getDistanceBetween(selfTeleporter.get(0), playerList.get(1)) - playerList.get(0).getSize() < 10) {
                    System.out.println("Teleporting");
                    playerAction.action = PlayerActions.TELEPORT;
                    firedTeleport = false;
                    hasActiveTeleporter = false;
                    System.out.println("Teleport: " + firedTeleport);
                    System.out.println("Active teleporter: " + hasActiveTeleporter);
                } else {
                    playerAction.heading = getHeadingBetween(playerList.get(1));
                }
                stopAfterburner();
            } else if (this.bot.getSize() > ((GameObject)playerList.get(1)).getSize() + 25 && !hasActiveTeleporter) {
                playerAction.action = PlayerActions.FIRETELEPORT;
                teleporterCount--;
                firedTeleport = true;
                hasActiveTeleporter = true;
                System.out.println("Teleport: " + firedTeleport);
                System.out.println("Active teleporter: " + hasActiveTeleporter);
                stopAfterburner();
            } else if (hasActiveTeleporter) {
                playerAction.heading = getHeadingBetween(playerList.get(1));
                stopAfterburner();
            } else if (supernovaAvail) {
                if (nearestShips > 150.0D) {
                    playerAction.action = PlayerActions.FIRESUPERNOVA;
                    supernovaAvail = false;
                    System.out.println("Fired Supernova URAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                } else if (this.bot.getSize() >= 30) {
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                }
                stopAfterburner();
            } else if (nearestShips < 150.0D && nearestShips > 50.0D) {
                if (this.bot.getSize() >= 30)
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                stopAfterburner();
            } else if (this.bot.getSize() > ((GameObject)playerList.get(1)).getSize() + 20) {
                startAfterburner();
            }
        } else {
            System.out.println("Normal Attacking");
            if (supernovaAvail) {
                if (nearestShips > 150.0D) {
                    playerAction.action = PlayerActions.FIRESUPERNOVA;
                    supernovaAvail = false;
                    System.out.println("Fired Supernova URAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                } else if (this.bot.getSize() >= 30) {
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                }
            } else if (nearestShips < 150.0D && nearestShips > 50.0D) {
                if (this.bot.getSize() >= 30)
                    playerAction.action = PlayerActions.FIRETORPEDOES;
            } else if (this.bot.getSize() > ((GameObject)playerList.get(1)).getSize() + 20) {
                startAfterburner();
            }
        }
        return playerAction;
    }

    private void fireTorpedoes() {
        System.out.println("Firing torpedoes");
        if (this.bot.getSize() >= 30)
            this.playerAction.action = PlayerActions.FIRETORPEDOES;
    }

    private void fireTeleporter() {
        System.out.println("Firing teleporter->>>>>>>>>>>>>>>>>>>>>>>>>>");
        this.playerAction.action = PlayerActions.FIRETELEPORT;
        teleporterCount--;
        firedTeleport = true;
        hasActiveTeleporter = true;
    }

    private void teleport() {
        System.out.println("Teleporting");
        this.playerAction.action = PlayerActions.TELEPORT;
        firedTeleport = false;
        hasActiveTeleporter = false;
    }

    private void stopAfterburner() {
        if (this.bot.effects.intValue() % 2 != 0) {
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
        Optional<GameObject> optionalBot = this.gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(this.bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        int triangleX = Math.abs((object1.getPosition()).x - (object2.getPosition()).x);
        int triangleY = Math.abs((object1.getPosition()).y - (object2.getPosition()).y);
        return Math.sqrt((triangleX * triangleX + triangleY * triangleY));
    }

    private boolean isTorpedoInRange(GameObject torpedo) {
        double rangeRadius = 1.5D * this.bot.size.intValue();
        if ((torpedo.getPosition()).x < this.bot.position.x + rangeRadius && (torpedo.getPosition()).x > this.bot.position.x - rangeRadius)
            return ((torpedo.getPosition()).y < this.bot.position.y + rangeRadius && (torpedo.getPosition()).y > this.bot.position.y - rangeRadius);
        return false;
    }

    private boolean isTorpedoHit(GameObject torpedo) {
        System.out.println("Got hit!");
        double dist = getDistanceBetween(this.bot, torpedo);
        double rangeDist = Math.sqrt(dist * dist + (this.bot.getSize() * this.bot.getSize()));
        int range = toDegrees(Math.acos(this.bot.getSize() / rangeDist));
        int torpedoRange = Math.abs(torpedo.currentHeading.intValue() - getHeadingBetween(torpedo, this.bot));
        return (torpedoRange <= range);
    }

    private void torpedoDefense(List<GameObject> torpList) {
        boolean multipleHeading = false;
        torpedoHit = 0;
        double firstHeading = -1.0D;
        int idxFirstHit = -1;
        if (torpList.size() != 0 && getDistanceBetween(torpList.get(0), this.bot) < 60.0D) {
            torpedoHit++;
            if (this.bot.effects.intValue() < 16) {
                System.out.println("Activate shield!!");
                this.playerAction.action = PlayerActions.ACTIVATESHIELD;
            }
        } else {
            for (int i = 0; i < torpList.size(); i++) {
                if (isTorpedoInRange(torpList.get(i)) && isTorpedoHit(torpList.get(i))) {
                    torpedoHit++;
                    if (firstHeading == -1.0D) {
                        firstHeading = ((GameObject)torpList.get(i)).currentHeading.intValue();
                        idxFirstHit = i;
                    } else {
                        double headingDifference = Math.abs(firstHeading - ((GameObject)torpList.get(i)).currentHeading.intValue());
                        if (headingDifference > 15.0D) {
                            multipleHeading = true;
                            if (this.bot.effects.intValue() < 16) {
                                System.out.println("Activate shield!!");
                                this.playerAction.action = PlayerActions.ACTIVATESHIELD;
                            }
                        }
                    }
                }
            }
            if (torpedoHit > 2) {
                if (this.bot.effects.intValue() < 16) {
                    System.out.println("Activate shield!!");
                    this.playerAction.action = PlayerActions.ACTIVATESHIELD;
                }
            } else if (torpedoHit > 0 && this.bot.getSize() > 80) {
                if (this.bot.effects.intValue() < 16) {
                    System.out.println("Activate shield!!");
                    this.playerAction.action = PlayerActions.ACTIVATESHIELD;
                }
            } else if (!multipleHeading && torpedoHit > 0) {
                this.playerAction.heading = getHeadingBetween(torpList.get(idxFirstHit), this.bot) + 90;
            }
        }
    }

    private double getDistanceBetween(GameObject bot, World world) {
        double botPosition = Math.sqrt(((bot.getPosition()).x * (bot.getPosition()).x + (bot.getPosition()).y * (bot.getPosition()).y));
        return world.getRadius().intValue() - botPosition;
    }

    private int getHeadingBetween(GameObject object1, GameObject object2) {
        int direction = toDegrees(Math.atan2(((object2.getPosition()).y - (object1.getPosition()).y), (
                (object2.getPosition()).x - (object1.getPosition()).x)));
        return (direction + 360) % 360;
    }

    private int getHeadingBetween(GameObject otherObject) {
        int direction = toDegrees(Math.atan2(((otherObject.getPosition()).y - (this.bot.getPosition()).y), (
                (otherObject.getPosition()).x - (this.bot.getPosition()).x)));
        return (direction + 360) % 360;
    }

    private int getHeadingBetween() {
        int direction = toDegrees(Math.atan2(-(this.bot.getPosition()).y,
                -(this.bot.getPosition()).x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int)(v * 57.29577951308232D);
    }
}
