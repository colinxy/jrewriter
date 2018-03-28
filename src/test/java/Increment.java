
class MyThread extends Thread {
    public void run() {
        for(int i = 0; i < 1000000; i++)
            // unsafe.getAndAddInt(Increment.obj, offset, 1);
            Increment.obj.x++;
    }
}

public class Increment {
    public int x = 0;
    public static Increment obj = new Increment();

    public static void main(String[] args) {
        MyThread t1 = new MyThread();
        MyThread t2 = new MyThread();
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch(InterruptedException e) {}
        System.out.println(obj.x);
    }
}
