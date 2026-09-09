import java.util.*;
import soot.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer {
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        UnitGraph ug = new BriefUnitGraph(body);
        MustPointsToAnalysis analysis = new MustPointsToAnalysis(ug);
        RedundantFieldEliminationAnalysisV2 analysisV2 = new RedundantFieldEliminationAnalysisV2();
        List<RedundantFields> redundantFields = analysisV2.findRedundantFields(ug, analysis);

        for (RedundantFields rf: redundantFields) {
            System.out.println(rf);
        }
    }
}

