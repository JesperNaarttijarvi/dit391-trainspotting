import java.util.concurrent.Semaphore;

public class BinarySemaphore extends Semaphore {

    public BinarySemaphore() {
        super(1);
    }

    @Override
    public void release() {
        if (super.availablePermits() < 1) super.release();
    }
}
