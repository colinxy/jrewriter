
import java.lang.reflect.*;
import jrewriter.RewriterClassLoader;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;


public class InheritTest {
    static RewriterClassLoader loader;
    Class<?> childClass;
    Class<?> parentClass;
    Object childInstance;
    Object parentInstance;

    @BeforeClass
    public static void classLoader() {
        loader = new RewriterClassLoader(InheritTest.class.getClassLoader());
    }

    @Before
    public void loadClass()
        throws ClassNotFoundException
        , InstantiationException
        , IllegalAccessException {

        childClass = loader.loadClass("Child");
        parentClass = loader.loadClass("Parent");
        childInstance = childClass.newInstance();
        parentInstance = parentClass.newInstance();
    }

    @Test
    public void test()
        throws NoSuchFieldException
        , IllegalAccessException {

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
