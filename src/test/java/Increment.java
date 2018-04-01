
class MyThread extends Thread {
    public void run() {
        for(int i = 0; i < 1000000; i++)
            // unsafe.getAndAddInt(Increment.obj, offset, 1);
            Increment.obj.x++;
    }
}

class MyThreadStatic extends Thread {
    public void run() {
        for(int i = 0; i < 1000000; i++)
            // unsafe.getAndAddInt(Increment.class, offset, 1);
            Increment.xs++;
    }
}

public class Increment {
    public int x = 0;
    public static int xs = 0;
    public static Increment obj = new Increment();

    public static void main(String[] args) {
        System.out.println(testField());
        System.out.println(testStatic());
    }

    public static void doit(Thread t1, Thread t2) {
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch(InterruptedException e) {}
    }

    public static int testField() {
        doit(new MyThread(), new MyThread());
        return obj.x;
    }

    public static int testStatic() {
        doit(new MyThreadStatic(), new MyThreadStatic());
        return xs;
    }
}
