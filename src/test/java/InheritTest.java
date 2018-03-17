
import java.lang.reflect.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;


public class InheritTest {
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
        , NoSuchFieldException {

        // make sure Child and Parent are already loaded
        Class<?> childClass = loader.loadClass("Child");
        Class<?> parentClass = loader.loadClass("Parent");
        Object childInstance;
        Object _parentInstance;
        try {
            childInstance = childClass.newInstance();
            _parentInstance = parentClass.newInstance();
        } catch (Error err) {
            err.printStackTrace();
            throw new Error(err);
        }

        System.out.println("\n--> Test output");

        Field childField = childClass.getDeclaredField("childField");
        Field parentField = parentClass.getDeclaredField("parentField");
        Field notsameFieldChild = childClass.getDeclaredField("notsameField");
        Field notsameFieldParent = parentClass.getDeclaredField("notsameField");

        int childFieldVal = (Integer) childField.get(childInstance);
        System.out.println("Child#childField = " + childFieldVal);
        int parentFieldVal = (Integer) parentField.get(childInstance);
        System.out.println("Parent#parentField = " + parentFieldVal);

        int notsameFieldChildVal = (Integer) notsameFieldChild.get(childInstance);
        System.out.println("Child#notsameField = " + notsameFieldChildVal);
        int notsameFieldParentVal = (Integer) notsameFieldParent.get(childInstance);
        System.out.println("Parent#notsameField = " + notsameFieldParentVal);
    }
}
