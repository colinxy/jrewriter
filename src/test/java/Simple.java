
public class Simple {
    int field = 0;

    public int refSelf() {
        int x = field + 1;
        return x;
    }

    public static int refOther1() {
        int x = new Simple().field + 1;
        return x;
    }

    public static int refOther2() {
        int x = new Holder().field + 1;
        return x;
    }
}

class Holder {
    int field = 0;
}
