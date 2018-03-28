
public class Simple {
    int field = 0;
    static int staticField = 100;
    final int immutable;

    static {
        System.out.println("Hello from static initializer");
    } {
        System.out.println("Hello from initializer");
        immutable = 100;
    }

    public int refSelf() {
        int x = field;
        field++;
        field += 1;
        field += -1;
        field--;
        System.out.println("before: " + x + "; after: " + field);
        return x;
    }

    public static int refOther1() {
        Simple s = new Simple();
        int x = s.field;
        s.field++;
        System.out.println("before: " + x + "; after: " + s.field);
        return x;
    }

    public static int refOther2() {
        Holder h = new Holder();
        int x = h.fieldOfHolder;
        h.fieldOfHolder++;
        System.out.println("before: " + x + "; after: " + h.fieldOfHolder);
        return x;
    }

    public static int refStatic() {
        int x = staticField;
        staticField++;
        System.out.println("before: " + x + "; after: " + staticField);
        return x;
    }
}
