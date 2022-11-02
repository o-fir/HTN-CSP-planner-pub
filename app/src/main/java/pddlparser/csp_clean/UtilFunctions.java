package pddlparser.csp_clean;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.problem.operator.Method;
import fr.uga.pddl4j.problem.operator.OrderingConstraintSet;
import fr.uga.pddl4j.util.BitVector;

public class UtilFunctions {

    //returns a totally ordered list of task ids of "tasks" in an order that satisfies the ordering  "orderingConstraints"
    //VERY IMPORTANT NOTE: WE ASSUME THAT THIS FUNCTION IS DETERMINISTIC, because first we use it to determine what values can a cell take when decomposing layer L into layer L+1, and then we use it when applying Rules 13-14 of TreeRex encoding. If the function is non-deterministic, rule 13-14 may create a constraint that points to a cell that can't take the needed value, if this makes sense. If not - check where totallyOrderedList function is used, and you'll figure it out
    static List<Integer> totallyOrderedList(List<Integer> tasks, OrderingConstraintSet orderingConstraints) {
        List<List<String>> orderList = UtilFunctions
                        .orderingConstraintsToStringList(orderingConstraints);
        //tmp store the position of each task from "tasks" in the "tasks_totallyOrdered"
        //we need this, because ordering constraints of task T and task T itself share the same position index in their respective lists
        //even though we can find an index of any task from tasks_totallyOrdered in tasks via .indexOf(), we need to remember that a network can have multiple tasks that share the same id, but have different ordering constraints. For example, we want to clean the room at the start and end of the plan. Both actions have same ids, but different ordering constraints (first clean must precede everyone, last clean should supercede everyion). indexOf() may confuse which is which, which is why tmp will store the positions of every action, rather than their ids.
        List< Integer> tmp = new ArrayList<>();
        
        //populate tmp
        for (int i = 0; i < tasks.size(); i++) {
            tmp.add(i);
        }
        

                //for every task
                for (int i = 0; i < orderList.size(); i++) {
                    //go through all ordering relations
                    for (int j = 0; j < orderList.get(i).size(); j++) {
                        if (j != i) {
                            //if task j should be executed after task i
                            if (orderList.get(i).get(j).contains("1")) {
                                //System.out.println(tasks.get(i) + " < " + tasks.get(j));
                                //System.out.println(i + " < " + j);
                                //System.out.println(tmp);
                                //but if task j is positioned before task i
                                if (tmp.indexOf(j) < tmp.indexOf(i)) {
                                    //put task j right after task i
                                    //System.out.println("pre-delete:" + tmp);
                                    tmp.remove(tmp.indexOf(j));
                                    //System.out.println("post-delete:" + tmp);
                                    tmp.add(tmp.indexOf(i), j);
                                }
                            }
                        }

                    }
                }
                
                //now that tmp contains the properly ordered indices, we can move the tasks that correspond to these indices
        List<Integer> tasks_totallyOrdered = new ArrayList<>();
        for (int index : tmp) {
            tasks_totallyOrdered.add(tasks.get(index));
        }
                return tasks_totallyOrdered;
    }

    //Note: for every task i returns a list of 0/1. 1 indicates that a task j happens after task i. 0 means that order doesnt matter
    static List<List<String>> orderingConstraintsToStringList(OrderingConstraintSet bv) {
        // TODO...
        // again, an ugly section, bcz I don't know how to iterate through bitvector,
        // maybe imporve
        String unrefined = bv.toBitString();
        String[] unrefinedList = unrefined.split("\\r?\\n");
        List<List<String>> tmp = new ArrayList<>();
        for (String s : unrefinedList) {
            String[] tmp2 = s.split(" ");
            List<String> tmp3 = new ArrayList<>();
            for (String ss : tmp2) {
                tmp3.add(ss);
            }
            tmp.add(tmp3);
        }
        return tmp;

    }

    static List<Integer> bitVectorToIntList(BitVector bv) {
        List<Integer> intL = new ArrayList<>();
        // TODO....
        // an ugly section, bcz I don't know how to iterate through bitvector
        if (bv.isEmpty()) {
            return intL;
        }
        String tmp = bv.toString();
        if (tmp.isEmpty()) {
            return intL;
        } else {
            tmp = tmp.substring(1, tmp.length() - 1);
            List<String> strL = Arrays.asList(tmp.split("\\s*,\\s*"));
            for (String s : strL)
                intL.add(Integer.valueOf(s));
            return intL;
        }
    }

