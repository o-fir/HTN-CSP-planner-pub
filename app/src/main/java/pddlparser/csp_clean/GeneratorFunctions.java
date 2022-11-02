package pddlparser.csp_clean;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Method;
import fr.uga.pddl4j.util.BitVector;

public class GeneratorFunctions {
    static long debugTotalTime1 = 0;
    static long debugTotalTime2 = 0;
    static long debugTotalTime3 = 0;
        // generates precondition and effect constraints for actions of each cell of the
    // layer
    public static boolean getActionExecutionConstraints(Layer layer, HTNProblem problem, BufferedOutputStream pw,
            boolean writeCommentary) throws IOException {
        if (writeCommentary) {
            pw.write(("\n% ACTION EXECUTION CONSTRAINTS FOR LAYER " + layer.getIndex() + "\n").getBytes());
        } else {
            pw.write("\n".getBytes());
        }
        for (int i = 0; i < layer.getCells().size(); i++) {
            LayerCell c = layer.getCells().get(i);
            //Note that with SAS+ encoding the state variables have 'none-of-those' value.
            //This value is important for the cases where variable should have all its facts set to false
            //Technically, we don't need to add any constraints to the encoding, as the frameAxioms 
            //will force the variable to take the only available value (-1), but adding additional constraint
            //should improve planner performance, I believe
            List<Integer> cliqueCompletelyFalse = new ArrayList<>();
            for (int j = 0; j < Strips2SasPlus.cliques.size();j++){
                cliqueCompletelyFalse.add(1);
            }

            for (Task t : c.getPrimitiveTasks()) {
                Action a = UtilFunctions.taskToAction(t, problem);
                String constraint = "";
                if(writeCommentary){
                 constraint = "% Action " + problem.getActions().indexOf(a) + " - " + a.getName() + " - layer: "
                        + layer.getIndex() + " cell: " + i + "\n";
                 constraint += "%" + UtilFunctions.debug_actionToString(a, problem) + "\n";
                }
                String clauses = "";
                //negative preconditions
                if (writeCommentary) {
                    constraint += "% negative preconditions\n";
                }
                BitVector bv = a.getPrecondition().getNegativeFluents();
                for (int j = bv.nextSetBit(0); j >= 0; j = bv.nextSetBit(j + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(j);
                    clauses += "c_" + clique + "_" + layer.getIndex() + "_" + i + " != " + j + " /\\ ";
                }

                if (!clauses.isEmpty()) {
                    clauses = clauses.substring(0, clauses.length() - 3);
                    constraint += "constraint e_" + layer.getIndex()
                            + "_"
                            + i + " = "+ (problem.getTasks().indexOf(t) + 1) + " -> (\n" + clauses + ");\n";
                } else {
                    if (writeCommentary) {
                        constraint += "% none\n";
                    }
                }
                //positive preconditions
                if (writeCommentary) {
                    constraint += "% positive preconditions\n";
                }
                clauses = "";

                bv = a.getPrecondition().getPositiveFluents();
                for (int j = bv.nextSetBit(0); j >= 0; j = bv.nextSetBit(j + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(j);
                    clauses += "c_" + clique + "_" + layer.getIndex() + "_" + i + " = " + j + " /\\ ";
                }

                if (!clauses.isEmpty()) {
                    clauses = clauses.substring(0, clauses.length() - 3);
                    constraint += "constraint e_" + layer.getIndex()
                            + "_"
                            + i + " = " + (problem.getTasks().indexOf(t) + 1) + " -> (\n" + clauses + ");\n";
                } else {
                    if (writeCommentary) {
                        constraint += "% none\n";
                    }
                }
                //positive effects
                if (writeCommentary) {
                    constraint += "% positive effects (NOTE: encoding doesn't support conditional effects)\n";
                }
                clauses = "";

                bv = a.getUnconditionalEffect().getPositiveFluents();
                for (int j = bv.nextSetBit(0); j >= 0; j = bv.nextSetBit(j + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(j);
                    clauses += "c_" + clique + "_" + layer.getIndex() + "_" + (i + 1) + " = " + j + " /\\ ";
                    cliqueCompletelyFalse.set(clique, cliqueCompletelyFalse.get(clique) + 1);
                }

                if (!clauses.isEmpty()) {
                    clauses = clauses.substring(0, clauses.length() - 3);
                    constraint += "constraint e_" + layer.getIndex()
                            + "_"
                            + i + " = " + (problem.getTasks().indexOf(t) + 1) + " -> (\n" + clauses + ");";

                            if(writeCommentary){ constraint +=
                            "%"+ cliqueCompletelyFalse + "\n\n";}
                } else {
                    if (writeCommentary) {
                        constraint += "% none\n";
                    }
                }
                /////////
                if (writeCommentary) {
                    constraint += "% negative effects (NOTE: encoding doesn't support conditional effects)\n";
                }
                clauses = "";

                bv = a.getUnconditionalEffect().getNegativeFluents();
                for (int j = bv.nextSetBit(0); j >= 0; j = bv.nextSetBit(j + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(j);
                    clauses += "c_" + clique + "_" + layer.getIndex() + "_" + (i + 1) + " != " + j + " /\\ ";
                    cliqueCompletelyFalse.set(clique, cliqueCompletelyFalse.get(clique) - 1);
                }
                if (!clauses.isEmpty()) {
                    clauses = clauses.substring(0, clauses.length() - 3);
                    constraint += "constraint e_" + layer.getIndex()
                            + "_"
                            + i + " = " + (problem.getTasks().indexOf(t) + 1) + " -> (\n" + clauses + ");";

                            if(writeCommentary){ constraint +=
                            "%"+ cliqueCompletelyFalse + "\n\n";}
                } else {
                    if (writeCommentary) {
                        constraint += "% none\n";
                    }
                }
                //NOTE - seems to be broken.
                //CONT. previous comment 
                //as previously mentioned, if we don't have a positive effect that forces clique variable into a value
                //but we have a negative effect that will "limit" the clique variable to not be some value
                //we will set the value of the variable to 'none-of-those'
                /*for (int j = 0; j < cliqueCompletelyFalse.size(); j++) {
                    if (cliqueCompletelyFalse.get(j) == 0) {
                        constraint += "constraint e_" + layer.getIndex()
                        + "_"
                        + i + " = "+ (problem.getTasks().indexOf(t) + 1) + " -> (\nclique"+cliqueCompletelyFalse.get(j)+"_"+layer.getIndex()+"_"+(i+1)+"=-1);%"+cliqueCompletelyFalse+"\n";
                    }
                }*/
                ////
                pw.write((constraint + "\n").getBytes());
            }
        }
        return true;
    }

    //method may be applied only if necessary preconditions (if any) hold
    public static boolean generateRule6_methodPreconditions(Layer layer, HTNProblem problem, BufferedOutputStream pw,
            boolean writeCommentary) throws IOException {
        if(writeCommentary){
            pw.write(("% RULE 6 (METHOD PRECONDITIONS) FOR LAYER " + layer.getIndex() + "\n").getBytes());
        } else {
            pw.write("\n".getBytes());}
        for (int i = 0; i < layer.getCells().size(); i++) {
            LayerCell c = layer.getCells().get(i);
            for (int j = 0; j < c.getMethods().size(); j++) {
                Method m = c.getMethods().get(j);
                //idem to Rule 5 (action preconditions part)
                String constraint = "";
                
        if(writeCommentary){
                constraint = "% Method " + c.getMethodsIndex().get(j)+ " - " 
                        + UtilFunctions.debug_methodToString(m, problem) + " \n";
            }
                String clauses = "";
                //OPTIMIZED VARIATION
                //go through negative fluents
                //... nextSetBit()
                //OPTIMIZED VARIATION END
                //go through every fluent and check whether it's used as positive or negative precondition
                String clause = "";

                //case - negative preconditions
                BitVector bv = m.getPrecondition().getNegativeFluents();
                for (int predId = bv.nextSetBit(0); predId >= 0; predId = bv.nextSetBit(predId + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(predId);
                    clause += "c_" + clique + "_" + layer.getIndex() + "_" + i + " != " + predId + " /\\ ";
                }
                //case - positive preconditions
                bv = m.getPrecondition().getPositiveFluents();
                for (int predId = bv.nextSetBit(0); predId >= 0; predId = bv.nextSetBit(predId + 1)) {
                    int clique = Strips2SasPlus.fluent2Clique.get(predId);
                    clause += "c_" + clique + "_" + layer.getIndex() + "_" + i + " = " + predId + " /\\ ";
                }
                if (!clause.isEmpty()) {
                    clauses += clause + "true";
                }
                
                //once all clauses are established, add them to the constraint
                if (!clauses.isEmpty()) {
                    constraint += "constraint e_"
                            + layer.getIndex()
                            + "_"
                            + i + " = "+ ((c.getMethodsIndex().get(j) + 1)*-1)+" -> (\n" + clauses + ");\n";
                } else {
                    if(writeCommentary){
                        constraint += "% none\n";
                    }
                }
                pw.write((constraint + "\n").getBytes());

                /*List<Integer> negPrecs = UtilFunctions.bitVectorToIntList(m.getPrecondition().getNegativeFluents());
                List<Integer> posPrecs = UtilFunctions.bitVectorToIntList(m.getPrecondition().getPositiveFluents());
                if (!negPrecs.isEmpty() || !posPrecs.isEmpty()) {
                    String constraint = "% Method " + problem.getMethods().indexOf(m) + " - " + m.getName()
                            + " - layer: "
                            + layer.getIndex() + " cell: " + i + "\n";
                    constraint += "% negative preconditions\n";
                    String clauses = "";
                    for (int predId : negPrecs) {
                        clauses += "holds_" + predId + "_" + layer.getIndex() + "_" + i + " = false /\\ ";
                    }
                    if (!clauses.isEmpty()) {
                        clauses = clauses.substring(0, clauses.length() - 3);
                        constraint += "constraint e_"
                                + layer.getIndex()
                                + "_"
                                + i + " = "+ ((problem.getMethods().indexOf(m) + 1)*-1)+" -> (\n" + clauses + ");\n";
                    } else {
                        constraint += "% none\n";
                    }
                    ////////////
                    constraint += "% positive preconditions\n";
                    clauses = "";
                    for (int predId : posPrecs) {
                        clauses += "holds_" + predId + "_" + layer.getIndex() + "_" + i + " = true /\\ ";
                    }
                    if (!clauses.isEmpty()) {
                        clauses = clauses.substring(0, clauses.length() - 3);
                        constraint += "constraint e_"
                                + layer.getIndex()
                                + "_"
                                + i + " = "+ ((problem.getMethods().indexOf(m) + 1)*-1)+" -> (\n" + clauses + ");\n";
                    } else {
                        constraint += "% none\n";
                    }
                    pw.write((constraint + "\n").getBytes());
                }*/
            }
        }
        return true;
    }
    
    
    
    //generates frame axioms for a layer - corresponds to rule 8 of TreeRex encoding
    public static boolean generateFrameAxioms(Layer layer, HTNProblem problem, BufferedOutputStream pw,
    boolean writeCommentary) throws IOException {

        //for each fact, apply frame axiom constraints for each cell
        for (int i = 0; i < layer.getCells().size() - 1; i++) {
            if (writeCommentary) {
                pw.write(("\n% FRAME AXIOM Layer: " + layer.getIndex() + " Cell: " + i + " \n").getBytes());
            } else {
                pw.write("\n".getBytes());
            }
            for (int predId = 0; predId < problem.getFluents().size(); predId++) {
                //skip static/constant fluents
                if (UtilFunctions.checkFluentNotStatic(predId, problem)) {
                    
                    if (writeCommentary) {
                        pw.write(("% fluent: "
                                + UtilFunctions.debug_fluentToString(problem.getFluents().get(predId), problem) + " \n")
                                .getBytes());
                    }
                    /////////////////////////
                    //these 2 lists indicate which tasks of the current cell affect current fluent
                    List<Integer> negTaskIds = new ArrayList<Integer>();
                    List<Integer> posTaskIds = new ArrayList<Integer>();

                    for (int k = 0; k < layer.getCells().get(i).getPrimitiveTasks().size(); k++) {
                        int taskIndex = layer.getCells().get(i).getPrimitiveTasksIndex().get(k);
                        Task t = layer.getCells().get(i).getPrimitiveTasks().get(k);
                        Action a = UtilFunctions.taskToAction(t, problem);
                        //TODO: DOES NOT SUPPORT CONDITIONAL EFFECTS!!!!!!

                        if (a.getUnconditionalEffect().getNegativeFluents().get(predId)) {
                            negTaskIds.add(taskIndex + 1);
                        } else if (a.getUnconditionalEffect().getPositiveFluents()
                                .get(predId)) {
                            posTaskIds.add(taskIndex + 1);
                        }

                    }
                    ////////////////////////
                    int clique = Strips2SasPlus.fluent2Clique.get(predId);
                    //TRUE -> FALSE
                    //can happen either if we apply method
                    pw.write(("constraint (c_" + clique + "_" + layer.getIndex() + "_" + i + "=" + predId
                            + " /\\ c_" +
                            clique + "_" + layer.getIndex() + "_" + (i + 1) + "!=" + predId + ")->(e_"
                            + layer.getIndex() + "_" + i + "<0\\/\n").getBytes());

                    //or one of the tasks from the negTaskIds list
                    for (Integer negId : negTaskIds) {
                        pw.write(("e_" + layer.getIndex() + "_"
                                + i
                                + " = " + negId + " \\/ \n").getBytes());
                    }
                    pw.write(("false);\n").getBytes());
                    //FALSE -> TRUE
                    //can happen either if we apply method
                    pw.write(("constraint (c_" + clique + "_" + layer.getIndex() + "_" + i + "!=" + predId
                            + " /\\ c_" +
                            clique + "_" + layer.getIndex() + "_" + (i + 1) + "=" + predId + ")->(e_" + layer.getIndex()
                            + "_" + i + "<0\\/\n").getBytes());

                    //or one of the tasks from the posTaskIds list
                    for (Integer posId : posTaskIds) {
                        pw.write(("e_" + layer.getIndex() + "_"
                                + i
                                + " = " + posId + " \\/ \n").getBytes());
                    }
                    pw.write(("false);\n").getBytes());

                }

            }
            pw.write("\n".getBytes());
        }

        return true;
    }
    //OLD VERSION (TO DELETE). DIfference: for each cell, for each fluent, for each task
    public static boolean generateFrameAxioms_OLD(Layer layer, HTNProblem problem, BufferedOutputStream pw) throws IOException {

        //for each fact, apply frame axiom constraints for each cell
        for (int i = 0; i < layer.getCells().size() - 1; i++) {
            
            pw.write(("\n% FRAME AXIOM Layer: " + layer.getIndex() + " Cell: " + i + " \n").getBytes());
            for (int predId = 0; predId < problem.getFluents().size(); predId++) {
                //skip static/constant fluents
                if (UtilFunctions.checkFluentNotStatic(predId, problem)) {
                    
                    pw.write(("% fluent: "
                            + problem.getPredicateSymbols().get(problem.getFluents().get(predId).getSymbol())
                            + " \n").getBytes());
                    Fluent f = problem.getFluents().get(predId);
                    //TRUE -> FALSE
                    pw.write(("constraint (holds_" + predId + "_" + layer.getIndex() + "_" + i + " = true /\\ holds_"
                            + predId
                            + "_" + layer.getIndex() + "_" + (i + 1) + " = false) -> (e_" + layer.getIndex()
                            + "_"
                            + i + " < 0 \\/ \n").getBytes());
                    //for true2false to happen, a task with negative effect must happen
                    String negTasks = "";
                    for (Task t : layer.getCells().get(i).getPrimitiveTasks()) {
                        Action a = UtilFunctions.taskToAction(t, problem);
                        //TODO: DOES NOT SUPPORT CONDITIONAL EFFECTS!!!!!!
                        BitVector v = a.getUnconditionalEffect().getNegativeFluents();
                        boolean prediction = v.get(predId);
                        
                        boolean result = false;
                        if (a.getUnconditionalEffect().getNegativeFluents().get(predId)) {
                            negTasks += "e_" + layer.getIndex() + "_"
                                    + i
                                    + " = " + (problem.getTasks().indexOf(t) + 1) + " \\/ \n";
                            //System.out.println("TRUUUUUEE");
                            result = true;
                        }

                        if(result != prediction){
                            System.out.println("ERROR");
                        System.out.println("debug stop...");
                        System.in.read();
                    }

                    }
                    pw.write(negTasks.getBytes());
                    pw.write(("false);\n").getBytes());
                    //FALSE -> TRUE
                    long debugStartTime = System.nanoTime();
                    pw.write(("constraint (holds_" + predId + "_" + layer.getIndex() + "_" + i + " = false /\\ holds_"
                            + predId
                            + "_" + layer.getIndex() + "_" + (i + 1) + " = true) -> (e_" + layer.getIndex()
                            + "_"
                            + i + " < 0 \\/ \n").getBytes());
                            long debugEndTime = System.nanoTime();
                            long duration = (debugEndTime - debugStartTime);
                            debugTotalTime1 += duration;
                    //for true2false to happen, a task with negative effect must happen
                    
                     long debugStartTime2 = System.nanoTime();
                    String posTasks = "";
                    for (Task t : layer.getCells().get(i).getPrimitiveTasks()) {
                        Action a = UtilFunctions.taskToAction(t, problem);
                        debugStartTime = System.nanoTime();
                        //TODO: DOES NOT SUPPORT CONDITIONAL EFFECTS!!!!!!
                        if (a.getUnconditionalEffect().getPositiveFluents()
                                .get(predId)) {
                            posTasks /*DEBUG +*/= "e_" + layer.getIndex() + "_"
                                    + i
                                    + " = " + (problem.getTasks().indexOf(t) + 1) + " \\/ \n";
                            pw.write(posTasks.getBytes());
                        }
                        debugEndTime = System.nanoTime();
                        duration = (debugEndTime - debugStartTime);
                        debugTotalTime3 += duration;

                    }
                    //DEBUG pw.write(posTasks.getBytes());
                     long debugEndTime2 = System.nanoTime();
                     duration = (debugEndTime2 - debugStartTime2);
                     debugTotalTime2 += duration;
                    pw.write("false);\n".getBytes());

                }

            }
            pw.write("\n".getBytes());
        }

        return true;
    }
    
    
    
    
    //generates constraints so that if fact holds true in cell I of layer L, it holds true in cell next(I) of layer L+1
    public static String generateFactPropagation(Layer layer, HTNProblem problem) {
        String output = "";
        for (int i = 0; i < layer.getCells().size(); i++) {
            String cellOutput = " % LAYER: " + layer.getIndex() + " CELL: " + i + "\n";
            for (int j = 0; j < problem.getFluents().size(); j++) {
                if (UtilFunctions.checkFluentNotStatic(j, problem)) {
                int clique = Strips2SasPlus.fluent2Clique.get(j);
                cellOutput += "constraint c_" + clique + "_" + layer.getIndex() + "_" + i + "=" + j + "<->c_"
                        + clique + "_" + (layer.getIndex() + 1) + "_" + layer.getNext(i) + "=" + j + ";\n";
                
            }
            }
            output += cellOutput + "\n";
        }

        return output;
    }
    
    
        //generates constraints so that if actions happens in cell I of layer L, it happens in cell next(I) of layer L+1
        public static String generateActionPropagation(Layer layer, HTNProblem problem) {
            String output = "";
            for (int i = 0; i < layer.getCells().size(); i++) {
                String cellOutput = " % LAYER: " + layer.getIndex() + " CELL: " + i + "\n";

                for (Task t : layer.getCells().get(i).getPrimitiveTasks()) {
                    cellOutput += "constraint e_" + layer.getIndex()
                            + "_" + i + " = "+ (problem.getTasks().indexOf(t) + 1) +" -> e_"
                            + (layer.getIndex() + 1) + "_" + layer.getNext(i) + " = "+ (problem.getTasks().indexOf(t) + 1) +";\n";
                }

                output += cellOutput + "\n";
            }

            return output;
        }
        
        
    //when a non-primitive task is reduced using a single action, we want to fill in the holes in allocated space with noops. If this explanation is confusing, reread TreeRex encoding to understand how it works - I can't explain better
    public static String generateActionNoopFillers(Layer layer, HTNProblem problem) {
        String output = "";
        for (int i = 0; i < layer.getCells().size(); i++) {
            LayerCell cell = layer.getCells().get(i);
            String cellOutput = "% LAYER: " + layer.getIndex() + " CELL: " + i + "\n";
            if (cell.getPrimitiveTasks().size() == 0) {
                cellOutput += "% no primitive tasks\n";
            } else {
                if (cell.getMaxE() < 2) {
                    cellOutput += "% maxE < 2\n";
                } else {
                    for (Task t : cell.getPrimitiveTasks()) {
                        for (int k = 1; k < cell.getMaxE(); k++) {
                            cellOutput += "constraint e_" + layer.getIndex()
                                    + "_" + i + " = "+ (problem.getTasks().indexOf(t) + 1) +" -> e_"
                                    + (layer.getIndex() + 1) + "_" + (layer.getNext(i) + k) + " = 0;\n";
                            /*cellOutput += "constraint e_" + (problem.getTasks().indexOf(t) + 1) + "_"
                                    + layer.getIndex()
                                    + "_" + i + " = true -> e_" + (problem.getTasks().indexOf(t) + 1) + "_"
                                    + (layer.getIndex() + 1) + "_" + (layer.getNext(i) + k) + ";\n";*/
                        }
                    }
                }
            }
            output += cellOutput + "\n";
        }
        return output;
    }

    
                //RULES 13-15 - when we pick a specific method at layer L, we must executed all of its subtasks in the layer L+1 (rule 13 for primitive subtasks, rule 14 for nonprimitive subtasks). And the leftover of allocated space (maxE), if any, is filled with noops (rule15)
                public static String generateMethodApplication(Layer layer, HTNProblem problem, BufferedOutputStream pw) throws IOException {
                    String output = "";

                    for (int i = 0; i < layer.getCells().size(); i++) {
                        LayerCell cell = layer.getCells().get(i);
                        String cellOutput = "% LAYER: " + layer.getIndex() + " CELL: " + i + "\n";
                        //for every method
                        if (cell.getMethods().size() == 0) {
                            cellOutput += "% no methods in the cell\n";
                        }
                        pw.write(cellOutput.getBytes());
                        for (int k = 0; k < cell.getMethods().size(); k++) {
                            Method m = cell.getMethods().get(k);
                            String methodOutput = "% Method " + m.getName() + "\n";
                            
                            pw.write(methodOutput.getBytes());
                            List<Integer> orderedTasks = UtilFunctions.totallyOrderedList(m.getSubTasks(),
                                    m.getOrderingConstraints());
                            //walk through an ordered list of its subtasks
                            for (int j = 0; j < orderedTasks.size(); j++) {
                                Task t = problem.getTasks().get(orderedTasks.get(j));
                                int taskIndex = orderedTasks.get(j);
                                //Rule 13 - for a primitive subtask, a primitive action is propagated into the cell of layer L+1
                                if (t.isPrimtive()) {
                                    methodOutput = "constraint e_"
                                            + layer.getIndex() + "_" + i + " = "+ ((cell.getMethodsIndex().get(k) + 1)*-1) + " -> e_"
                                             + (layer.getIndex() + 1) + "_"
                                            + (layer.getNext(i) + j) + " = " + (taskIndex + 1) + ";\n";
                                            
                                    pw.write(methodOutput.getBytes());
                                }
                                //Rule 14 - for a non-primitive subtask, a suite of possible methods is propagated into cell of layer L+1
                                else {
                                    //find all the methods of the non_primitive subtask
                                    List<Method> subMethods = new ArrayList<>();
                                    List<Integer> subMethodsIndex = new ArrayList<>();
                                    for (int q = 0; q < problem.getMethods().size(); q++){
                                        Method m_prime = problem.getMethods().get(q);
                                        if (m_prime.getTask() == orderedTasks.get(j)) {
                                            subMethods.add(m_prime);
                                            subMethodsIndex.add(q);
                                        }
                                    }
                                    //set the constraint
                                    methodOutput = "constraint e_"
                                            + layer.getIndex() + "_" + i + " = "
                                            + ((cell.getMethodsIndex().get(k) + 1) * -1) + " -> (";
                                    pw.write(methodOutput.getBytes());
                                    String tmp = "";
                                    for (int q = 0; q < subMethods.size(); q++) {
                                        Method m_prime = subMethods.get(q);
                                        tmp = "e_"
                                                + (layer.getIndex() + 1) + "_"
                                                + (layer.getNext(i) + j) + " = "+((subMethodsIndex.get(q) + 1)*-1) +" \\/ ";
                                       pw.write(tmp.getBytes());
                                    }
                                    tmp = tmp.substring(0, tmp.length() - 3);
                                    methodOutput = "false);\n";
                                            
                                    pw.write(methodOutput.getBytes());
                                }
                            }
                            //Rule 15 - fill the noops
                            if (orderedTasks.size() < cell.getMaxE()) {
                                for (int j = orderedTasks.size(); j < cell.getMaxE(); j++) {
                                    methodOutput = "constraint e_"
                                            + layer.getIndex() + "_" + i + " = "+ ((cell.getMethodsIndex().get(k) + 1)*-1) +" -> e_"
                                            + (layer.getIndex() + 1) + "_"
                                            + (layer.getNext(i) + j) + " = 0; % noop\n";
                                    pw.write(methodOutput.getBytes());
                                }
                            }
                        }
                        output = "\n";
                        pw.write(output.getBytes());
                    }

                    return output;
                }
    
    
                public static String generateAllPrimitiveLayerConstraint_rule16(Layer layer, HTNProblem problem) {

                    String tmp = "";
                    for (int i = 0; i < layer.getCells().size(); i++) {
                        tmp += "e_" + layer.getIndex() + "_" + i + " >= 0 /\\ ";
                    }
                    tmp = tmp.substring(0, tmp.length() - 3);

                    return "constraint " + tmp + "; %RULE 16";
                }
                
                
    //generate a list of elements executable in the cell - element_[method/action id]_[layer]_[cell]
    public static boolean getCellDomain(Layer layer, HTNProblem problem, BufferedOutputStream pw,
            boolean writeCommentary) throws IOException {
        String mznOutput = "";
        if (writeCommentary) {
            mznOutput += "% -- cell action/method variables\n";
            mznOutput += "% -- element(layer, cell) = possible_action - indicates which action or reduction happens in this cell";
            mznOutput += "% -- primitive(layer, cell) - indicates if the task in the cell is primitive or not. A constraint will be used on this variable to ensure no action and reduction occurs simultaneously. PERSONAL NOTE: Not sure how useful it is, but maybe original TreeRex encoding's language had some quirk that justified creating this variable?\n";
        } else {
            mznOutput += "\n";
        }
        pw.write(mznOutput.getBytes());
        for (int i = 0; i < layer.getCells().size(); i++) {
            LayerCell cell = layer.getCells().get(i);
            pw.write("var {".getBytes());
            for (int j = 0; j < cell.getPrimitiveTasks().size(); j++) {
                pw.write(((cell.getPrimitiveTasksIndex().get(j) + 1) + ", ").getBytes());
            }
            for (int j = 0; j < cell.getMethods().size(); j++) {
                pw.write((((cell.getMethodsIndex().get(j) + 1) * -1) + ", ").getBytes());
            }

            if (cell.getNoop()) {
                pw.write((0 + ", ").getBytes());
            }

            //because the domain string ends with ", ", I will add first primitive task or first method or noop
            if (cell.getPrimitiveTasks().size() > 0) {

                pw.write(((cell.getPrimitiveTasksIndex().get(0) + 1) + "").getBytes());
            } else if (cell.getMethods().size() > 0) {

                pw.write((((cell.getMethodsIndex().get(0) + 1) * -1) + "").getBytes());
            } else {
                pw.write("0".getBytes());
            }

            pw.write(("}: e_" + layer.getIndex() + "_" + i + ";\n").getBytes());

        }
        return true;
    }
    
    
    //generates a list of facts that may be changes in this layer - holds_[fluent id]_[layer index]_[cell index]
    public static boolean getLayerFacts(Layer layer,  HTNProblem problem, BufferedOutputStream pw, boolean writeCommentary) throws IOException {
        String mznOutput = "";
        if (writeCommentary) {
            mznOutput += "% -- cell fact variables\n";
            mznOutput += "% -- clique(layer, cell) = fact indicates which fact from mutex clique holds in the layer cell\n";
            mznOutput += "% -1 in the clique domain stands for none_of_those (check Toropila thesis for info)\n";
        }
            pw.write(mznOutput.getBytes());

            //////////////
            List<Collection<Integer>> cliques = Strips2SasPlus.cliques;
            for (int i = 0; i < cliques.size();i++){
                Collection<Integer> clique = cliques.get(i);
                
                for (int cellIndex = 0; cellIndex < layer.getCells().size(); cellIndex++) {
                    mznOutput = "var {";
                    pw.write(mznOutput.getBytes());
                    String debug = "";
                    for (Integer j : clique) {
                        Fluent f = problem.getFluents().get(j);
                        //System.out.println(UtilFunctions.debug_fluentToString(f, problem));
                        mznOutput = j + ", ";
                        debug += UtilFunctions.debug_fluentToString(f, problem) + " ";
                        pw.write(mznOutput.getBytes());
                    }
                    String out = "-1}:c_"+i+"_" + layer.getIndex() + "_" + cellIndex + ";";
                    if(writeCommentary){
                        out += "%"+debug;
                    }
                    out += "\n";
                    pw.write((out).getBytes());
                }

            }
            
    
            return true;
    
        }


}
