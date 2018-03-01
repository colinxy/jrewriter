
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

        System.out.println("\n--> Test output");

        int x1 = (Integer) simple.getMethod("refSelf").invoke(instance);
        System.out.println("refSelf: " + x1);

        int x2 = (Integer) simple.getMethod("refOther1").invoke(null);
        System.out.println("refOther1: " + x2);
    }

}