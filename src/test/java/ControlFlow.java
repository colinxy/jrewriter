
import java.util.Random;


public class ControlFlow {
    int field = 0;
    static int staticField = 0;
    final static Random rand = new Random();

    public static int endOfBranch() {
        ControlFlow cf = new ControlFlow();
        int x = rand.nextInt(1000);
        if (x < 100) {
            System.out.println("x < 100");;
        } else {
            System.out.println("x >= 100");
            cf.field++;
        }
        return 12345;
    }

    public static int endOfBranchStatic() {
        int x = rand.nextInt(1000);
        if (x < 100) {
            System.out.println("x < 100");;
        } else {
            System.out.println("x >= 100");
            staticField++;
        }
        return 12345;
    }
}