    //function that converts item from bitvector into a proper index. An alternative for bitVectorToIntList
    public Integer conversionOtoC(Integer original, Integer step, Integer total_size) { return original + (step * total_size) + 1; }

    

    //verifies if the fluent in question has a possibility to change somewhere
    static List<Boolean> fluentIsStatic = new ArrayList<Boolean>();

    public static boolean checkFluentNotStatic(int i, HTNProblem problem) {
        //initialize the table with values for all fluents at the VERY FIRST call to this function during the app lifetime
        
        if (fluentIsStatic.isEmpty()) {
            for (Fluent f : problem.getFluents()) {
                boolean isUsedSomewhere = false;
        
                for (Action a : problem.getActions()) {
                    if (fluentIsUsedByAction(a, f, problem)) {
                        isUsedSomewhere = true;
                        break;
                    }
                }
                fluentIsStatic.add(!isUsedSomewhere);
            }
        }
        //when the table is initialized, this function just looks up values in the said table
        return !fluentIsStatic.get(i);
    }



        public static boolean fluentIsUsedByAction(Action a, Fluent f, HTNProblem problem) {

            if (a.getUnconditionalEffect().getNegativeFluents()
                    .get(problem.getFluents().indexOf(f))
                    || a.getUnconditionalEffect().getPositiveFluents()
                            .get(problem.getFluents().indexOf(f))) {
                return true;
            } else {
                for (ConditionalEffect e : a.getConditionalEffects()) {
                    if (e.getEffect().getNegativeFluents()
                            .get(problem.getFluents().indexOf(f))
                            || e.getEffect().getPositiveFluents()
                                    .get(problem.getFluents().indexOf(f))) {
                        return true;
                    }
                }
            }
            return false;
        }
        

        //find the action that corresponds to a task
        public static Action taskToAction(Task t, HTNProblem problem) {
            /////////////////////////////
            //TODO maybe redo this method? getTaskResolvers() func presumably returns a LIST of operators for a task
            //not a single operator, so it's possible I'm missing something
            List<Integer> taskResolvers = problem.getTaskResolvers().get(problem.getTasks().indexOf(t));
            if(taskResolvers.isEmpty()){
                return null;
            } else {
                Action a = problem.getActions().get(taskResolvers.get(0));
                return a;
            }
            
            
        }
        
        public static String taskToStringDebug(int taskId, HTNProblem problem) {
            String output = "";
            if (taskId >= 0) {
                Task task = problem.getTasks().get(taskId);
                output += problem.getTasks().indexOf(task) + " " + problem.getTaskSymbols().get(task.getSymbol()) + " ";
                int[] argums = task.getArguments();
                for (int arg : argums) {
                    output += problem.getConstantSymbols().get(arg) + " ";
                }
            } else {
                return "noop";
            }

            return output;
        }
        

        static String debug_fluentToString(Fluent f, HTNProblem problem) {
            String out = "";
            String symbol = problem.getPredicateSymbols().get(f.getSymbol());
            out += symbol + "(";
            int[] args = f.getArguments();
            for (int i = 0; i < args.length; i++) {
                out += problem.getConstantSymbols().get(args[i]) + " ";
            }
            out += ")";
            return out;

        }
        static String debug_actionToString(Action a, HTNProblem problem) {
            String out = "";
            String symbol = a.getName();
            out += symbol + "(";
            int[] args = a.getInstantiations();

            for (int i = 0; i < args.length; i++) {
                out += problem.getConstantSymbols().get(args[i]) + " ";
            }
            out += ")";
            return out;

        }

        static String debug_methodToString(Method m, HTNProblem problem) {
            String out = "";
            String symbol = m.getName();
            out += symbol + "(";
            int[] args = m.getInstantiations();

            for (int i = 0; i < args.length; i++) {
                out += problem.getConstantSymbols().get(args[i]) + " ";
            }
            out += ")";
            return out;

        }
        
        static void debug_layerToString(Layer l, HTNProblem problem) {
            
            for (int i = 0; i < l.getCells().size(); i++) {

                System.out.println("Layer " + l.getIndex() + " Cell " + i);
                for (int id : l.getCells().get(i).getPrimitiveTasksIndex()) {
                    Action a = UtilFunctions.taskToAction(problem.getTasks().get(id), problem);
                    System.out.println(id +" " +UtilFunctions.debug_actionToString(a, problem));
                }
                for (int id : l.getCells().get(i).getMethodsIndex()) {
                    
                    System.out.println(id+" "+UtilFunctions.debug_methodToString(problem.getMethods().get(id), problem));
                }
            }
        }
    

}
