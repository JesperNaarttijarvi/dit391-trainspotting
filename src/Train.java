import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

class Train implements Runnable {
    private final int TRAIN_ID;
    private int TRAIN_SPEED;
    private HashMap<String, Semaphore> semaphores;
    private TSimInterface tsi;
    private String lastSensor = "";

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
            while (true) {
                tsi.setSpeed(TRAIN_ID, this.TRAIN_SPEED);

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
        tsi.setSpeed(TRAIN_ID,0);
        switch (sensorPos) {
            case "Unkown sensor":
                break;

            case "H.N.N.1":
                if (lastSensor.equals("H.N.N.2")) stopAndTurn();
                lastSensor = sensorPos;
                break;

            case "H.N.N.2":
                if(lastSensor.equals("H.N.N.3")) {
                    tryRelease("CP.N.M");
                } else if (lastSensor.equals("H.N.N.1")){
                    acquire("CP.N.M");
                }
                lastSensor = sensorPos;
                break;

            case "H.N.N.3":
                if(lastSensor.equals("H.N.N.2")) {
                    tryRelease("CP.N.M");
                    acquire("CP.M.E");
                    changeSwitch(SwitchLocation.NORTH, "RIGHT");
                } else if (lastSensor.equals("CP.E.E")){ // since we know it must be cp. or h.n...
                    tryRelease("CP.M.E");
                    acquire("CP.N.M");
                }
                lastSensor = sensorPos;
                break;

            case "H.N.S.1":
                if (lastSensor.equals("H.N.S.2"))  stopAndTurn();
                lastSensor = sensorPos;
                break;

            case "H.N.S.2":
                if(lastSensor.equals("H.N.S.3")) {
                    tryRelease("CP.N.M");
                } else if (lastSensor.equals("H.S.N.1")){
                    acquire("CP.N.M");
                }

                lastSensor = sensorPos;
                break;

            case "H.N.S.3":
                if(lastSensor.equals("H.N.S.2")) {
                    tryRelease("CP.N.M");
                    acquire("CP.M.E");
                    changeSwitch(SwitchLocation.NORTH, "LEFT");
                } else if (lastSensor.equals("CP.E.E")){ // catches both CP... sensors
                    tryRelease("CP.M.E");
                    acquire("CP.N.M");
                }
                lastSensor = sensorPos;
                break;

            case "CP.E.E":
                if (lastSensor.substring(0,3).equals("H.N")){ // catches both HN... sensors
                    tryRelease("H.N.S");
                    if (!tryAcquire("CP.M.M")) {
                        changeSwitch(SwitchLocation.EAST, "LEFT");
                    } else {
                        changeSwitch(SwitchLocation.EAST, "RIGHT");
                    }
                } else if  (lastSensor.substring(0,3).equals("CP.")){
                    tryRelease("CP.M.M");
                    if (!tryAcquire("H.N.S")) {
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
                    acquire("CP.M.E");
                    changeSwitch(SwitchLocation.EAST, "RIGHT");
                }
                lastSensor = sensorPos;
                break;

            case "CP.S.E":
                if(lastSensor.equals("CP.E.E")) {
                    tryRelease("CP.M.E");
                } else if (lastSensor.equals("CP.S.W")) {
                    acquire("CP.M.E");
                    changeSwitch(SwitchLocation.EAST, "LEFT");
                }
                lastSensor = sensorPos;
                break;

            case "CP.N.W":
                if(lastSensor.equals("CP.W.W")) {
                    tryRelease("CP.M.W");
                } else if (lastSensor.equals("CP.N.E")) {
                    acquire("CP.M.W");
                    changeSwitch(SwitchLocation.WEST, "LEFT");
                }
                lastSensor = sensorPos;
                break;

            case "CP.S.W":
                if(lastSensor.equals("CP.W.W")) {
                    tryRelease("CP.M.W");
                } else if (lastSensor.equals("CP.S.E")) {
                    acquire("CP.M.W");
                    changeSwitch(SwitchLocation.WEST, "RIGHT");
                }
                lastSensor = sensorPos;
                break;

            case "CP.W.W":
                if (lastSensor.substring(0,3).equals("H.S")){ // catches both HN... sensors
                    tryRelease("H.S.S");
                    if (tryAcquire("CP.M.M")) {
                        changeSwitch(SwitchLocation.WEST, "RIGHT");
                    } else {
                        changeSwitch(SwitchLocation.WEST, "LEFT");
                    }
                } else if  (lastSensor.substring(0,3).equals("CP.")){
                    tryRelease("CP.M.M");
                    if (tryAcquire("H.S.S")) {
                        changeSwitch(SwitchLocation.SOUTH, "LEFT");
                    } else {
                        changeSwitch(SwitchLocation.SOUTH, "RIGHT");
                    }
                }
                lastSensor = sensorPos;
                break;

            case "H.S.N.1":
                if (lastSensor.equals("H.S.N.2")) {
                    acquire("CP.M.W");
                    changeSwitch(SwitchLocation.SOUTH, "LEFT");
                } else if (lastSensor.equals("CP.W.W")){
                    tryRelease("CP.M.W");
                }
                lastSensor = sensorPos;
                break;

            case "H.S.N.2":
                if (lastSensor.equals("H.S.N.1")) stopAndTurn();
                lastSensor = sensorPos;
                break;

            case "H.S.S.1":
                if (lastSensor.equals("H.S.S.2")) {
                    acquire("CP.M.W");
                    changeSwitch(SwitchLocation.SOUTH, "RIGHT");
                } else if (lastSensor.equals("CP.W.W")){
                    tryRelease("CP.M.W");
                }
                lastSensor = sensorPos;
                break;

            case "H.S.S.2":
                if (lastSensor.equals("H.S.S.1")) stopAndTurn();
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


    /**
     * Calls acquire function from the semaphores in the HashMap
     */
    private void acquire(String semaphoreName) throws InterruptedException{
        semaphores.get(semaphoreName).acquire();
        System.out.println("Acquired " + semaphoreName);
    }

    /**
     *Calls tryAcquire function from the semaphores in the HashMap
     */
    private boolean tryAcquire(String semaphoreName){
        return semaphores.get(semaphoreName).tryAcquire();
    }

    /**
     * If semaphore is acquired, it'll be released
     */
    private void tryRelease(String semaphoreName) {
        Semaphore semaphore = this.semaphores.get(semaphoreName);
        if (semaphore.availablePermits() < 1) {
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
        sleep(delay);
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
            case 611:
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
            case 709:
                return "CP.N.W";
            case 1209:
                return "CP.N.E";
            case 710:
                return "CP.S.W";
            case 1210:
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
