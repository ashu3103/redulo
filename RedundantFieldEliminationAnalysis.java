
import java.util.*;
import soot.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArrayFlowUniverse;
import soot.toolkits.scalar.ArrayPackedSet;
import soot.toolkits.scalar.BoundedFlowSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.FlowUniverse;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.SootField;

public class RedundantFieldEliminationAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<RedundantFieldEliminationAnalysis.FieldRefKey>> {
    /** Identifies a specific "base.field" reference for caching purposes. */
    public static final class FieldRefKey {
        final Value base;
        final SootField field;
 
        FieldRefKey(Value base, SootField field) {
            this.base = base;
            this.field = field;
        }
 
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FieldRefKey)) {
                return false;
            }
            FieldRefKey other = (FieldRefKey) o;
            return base.equivTo(other.base) && field.equals(other.field);
        }

        @Override
        public int hashCode() {
            // We can't derive a hash consistent with equivTo() for an
            // arbitrary Value, so we only hash on the field. This is still
            // correct (equal objects hash equal), just coarser-grained.
            return 31 * field.hashCode();
        }
 
        @Override
        public String toString() {
            return base + "." + field.getSignature();
        }

    }

    protected final Map<Unit, FlowSet<FieldRefKey>> unitToKillSet;
    protected final Map<Unit, FlowSet<FieldRefKey>> unitToGenerateSet;
    protected final FlowSet<FieldRefKey> emptySet;
    private final List<FieldRefKey> universe;


    public RedundantFieldEliminationAnalysis(DirectedGraph<Unit> dg) {
        super(dg);
        // Universe of all field references that appear anywhere in the body.
        LinkedHashSet<FieldRefKey> fieldRefs = new LinkedHashSet<>();
        /* we need a universe of all field references */
        final UnitGraph g = (UnitGraph) dg;
        for (Unit u : g.getBody().getUnits()) {
            for (ValueBox box : u.getUseAndDefBoxes()) {
                Value v = box.getValue();
                if (v instanceof InstanceFieldRef) {
                    InstanceFieldRef ref = (InstanceFieldRef) v;
                    fieldRefs.add(new FieldRefKey(ref.getBase(), ref.getField()));
                }
            }
        }

        /* initialize the emptySet */
        this.universe = new ArrayList<>(fieldRefs);
        FlowUniverse<FieldRefKey> fieldRefsUniv =
                new ArrayFlowUniverse<>(universe.toArray(new FieldRefKey[0]));
        this.emptySet = new ArrayPackedSet<>(fieldRefsUniv);

        this.unitToKillSet = new HashMap<>(g.size() * 2 + 1, 0.7f);
        this.unitToGenerateSet = new HashMap<>(g.size() * 2 + 1, 0.7f);

        /* create gen set */
        // doAnalysis();
    }
    
    @Override
    protected FlowSet<FieldRefKey> newInitialFlow() {
        BoundedFlowSet<FieldRefKey> out = (BoundedFlowSet) emptySet.clone();
        out.complement(out);
        return out;
    }

    @Override
    protected FlowSet<FieldRefKey> entryInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void flowThrough(FlowSet<FieldRefKey> out, Unit u, FlowSet<FieldRefKey> in) {
        // Perform kill
        in.intersection(unitToKillSet.get(u), out);
        // Perform gen
        out.union(unitToGenerateSet.get(u));
    }

    @Override
    protected void merge(FlowSet<FieldRefKey> out1, FlowSet<FieldRefKey> out2, FlowSet<FieldRefKey> in) {
        out1.intersection(out2, in);
    }

    @Override
    protected void copy(FlowSet<FieldRefKey> out, FlowSet<FieldRefKey> in) {
        out.copy(in);
    }
}
