# jrewriter

Run tests with `gradle test --rerun-tasks`.

## `GetSetRewriter`
Not developed. See [README-getset.md](README-getset.md)


## `IncrementRewriter`

*Java 8 required*

Rewrite increment on `int` or `long` to be atomic using `getAndAddInt`
and `getAndAddLong` methods in `sun.misc.Unsafe`. As a result,
unsynchronized update on counters is made atomic.

See [src/test/java/Increment.java](src/test/java/Increment.java) as an
example.

```bash
$ gradle build
$ cd src/test/java
$ javac Increment.java
$ java Increment
# gives unsynchronized results
1926209
1509385
$ java -cp .:../../../build/libs/jrewriter-0.1.0.jar -Djava.system.class.loader=jrewriter.RewriterClassLoader Increment
# gives synchronized results
2000000
2000000
```
