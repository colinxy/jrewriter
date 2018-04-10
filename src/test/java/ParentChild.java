
public class ParentChild {

    public static int testParentField() {
        Parent p = new Child();
        p.parentField++;
        return p.parentField;
    }

    public static int testNotsameFieldParent() {
        Parent p = new Child();
        p.notsameField++;
        return p.notsameField;
    }

    public static int testNotsameFieldChild() {
        Child c = new Child();
        c.notsameField++;
        return c.notsameField;
    }
}
