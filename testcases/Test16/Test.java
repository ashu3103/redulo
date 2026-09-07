class Node {
    Node f1;
    int f2;
    
    void foo() {
        Node x = this.f1;
    }
}

public class Test {
    public static void main(String[] args) {
        Node a, x, y, z;
        int c, d;
        a = new Node();
        a.f1 = new Node();
        a.f1.f2 = 10;
        a.f1.f1 = new Node();
        x = a.f1;
        y = a.f1.f1; // Partially Redundant
        c = a.f1.f2; // Partially Redundant
        a.f1.f2 = 20; // Partially Redundant
        z = a.f1.f1; // Redundant
        d = a.f1.f2; // Partially Redundant 
        System.err.println("Test completed: "+ a + " " + z + " " + x + " " + d);
    }
}