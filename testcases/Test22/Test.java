class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a, b, x, y, z;
        a = new Node();
        b = new Node();
        a.f1 = new Node();
        x = new Node();
        y = new Node();
        if (a == b) {
            x = a.f1;
            z = a.f1; // Redundant
        } else {
            y = a.f1;
            z = a.f1; // Redundant
        }
        Node w = a.f1; // Redundant
        System.err.println("Test completed: "+ a + " " + b + " " + z + " " + x + " " + y + " " + w);
    }

}