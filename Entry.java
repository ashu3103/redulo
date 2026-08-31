import soot.*;
import soot.options.Options;

public class Entry {
    public static void main(String[] args) {
        // Check if the required number of arguments provided
        if (args.length < 2) {
            System.out.println("Usage: java [options] Entry [precision] [class-name]");
            System.exit(1);
        }
        
        String classPath = "./testcases/" + args[1];
    
        // Set up arguments for Soot
        String[] sootArgs = {
            "-cp", classPath,
            "-pp",  // sets the class path for Soot
            "-f", "J",
            "-t", "1",
            "-main-class", args[1], // specify the main class
            "-process-dir", classPath
        };

        // create transformer for analysis
        AnalysisTransformer analysisTransformer = new AnalysisTransformer(Integer.parseInt(args[0]));
        // Add transformer to the appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        PackManager.v().getPack("jtp").add(new Transform("jtp.dfa", analysisTransformer));
        // Set Soot options
        Options.v().set_keep_line_number(true);
        // Call Soot's main method with arguments
        soot.Main.main(sootArgs);
    }
}
