
import java.lang.reflect.InvocationTargetException;
import jrewriter.RewriterClassLoader;
import org.junit.Test;


public class MainTest {
    @Test()
    public void test()
        throws ClassNotFoundException
        , InstantiationException
        , IllegalAccessException
        , NoSuchMethodException
        , InvocationTargetException {

        RewriterClassLoader loader =
            new RewriterClassLoader(MainTest.class.getClassLoader());
        Class<?> simple = loader.loadClass("Simple");
        Object instance = simple.newInstance();
        simple.getMethod("refSelf").invoke(instance);
    }
}
