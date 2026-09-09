import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

class RFEState {
    Map<HeapObject, Local> loadedMap = new HashMap<>();
    boolean top = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RFEState)) return false;

        RFEState other = (RFEState) o;

        return this.top == other.top
                && this.loadedMap.equals(other.loadedMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(top, loadedMap);
    }
}

public class RedundantFieldEliminationAnalysis extends ForwardFlowAnalysis<Unit, RFEState> {
    protected final MustPointsToAnalysis mpta;

    public RedundantFieldEliminationAnalysis(DirectedGraph<Unit> dg, MustPointsToAnalysis mpta) {
        super(dg);
        this.mpta = mpta;
        doAnalysis();
    }

    private boolean IsStackLocal(Value v) {
        if (v instanceof Local) {
            Local l = (Local) v;
            return l.isStackLocal();
        }
        return false;
    }

    @Override
    protected RFEState newInitialFlow() {
        RFEState rfeState = new RFEState();
        rfeState.top = true;
        return rfeState;
    }

    @Override
    protected RFEState entryInitialFlow() {
        RFEState rfeState = new RFEState();
        rfeState.top = false;
        return rfeState;
    }

    @Override
    protected void flowThrough(RFEState in, Unit u, RFEState out) {
        if (in.top) {
            out.top = true;
            out.loadedMap.clear();
            return;
        }

        copy(in, out);

        Set<Edge> edges = this.mpta.getFlowBefore(u).edges;

        Stmt stmt = (Stmt)u;
        boolean hasInvoke = stmt.containsInvokeExpr();
        if (hasInvoke) {
            InvokeExpr expr = stmt.getInvokeExpr();
            if (!mpta.IsConstructor(expr)) {
                out.loadedMap.clear();
            }
        } else if (stmt instanceof AssignStmt) {
            /* Field references */
            AssignStmt assign = (AssignStmt) stmt;
            Value lhs = assign.getLeftOp();
            Value rhs = assign.getRightOp();
            
            if (lhs instanceof Local && rhs instanceof NewExpr) {  // r = new ...
                if (!IsStackLocal(lhs)) {
                    for (Map.Entry<HeapObject, Local> entry: out.loadedMap.entrySet()) {
                        if (entry.getValue().equals(lhs)) {
                            out.loadedMap.remove(entry.getKey());
                        }
                    }
                }
            } else if (lhs instanceof Local && rhs instanceof InstanceFieldRef) {   // r1 = r2.f
                Local lhsLocal = (Local)lhs;
                InstanceFieldRef rhsFieldRef = (InstanceFieldRef)rhs;
                Local rhsBase = (Local) rhsFieldRef.getBase();
                SootField rhsField = rhsFieldRef.getField();
                HeapObject obj1 = mpta.objectPointedToByLocal(edges, rhsBase);
                HeapObject obj2 = mpta.objectPointedToByField(edges, obj1, rhsField);

                if (obj2 != null && !IsStackLocal(lhs)) {
                    out.loadedMap.put(obj2, lhsLocal);
                }
            } else if (lhs instanceof InstanceFieldRef && rhs instanceof Local) {  // r1.f = r2
                Local rhsLocal = (Local)rhs;
                InstanceFieldRef lhsFieldRef = (InstanceFieldRef)lhs;
                Local lhsBase = (Local)lhsFieldRef.getBase();
                SootField lhsField = lhsFieldRef.getField();

                HeapObject obj1 = mpta.objectPointedToByLocal(edges, rhsLocal);
                HeapObject obj2 = mpta.objectPointedToByLocal(edges, lhsBase);
                HeapObject obj3 = mpta.objectPointedToByField(edges, obj2, lhsField);

                Local loadedLocal = out.loadedMap.get(obj1);
                if (loadedLocal != null && obj3 != null) {
                    out.loadedMap.put(obj3, loadedLocal);
                } else if (obj3 != null) {
                    out.loadedMap.remove(obj3);
                }
            } else {
                // Do nothing
            }
        } else {
            // do nothing
        }
    }

    @Override
    protected void merge(RFEState in1, RFEState in2, RFEState out) {
        if (in1.top) {
            copy(in2, out);
            return;
        }

        if (in2.top) {
            copy(in1, out);
            return;
        }

        out.top = false;
        out.loadedMap.clear();

        for (Map.Entry<HeapObject, Local> entry : in1.loadedMap.entrySet()) {
            HeapObject object = entry.getKey();
            Local local1 = entry.getValue();
            Local local2 = in2.loadedMap.get(object);

            if (local2 != null && local1.equivTo(local2)) {
                out.loadedMap.put(object, local1);
            }
        }
    }

    @Override
    protected void copy(RFEState in, RFEState out) {
        out.top = in.top;
        out.loadedMap.clear();
        out.loadedMap.putAll(in.loadedMap);
    }
}
