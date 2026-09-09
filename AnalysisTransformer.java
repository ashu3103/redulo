import java.util.*;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer {
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        UnitGraph ug = new BriefUnitGraph(body);
        MustPointsToAnalysis mpta = new MustPointsToAnalysis(ug);
        RedundantFieldEliminationAnalysis rdloa = new RedundantFieldEliminationAnalysis(ug, mpta);

        String res = "";
        for (Unit u: body.getUnits())  {
            Set<Edge> edges = mpta.getFlowBefore(u).edges;
            Map<HeapObject, Local> loaded = rdloa.getFlowBefore(u).loadedMap;

            // cases by statements
            Stmt stmt = (Stmt)u;
            if (stmt.containsInvokeExpr())
                continue;

            for (ValueBox box: u.getUseBoxes()) {
                Value value = box.getValue();

                if (value instanceof InstanceFieldRef) {
                    InstanceFieldRef ref = (InstanceFieldRef)value;
                    Local base = (Local)ref.getBase();
                    SootField field = ref.getField();
                    HeapObject obj1 = mpta.objectPointedToByLocal(edges, base);
                    HeapObject obj2 = mpta.objectPointedToByField(edges, obj1, field);

                    if (obj2 == null)
                        continue;

                    Local previousLoaded = loaded.get(obj2);
                    if (previousLoaded != null) {
                        LineNumberTag ln = (LineNumberTag)u.getTag("LineNumberTag");
                        res = res + ln + ":" + ref + " " + previousLoaded + '\n';
                    }
                }
            }
        }

        if (res != "") {
            System.out.println(body.getMethod().getName());
            System.out.println(res);
        }
    }
}

