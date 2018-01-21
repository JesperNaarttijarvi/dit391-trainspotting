import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

class Train implements Runnable {
    private final int TRAIN_ID;
    private int TRAIN_SPEED;
    private ArrayList<BinarySemaphore> semaphores;
    private TSimInterface tsi;
    public DIRECTION direction;


    /**
     * @param TRAIN_ID    asd
     * @param TRAIN_SPEED asd
     * @param semaphores  asd
     */
    Train(int TRAIN_ID, int TRAIN_SPEED, ArrayList<BinarySemaphore> semaphores) {
        this.TRAIN_ID = TRAIN_ID;
        this.TRAIN_SPEED = TRAIN_SPEED;
        this.semaphores = semaphores;
        this.tsi = TSimInterface.getInstance();
    }

    enum DIRECTION {
        SOUTH,
        NORTH
    }

    @Override
    public void run() {
        while (true) {
            try {
                tsi.setSpeed(TRAIN_ID, TRAIN_SPEED);

                SensorEvent sensorEvent = tsi.getSensor(TRAIN_ID);

                if (sensorEvent.getStatus() == SensorEvent.ACTIVE) {
                    String sensorPos = sensor(sensorEvent);
                    trainStop();

                    //TODO TA BORT DUBLETTER, ALLTSÅ ALLT!
                    switch (sensorPos) {
                        case "Unknown Sensor":
                            System.out.println("Unknown Sensor");
                            break;

                        case "HOME.N.0":
                            if (this.direction == null) this.direction = DIRECTION.SOUTH;
                            else if (this.direction == DIRECTION.NORTH) {
                                stopAndTurn();
                                getSemaphore("HOME_NORTH").release();
                            }
                            else getSemaphore("HOME_NORTH").acquire();
                            break;

                        case "HOME.N.1":
                            if (this.direction == DIRECTION.NORTH){
                                stopAndTurn();
                                getSemaphore("HOME_NORTH").release();
                            }
                            else getSemaphore("HOME_NORTH").acquire();
                            break;

                        case "HOME.SE.0":
                            if (this.direction == null) this.direction = DIRECTION.NORTH;
                            else if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                            break;

                        case "HOME.SE.1":
                            if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                            break;

                        case "MIDDLE.NW.0":
                            if (this.direction == DIRECTION.NORTH) getSemaphore("HOME_NORTH").acquire();
                            else getSemaphore("HOME_NORTH").release();
                            break;

                        case "MIDDLE.NW.1":
                            if (this.direction == DIRECTION.NORTH) getSemaphore("HOME_NORTH").acquire();
                            else getSemaphore("HOME_NORTH").release();
                            break;

                        case "MIDDLE.NE.0":
                            if (this.direction == DIRECTION.SOUTH) {
                                getSemaphore("CRITICAL_EAST").acquire();
                                flipSwitch(17, 7, switchtoRIGHT());//flip east switch up
                                acquireMiddle(15,9,"SOUTH");

                            } else {
                                getSemaphore("CRITICAL_EAST").release();
                                getSemaphore("MIDDLE_SOUTH").release();
                            }
                            break;

                        case "MIDDLE.NE.1":
                            if (this.direction == DIRECTION.SOUTH) {
                                getSemaphore("CRITICAL_EAST").acquire();
                                flipSwitch(17, 7, switchtoLEFT());//flip east switch down
                                acquireMiddle(15,9,"SOUTH");

                            } else {
                                getSemaphore("CRITICAL_EAST").release();
                                getSemaphore("MIDDLE_SOUTH").release();
                            }
                            break;

                        case "MIDDLE.SW.0":
                            if (this.direction == DIRECTION.SOUTH) {
                                getSemaphore("CRITICAL_WEST").acquire();
                                flipSwitch(4, 9, switchtoLEFT()); //flip west switch up
                                acquireHomeSouth();

                            } else getSemaphore("CRITICAL_WEST").release();
                            break;

                        case "MIDDLE.SW.1":
                            if (this.direction == DIRECTION.SOUTH) {
                                getSemaphore("CRITICAL_WEST").acquire();
                                flipSwitch(4, 9, switchtoRIGHT());//flip west switch down
                                acquireHomeSouth();

                            } else getSemaphore("CRITICAL_WEST").release();
                            break;

                        case "MIDDLE.SE.0": // TODO IF OCCUPIED
                            if (this.direction == DIRECTION.NORTH) {
                                getSemaphore("CRITICAL_EAST").acquire();
                                flipSwitch(15, 9, switchtoRIGHT()); //flip east switch up
                                acquireMiddle(17,7,"NORTH");
                            } else {
                                getSemaphore("CRITICAL_EAST").release();
                                getSemaphore("MIDDLE_NORTH").release();
                            }
                            break;

                        case "MIDDLE.SE.1":
                            if (this.direction == DIRECTION.NORTH) {
                                getSemaphore("CRITICAL_EAST").acquire();
                                flipSwitch(15, 9, switchtoLEFT()); //flip east switch DOWN
                                acquireMiddle(17,7,"NORTH");
                            } else {
                                getSemaphore("CRITICAL_EAST").release();
                                getSemaphore("MIDDLE_NORTH").release();
                            }
                            break;

                        case "HOME.SW.0": // TODO IF OCCUPIED
                            //If going north
                            if (this.direction == DIRECTION.NORTH) {
                                getSemaphore("CRITICAL_WEST").acquire();
                                flipSwitch(3, 11, switchtoLEFT());//flip bottom left switch UP
                                acquireMiddle(4,9,"SOUTH");
                                getSemaphore("HOME_SOUTH").release();
                            } else {
                                getSemaphore("CRITICAL_WEST").release();
                                getSemaphore("MIDDLE_SOUTH").release();
                            }
                            break;

                        case "HOME.SW.1": // TODO IF OCCUPIED
                            if (this.direction == DIRECTION.NORTH) {
                                getSemaphore("CRITICAL_WEST").acquire();
                                flipSwitch(3, 11, switchtoRIGHT());//flip bottom left switch DOWN
                                acquireMiddle(4,9,"SOUTH");
                                getSemaphore("HOME_SOUTH").release();
                            } else {
                                getSemaphore("CRITICAL_WEST").release();
                                getSemaphore("MIDDLE_SOUTH").release();
                            }
                            break;
                    }

                }
            } catch (CommandException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    void acquireHomeSouth() {
        if (getSemaphore("HOME_SOUTH").tryAcquire()) {
            System.out.println("****i LOCKED the UPPER HOME SOUTH");
            flipSwitch(3, 11, switchtoLEFT()); //upper track
        } else {
            System.out.println("****LOCKED!!! Taking LOWER HOME SOUTH");
            flipSwitch(3, 11, switchtoRIGHT()); //lower track
        }
    }

    void acquireMiddle(int x, int y, String cardinalDir) {

        int thisDir = (this.direction == DIRECTION.SOUTH)? 0 : 1;
        int otherDir = (this.direction == DIRECTION.NORTH)? 0 : 1;

        if (getSemaphore("MIDDLE_"+cardinalDir).tryAcquire()) {
            System.out.println("****i LOCKED the LOWER MIDDLE "+cardinalDir);
            flipSwitch(x, y, thisDir); //upper track
        } else {
            System.out.println("****LOCKED!!! Taking UPPER MIDDLE "+cardinalDir);
            flipSwitch(x, y, otherDir); //lower track
        }
    }

    void trainStop() throws CommandException {
        this.tsi.setSpeed(this.TRAIN_ID, 0);
    }

    String sensor(SensorEvent sensorEvent) {
        int sensor_x = sensorEvent.getXpos();
        int sensor_y = sensorEvent.getYpos();
        return getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */
    }

    /**
     * Stops the train and waits 1-2 seconds. Changes direction after wait.
     */
    void stopAndTurn() throws CommandException, InterruptedException {

        int delay = 1000 + (20 * Math.abs(TRAIN_SPEED));
        this.tsi.setSpeed(this.TRAIN_ID, 0);
        sleep(delay); //delay wont wait until train speed is 0. must be fixed.
        this.TRAIN_SPEED = -this.TRAIN_SPEED;

        if (this.direction == DIRECTION.NORTH) this.direction = DIRECTION.SOUTH;
        else this.direction = DIRECTION.NORTH;
    }

    Semaphore getSemaphore(String name) {
        switch (name) {
            case "CRITICAL_EAST":
                return semaphores.get(0);
            case "CRITICAL_WEST":
                return semaphores.get(1);
            case "HOME_SOUTH":
                return semaphores.get(2);
            case "MIDDLE_SOUTH":
                return semaphores.get(3);
            case "MIDDLE_NORTH":
                return semaphores.get(4);
            case "HOME_NORTH":
                return semaphores.get(5);
            default:
                return null;
        }
    }

    String getSensorPos(int x, int y) {
        switch ((x * 100 + y)) {
            case 1107:
                return "MIDDLE.NW.0";
            case 1108:
                return "MIDDLE.NW.1";
            case 1003:
                return "HOME.N.0";
            case 1005:
                return "HOME.N.1";
            case 1011:
                return "HOME.SE.0";
            case 1013:
                return "HOME.SE.1";
            case 1407:
                return "MIDDLE.NE.0";
            case 1408:
                return "MIDDLE.NE.1";
            case 1209:
                return "MIDDLE.SE.0";
            case 1210:
                return "MIDDLE.SE.1";
            case 511:
                return "HOME.SW.0";
            case 513:
                return "HOME.SW.1";
            case 709:
                return "MIDDLE.SW.0";
            case 710:
                return "MIDDLE.SW.1";
            default:
                return "Unknown sensor";
        }
    }

    void flipSwitch(int x, int y, int direction) {
        while (true) {
            try {
                tsi.setSwitch(x, y, direction); //flip west switch up
                break;
            } catch (CommandException e) {

            }
        }
    }

    int switchtoLEFT() {
        return TSimInterface.SWITCH_LEFT; // return 1
    }

    int switchtoRIGHT() {
        return TSimInterface.SWITCH_RIGHT; // return 2
    }
}



