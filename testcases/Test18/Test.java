class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        Node c = new Node();
        a.f1 = b;
        c.f1 = b;
        Node x, y;
        if(args.length > 0) {
            x = a.f1;
            y = x;
            x = c;
        } else {
            x = a.f1;
            y = x;
            x = c;
        }
        Node z = a.f1;    // Redundant (Can be replaced by y i.e r8)
        System.err.println("Test completed: "+ a + " " + b + " " + c + " " + x + " " + y + " " + z);
    }
}

