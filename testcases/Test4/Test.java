class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        a.f2 = args.length;;
        new Test().foo(a.f2);
        Node b = a.f1;
        int c = a.f2;
        Node e = a.f1; // Redundant
        System.err.println("Test completed: "+ b + " " + c + " " + e);
    }

    void foo(int p1) {
        int x;
        Node y = new Node();
        Node z = new Node();
        y.f1 = new Node();
        y.f2 = 10;
        z.f1 = new Node();
        y = z.f1;
        z.f2 = 20;
        x = z.f2;
        System.err.println("Test completed: "+ y + " " + z);
    }
}
