
public class Simple {
    int field = 0;

    public int refSelf() {
        int x = field + 1;
        field = x;
        return x;
    }

    public static int refOther1() {
        Simple s = new Simple();
        int x = s.field + 1;
        s.field = x;
        return x;
    }

    public static int refOther2() {
        Holder h = new Holder();
        int x = h.field + 1;
        h.field = x;
        return x;
    }
}

class Holder {
    int field = 0;
}
