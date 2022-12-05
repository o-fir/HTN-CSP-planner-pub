package uga.lig.csp_clean_wStaticFluents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Method;

public class Validator {

    public static List<ValidatorItem> methods = new ArrayList<>();
    public static List<ValidatorItem> primitiveTasks = new ArrayList<>();

    // load the solution into memory
    public static boolean parsePlan(String filepath) throws FileNotFoundException, IOException {
        methods = new ArrayList<>();
        primitiveTasks = new ArrayList<>();
        File file = new File(filepath);
        // read file line by line
        // extract only methods and primitive tasks, ignore predicates
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line
                line = line.substring(0, line.length() - 1);// remove ";"
                String[] data = line.split(" = ");
                String[] key = data[0].split("_");
                String name = key[0];
                // "e" for element
                if (name.contains("e") && !name.contains("c")) {
                    int value = Integer.parseInt(data[1]);
                    int id = -1;
                    int layer = Integer.parseInt(key[1]);
                    int cell = Integer.parseInt(key[2]);
                    boolean isMethod = false;
                    if (value < 0) {
                        isMethod = true;
                        id = Math.abs(value);
                    } else {
                        id = value;
                    }

                    if (isMethod) {
                        methods.add(new ValidatorItem(cell, layer, id, isMethod));
                    } else {
                        primitiveTasks.add(new ValidatorItem(cell, layer, id, isMethod));
                    }
                }
            }
        }
        return true;
    }

    public static void formatPlan(List<Layer> network, HTNProblem problem) {
        String fileOutput = "";
        // PART 1 - primitive actions of the final layer
        String part1 = "==>\n";
        fileOutput += "==>\\n";
        // extract primitive actions performed in the final layer
        // ignore last cell, as last cell is a placeholder for goal
        Layer finalLayer = network.get(network.size() - 1);
        for (int i = 0; i < finalLayer.getCells().size() - 1; i++) {
            LayerCell cell = finalLayer.getCells().get(i);

            // find which action was applied in the cell
            ValidatorItem valItem = null;
            boolean found = false;
            int j = 0;
            while (!found && j < primitiveTasks.size()) {
                valItem = primitiveTasks.get(j);
                if (valItem.getLayer() == finalLayer.getIndex() && valItem.getCell() == i) {
                    found = true;
                }
                j++;
            }

            // ignore all noops, as they aren't part of the plan
            if (valItem.getId() != 0 && found) {
                // prepare the formatted string
                // because we may see tasks with repeating pddl4j ids, id will be the layer+cell
                // (note + stands for concat, not addition). Note, we also add 1 at the
                // beginning, because pandaParser has problem with things that start with 0
                String taskOutput = "1" + valItem.getLayer() + "" + valItem.getCell() + " ";
                // step 1.1 - get name
                // NOTE THAT WHEN BUILDING MINIZINC INPUT, WE HAVE INCREMENTED ID BY 1, SO THAT
                // 0 HOLDS NOOP. NOW WE DECREMENT IT BACK
                Task task = problem.getTasks().get(valItem.getId() - 1);
                taskOutput += problem.getTaskSymbols().get(task.getSymbol()) + " ";
                // step 1.2 - get task parameters
                int[] args = task.getArguments();
                for (int arg : args) {
                    taskOutput += problem.getConstantSymbols().get(arg) + " ";

                }
                // step 1.2.a - remove the final white space
                taskOutput = taskOutput.substring(0, taskOutput.length() - 1);
                /*
                 * taskOutput += " | element_" + (valItem.getId()) + "_" + valItem.getLayer() +
                 * "_"
                 * + valItem.getCell();
                 */// DEBUG
                part1 += taskOutput + "\n";

                fileOutput += taskOutput + "\\n";
            }

        }
        System.out.println(part1);
        // PART 2 - non-primitive actions (decompositions + methods)
        String part2 = "";
        // step 2.1
        // defining root - initial task network
        String root = "root ";
        // +
        // step 2.2
        // decompose layer by layer - note, that layers are by definition totally
        // ordered. Ignore last layer, as it's totally primitive by definition
        for (int q = 0; q < network.size() - 1; q++) {
            Layer layer = network.get(q);
            // for every cell (ignore last cell, as it's a placeholder for goal)
            for (int i = 0; i < layer.getCells().size() - 1; i++) {
                LayerCell cell = layer.getCells().get(i);
                // check which action was applied by the solver for the cell
                ValidatorItem valItem = methods.get(0);
                boolean found = false;
                int j = 0;
                while (!found && j < methods.size()) {
                    valItem = methods.get(j);
                    if (valItem.getLayer() == layer.getIndex() && valItem.getCell() == i) {
                        found = true;
                    }
                    j++;
                }

                String output = "";
                // if method was applied in the cell, find the corresponding task and do the
                // formatting
                if (valItem.getIsMethod()) {
                    Method m = problem.getMethods().get(valItem.getId() - 1);
                    Task t = problem.getTasks().get(m.getTask());
                    output += "1" + valItem.getLayer() + "" + valItem.getCell() + " "
                            + problem.getTaskSymbols().get(t.getSymbol()) + " "; // we use (taskId + methodId) instead
                                                                                 // of taksId, because validator doesn't
                                                                                 // accept recurring taskIds, and our
                                                                                 // plan may have repeating abstract
                                                                                 // tasks, which have different methods
                                                                                 // applied to them
                    // get task parameters
                    int[] args = t.getArguments();
                    for (int arg : args) {
                        output += problem.getConstantSymbols().get(arg) + " ";
                    }
                    // specify method
                    output += "-> " + m.getName() + " ";
                    // specify children
                    // a. go through children cells
                    for (int childCellIterator = network.get(q).getNext(i); childCellIterator < network.get(q)
                            .getNext(i) + cell.getMaxE(); childCellIterator++) {
                        // b. find which action was applied at the child cell
                        ValidatorItem valChild = null;
                        boolean foundChild = false;

                        int z = 0;
                        // CASE 1. Primitive child
                        while (!foundChild && z < primitiveTasks.size()) {
                            valChild = primitiveTasks.get(z);
                            // ignore noops
                            if (valChild.getId() != 0) {
                                if (valChild.getLayer() == network.get(q + 1).getIndex()
                                        && valChild.getCell() == childCellIterator) {
                                    foundChild = true;
                                }
                            }
                            z++;
                        }
                        // if the primitive child appears sometime before the final layer, find it in
                        // the final layer
                        /*
                         * if (foundChild && q < network.size() - 2) {
                         * //loop through all lower layers
                         * for (z = q + 1; z < network.size() - 1; z++) {
                         * int newPos = network.get(valChild.getLayer()).getNext(valChild.getCell());
                         * valChild.setCell(newPos);
                         * valChild.setLayer(z);
                         * }
                         * }
                         */
                        // CASE 2. Non-primitive child
                        if (!foundChild) {
                            int methodIterator = 0;
                            while (!foundChild && methodIterator < methods.size()) {
                                valChild = methods.get(methodIterator);
                                if (valChild.getLayer() == network.get(q + 1).getIndex()
                                        && valChild.getCell() == childCellIterator) {
                                    foundChild = true;
                                }
                                methodIterator++;
                            }

                            // if we found a method (aka non-primitive task), we don't need to attempt
                            // decomposition to find the method in final layer, like we did with primitive
                            // children
                        }
                        // append the task mapping to output
                        if (foundChild) {
                            output += "1" + valChild.getLayer() + "" + valChild.getCell() + " ";
                        } else {
                            // output += "child not found ";
                        }
                    }

                    part2 += output + "\n";

                    fileOutput += output + "\\n";
                    // populate root
                    if (q == 0) {
                        root += "1" + valItem.getLayer() + "" + valItem.getCell() + " ";
                    }
                }
                // if primitive task was applied in the cell, ignore it - part 1 deals with
                // primitive tasks

            }
        }
        System.out.println(root);
        System.out.println(part2);
        System.out.println("<==");

        // write to file
        File file = new File("C:/Users/oleksandr.firsov/Desktop/TEEX_PLAN.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println(fileOutput + "<==");
            pw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
