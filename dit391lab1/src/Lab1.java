import TSim.*;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Lab1 {

    public Lab1(Integer speed1, Integer speed2) {
        ArrayList<Semaphore> semaphores = new ArrayList<>();

        semaphores.add(new Semaphore(1)); //CRIT EAST
        semaphores.add(new Semaphore(1)); //CRIT WEST
        semaphores.add(new Semaphore(1)); //HOME SOUTH
        semaphores.add(new Semaphore(1)); //MIDDLE SOUTH
        semaphores.add(new Semaphore(1)); //MIDDLE NORTH
        semaphores.add(new Semaphore(1)); //HOME NORTH INTERSECTION

        try {
            Train train1 = new Train(1, speed1, semaphores);
            Train train2 = new Train(2, speed2, semaphores);


            train1.direction = DIRECTION.NORTH;
            train2.direction = DIRECTION.SOUTH;


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
     *
     */
    class Train extends Thread {
        private final int TRAIN_ID;
        private int TRAIN_SPEED;
        private ArrayList<Semaphore> semaphores;
        private TSimInterface tsi;
        private String lastSensor;
        public DIRECTION direction;


        /**
         * @param TRAIN_ID    asd
         * @param TRAIN_SPEED asd
         * @param semaphores  asd
         */
        Train(int TRAIN_ID, int TRAIN_SPEED, ArrayList<Semaphore> semaphores) {
            this.TRAIN_ID = TRAIN_ID;
            this.TRAIN_SPEED = TRAIN_SPEED;
            this.semaphores = semaphores;
            this.tsi = TSimInterface.getInstance();
        }

        @Override
        public void run() {

            /**
             * Since trains are heading LEFT
             * switchRIGHT will flip the switch UP
             * switchLEFT will flip the switch DOWN
             */

            while (true) {
                try {
                    tsi.setSpeed(TRAIN_ID, TRAIN_SPEED);
                    SensorEvent sensorEvent = tsi.getSensor(TRAIN_ID);

                    if (sensorEvent.getStatus() == SensorEvent.INACTIVE){

                        int sensor_x = sensorEvent.getXpos();
                        int sensor_y = sensorEvent.getYpos();
                        String sensorPos = getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */

                        switch (sensorPos){
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

                    }

                    if (sensorEvent.getStatus() == SensorEvent.ACTIVE) {

                        int sensor_x = sensorEvent.getXpos();
                        int sensor_y = sensorEvent.getYpos();
                        String sensorPos = getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */

                        switch (sensorPos) {
                            case "Unknown Sensor":
                                System.out.println("Unknown Sensor");
                                break;

                            case "HOME.N.0":
                                trainStop();
                                if (lastSensor == null) this.direction = DIRECTION.SOUTH;
                                else if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                else getSemaphore("HOME_NORTH").acquire();
                                break;

                            case "HOME.N.1":
                                trainStop();
                                if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                else getSemaphore("HOME_NORTH").acquire();
                                break;

                            case "HOME.S.0":
                                trainStop();
                                if (lastSensor == null) {
                                    this.direction = DIRECTION.NORTH;
                                } else if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                                else {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    tsi.setSwitch(3, 11, switchtoRIGHT());//flip bottom left switch UP
                                }
                                break;

                            case "HOME.S.1":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH) stopAndTurn();
                                else {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    tsi.setSwitch(3, 11, switchtoLEFT());//flip bottom left switch DOWN
                                }
                                break;

                            case "MIDDLE.N.0":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    tsi.setSwitch(17, 7, switchtoRIGHT()); //flip east switch up
                                } else {
                                    getSemaphore("HOME_NORTH").acquire();
                                }
                                break;

                            case "MIDDLE.N.1":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    tsi.setSwitch(17, 7, switchtoLEFT()); //flip east switch down
                                } else {
                                    getSemaphore("HOME_NORTH").acquire();
                                }
                                break;

                            case "MIDDLE.S.0":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    System.out.println("****LOCKING CRITICAL WEST");
                                    tsi.setSwitch(4, 9, switchtoLEFT()); //flip west switch up
                                } else {

                                    getSemaphore("CRITICAL_EAST").acquire();
                                    System.out.println("****LOCKING CRITICAL EAST");
                                    tsi.setSwitch(15, 9, switchtoRIGHT()); //flip east switch up
                                }
                                break;

                            case "MIDDLE.S.1":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH) {
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    tsi.setSwitch(4, 9, switchtoRIGHT()); //flip west switch down
                                } else {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    tsi.setSwitch(15, 9, switchtoLEFT()); //flip east switch down
                                }
                                break;

                            case "CRITICAL.E.0": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.NORTH) {
                                    if (getSemaphore("MIDDLE_NORTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the LOWER MIDDLE NORTH");
                                        tsi.setSwitch(17, 7, switchtoLEFT()); //lower track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking UPPER MIDDLE NORTH");
                                        tsi.setSwitch(17, 7, switchtoRIGHT()); //upper tack
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

                            case "CRITICAL.E.1": // TODO IF OCCUPIED
                                //if going south
                                if (this.direction == DIRECTION.SOUTH) {
                                    if (getSemaphore("MIDDLE_SOUTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the UPPER MIDDLE SOUTH");
                                        tsi.setSwitch(15, 9, switchtoRIGHT()); //upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER MIDDLE SOUTH");
                                        tsi.setSwitch(15, 9, switchtoLEFT()); //lower tack
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
                                        tsi.setSwitch(4, 9, switchtoLEFT()); //upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER MIDDLE SOUTH");
                                        tsi.setSwitch(4, 9, switchtoRIGHT()); //lower tack
                                    }

                                //if going south
                                }else getSemaphore("MIDDLE_SOUTH").release();
                                break;

                            case "CRITICAL.W.1": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.SOUTH) {

                                    if (getSemaphore("HOME_SOUTH").tryAcquire()) {
                                        System.out.println("****i LOCKED the UPPER HOME SOUTH");
                                        tsi.setSwitch(3, 11, switchtoLEFT());//upper track
                                    } else {
                                        System.out.println("****LOCKED!!! Taking LOWER HOME SOUTH");
                                        tsi.setSwitch(3, 11, switchtoRIGHT());//lower track
                                    }

                                } else{
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

                        lastSensor = sensorPos;
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

        /**
         * Stops the train and waits 1-2 seconds. Changes direction after wait.
         */
        void stopAndTurn() throws CommandException, InterruptedException {

            int delay = 1000 + (20 * Math.abs(TRAIN_SPEED));


            this.tsi.setSpeed(this.TRAIN_ID, 0);
            this.sleep(delay); //delay wont wait until train speed is 0. must be fixed.
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


    }

    enum DIRECTION {
        SOUTH,
        NORTH
    }

    int switchtoLEFT(){
        return TSimInterface.SWITCH_LEFT;
    }

    int switchtoRIGHT(){
        return TSimInterface.SWITCH_RIGHT;
    }


    /**
     * @param string
     * @param c
     * @return
     */
    String takeWhile(String string, char c) {
        int i;
        for (i = 0; i < string.length(); i++) {
            if (string.charAt(i) == c) {
                return (string.substring(0, i));
            }
        }
        return string.substring(0, i);
    }
}
