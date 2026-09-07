class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a, b, x;
        a = new Node();
        b = new Node();
        a.f1 = new Node();
        if (a == b) {
            x = a.f1;
        } else {
            x = a.f1;
        }
        Node z = a.f1;
        System.err.println("Test completed: "+ a + " " + b + " " + z);
    }

}