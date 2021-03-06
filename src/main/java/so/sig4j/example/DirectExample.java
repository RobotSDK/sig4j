package so.sig4j.example;

import so.sig4j.ConnectionType;
import so.sig4j.signal.Signal0;

public class DirectExample {

    public static void main(final String[] args) {
        final A a = new A();
        final B b0 = new B();
        final B b1 = new B();

        a.signal.connect(B::staticPrint);
        a.signal.connect(b0::dynamicPrint);
        a.signal.connect(b1::dynamicPrint, ConnectionType.DIRECT);
        a.signal.connect(() -> System.out.println("This is a Lambda"));
        a.signal.emit();
    }


    private static class A {
        public final Signal0 signal = new Signal0();
    }

    private static class B {
        private static int N = 0;
        private final int n = N++;

        public static void staticPrint() {
            System.out.println("This is a static function of B");
        }

        public void dynamicPrint() {
            System.out.println("This is B with number: " + n);
        }
    }
}
