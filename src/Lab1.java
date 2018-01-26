import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Lab1 {
  private HashMap<String, Semaphore> semaphores = new HashMap<>();


  public Lab1(Integer TRAIN1_SPEED, Integer TRAIN2_SPEED) {
      initateSemaphores();
    try {
      Train train1 = new Train(1, TRAIN1_SPEED, semaphores);
      Train train2 = new Train(2, TRAIN2_SPEED, semaphores);
      Thread t1 = new Thread(train1);
      Thread t2 = new Thread(train2);

      t1.start();
      t2.start();
      t1.join();
      t2.join();
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
