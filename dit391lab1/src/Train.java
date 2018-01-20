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
    private String lastSensor;
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

                String sensorPos;

                switch (sensorEvent.getStatus()) {
                    /** TODO: DRAW A MAP WITH SENSORS */

                    case SensorEvent.INACTIVE:
                        sensorPos = sensor(sensorEvent);
                        switch (sensorPos) {
                            case "HOME.I.0":
                                getSemaphore("HOME_NORTH").release();
                                break;
                            case "HOME.I.1":
                                getSemaphore("HOME_NORTH").release();
                                break;
                            case "CRITICAL.E.0":
                                if (this.direction == DIRECTION.NORTH) getSemaphore("CRITICAL_EAST").release();
                                break;
                            case "CRITICAL.E.1":
                                if (this.direction == DIRECTION.SOUTH) getSemaphore("CRITICAL_EAST").release();
                                break;
                            case "CRITICAL.W.0":
                                if (this.direction == DIRECTION.NORTH) getSemaphore("CRITICAL_WEST").release();
                                break;
                            case "CRITICAL.W.1":
                                if (this.direction == DIRECTION.SOUTH) getSemaphore("CRITICAL_WEST").release();
                                break;
                        }
                        break;

                    case SensorEvent.ACTIVE:
                        sensorPos = sensor(sensorEvent);
                        trainStop();
                        switch (sensorPos) {
                            case "Unknown Sensor":
                                System.out.println("Unknown Sensor");
                                break;

                            case "HOME.N.0":
                                if (this.direction == null) this.direction = DIRECTION.SOUTH;
                                else if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                else getSemaphore("HOME_NORTH").acquire();
                                break;

                            case "HOME.N.1":
                                if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                else getSemaphore("HOME_NORTH").acquire();
                                break;

                            case "HOME.S.0":
                                if (this.direction == null) this.direction = DIRECTION.NORTH;
                                else if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                                else {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    flipSwitch(3, 11, switchtoLEFT());//flip bottom left switch UP
                                }
                                break;

                            case "HOME.S.1":
                                if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                                else {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    flipSwitch(3, 11, switchtoLEFT());//flip bottom left switch DOWN
                                }
                                break;

                            case "MIDDLE.N.0":
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    flipSwitch(17, 7, switchtoRIGHT());//flip east switch up
                                } else {
                                    getSemaphore("HOME_NORTH").acquire();
                                }
                                break;

                            case "MIDDLE.N.1":
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    flipSwitch(17, 7, switchtoLEFT());//flip east switch down
                                } else {
                                    getSemaphore("HOME_NORTH").acquire();
                                }
                                break;

                            case "MIDDLE.S.0":
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    System.out.println("****LOCKING CRITICAL WEST");
                                    flipSwitch(4, 9, switchtoLEFT()); //flip west switch up

                                } else {

                                    getSemaphore("CRITICAL_EAST").acquire();
                                    System.out.println("****LOCKING CRITICAL EAST");
                                    flipSwitch(15, 9, switchtoRIGHT()); //flip east switch up

                                }
                                break;

                            case "MIDDLE.S.1":
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    flipSwitch(4, 9, switchtoRIGHT());//flip west switch down
                                } else {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    flipSwitch(15, 9, switchtoRIGHT());//flip east switch down
                                }
                                break;

                            case "CRITICAL.E.0": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.NORTH) {
                                    if (getSemaphore("MIDDLE_NORTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the LOWER MIDDLE NORTH");
                                        flipSwitch(17,7,switchtoLEFT());//lower track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking UPPER MIDDLE NORTH");
                                        flipSwitch(17,7,switchtoRIGHT()); //uppertrack
                                    }

                                } else {
                                    getSemaphore("MIDDLE_NORTH").release();
                                    /*
                                    can release other trains semaphore. not good cause may imply
                                    collision if other train malfunction / very slow
                                    fix?
                                     */
                                }
                                break;

                            case "CRITICAL.E.1":
                                //if going south
                                if (this.direction == DIRECTION.SOUTH) {
                                    if (getSemaphore("MIDDLE_SOUTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the UPPER MIDDLE SOUTH");
                                        flipSwitch(15,9,switchtoRIGHT()); //upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER MIDDLE SOUTH");
                                        flipSwitch(15,9,switchtoLEFT()); //lower track
                                    }

                                    //If going north
                                } else {
                                    getSemaphore("MIDDLE_SOUTH").release();
                                    System.out.println("****RELEASED MIDDLE SOUTH");
                                    /*
                                    can release other trains semaphore. not good cause may imply
                                    collision if other train malfunction / very slow
                                    fix?
                                     */
                                }
                                break;

                            case "CRITICAL.W.0": // TODO IF OCCUPIED
                                //If going north
                                if (this.direction == DIRECTION.NORTH) {
                                    if (getSemaphore("MIDDLE_SOUTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the UPPER MIDDLE SOUTH");
                                        flipSwitch(4,9,switchtoLEFT()); //upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER MIDDLE SOUTH");
                                        flipSwitch(4,9,switchtoRIGHT()); //lower track

                                    }

                                    //if going south
                                } else {
                                    System.out.println("****RELEASED MIDDLE SOUTH");
                                    getSemaphore("MIDDLE_SOUTH").release();
                                }
                                break;

                            case "CRITICAL.W.1": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.SOUTH) {

                                    if (getSemaphore("HOME_SOUTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the UPPER HOME SOUTH");
                                        flipSwitch(3,11,switchtoLEFT()); //upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER HOME SOUTH");
                                        flipSwitch(3,11,switchtoRIGHT()); //lower track
                                    }

                                } else {
                                    getSemaphore("HOME_SOUTH").release();
                                    System.out.println("****RELEASED UPPER HOME SOUTH");
                                }
                                /*
                                    can release other trains semaphore. not good cause may imply
                                    collision if other train malfunction / very slow
                                    fix?
                                */
                                break;
                        }

                }

            } catch (CommandException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
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
            case 707:
                return "HOME.I.0";
            case 808:
                return "HOME.I.1";
            case 1003:
                return "HOME.N.0";
            case 1005:
                return "HOME.N.1";
            case 1011:
                return "HOME.S.0";
            case 1013:
                return "HOME.S.1";
            case 1307:
                return "MIDDLE.N.0";
            case 1308:
                return "MIDDLE.N.1";
            case 1009:
                return "MIDDLE.S.0";
            case 1010:
                return "MIDDLE.S.1";
            case 1807:
                return "CRITICAL.E.0";
            case 1609:
                return "CRITICAL.E.1";
            case 309:
                return "CRITICAL.W.0";
            case 211:
                return "CRITICAL.W.1";
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

    int switchtoLEFT() { return TSimInterface.SWITCH_LEFT; }

    int switchtoRIGHT() { return TSimInterface.SWITCH_RIGHT; }
}



