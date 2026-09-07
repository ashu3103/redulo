class Node {
    Node f1;
    int f2;

    void foo(Node p1) {
        p1.f1 = new Node(); 
    }
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        a.f1.f1 = new Node();
        a.f1.f1.f1 = new Node();
        a.f1.f1.f1.f1 = new Node();
        a.f1.f1.f1.f1.f1 = new Node();
        Node b = new Node();
        b.f1 = new Node();
        b.f1.f1 = new Node();
        b.f1.f1.f1 = new Node();
        b.f1.f1.f1.f1 = new Node();
        b.f1.f1.f1.f1.f1 = new Node();
        Node c;
        c = a.f1.f1.f1; 
        Node d = b.f1.f1.f1.f1.f1; 
        c.foo(d);
        Node e = a.f1.f1;
        Node f = b.f1.f1.f1;
        Node g = a.f1.f1; // Redundant
        Node h = b.f1.f1;
        Node i = b.f1.f1.f1; // Redundant
        System.err.println("Test completed: "+ f + " " + g + " " + h + " " + i + " " + e);
    }

}