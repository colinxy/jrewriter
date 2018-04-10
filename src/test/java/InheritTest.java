
import java.lang.reflect.*;
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
public class InheritTest {
    static RewriterClassLoader loader;
    Class<?> childClass;
    Class<?> parentClass;
    Class<?> testerClass;

    Object childInstance;
    Object parentInstance;

    Field childField;
    Field parentField;
    Field notsameFieldChild;
    Field notsameFieldParent;

    private String methodName;
    private int result;

    public InheritTest(String methodName, int result) {
        this.methodName = methodName;
        this.result = result;
    }

    @BeforeClass
    public static void classLoader() {
        loader = new RewriterClassLoader(InheritTest.class.getClassLoader());
    }

    @Before
    public void loadClass()
        throws ClassNotFoundException
        , InstantiationException
        , IllegalAccessException
        , NoSuchFieldException {

        childClass = loader.loadClass("Child");
        parentClass = loader.loadClass("Parent");
        testerClass = loader.loadClass("ParentChild");

        childInstance = childClass.newInstance();
        parentInstance = parentClass.newInstance();

        childField = childClass.getDeclaredField("childField");
        parentField = parentClass.getDeclaredField("parentField");
        notsameFieldChild = childClass.getDeclaredField("notsameField");
        notsameFieldParent = parentClass.getDeclaredField("notsameField");
    }

    @Test
    public void checkResult()
        throws NoSuchMethodException
        , IllegalAccessException
        , InvocationTargetException {
        int res = (Integer) testerClass.getMethod(methodName).invoke(null);
        assertEquals(result, res);
        System.out.println(methodName + ": " + res);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"testParentField", 2},
                {"testNotsameFieldParent", 12346},
                {"testNotsameFieldChild", 54322},
            });
    }
}
