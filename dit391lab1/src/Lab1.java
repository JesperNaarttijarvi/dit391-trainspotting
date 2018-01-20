import TSim.*;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Lab1 {

    public Lab1(Integer speed1, Integer speed2) {
        ArrayList<Semaphore> semaphores = new ArrayList<>();

        for (int i = 0; i < 4; i++){
            semaphores.add(new Semaphore(1));
        }

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


            while (true) {
                try {
                    tsi.setSpeed(TRAIN_ID, TRAIN_SPEED);
                    SensorEvent sensorEvent = tsi.getSensor(TRAIN_ID);
                    if (sensorEvent.getStatus() == SensorEvent.ACTIVE) {

                        int sensor_x = sensorEvent.getXpos();
                        int sensor_y = sensorEvent.getYpos();
                        String sensorPos = getSensorPos(sensor_x, sensor_y); /** TODO: DRAW A MAP WITH SENSORS */

                        switch (sensorPos) {
                            case "Unknown Sensor":
                                System.out.println("Unknown Sensor");
                                break;

                            case "HOME.N.0":
                                if (lastSensor == null) this.direction = DIRECTION.SOUTH;
                                else if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                break;

                            case "HOME.N.1":
                                if (this.direction == DIRECTION.NORTH) stopAndTurn();
                                break;

                            case "HOME.S.0":
                                if (lastSensor == null) this.direction = DIRECTION.NORTH;
                                else if (this.direction == DIRECTION.SOUTH){
                                    stopAndTurn();
                                    tsi.setSwitch(3, 11, 1);
                                }
                                break;

                            case "HOME.S.1":
                                if (this.direction == DIRECTION.SOUTH){
                                    stopAndTurn();
                                    tsi.setSwitch(3, 11, 0);
                                }
                                break;

                            case "MIDDLE.N.0":
                                if (this.direction == DIRECTION.SOUTH){
                                    trainStop();

                                    getSemaphore("CRITICAL_EAST").acquire();

                                    trainStart();
                                    tsi.setSwitch(17, 7, 0);
                                }
                                break;

                            case "MIDDLE.N.1":
                                if (this.direction == DIRECTION.SOUTH){
                                    trainStop();

                                    getSemaphore("CRITICAL_EAST").acquire();

                                    //trainStart();
                                    tsi.setSwitch(17, 7, 1);
                                }
                                break;

                            case "MIDDLE.S.0":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH){
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    tsi.setSwitch(4, 9, 1);
                                }
                                else {
                                    System.out.println("SDÖFLKSDÖLFKSDÖFLK");
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    System.out.println("SDÖFLKSDÖLFKSDÖFLK");
                                    tsi.setSwitch(15, 9, 0);
                                }
                                break;

                            case "MIDDLE.S.1":
                                trainStop();
                                if (this.direction == DIRECTION.SOUTH){
                                    getSemaphore("CRITICAL_WEST").acquire();
                                    tsi.setSwitch(4, 9, 0);
                                }
                                else {
                                    getSemaphore("CRITICAL_EAST").acquire();
                                    tsi.setSwitch(15, 9, 1);
                                }
                                break;

                            case "CRITICAL.E.0": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.NORTH) {
                                    tsi.setSwitch(17, 7, 0);
                                    getSemaphore("CRITICAL_EAST").release();
                                }
                                break;

                            case "CRITICAL.E.1": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.SOUTH){
                                    tsi.setSwitch(15, 9, 0);
                                    getSemaphore("CRITICAL_EAST").release();
                                }
                                break;

                            case "CRITICAL.W.0": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.NORTH){
                                    tsi.setSwitch(4, 9, 0);
                                    getSemaphore("CRITICAL_WEST").release();
                                }
                                break;

                            case "CRITICAL.W.1": // TODO IF OCCUPIED
                                if (this.direction == DIRECTION.SOUTH) {
                                    tsi.setSwitch(3, 11, 0);
                                    getSemaphore("CRITICAL_WEST").release();
                                }
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

        void trainStop() throws CommandException{
            this.tsi.setSpeed(this.TRAIN_ID, 0);
        }

        void trainStart() throws CommandException{
            this.TRAIN_SPEED = this.TRAIN_SPEED;
        }

        /**
         * Stops the train and waits 1-2 seconds. Changes direction after wait.
         */
        void stopAndTurn() throws CommandException, InterruptedException {

            int delay = 1000 + (20 * Math.abs(TRAIN_SPEED));


            this.tsi.setSpeed(this.TRAIN_ID, 0);
            this.sleep(delay); //delay wont wait until train speed is 0. must be fixed.
            System.out.println(TRAIN_SPEED);
            this.TRAIN_SPEED = -this.TRAIN_SPEED;

            if (this.direction == DIRECTION.NORTH) this.direction = DIRECTION.SOUTH;
            else this.direction = DIRECTION.NORTH;
        }

        Semaphore getSemaphore(String name){
            switch (name){
                case "CRITICAL_EAST":
                    return semaphores.get(0);
                case "CRITICAL_WEST":
                    return semaphores.get(1);
                default:
                    return null;
            }
        }

        String getSensorPos(int x, int y) {
            switch ((x * 100 + y)) {
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
