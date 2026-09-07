import java.util.*;
import soot.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer {
    protected int precision;

    public AnalysisTransformer(int precision) {
        this.precision = precision;
    }

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        UnitGraph ug = new BriefUnitGraph(body);
        MustPointsToAnalysis analysis = new MustPointsToAnalysis(ug);

        System.out.println("Method: " + body.getMethod().getName());
        for (Unit u: body.getUnits()) {
            Set<Edge> in = analysis.getFlowBefore(u);
            Set<Edge> out = analysis.getFlowAfter(u);
            System.out.println("Unit: " + u + " | IN: " + in + " | OUT: " + out);
        }
    }
}

