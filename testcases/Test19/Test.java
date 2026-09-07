class Node {
    Node f1;
    Node f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        Node c = new Node();
        Node d = new Node();
        a.f1 = b;
        b.f2 = c;
        c.f1 = d;
        Node x = a.f1;        // b
        Node y = x.f2;        // c
        Node z = y.f1;        // d
        Node t = a.f1;        // Redundant
        Node u = t.f2;        // Redundant
        Node v = u.f1;        // Redundant
        System.err.println("Test completed: "+ a + " " + b + " " + c + " " + d + " " + x + " " + y + " " + z + " " + t + " " + u + " " + v);
    }
}