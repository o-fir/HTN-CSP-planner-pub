package pddlparser.csp_clean;

import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.parser.Message;
import fr.uga.pddl4j.parser.PDDLParser;
import fr.uga.pddl4j.parser.ParsedProblem;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Method;
import fr.uga.pddl4j.util.BitVector;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

    final class MyOutput {
        private final boolean isSAT;
        private final int solveTime;
    
        public MyOutput(boolean isSAT, int solveTime) {
            this.isSAT = isSAT;
            this.solveTime = solveTime;
        }
    
        public boolean getSAT() {
            return isSAT;
        }
    
        public int getSolveTime() {
            return solveTime;
        }
    }


public class CoreMain {

    

    /**
     * The main method the class. The first argument must be the path to the PDDL
     * domain description and the second
     * argument the path to the PDDL problem description.
     *
     * @param args the command line arguments.
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {

        // Checks the number of arguments from the command line
        /*
         * if (args.length != 2) {
         * System.out.println("Invalid command line");
         * return;
         * }
         */

        ///////////////////////////////////////////////
        //CONFIG
        //TODO;
        
        //"Disables" SAS+ encoding - facts are still encoded as numeric variables, but we assume they're all non-mutex
        boolean skipSAS = true;
        //Enables comments in .mzn. PRO - .mzn becomes somewhat interpretable, CON - .mzn file is a lot larger
        boolean includeDebugCommentaryInMinizinc = false;

        
        //variables that store execution times for debugging
        //total time of pipeline execution - from PDDL4J parsing to minizinc plan
        long durationSystemTotal;
        long startSystemTotal; 
        //time spent on PDDL4J - from parsing file to instantiating
        long durationPDDL4J;
        long startPDDL4J;
        //total time spent on SAS+
        long durationSASTotal;
        long startSASTotal;
        
        //time spent on encoding
        long durationEncodingTotal;
        long startEncodingTotal;
        long endEncodingTotal;
        
        //time spent on rules
        long durationDecomposeLayer = 0;
        long durationCellDomain = 0;
        long durationLayerFacts = 0;
        long durationRule1 = 0;
        long durationRule4 = 0;
        long durationRule5 = 0;
        long durationRule6 = 0;
        long durationRule8 = 0;
        long durationRule10 = 0;
        long durationRule11 = 0;
        long durationRule12 = 0;
        long durationRules13_15 = 0;
        long durationFileCopy = 0;
        long startTmp;


        //time spent only by minizinc to read problem, solve it, and write the solution
        long durationMinizincTotal = 0;
        long durationSolveTime = 0;


