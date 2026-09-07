class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        a.f1.f1 = new Node();
        a.f1.f2 = 10;
        Node x = a.f1.f1;
        a.f1.f2 = 20;
        Node z = a.f1.f1; // Redundant
        Test b = new Test();
        b.foo();
        b.bar();
        System.err.println("Test completed: " + a + " " + x + " " + z);
    }
    void foo() {
        Node y = new Node();
        Node z;
        y.f1 = new Node();
        z = y.f1;
        Node d = y.f1;  // Redundant
        System.err.println("Test completed: "+ y + " " + z + " " + d);
    }

    void bar() {
        Node d = new Node();
        d.f2 = 10;
        int z = d.f2;
        int e = d.f2;  // Redundant
        System.err.println("Test completed: "+ " " + d + " " + z + " " + e);
    }
}