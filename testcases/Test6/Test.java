class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        a.f1.f1 = new Node();
        a.f1.f1.f1 = new Node();
        a.f1.f1.f1.f1 = new Node();
        a.f1.f1.f1.f1.f1 = new Node();
        Node b;
        b = a.f1; 
        Node c;
        c = a.f1.f1; // Partialy redundant
        Node e = a.f1.f1; // Redundant
        System.err.println("Test completed: "+ b + " " + c + " " + e);
    }
}