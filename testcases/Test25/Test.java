class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        a.f1 = new Node();
        b.f1 = new Node();
        a.f2 = 10;
        b.f2 = 20;

        if (a.f2 > 0) {
            a = b;
        } else {
            a.f1 = new Node();
        }

        Node c = a.f1;
        Node d = a.f1;
        System.err.println("Test completed: " + c + " " + d);
    }
}