        ///////////////////////////////////////////////
        // LOAD PROBLEM FILES
        String benchmarkPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/"; <------------- CHANGE
        String outputPath = "C:/Users/oleksandr.firsov/Desktop/"; <------------ CHANGE
        //TRANSPORT
        String domainPath = benchmarkPath+"transport/domain.hddl";
        String problemPath = benchmarkPath+"transport/p01.hddl";
        //CHILDSNACK
        //String domainPath = benchmarkPath+"childsnack/domain.hddl";
        //String problemPath = benchmarkPath+"childsnack/p01.hddl";
        //BASSETTI NONTEMPORAL
        //String domainPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_nontemporal/domain.hddl";
        //String problemPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_nontemporal/p04.hddl";
        //BASSETTI NONTEMPORAL
        //String domainPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_nontemporal/domain.hddl";
        //String problemPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_nontemporal/p04.hddl";
        //BASSETTI NONTEMPORAL JAVASCRIPT (Teexma OptiPlan server)
        /*String domainPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_javascript/domain.hddl";
        String problemPath = "C:/txdev-tx-annex/OptiPlan/pddlParser/app/src/main/resources/benchmarks/bassetti_javascript/p02_gen.hddl";*/
        String minizincProblemPath = outputPath+"treeRex_javasideLayers_mzn.mzn";
        String minizincProblem_withRule16 = outputPath+"tmpD.mzn";
        String solutionPath = outputPath+"output.txt";


        
        startSystemTotal = System.nanoTime();
        startPDDL4J = System.nanoTime();
        try {

            // Creates an instance of the PDDL parser
            final PDDLParser parser = new PDDLParser();
            // Parses the domain and the problem files.
            final ParsedProblem parsedProblem = parser.parse(domainPath, problemPath);
            // Gets the error manager of the parser
            final ErrorManager errorManager = parser.getErrorManager();
            // Checks if the error manager contains errors
            if (!errorManager.isEmpty()) {
                // Prints the errors
                for (Message m : errorManager.getMessages()) {
                    System.out.println(m.toString());
                }
            } else {
                /*
                 * // Prints that the domain and the problem were successfully parsed
                 * System.out.print("\nparsing domain file \"" + args[0] +
                 * "\" done successfully");
                 * System.out.print("\nparsing problem file \"" + args[1] +
                 * "\" done successfully\n\n");
                 * // Print domain and the problem parsed
                 * System.out.println(parsedProblem.toString());
                 */
                ///////////////////////////////////////////////
                // STEP 2 - GROUND THE PROBLEM
                final HTNProblem problem = new HTNProblem(parsedProblem);
                System.out.println("Beginning search...");
                // Instantiate the planning problem
                problem.instantiate();
                durationPDDL4J = System.nanoTime() - startPDDL4J;
                System.out.println("problem instantiated");
                // Print the list of actions of the instantiated problem
                
                System.out.println("methods num: " + problem.getMethods().size());
                System.out.println("actions num: " + problem.getActions().size());
                System.out.println("tasks num: " + problem.getTasks().size());
                System.out.println("fluent num: " + problem.getFluents().size());
                System.out.println("--------------------------");
                /*Scanner userInput2 = new Scanner(System.in);
                String input2;
                System.out.println("solve? (Press Enter)"); 
                input2 = userInput2.nextLine();*/


                ///////////////////////////////////////////////
                // STEP 3 - STRIPS TO SAS VARIABLES (MUTEX FACTS AS A SINGLE VARIABLE)
                if (skipSAS) {
                    System.out.println("SAS skipped. Creating mutex clique per each fact...");
                    Strips2SasPlus.cliquePerFact(problem);
                    durationSASTotal = 0;

                } else {
                    startSASTotal = System.nanoTime();
                    System.out.println("calling H2 heuristic...");
                    Strips2SasPlus.callH2Hheuristic(problem);
                    System.out.println("H2 heuristic done");
                    System.out.println("creating mutex fact sets...");
                    Strips2SasPlus.createFactSets(problem);
                    System.out.println("fact sets generated. Timeout at "+ Strips2SasPlus.timeout + "ms");
                    System.out.println("greedy covering...");
                    Strips2SasPlus.greedyCovering(problem);
                    System.out.println("greedy covering done");
                    System.out.println("SAS variables generated");
                    durationSASTotal = System.nanoTime() - startSASTotal;
                }

                //MUTEX CLIQUES DEBUG LOGS - DO NOT DELETE
                //1. mutex pairs
                
                /*HashMap<Collection<Integer>, Integer> heuristicTable_atomPairs = Strips2SasPlus.heuristicTable;
                for (int i = 0; i < problem.getFluents().size(); i++) {
                    for (int j = i + 1; j < problem.getFluents().size(); j++) {
                            Fluent f1 = problem.getFluents().get(i);
                            Fluent f2 = problem.getFluents().get(j);
                            String a1 = UtilFunctions.debug_fluentToString(f1, problem);
                            String a2 = UtilFunctions.debug_fluentToString(f2, problem);
                            Set<Integer> col = Collections.unmodifiableSet(
                                    new HashSet<>(Arrays.asList(i, j)));
                            int val = heuristicTable_atomPairs.get(col);
                            System.out.println(a1 + "->" + i + " | " + a2 + "->" + j + " : " + val);
                    }
                }*/
                //2. positive preconditions of some action (here - 11)
                /*System.out.println("-----------");
                for (Action a : problem.getActions()) {
                    
                    if (a.getPrecondition().getPositiveFluents().get(11)) {
                        String out = UtilFunctions.debug_actionToString(a, problem);
                        System.out.println(out);
                    }
                }*/
                //3. output mutex cliques in console
                System.out.println("------------");
                System.out.println("MUTEX CLIQUES:");

                List<Collection<Integer>> cliques = Strips2SasPlus.cliques;
                /*for (Collection<Integer> clique : cliques) {
                    System.out.println("Clique: ");
                    for (Integer i : clique) {
                        Fluent f = problem.getFluents().get(i);
                        System.out.println(UtilFunctions.debug_fluentToString(f, problem));
                    }
                
                }*/
                System.out.println("------------");
                




                ///////////////////////////////////////////////
                // PREPARE TO WRITE PROBLEM TO FILE
                File file = new File(minizincProblemPath);
                BufferedOutputStream pw = new BufferedOutputStream(new FileOutputStream(
                    minizincProblemPath));

                ///////////////////////////////////////////////
                //STEP - ENCODING
                //-----
                // ENCODE INITIAL LAYER
                // 1. TRANSFORM INITIAL NETWORK INTO TOTALLY-ORDERED LIST OF
                // PRIMITIVE/NON-PRIMITIVE TASKS
                List<Integer> tasks_totallyOrdered = UtilFunctions
                        .totallyOrderedList(problem.getInitialTaskNetwork().getTasks(),
                                problem.getInitialTaskNetwork().getOrderingConstraints());
                

                // 2. TRANSFORM THE TOTALLY-ORDERED LIST INTO A HIERARCHICAL LAYER
                // (List<LayerCell>)
                // tasks_totallyOrdered contains primitive or non-primitive tasks. Non-primitive
                // tasks should be transformed into reductions (methods)
                List<LayerCell> cells = new ArrayList<>();
                List<Task> tasks = problem.getTasks();
                // 2.1 Transform every task into a cell
                for (int i = 0; i < tasks_totallyOrdered.size(); i++) {
                    LayerCell cell = new LayerCell();
                    Task t = tasks.get(tasks_totallyOrdered.get(i));
                    if (t.isPrimtive()) {
                        // if task is primitive - add it to the cell
                        cell.addTask(t, tasks_totallyOrdered.get(i));
                    } else {
                        // if task is non-primitive, add all methods, applicable to this task, to the
                        // cell
                        for (int j = 0; j < problem.getMethods().size(); j++) {
                            Method m = problem.getMethods().get(j);
                            if (m.getTask() == tasks_totallyOrdered.get(i)) {
                                cell.addMethod(m, j);
                            }
                        }
                    }
                    cells.add(cell);
                }

                //////////////////////////////////////////////////
                // 3. CREATE THE INITIAL LAYER
                Layer initialLayer = new Layer(0);
                initialLayer.setCells(cells);
                // ENCODE RULE 3 (the last cell of initial layer is blank)
                LayerCell blank = new LayerCell();
                blank.addNoop();
                List<LayerCell> tmp = new ArrayList<>();
                tmp.add(blank);
                initialLayer.addCells(tmp);


                //////////////////////////////////////////////////
                // 4. DECOMPOSE THE INITIAL LAYER (we can safely assume the layer is to be
                // decomposed)

                startTmp = System.nanoTime();
                Layer layer2 = initialLayer.decomposeLayer(problem);
                durationDecomposeLayer += System.nanoTime() - startTmp;

                // create minizinc variables representing layer cells (values are possible actions and methods)
                startTmp = System.nanoTime();
                GeneratorFunctions.getCellDomain(initialLayer, problem, pw, 
                        includeDebugCommentaryInMinizinc);
                durationCellDomain += System.nanoTime() - startTmp;
                // create minizinc variables for facts (aka states) 
                startTmp = System.nanoTime();
                GeneratorFunctions.getLayerFacts(initialLayer, problem, pw, includeDebugCommentaryInMinizinc);
                durationLayerFacts += System.nanoTime() - startTmp;

                //idem. for second layer
                startTmp = System.nanoTime();
                GeneratorFunctions.getCellDomain(layer2, problem, pw, includeDebugCommentaryInMinizinc);
                durationCellDomain += System.nanoTime() - startTmp;
                startTmp = System.nanoTime();
                GeneratorFunctions.getLayerFacts(layer2, problem, pw, includeDebugCommentaryInMinizinc);
                durationLayerFacts += System.nanoTime() - startTmp;
                
                /////////////////////////////////////
                // 5. ENCODING RULES (constraints for the previously declared variables)
                // RULE 1 - initial state predicates hold in step 0, layer 0
                // NOTE: initial state is a COMPLETE state
                pw.write("% RULE 1 - initial state predicates hold in step 0\n".getBytes());
                        
                startTmp = System.nanoTime();
                List<Boolean> cliqueInitialized = new ArrayList<>();
                for (int i = 0; i < cliques.size(); i++) {
                    cliqueInitialized.add(false);
                }
                BitVector bv = problem.getInitialState().getPositiveFluents();
                for (int i = bv.nextSetBit(0); i >= 0; i = bv.nextSetBit(i + 1)) {
                    //ignore static fluents
                    if (UtilFunctions.checkFluentNotStatic(i, problem)) {
                        int clique = Strips2SasPlus.fluent2Clique.get(i);
                        pw.write(("constraint c_" + clique + "_0_0 = " + i + ";").getBytes());
                        if (includeDebugCommentaryInMinizinc) {
                            pw.write(("%"
                            + UtilFunctions.debug_fluentToString(problem.getFluents().get(i), problem)
                                    + "\n").getBytes());
                        }
                        cliqueInitialized.set(clique, true);
                    }
                }
                //note that chunk above for rule 1 defines variable (clique) value if one of its mutex facts is true in initial state
                //but what if none of the facts from the clique are true in the initial state?
                //this is why for all such "uninitialized" clique variables we set the value as -1 or "none_of_those"
                for (int i = 0; i < cliqueInitialized.size(); i++) {
                    if (!cliqueInitialized.get(i)) {
                        String o = "\nconstraint c_" + i + "_0_0 = -1;";
                        if (includeDebugCommentaryInMinizinc) {
                            o += "%none of the facts is in the initial state";
                        }
                        pw.write((o).getBytes());
                    }
                }
                durationRule1 += System.nanoTime() - startTmp;

                System.out.println("r 1 encoded");
                // RULE 2 - for each cell of the initial layer, one of its task reductions must
                // hold
                pw.write(
                        "\n% RULE 2 - for each cell of the initial layer, one of its task reductions must hold\n".getBytes());
                pw.write("% RULE 2 is redundant, as variable will forcibly take one of the given values".getBytes());


                // RULE 3 - the last cell of initial layer contains blank
                pw.write("\n% RULE 3 - the last cell of initial layer contains blank".getBytes());
                pw.write("% rule 3 is enacted during the creation of initial layer".getBytes());
                // rule 3 is situated in the declaration of initial layer
                System.out.println("r 2-3 redundant");

                // RULE 4 - at the last position of the initial layer (i.e., the blank cell from rule
                // 3) all the goal facts hold
                // NOTE: Goal is a PARTIAL state
                startTmp = System.nanoTime();
                pw.write(
                        "\n% RULE 4 - at the last position of the initial layer (the blank cell from rule 3), all the goal facts hold\n"
                                .getBytes());
                //positive fluents
                bv = problem.getGoal().getPositiveFluents();
                for (int i = bv.nextSetBit(0); i >= 0; i = bv.nextSetBit(i + 1)) {
                    if (UtilFunctions.checkFluentNotStatic(i, problem)) {
                        String debug = "";
                        if(includeDebugCommentaryInMinizinc){
                            debug = UtilFunctions.debug_fluentToString(problem.getFluents().get(i), problem);
                        }
                        int clique = Strips2SasPlus.fluent2Clique.get(i);
                        pw.write(("constraint c_" + clique + "_0_" + (initialLayer.getCells().size() - 1) + "="
                                + i + "; %"+debug+"\n").getBytes());
                    }
                }
                //negative fluents
                bv = problem.getGoal().getNegativeFluents();     
                for (int i = bv.nextSetBit(0); i >= 0; i = bv.nextSetBit(i + 1)) {
                    if (UtilFunctions.checkFluentNotStatic(i, problem)) {
                        String debug = "";
                        if(includeDebugCommentaryInMinizinc){
                            debug = UtilFunctions.debug_fluentToString(problem.getFluents().get(i), problem);
                        }
                        int clique = Strips2SasPlus.fluent2Clique.get(i);
                        pw.write(("constraint c_" + clique + "_0_" + (initialLayer.getCells().size() - 1) + "!="
                                + i + "; %"+debug+"\n").getBytes());
                    }
                }           

                durationRule4 += System.nanoTime() - startTmp;
                System.out.println("r 4 encoded");

                //RULES 1-4 are unique in the sense that they're encoded only for the initial layer
                //the subsequent rules will be placed in a loop so that they are applied to all layers

                //COMBINE LAYERS INTO A NETWORK
                List<Layer> network = new ArrayList<>();
                network.add(initialLayer);
                network.add(layer2);


                boolean unsat = true;
                int firstUnprocessedLayer = 0; //when we refine the problem by expanding/reducing the hierarchical layer, we don't want to reencode rules from the previous problem, since we're still writing to the same file

                while (unsat) {
                    for (int i = firstUnprocessedLayer; i < network.size(); i++) {
                        Layer l = network.get(i);
                        System.out.println("Rules for layer " + i);
                        pw.write(("%%%%%%%%%PROCESSING LAYER " + l.getIndex() + "%%%%%%%%%").getBytes());
                        // RULE 5 - constraints for changing predicate values upon action execution
                        startTmp = System.nanoTime();
                        pw.write(
                                "\n% RULE 5 - constraints for changing predicate values upon action execution".getBytes());
                        GeneratorFunctions.getActionExecutionConstraints(l, problem, pw, includeDebugCommentaryInMinizinc);
                        
                        durationRule5 += System.nanoTime() - startTmp;
                        
                        System.out.println("r 5 encoded");
                        //RULE 6 - applying a method implies method preconditions
                        startTmp = System.nanoTime();
                        pw.write("\n% RULE 6 - applying a method implies method preconditions".getBytes());
                        GeneratorFunctions.generateRule6_methodPreconditions(l, problem, pw, includeDebugCommentaryInMinizinc);
                        
                        durationRule6 += System.nanoTime() - startTmp;
                        System.out.println("r 6 encoded");
                        //RULE 7 - mutual exclusion between primitive tasks and reductions
                        pw.write("\n% RULE 7 - mutual exclusion between primitive tasks and reductions\n".getBytes());
                        pw.write("% RULE 7 is redundant, as cell variable may hold one value at a time\n".getBytes());
                        System.out.println("r 7 redundant");
                        //RULE 8 - frame axioms
                        pw.write("\n% RULE 8 - frame axioms".getBytes());
                        startTmp = System.nanoTime();
                        GeneratorFunctions.generateFrameAxioms(l, problem, pw, includeDebugCommentaryInMinizinc);

                        durationRule8 += System.nanoTime() - startTmp;
                        System.out.println("r 8 encoded");
                        //RULE 9 - action mutual exclusion. No two actions may happen simultaneously
                        startTmp = System.nanoTime();
                        pw.write(
                                "\n% RULE 9 - action mutual exclusion. No two actions may happen simultaneously\n".getBytes());
                        pw.write("% RULE 9 is redundant, as cell variable can hold only one value at a time\n".getBytes());
                        System.out.println("r 9 redundant");
                        
                        //NOTE: Rules 10-15 are applied to all layers BUT the last, since we can't decompose the last layer
                        if (i != network.size() - 1) {
                            //RULE 10 - propagate facts to lower layers
                            startTmp = System.nanoTime();
                            pw.write("\n% RULE 10".getBytes());
                            pw.write(GeneratorFunctions.generateFactPropagation(l, problem).getBytes());
                            durationRule10 += System.nanoTime() - startTmp;
                            System.out.println("r 10 encoded");
                            

                            //RULE 11 - propagate actions to lower layers
                            startTmp = System.nanoTime();
                            pw.write("\n% RULE 11".getBytes());
                            pw.write(GeneratorFunctions.generateActionPropagation(l, problem).getBytes());
                            durationRule11 += System.nanoTime() - startTmp;
                            System.out.println("r 11 encoded");


                            //RULE 12 - fill blanks when method is decomposed using single action
                            startTmp = System.nanoTime();
                            pw.write("\n% RULE 12".getBytes());
                            pw.write(GeneratorFunctions.generateActionNoopFillers(l, problem).getBytes());
                            durationRule12 += System.nanoTime() - startTmp;
                            System.out.println("r 12 encoded");

                            //RULES 13-15
                            //when reducing a cell in layer L into a set of subtasks in layer L+1, we always allocate N cells, where N is the maximal size of reduction. For example, if cell A can be decomposed using m0 and m1, where m0 consists of 2 tasks, and m1 of 4, we will always allocate 4 cells. If we apply m0, we will set the values of the latter 2 cells to blank/noop.
                            //Rule 13 ensures that when we pick a specific method for cell in layer L, we execute all of its subtasks in layer L+1. Rule 13 ensures this for primitive subtasks
                            //Rule 14 does the same for nonprimitive subtasks
                            //Rule 15 fills in the leftover of allocated space (e.g., last 2 cells from m0) with noops
                            startTmp = System.nanoTime();
                            pw.write("\n% RULES 13-15".getBytes());
                            GeneratorFunctions.generateMethodApplication(l, problem, pw).getBytes();
                            durationRules13_15 += System.nanoTime() - startTmp;
                            System.out.println("r 13-15 encoded");
                        }

                    }

                    pw.write("\n%..".getBytes());
                    pw.write("\n".getBytes());
                    pw.write("\n".getBytes());
                    pw.write("\n".getBytes());
                    pw.flush();
                    pw.close();

                    //before attempting to solve check if the last layer cells have at least one primitive task (or noop),
                    //otherwise it's meaningless to search for solution, since it's by default UNSAT 
                    Layer lastLayer = network.get(network.size() - 1);
                    boolean lastLayerPrimitive = true;
                    for (LayerCell c : lastLayer.getCells()) {
                        if (c.getPrimitiveTasks().size() == 0 && c.getNoop() == false) {
                            lastLayerPrimitive = false;
                            break;
                        }
                    }
                    
                    if (lastLayerPrimitive) {

                        //RULE 16 - the plan is satisfiable only if the last layer is composed of primitive actions only
                        //while all the previous rules remain when the problem expands, rule 16 always changes, since the what is considered last layer changes as well
                        //A quirk of the current system is that it keeps a file without rule 16, where it writes all constraints, and then at the very end it copies itself into another file, to which rule 16 is added.
                        //An alternative solution could be to write a single file, where rule 16 is on the last line, which we remove, when we need to modify the problem
                        //This design choice isn't critical and won't matter in the future, since we'll be using an API anyways
                        startTmp = System.nanoTime();
                        copyFile_addRule16(file, minizincProblem_withRule16, GeneratorFunctions
                                .generateAllPrimitiveLayerConstraint_rule16(network.get(network.size() - 1), problem));
                        System.out.println("file copied + r 16 encoded");
                        durationFileCopy += System.nanoTime() - startTmp;

                        //////////////////////////////////////////////////////
                        //SOLVING
                        System.out.println("solving....");

                        
                        /*output is saved to a .txt file */
                        String[] cmds = { "minizinc", "--solver", "chuffed", "-s", minizincProblem_withRule16};

                        startTmp = System.nanoTime();
                        MyOutput res = callMinizincCmd_OLD(cmds, solutionPath);
                        unsat = !res.getSAT();
                        durationSolveTime += res.getSolveTime();
                        durationMinizincTotal += (System.nanoTime() - startTmp);
                        long duration = System.nanoTime() - startTmp;
                        System.out.println("time spent in Minizinc: " + (duration / 1000000) + "ms");
                    } else {
                        System.out.println("UNSAT by default");
                    }

                    // CASE 1 - PROBLEM WAS UNSAT
                     if (unsat) {

                        
                        System.out.println(
                                "UNSAT. Increasing layers from " + network.size() + " to " + (network.size() + 1));

                        //pauses execution before expanding a layer - useful for debugging
                        /*System.out.println("next iteration?");
                        
                Scanner userInput = new Scanner(System.in);
                String input = userInput.nextLine();*/

                        //setting "append" to true, because we're modifying the file, not creating a new one
                        pw = new BufferedOutputStream(new FileOutputStream(
                    minizincProblemPath, true));

                        //FIRST, WE ADD RULES 10-15 WHAT WAS THE LAST LAYER, SINCE IT'S NO LONGER LAST LAYER
                        //propagate rules 10-15 to the last layer
                        System.out.println("Appending rules to layer " + (network.size() - 1));
                        Layer l = network.get(network.size() - 1);
                        //RULE 10 - propagate facts to lower layers
                        startTmp = System.nanoTime();
                        pw.write("\n% RULE 10".getBytes());
                        pw.write(GeneratorFunctions.generateFactPropagation(l, problem).getBytes());
                        durationRule10 += System.nanoTime() - startTmp;
                        System.out.println("r 10 appended");

                        //RULE 11 - propagate actions to lower layers
                        startTmp = System.nanoTime();
                        pw.write("\n% RULE 11".getBytes());
                        pw.write(GeneratorFunctions.generateActionPropagation(l, problem).getBytes());
                        durationRule11 += System.nanoTime() - startTmp;
                        System.out.println("r 11 appended");

                        //RULE 12 - fill blanks when method is decomposed using single action
                        startTmp = System.nanoTime();
                        pw.write("\n% RULE 12".getBytes());
                        pw.write(GeneratorFunctions.generateActionNoopFillers(l, problem).getBytes());
                        durationRule12 += System.nanoTime() - startTmp;
                        System.out.println("r 12 appended");

                        //RULES 13-15 - when we pick a specific method at layer L, we must executed all of its subtasks in the layer L+1 (rule 13 for primitive subtasks, rule 14 for nonprimitive subtasks). And the leftover of allocated space (maxE), if any, is filled with noops (rule15)
                        startTmp = System.nanoTime();
                        pw.write("\n% RULES 13-15".getBytes());
                        GeneratorFunctions.generateMethodApplication(l, problem, pw).getBytes();
                        durationRules13_15 += System.nanoTime() - startTmp;
                        System.out.println("r 13-15 appended");

                        //THEN WE ADD A NEW LAYER TO THE NETWORK
                        //Introduce new layer
                        startTmp = System.nanoTime();
                        Layer layerNew = network.get(network.size() - 1).decomposeLayer(problem);
                        durationDecomposeLayer += System.nanoTime() - startTmp;
                        
                        //DEBUG - display the new layer
                        /* UtilFunctions.debug_layerToString(layerNew, problem);
                            layerNew.debugCells(problem);
                            System.out.println("proceed ?");
                            input2 = userInput2.nextLine();*/

                        //Add cell and fact variables for the new layer
                        pw.write(("% LAYER " + layerNew.getIndex() + " variables").getBytes());
                        startTmp = System.nanoTime();
                        GeneratorFunctions.getCellDomain(layerNew, problem, pw, includeDebugCommentaryInMinizinc);
                        durationCellDomain += System.nanoTime() - startTmp;
                        startTmp = System.nanoTime();
                        GeneratorFunctions.getLayerFacts(layerNew, problem, pw, includeDebugCommentaryInMinizinc);
                        durationLayerFacts += System.nanoTime() - startTmp;
                        network.add(layerNew);

                        
                    }

                    firstUnprocessedLayer = network.size() - 1; // process only last layer
                     
                    
                    //unsat = false;
                }
                System.out.println("input: " + minizincProblem_withRule16);
                System.out.println("output: " + solutionPath);
                durationSystemTotal = (System.nanoTime() - startSystemTotal); 
                System.out.println("PROBLEM SOLVED");

                long encodingTimeCuml = durationSASTotal + durationDecomposeLayer + durationCellDomain
                        + durationLayerFacts + durationRule1 + durationRule4 + durationRule5 + durationRule6
                        + durationRule8 + durationRule10 + durationRule11 + durationRule12 + durationRules13_15
                        + durationFileCopy;
                

                
                System.out.println("total actions " + problem.getActions().size());
                System.out.println("total tasks " + problem.getTasks().size());
                System.out.println("total fluents " + problem.getFluents().size());
                System.out.println("-------------------");
                System.out.println("TIME:");
                System.out.println("Total time spent:   " + (durationSystemTotal / 1000000) + "ms   100%");
                System.out.println("PDDL4J:             " + durationPDDL4J / 1000000 + "ms      "+percentage(durationSystemTotal, durationPDDL4J));
                System.out.println("Minizinc total:     " + durationMinizincTotal / 1000000 + "ms   "+percentage(durationSystemTotal, durationMinizincTotal));
                System.out.println("Java encoding:      " + (encodingTimeCuml / 1000000) + "ms   "
                        + percentage(durationSystemTotal, encodingTimeCuml));
                System.out.println("Solve time only:   " + (durationSolveTime ) + "ms   "+percentage(durationSystemTotal, durationSolveTime));
                System.out.println("JAVA ENCODING TIME DECOMPOSITION:");
                System.out.println("SAS encoding:       " + durationSASTotal / 1000000 + "ms    "+percentage(durationSystemTotal, durationSASTotal));
                System.out.println("decomposeLayer():   " + durationDecomposeLayer / 1000000 + "ms  "+percentage(durationSystemTotal, durationDecomposeLayer));
                System.out.println("getCellDomain():    " + durationCellDomain / 1000000 + "ms  "+percentage(durationSystemTotal, durationCellDomain));
                System.out.println("getLayerFacts():    " + durationLayerFacts / 1000000 + "ms  "+percentage(durationSystemTotal, durationLayerFacts));
                System.out.println("rule 1:             " + durationRule1 / 1000000 + "ms   "+percentage(durationSystemTotal, durationRule1));
                System.out.println("rule 4:             " + durationRule4 / 1000000 + "ms   "+percentage(durationSystemTotal, durationRule4));
                System.out.println("rule 5:             " + durationRule5 / 1000000 + "ms   "+percentage(durationSystemTotal, durationRule5));
                System.out.println("rule 6:             " + durationRule6 / 1000000 + "ms   "+percentage(durationSystemTotal, durationRule6));
                System.out.println("rule 8:             " + durationRule8 / 1000000 + "ms   "+percentage(durationSystemTotal, durationRule8));
                System.out.println("rule 10:            " + durationRule10 / 1000000 + "ms  "+percentage(durationSystemTotal, durationRule10));
                System.out.println("rule 11:            " + durationRule11 / 1000000 + "ms  "+percentage(durationSystemTotal, durationRule11));
                System.out.println("rule 12:            " + durationRule12 / 1000000 + "ms  "+percentage(durationSystemTotal, durationRule12));
                System.out.println("rules 13-15:        " + durationRules13_15 / 1000000 + "ms  "+percentage(durationSystemTotal, durationRules13_15));
                System.out.println("file copy:   " + durationFileCopy / 1000000 + "ms    "+percentage(durationSystemTotal, durationFileCopy));
                System.out.println("Apply validator (y/n)?");
                Scanner userInput = new Scanner(System.in);
                String input = userInput.nextLine();
                if (input.contains("y")) {
                    Validator.parsePlan(solutionPath);
                    Validator.formatPlan(network, problem);

                }
            }
            // This exception could happen if the domain or the problem does  not exist
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    //if A is 100%, how many % is B?
    public static String percentage(long A, long B) {
        int res = Math.round(B * 100 / A);
        return res + "%";
    }
    
    //stores output in a .txt file, returns true if SAT, false if UNSAT
    public static boolean callMinizincCmd(String[] commands, String solutionPath) throws IOException {
        boolean SAT = true;
        Runtime rt = Runtime.getRuntime();
        System.out.println("cmds: " + commands + " " + solutionPath);
        rt.exec(commands);

        /*BufferedReader stdInput = new BufferedReader(new 
             InputStreamReader(proc.getInputStream()));
        
        BufferedReader stdError = new BufferedReader(new 
             InputStreamReader(proc.getErrorStream()));
        
             // Read the output from the command
        
             File file = new File(solutionPath);
             
             PrintWriter pw = new PrintWriter(new FileWriter(file));
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            pw.write(s+"\n");
            if (s.contains("UNSAT")) {
                SAT = false;
            }
        }
        
        // Read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
        
        pw.flush();
        pw.close();*/
        System.out.println("SAT = " + SAT);
        return SAT;
    }

    
    //stores output in a .txt file, returns true if SAT, false if UNSAT
    public static MyOutput callMinizincCmd_OLD(String[] commands, String solutionPath) throws IOException {
        boolean SAT = true;
        int time = 0;
        Runtime rt = Runtime.getRuntime();
        System.out.println("cmds: " + commands[0]+ " " + commands[1]+ " "+ commands[2]+ " "+ commands[3] + " "+solutionPath);
Process proc = rt.exec(commands);

BufferedReader stdInput = new BufferedReader(new 
     InputStreamReader(proc.getInputStream()));

BufferedReader stdError = new BufferedReader(new 
     InputStreamReader(proc.getErrorStream()));

     // Read the output from the command

     File file = new File(solutionPath);
     
     PrintWriter pw = new PrintWriter(new FileWriter(file));
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            if(!s.contains("%")){
                pw.write(s + "\n");
            } else {
                if (s.contains("solveTime")) {
                    float d = Float.parseFloat(s.split("=")[1]);
                    time =Math.round(d*1000);
                }
            }
            if (s.contains("UNSAT")) {
                SAT = false;
            }
        }

// Read any errors from the attempted command
while ((s = stdError.readLine()) != null) {
    System.out.println(s);
}

pw.flush();
pw.close();
System.out.println("SAT = " + SAT);
return new MyOutput(SAT, time);
    }

