
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class SimpleTest {
    static RewriterClassLoader loader;
    Class<?> simple;

    private String methodName;
    private int result;

    public SimpleTest(String methodName, int result) {
        this.methodName = methodName;
        this.result = result;
    }

    @BeforeClass
    public static void classLoader() {
        loader = new RewriterClassLoader(SimpleTest.class.getClassLoader());
    }

    @Before
    public void loadClass() throws ClassNotFoundException {
        simple = loader.loadClass("Simple");
    }

    @Test
    public void checkResult()
        throws NoSuchMethodException
        , IllegalAccessException
        , InvocationTargetException {
        int res = (Integer) simple.getMethod(methodName).invoke(null);
        assertEquals(result, res);
        System.out.println(methodName + ": " + res);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"refSelf", 0},
                {"incResult", 2},
                {"refOther1", 1},
                {"refOther2", 1},
                {"refStatic", 101},
                {"notIncrement1", 1},
                {"notIncrement2", 1}
            });
    }
}
