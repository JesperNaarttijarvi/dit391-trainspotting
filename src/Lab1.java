import TSim.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Lab1 {
  private HashMap<String, Semaphore> semaphores = new HashMap<>();


  public Lab1(Integer TRAIN1_SPEED, Integer TRAIN2_SPEED) {
      initateSemaphores();
    try {
      Train train1 = new Train(1, TRAIN1_SPEED, semaphores);
      Train train2 = new Train(2, TRAIN2_SPEED, semaphores);
      train1.start();
      train2.start();
      train1.join();
      train2.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Initates all semaphores.
   * H Refers to home
   * CP refers to crosspoint
   * N,E,S,W North, East, South, West
   * M Refers to middle
   */
  private void initateSemaphores() {
    addSemaphore("H.N.S");
    addSemaphore("CP.N.M");
    addSemaphore("CP.M.E");
    addSemaphore("CP.M.M");
    addSemaphore("CP.M.W");
    addSemaphore("H.S.S");
  }

  private void addSemaphore(String key) {
    semaphores.put(key, new Semaphore(1, true));
  }
}

/**
 *
 */
class Train extends Thread {
  private final int TRAIN_ID;
  private int TRAIN_SPEED;
  private HashMap<String, Semaphore> semaphores;
  private TSimInterface tsi;
  private String lastSensor;

  /**
   * @param TRAIN_ID    Should be serial and unique
   * @param TRAIN_SPEED Interval between [-20, 20]
   */
  Train(int TRAIN_ID, int TRAIN_SPEED, HashMap<String, Semaphore> semaphores) {
    this.TRAIN_ID = TRAIN_ID;
    this.TRAIN_SPEED = TRAIN_SPEED;
    this.semaphores = semaphores;
    this.tsi = TSimInterface.getInstance();
  }

  @Override
  public void run() {
    try {
      tsi.setSpeed(TRAIN_ID, this.TRAIN_SPEED);
      while (true) {

        SensorEvent sensorEvent = tsi.getSensor(TRAIN_ID);
        if (sensorEvent.getStatus() == SensorEvent.ACTIVE) {
          int sensor_x = sensorEvent.getXpos();
          int sensor_y = sensorEvent.getYpos();
          String sensorPos = getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */
          onSensorEvent(sensorPos);
        }
      }
    } catch (CommandException | InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Makes actions based on the trains direction and available semaphores
   * @param sensorPos Position of Sensor in String
   */
  private void onSensorEvent(String sensorPos) throws CommandException, InterruptedException {
          switch (sensorPos) {
      case "Unkown sensor":
        break;

      case "H.N.N.1":
        if (lastSensor == null) {
          lastSensor = sensorPos;
        }
        if (lastSensor.equals("H.N.N.2")) {
          stopAndTurn();
        }
        lastSensor = sensorPos;
        break;

      case "H.N.N.2":
        if(lastSensor.equals("H.N.N.3")) {
            tryRelease("CP.N.M");
        } else if (lastSensor.equals("H.N.N.1")){
            tryAcquire("CP.N.M");
        }
        lastSensor = sensorPos;
        break;

      case "H.N.N.3":
        if(lastSensor.equals("H.N.N.2")) {
            tryRelease("CP.N.M");
            tryAcquire("CP.M.E");
            changeSwitch(SwitchLocation.NORTH, "RIGHT");
        } else if (lastSensor.equals("CP.E.E")){ // since we know it must be cp. or h.n...
            tryRelease("CP.M.E");
            tryAcquire("CP.N.M");
        }
        lastSensor = sensorPos;
        break;

      case "H.N.S.1":
          if (lastSensor.equals("H.N.S.2")) {
              stopAndTurn();
          }
          lastSensor = sensorPos;
        break;

      case "H.N.S.2":
          if(lastSensor.equals("H.N.S.3")) {
              tryRelease("CP.N.M");
          } else if (lastSensor.equals("H.S.N.1")){
              tryAcquire("CP.N.M");
          }

        lastSensor = sensorPos;
        break;

      case "H.N.S.3":
          if(lastSensor.equals("H.N.S.2")) {
              tryRelease("CP.N.M");
              tryAcquire("CP.M.E");
              changeSwitch(SwitchLocation.NORTH, "LEFT");
          } else if (lastSensor.equals("CP.E.E")){ // catches both CP... sensors
              tryRelease("CP.M.E");
              tryAcquire("CP.N.M");
          }
        lastSensor = sensorPos;
        break;

        case "CP.E.E":
            if (lastSensor.substring(0,3).equals("H.N")){ // catches both HN... sensors
                tryRelease("H.N.S");
                Boolean available = acquireIfEmpty("CP.M.M");
                if (!available) {
                    changeSwitch(SwitchLocation.EAST, "LEFT");
                } else {
                    changeSwitch(SwitchLocation.EAST, "RIGHT");
                }
            } else if  (lastSensor.substring(0,3).equals("CP.")){
                tryRelease("CP.M.M");
                Boolean available = acquireIfEmpty("H.N.S");
                if (!available) {
                    changeSwitch(SwitchLocation.NORTH, "RIGHT");
                } else {
                    changeSwitch(SwitchLocation.NORTH, "LEFT");
                }
            }
            lastSensor = sensorPos;
            break;

      case "CP.N.E":
        if(lastSensor.equals("CP.E.E")) {
            tryRelease("CP.M.E");
        } else if (lastSensor.equals("CP.N.W")) {
            tryAcquire("CP.M.E");
            changeSwitch(SwitchLocation.EAST, "RIGHT");
        }
        lastSensor = sensorPos;
        break;

        case "CP.S.E":
            if(lastSensor.equals("CP.E.E")) {
                tryRelease("CP.M.E");
            } else if (lastSensor.equals("CP.S.W")) {
                tryAcquire("CP.M.E");
                changeSwitch(SwitchLocation.EAST, "LEFT");
            }
            lastSensor = sensorPos;
            break;

      case "CP.N.W":
          if(lastSensor.equals("CP.W.W")) {
              tryRelease("CP.M.W");
          } else if (lastSensor.equals("CP.N.E")) {
              tryAcquire("CP.M.W");
              changeSwitch(SwitchLocation.WEST, "LEFT");
          }
        lastSensor = sensorPos;
        break;

      case "CP.S.W":
          if(lastSensor.equals("CP.W.W")) {
              tryRelease("CP.M.W");

          } else if (lastSensor.equals("CP.S.E")) {
              tryAcquire("CP.M.W");
              changeSwitch(SwitchLocation.WEST, "RIGHT");
          }
        lastSensor = sensorPos;
        break;

        case "CP.W.W":
            if (lastSensor.substring(0,3).equals("H.S")){ // catches both HN... sensors
                tryRelease("H.S.S");
                Boolean available = acquireIfEmpty("CP.M.M");
                if (!available) {
                    changeSwitch(SwitchLocation.WEST, "RIGHT");
                } else {
                    changeSwitch(SwitchLocation.WEST, "LEFT");
                }
            } else if  (lastSensor.substring(0,3).equals("CP.")){
                tryRelease("CP.M.M");
                Boolean available = acquireIfEmpty("H.S.S");
                if (!available) {
                    changeSwitch(SwitchLocation.SOUTH, "LEFT");
                } else {
                    changeSwitch(SwitchLocation.SOUTH, "RIGHT");
                }
            }
            lastSensor = sensorPos;
            break;

        case "H.S.N.1":
            if (lastSensor.equals("H.S.N.2")) {
                tryAcquire("CP.M.W");
                changeSwitch(SwitchLocation.SOUTH, "LEFT");
            } else if (lastSensor.equals("CP.W.W")){
                tryRelease("CP.M.W");
            }

            lastSensor = sensorPos;
            break;

        case "H.S.N.2":
            if (lastSensor == null) {
                lastSensor = sensorPos;
            }
            if (lastSensor.equals("H.S.N.1")) {
                stopAndTurn();
            }
            lastSensor = sensorPos;
            break;

        case "H.S.S.1":
            if (lastSensor.equals("H.S.S.2")) {
                tryAcquire("CP.M.W");
                changeSwitch(SwitchLocation.SOUTH, "RIGHT");
            } else if (lastSensor.equals("CP.W.W")){
                tryRelease("CP.M.W");
            }
            lastSensor = sensorPos;
            break;

        case "H.S.S.2":
            if (lastSensor.equals("H.S.S.1")) {
                stopAndTurn();
            }
            lastSensor = sensorPos;
            break;
    }
  }

  private void changeSwitch(SwitchLocation railSwitch, String railSwitchLocation) throws CommandException{
    int x;
    int y;
    int dir;
    if (railSwitchLocation.equals("LEFT")) {
      dir = TSimInterface.SWITCH_LEFT;
    } else if (railSwitchLocation.equals("RIGHT")) {
      dir = TSimInterface.SWITCH_RIGHT;
    } else {
      System.out.println("Something went wrong");
      dir = 0;
    }

    if(railSwitch.equals(SwitchLocation.NORTH)){
      x = 17;
      y = 7;
    } else if (railSwitch.equals(SwitchLocation.EAST)) {
      x = 15;
      y = 9;
    } else if (railSwitch.equals(SwitchLocation.SOUTH)) {
      x = 3;
      y = 11;
    } else if (railSwitch.equals(SwitchLocation.WEST)) {
      x = 4;
      y = 9;
    } else {
      System.out.println("Something went wrong");
      x = 0;
      y = 0;
    }

    if (dir > 0 && x > 0) {
      this.tsi.setSwitch(x,y,dir);
    }

  }

  private boolean acquireIfEmpty(String semaphoreName) throws CommandException, InterruptedException {
      boolean acquired = false;
      Semaphore semaphore = this.semaphores.get(semaphoreName);
      int availablePermits = semaphore.availablePermits();
      if (availablePermits > 0) {
          tryAcquire(semaphoreName);
          acquired = true;
      }
      return acquired;
  }

  /**
   * Sets trainspeed to 0 and blocks until semaphore is acquired.
   */
  private void tryAcquire(String semaphoreName) throws CommandException, InterruptedException {
    Semaphore semaphore = this.semaphores.get(semaphoreName);
    this.tsi.setSpeed(this.TRAIN_ID, 0);
    semaphore.acquire();
    this.tsi.setSpeed(this.TRAIN_ID, TRAIN_SPEED);
      System.out.println("Acquired " + semaphoreName);
  }

  /**
   * If semaphore is acquired, it'll be released
   */
  private void tryRelease(String semaphoreName) {
    Semaphore semaphore = this.semaphores.get(semaphoreName);
    if (semaphore.availablePermits() == 0) {
      semaphore.release();
        System.out.println("Released " + semaphoreName);
    }

  }

  /**
   * Stops the train and waits 1-2 seconds. Changes direction after wait.
   */
  private void stopAndTurn() throws CommandException, InterruptedException {
    int ORIGINAL_SPEED = this.TRAIN_SPEED;
    int delay = 1500 + (30 * Math.abs(TRAIN_SPEED));
    this.tsi.setSpeed(this.TRAIN_ID, 0);
    this.sleep(delay);
    this.TRAIN_SPEED = ORIGINAL_SPEED * -1;
    this.tsi.setSpeed(this.TRAIN_ID, TRAIN_SPEED);
  }

  private String getSensorPos(int x, int y) {
    switch ((x * 100 + y)) {
      case 1103:
        return "H.N.N.1";
      case 606:
        return "H.N.N.2";
      case 1207:
        return "H.N.N.3";
      case 1105:
        return "H.N.S.1";
      case 905:
        return "H.N.S.2";
      case 1208:
        return "H.N.S.3";
      case 511:
        return "H.S.N.1";
      case 1111:
        return "H.S.N.2";
      case 513:
        return "H.S.S.1";
      case 1113:
        return "H.S.S.2";
        case 1909:
            return "CP.E.E";
        case 109:
            return "CP.W.W";
      case 609:
        return "CP.N.W";
      case 1309:
        return "CP.N.E";
      case 610:
        return "CP.S.W";
      case 1310:
        return "CP.S.E";
      default:
        return "Unknown sensor";
    }
  }

  enum SwitchLocation {
    NORTH,
    EAST,
    SOUTH,
    WEST;
  }
}
