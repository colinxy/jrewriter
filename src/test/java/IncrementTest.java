
import java.lang.reflect.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;


public class IncrementTest {
    RewriterClassLoader loader;

    @Before()
    public void setUp() {
        loader = new RewriterClassLoader(SimpleTest.class.getClassLoader());
    }

    @Test()
    public void test()
        throws ClassNotFoundException
        , InstantiationException
        , IllegalAccessException
        , InvocationTargetException
        , NoSuchMethodException {

        Class<?> increment = loader.loadClass("Increment");
        increment.getMethod("main", String[].class).invoke(null, (Object)null);
    }
}
