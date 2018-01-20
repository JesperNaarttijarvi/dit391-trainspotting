import TSim.*;

import java.util.ArrayList;

public class Lab1 extends Thread {

    public Lab1(Integer speed1, Integer speed2) {

        ArrayList<BinarySemaphore> semaphores = new ArrayList<>();

        semaphores.add(new BinarySemaphore()); //CRIT EAST
        semaphores.add(new BinarySemaphore()); //CRIT WEST
        semaphores.add(new BinarySemaphore()); //HOME SOUTH
        semaphores.add(new BinarySemaphore()); //MIDDLE SOUTH
        semaphores.add(new BinarySemaphore()); //MIDDLE NORTH
        semaphores.add(new BinarySemaphore()); //HOME NORTH INTERSECTION

        Train train1 = new Train(1, speed1, semaphores);
        Train train2 = new Train(2, speed2, semaphores);

        Thread t0 = new Thread(train1);
        Thread t1 = new Thread(train2);
        try {
            t0.start();
            t1.start();
            t0.join();
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


}
