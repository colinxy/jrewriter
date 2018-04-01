## `GetSetRewriter`

*Currently not developed.* To expereiment, do the following
replacement in `RewriterClassLoader.java`.

```java
Rewriter rewriter = new GetSetRewriter(pool, cc);
// Rewriter rewriter = new IncrementRewriter(pool, cc);
```


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
