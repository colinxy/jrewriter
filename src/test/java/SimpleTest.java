
import java.lang.reflect.InvocationTargetException;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;


public class SimpleTest {
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
        , NoSuchMethodException
        , InvocationTargetException {

        Class<?> simple = loader.loadClass("Simple");
        Object instance = simple.newInstance();
        simple.getMethod("refSelf").invoke(instance);
    }

}
