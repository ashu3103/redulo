class Node {
    Node f1;
    int f2;
    
    void foo() {
        Node x = this.f1;
    }
}

public class Test {
    public static void main(String[] args) {
        Node a, b, x;
        a = new Node();
        b = new Node();
        a.f1 = new Node();
        a.f1.f1 = new Node();
        Node y = a.f1; // 1st Store
        Node z = a.f1.f1; // Partially redundant
        z.foo();
        Node w = a.f1; // Redundant 
        x = a.f1.f1; // Redundant
        System.err.println("Test completed: "+ a + " " + b + " " + y + " " + z + " " + x + " " + w);
    }
}