    //very ug
    public static void copyFile_addRule16(File src, String tmp, String rule16) throws IOException {
File tempFile = new File(tmp);

BufferedReader reader = new BufferedReader(new FileReader(src));
BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

//String lineToRemove = "RULE 16";
String currentLine;

while ((currentLine = reader.readLine()) != null) {
    // trim newline when comparing with lineToRemove
    String trimmedLine = currentLine.trim();
    //System.out.println(trimmedLine);
    //if(trimmedLine.contains(lineToRemove)) continue;
    writer.write(currentLine + System.getProperty("line.separator"));
}
writer.flush();
writer.write(rule16 + "\n");
writer.close(); 
reader.close();
String name = src.getName();
//System.out.println(src.delete());
//boolean successful = tempFile.renameTo(src);
//System.out.println(successful);
    }
    public static void removeRule16_2(String src)throws IOException  {

            File tmp = File.createTempFile("tmp", "");
        
            BufferedReader br = new BufferedReader(new FileReader(src));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
        
        
        
            String l;
            while (null != (l = br.readLine())) {
                if (l.contains("RULE 16")) {

                    System.out.println(l);
                }else{
                    bw.write(String.format("%s%n", l));
                }
            }
        
            br.close();
            bw.close();
        
            File oldFile = new File(src);
            if (oldFile.delete())
                tmp.renameTo(oldFile);
        
    }

    




}
