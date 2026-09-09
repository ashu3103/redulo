class Node {
    Node f1;
    int f2;
}

public class Test12 {
    public static void main(String[] args) {
        Node a = new Node();        
        a.f1 = new Node();
        Node b = a.f1;    // 1st Store
        if(args.length > 0) {
            a.f1 = new Node();
        }
        Node c = a.f1;
        System.err.println("Test12 completed: "+ a + " " + b + " " + c);
    }
}
