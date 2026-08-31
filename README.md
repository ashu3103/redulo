# redulo

An intraprocedural analysis to track redundant loads in java bytecode using Soot framework

## Motivation

By default, Java allocates objects on the heap, while local variables on the stack hold references to those objects. Consequently, accessing an object involves an additional level of indirection. As the level of indirection increases, the associated overhead can become more significant. For example, consider a class with a self-referential field:

```
class Node {
    int data;
    Node next;
}
```

To access the `data` field of a node referenced indirectly through another node, multiple pointer dereferences are required: first from the stack variable to the head node, then from the head node to the target node through `next`, and finally from the target node to its `data` field.

These additional indirections can negatively affect performance and increase memory-access latency, particularly when the referenced objects are scattered across the heap. Such accesses can result in cache misses because each dereference may require fetching data from a different memory location. Moreover, fetching these scattered objects into the cache can evict other data that may be needed shortly afterward, further reducing cache efficiency. Also from the perspective of static analysis, redundant loads also reduce analysis precision by introducing extra dependencies and statements that do not contribute new information. Eliminating such redundancies is therefore beneficial both for optimization and for improving the clarity and effectiveness of program analyses.

Sample Program:

```
1 class Node {
2   Node f1;
3   Node f2;
4   Node g;
5   Node() {}
6 }
7
8 public class Test {
9   public static void main(String[] args) {
10      Node a = new Node(); // O10
11      a.f1 = new Node(); // O11
12      Node b = new Node(); // O12
13      b.f1 = new Node(); // O13
14      a.f2 = new Node(); // O14
15      Node c = a.f1;
16      a.f2 = a.f1; // Redundant
17      b.f1 = a.f2; // Redundant
18  }
19 }
```

In the above program, the loads at line 16 (`a.f1`) and 17 (`a.f2`) are redundant since these objects have already been loaded in line 11 and 14 respectively and never been rewritten since then.

## Methodology

// TODO

## Installation and Build

This is tested only for openjdk 11 given the soot version (4.6.0) in the library so make sure your system has openjdk version 11 installed before following the below steps:

```
git clone https://github.com/ashu3103/redulo
make run PRECISION_LVL=1 CLASS_NAME=Test1
```

`PRECISION_LVL` should be 1 (no better optimization levels are supported currently)
`CLASS_NAME` should be the name of any one of the subdirectory in testcases.


## Adding testcases

The new testcase directory must be created inside the `testcases/` project subdirectory. The directory name and the name of the main Java class file must be identical, and the class must contain a `main` entrypoint method.

Follow these steps as a reference:

```
cd testcases/
mkdir Test3
cd Test3
touch Test3.java
cat > Test3.java <<'EOF'
public class Test3 {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
EOF
```

In this example, the directory is named `Test3`, the Java file is `Test3.java`, and the file contains the `Test3` class with a main method.

