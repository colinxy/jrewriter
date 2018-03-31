
import java.lang.reflect.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertEquals;


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
        assertEquals(increment.getMethod("doit").invoke(null),
                     2000000);
    }
}
