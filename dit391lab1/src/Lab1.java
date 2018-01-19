import TSim.*;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Lab1 {

  public Lab1(Integer speed1, Integer speed2) {
    ArrayList<Semaphore> semaphores = new ArrayList<>();

    try {
      Train train1 = new Train(1,speed1,semaphores);
      Train train2 = new Train(2,0,semaphores);
      train1.start();
      train2.start();
      train1.join();
      train2.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   *
   */
  class Train extends Thread {
    private final int TRAIN_ID;
    private final int TRAIN_SPEED;
    private ArrayList<Semaphore> semaphores;
    private TSimInterface tsi;
    private String lastSensor;
    private DIRECTION direction;


    /**
     *
     * @param TRAIN_ID asd
     * @param TRAIN_SPEED asd
     * @param semaphores asd
     */
    Train(int TRAIN_ID, int TRAIN_SPEED, ArrayList<Semaphore> semaphores) {
      this.TRAIN_ID = TRAIN_ID;
      this.TRAIN_SPEED = TRAIN_SPEED;
      this.semaphores = semaphores;
      this.tsi = TSimInterface.getInstance();
    }

    @Override
    public void run() {
      while (true) {
        try {
          tsi.setSpeed(TRAIN_ID,TRAIN_SPEED);
          SensorEvent sensorEvent = tsi.getSensor(TRAIN_ID);
          if (sensorEvent.getStatus() == SensorEvent.ACTIVE) {
            System.out.println(sensorEvent.toString());
            int sensor_x = sensorEvent.getXpos();
            int sensor_y = sensorEvent.getYpos();
            String sensorPos = getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */
            System.out.println(sensorPos);
            switch (sensorPos) {
              case "Unkown sensor":
                break;
              case "HOME.N.1" :
                if (lastSensor == null) {
                  direction = DIRECTION.SOUTH;
                } else if (lastSensor.equals("HOME.N.1")){
                  direction = DIRECTION.SOUTH;
                  stopAndTurn();
                }
                break;
              case "HOME.N.2" :
                if (lastSensor.equals("HOME.N.2")){
                  stopAndTurn();
                }
                break;
              case "HOME.S.1" :
                if (lastSensor == null) {
                  direction = DIRECTION.NORTH;
                } else if (lastSensor.equals("HOME.S.1") || true){
                  stopAndTurn();
                }
              case "HOME.S.2" :
                stopAndTurn();
                //if (lastSensor.equals("HOME.S.2") || true){
                //  stopAndTurn();

                //}

            }
          }

        } catch (CommandException | InterruptedException e) {
          e.printStackTrace();
          System.exit(1);
        }
      }
    }

    /**
     * Stops the train and waits 1-2 seconds. Changes direction after wait.
     */
    void stopAndTurn() throws CommandException, InterruptedException {
      int ORIGINAL_SPEED = this.TRAIN_SPEED;
      int delay = 1000+(20*Math.abs(ORIGINAL_SPEED));
      System.out.println(ORIGINAL_SPEED);
      System.out.println(delay);
      if (this.direction == DIRECTION.NORTH) {
        this.direction = DIRECTION.SOUTH;
      } else {
        this.direction = DIRECTION.NORTH;
      }
      this.tsi.setSpeed(this.TRAIN_ID,0);
      this.sleep(delay); //** TODO: Fix sleep;
      this.tsi.setSpeed(this.TRAIN_ID, -ORIGINAL_SPEED);
    }

    String getSensorPos(int x, int y) {
      switch ((x*100+y)){
        case 1003:
          return "HOME.N.1";
        case 1005:
          return "HOME.N.2";
        case 1011:
          return "HOME.S.1";
        case 1013:
          return "HOME.S.2";
        default:
          return "Unknown sensor";
      }
    }
  }

  enum DIRECTION {
    SOUTH,
    NORTH;
  }

  /**
   *
   * @param string
   * @param c
   * @return
   */
  String takeWhile (String string, char c) {
    int i;
    for (i = 0; i < string.length(); i++) {
      if (string.charAt(i) == c) {
        return (string.substring(0,i));
      }
    }
    return string.substring(0, i);
  }
}
