class Node {
    Node f1;
    Node f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        a.f1 = new Node();
        a.f2 = new Node();
        b.f1 = new Node();
        b.f2 = new Node();

        Node c = b.f2;
        Node d = a.f1;

        a = b;

        Node e = a.f1;
        Node f = a.f2;
        System.err.println("Test completed " + c + " " + d + " " + e + " " + f);
    }
}
