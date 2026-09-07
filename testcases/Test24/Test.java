class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        Node c = new Node();
        a.f1 = new Node();
        a.f2 = 5;

        b.f1 = new Node();
        b.f2 = 3;

        c.f1 = new Node();
        c.f2 = 5;

        b.f1 = c.f1;
        a.f2 = b.f2 + c.f2;
        int d = a.f2;

        System.err.println("Test completed " + c + " " + d);
    }
}