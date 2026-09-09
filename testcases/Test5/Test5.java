class Node {
    Node f1;
    int f2;
}

public class Test5 {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        a.f1.f1 = new Node();
        Node b = a.f1;
        Node e = a.f1.f1; // (Partial Redundant)
        System.err.println("Test5 completed: "+ a + " " + b + " " + e);
    }

}