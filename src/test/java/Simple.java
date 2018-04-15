
public class Simple {
    int field = 0;
    int anotherField = 123;
    static int staticField = 100;
    final int immutable;

    final static int staticImmutable;

    static {
        System.out.println("Hello from static initializer");
        staticImmutable = 1000;
    } {
        System.out.println("Hello from initializer");
        immutable = 100;
    }

    public static int staticField() {
        return staticImmutable;
    }

    public static int refSelf() {
        Simple s = new Simple();
        int x = s.field;
        s.field++;
        s.field += 1;
        s.field += -1;
        s.field--;
        System.out.println("before: " + x + "; after: " + s.field);
        return s.field;
    }

    public static int incResult() {
        Simple s = new Simple();
        // TODO: support increment result
        int postfix = s.field++;
        int prefix = ++s.field;
        System.out.println("field++: " + postfix + "; ++field: " + prefix);
        return prefix - postfix;
    }

    public static int refOther1() {
        Simple s = new Simple();
        int x = s.field;
        s.field++;
        System.out.println("before: " + x + "; after: " + s.field);
        return s.field;
    }

    public static int refOther2() {
        Holder h = new Holder();
        int x = h.fieldOfHolder;
        h.fieldOfHolder++;
        System.out.println("before: " + x + "; after: " + h.fieldOfHolder);
        return h.fieldOfHolder;
    }

    public static int refStatic() {
        int x = staticField;
        staticField++;
        System.out.println("before: " + x + "; after: " + staticField);
        return staticField;
    }

    public static int notIncrement1() {
        Simple s = new Simple();
        s.anotherField = s.field + 1;
        System.out.println("anotherField: " + s.anotherField +
                           "; field: " + s.field);
        return s.anotherField;
    }

    public static int notIncrement2() {
        Simple s1 = new Simple();
        Simple s2 = new Simple();
        s2.field = s1.field + 1;
        System.out.println("s1.field: " + s1.field +
                           "; s2.field: " + s2.field);
        return s2.field;
    }
}
