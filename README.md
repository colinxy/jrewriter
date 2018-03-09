# jrewriter

Test with `gradle test --rerun-tasks`.


## `GetSetRewriter`
Rewrite direct field references with getters and setters at bytecode
level.

To run the library, first build with `gradle build`, then put the jar
located at `build/libs/jrewriter-<version>.jar` file in your classpath
and pass `-Djava.system.class.loader=jrewriter.RewriterClassLoader` to
java.

Here is an example to test with a single java class:

```java
public class Example {
    int x;

    public static void main(String... args) {
        new Example().greet();
    }

    public Example() {
        x = 0;
    }

    public void greet() {
        x = 1;
        System.out.println("hello world");
    }
}
```

```bash
$ javac Example.java
$ java -cp .:<jrewriter-jar> -Djava.system.class.loader=jrewriter.RewriterClassLoader Example
```


## `IncrementRewriter`

Rewrite increment on `int` or `long` to be atomic using
`sun.misc.Unsafe`.


```java
// IncCorrect.java

import sun.misc.Unsafe;
import java.lang.reflect.*;

class MyThreadCorrect extends Thread {
    public Unsafe getUnsafe() {
        Field f;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
            throw new Error(ex);
        }

        f.setAccessible(true);

        Unsafe unsafe;
        try {
            unsafe = (Unsafe) f.get(null);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            throw new Error(ex);
        }

        return unsafe;
    }

    public void run() {
        // Unsafe unsafe = Unsafe.getUnsafe();
        Unsafe unsafe = getUnsafe();

        Field x;
        try {
            x = IncCorrect.class.getDeclaredField("x");
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
            throw new Error(ex);
        }
        long offset = unsafe.staticFieldOffset(x);

        for(int i = 0; i < 10000; i++)
            unsafe.getAndAddInt(IncCorrect.class, offset, 1);
            // IncCorrect.x++;
    }
}

class IncCorrect {
    public static int x = 0;
    public static void main(String[] args) {
        MyThreadCorrect t1 = new MyThreadCorrect();
        MyThreadCorrect t2 = new MyThreadCorrect();
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch(InterruptedException e) {}
        System.out.println(x);
    }
}
```

Increment of field `x` of IncCorrect is replaced by atomic operation
`unsafe.getAndAddInt`.
