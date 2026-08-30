
import java.util.*;
import soot.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import soot.Value;
import soot.ValueBox;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArrayFlowUniverse;
import soot.toolkits.scalar.ArrayPackedSet;
import soot.toolkits.scalar.BoundedFlowSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.FlowUniverse;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.jimple.FieldRef;

public class RedundantFieldEliminationAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Value>> {
    protected final Map<Unit, BoundedFlowSet<Value>> unitToKillSet;
    protected final Map<Unit, BoundedFlowSet<Value>> unitToGenerateSet;
    protected final FlowSet<Value> emptySet;

    public RedundantFieldEliminationAnalysis(DirectedGraph<Unit> dg) {
        super(dg);
        HashSet<Value> fieldRefs = new HashSet<>();
        /* we need a universe of all field references */
        final UnitGraph g = (UnitGraph) dg;
        for (Unit u : g.getBody().getUnits()) {
            // print all values of defs
            List<ValueBox> defs = u.getDefBoxes();
            for (ValueBox d : defs) {
                Value v = d.getValue();
                if (v instanceof FieldRef) {
                    FieldRef fieldRef = (FieldRef) v;
                    System.out.println("Unit: " + u);
                    System.out.println("FieldRef: " + fieldRef);
                }
            }
        }

        /* initialize the emptySet */
        FlowUniverse<Value> fieldRefsUniv = new ArrayFlowUniverse<Value>(fieldRefs.toArray(new Value[fieldRefs.size()]));
        this.emptySet = new ArrayPackedSet<Value>(fieldRefsUniv);
        /* create kill set */
        
        this.unitToKillSet = new HashMap<Unit, BoundedFlowSet<Value>>(g.size() * 2 + 1, 0.7f);
        this.unitToGenerateSet = new HashMap<Unit, BoundedFlowSet<Value>>(g.size() * 2 + 1, 0.7f);
        /* create gen set */
        // doAnalysis();
    }
    
    @Override
    protected FlowSet<Value> newInitialFlow() {
        BoundedFlowSet<Value> out = (BoundedFlowSet) emptySet.clone();
        out.complement(out);
        return out;
    }

    @Override
    protected FlowSet<Value> entryInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void flowThrough(FlowSet<Value> out, Unit u, FlowSet<Value> in) {
        // Perform kill
        in.intersection(unitToKillSet.get(u), out);
        // Perform gen
        out.union(unitToGenerateSet.get(u));
    }

    @Override
    protected void merge(FlowSet<Value> out1, FlowSet<Value> out2, FlowSet<Value> in) {
        out1.intersection(out2, in);
    }

    @Override
    protected void copy(FlowSet<Value> out, FlowSet<Value> in) {
        out.copy(in);
    }
}
