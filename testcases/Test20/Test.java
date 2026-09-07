class Node {
    Node f1;
    int f2;
    
    void foo() {
        Node x = this.f1;
    }
}

public class Test {
    public static void main(String[] args) {
        Node a, b, c, d, e, f, g, h, i;
        a = new Node();
        b = new Node();
        c = new Node();
        d = new Node();
        a.f1 = b;
        b.f1 = c;
        c.f1 = d;
        d.f1 = a; // Cycle [a.f -> b.f -> c.f -> d.f -> a.f]
        e = a.f1;
        f = b.f1;
        g = e.f1; 
        System.err.println("Test completed: "+ a + " " + b + " " + c + " " + d + " " + e + " " + f);    
    }
}