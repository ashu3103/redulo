import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;

class RedundantFields {
    Unit u;
    InstanceFieldRef fieldRef;
    Local local;

    public RedundantFields(Unit u, InstanceFieldRef v, Local l) {
        this.u = u;
        this.fieldRef = v;
        this.local =  l;
    }

    @Override
    public String toString() {
        LineNumberTag ln = (LineNumberTag)u.getTag("LineNumberTag");
        return ln.getLineNumber() + ":" + fieldRef.toString() + " " + local.toString();
    }
}

public class RedundantFieldEliminationAnalysisV2 {

    protected final Map<HeapObject, Local> loadedObjctToLocalMap;
    
    private boolean IsStackLocal(Value v) {
        if (v instanceof Local) {
            Local l = (Local) v;
            return l.isStackLocal();
        }
        return false;
    }

    public RedundantFieldEliminationAnalysisV2() {
        loadedObjctToLocalMap = new HashMap<>();
    }

    public List<RedundantFields> findRedundantFields(DirectedGraph<Unit> dg, MustPointsToAnalysis mpta) {
        List<RedundantFields> redFs = new ArrayList<>();

        UnitGraph g = (UnitGraph)dg;
        for (Unit u: g.getBody().getUnits()) {
            // get the must points to graph at this program point
            Set<Edge> edges = mpta.getFlowBefore(u);

            Stmt stmt = (Stmt)u;
            boolean hasInvoke = stmt.containsInvokeExpr();

            if (hasInvoke && !mpta.IsConstructor((InvokeStmt)stmt)) {
                this.loadedObjctToLocalMap.clear();
            } else if (stmt instanceof AssignStmt) {
                /* Field references */
                AssignStmt assign = (AssignStmt) stmt;
                Value lhs = assign.getLeftOp();
                Value rhs = assign.getRightOp();
                
                if (lhs instanceof Local && rhs instanceof NewExpr) {
                    if (!IsStackLocal(lhs)) {
                        for (Map.Entry<HeapObject, Local> entry: this.loadedObjctToLocalMap.entrySet()) {
                            if (entry.getValue().equals(lhs)) {
                                this.loadedObjctToLocalMap.remove(entry.getKey());
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

                    Local loadedLocal = this.loadedObjctToLocalMap.get(obj2);
                    if (loadedLocal != null) { // check if object is already loaded and mark it redundant
                        redFs.add(new RedundantFields(u, rhsFieldRef, loadedLocal));
                    }

                    if (!IsStackLocal(lhs)) {
                        this.loadedObjctToLocalMap.put(obj2, lhsLocal);
                    }
                } else if (lhs instanceof InstanceFieldRef && rhs instanceof Local) {  // r1.f = r2
                    Local rhsLocal = (Local)rhs;
                    InstanceFieldRef lhsFieldRef = (InstanceFieldRef)lhs;
                    Local lhsBase = (Local)lhsFieldRef.getBase();
                    SootField lhsField = lhsFieldRef.getField();

                    HeapObject obj1 = mpta.objectPointedToByLocal(edges, rhsLocal);
                    HeapObject obj2 = mpta.objectPointedToByLocal(edges, lhsBase);
                    HeapObject obj3 = mpta.objectPointedToByField(edges, obj2, lhsField);

                    Local loadedLocal = this.loadedObjctToLocalMap.get(obj1);
                    if (loadedLocal != null && obj3 != null) {
                        this.loadedObjctToLocalMap.put(obj3, loadedLocal);
                    } else if (obj3 != null) {
                        this.loadedObjctToLocalMap.remove(obj3);
                    }
                } else {
                    // Do nothing
                }
            } else {
                // do nothing
            }
        }
        
        return redFs;
    }
}
