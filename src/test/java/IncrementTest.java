
import java.lang.reflect.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;


public class IncrementTest {
    static RewriterClassLoader loader;
    Class<?> increment;

    @BeforeClass
    public static void classLoader() {
        loader = new RewriterClassLoader(IncrementTest.class.getClassLoader());
    }

    @Before
    public void loadClass() throws ClassNotFoundException {
        increment = loader.loadClass("Increment");
    }

    @Test
    public void testField()
        throws IllegalAccessException
        , InvocationTargetException
        , NoSuchMethodException {
        assertEquals(increment.getMethod("testField").invoke(null),
                     2000000);
    }

    @Test
    public void testStatic()
        throws IllegalAccessException
        , InvocationTargetException
        , NoSuchMethodException {
        assertEquals(increment.getMethod("testStatic").invoke(null),
                     2000000);
    }
}
