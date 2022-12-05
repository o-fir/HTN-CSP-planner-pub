package uga.lig.csp_clean_layerCellsWactions;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.Method;

public class Layer {
    private List<LayerCell> cells;
    private List<Integer> next;
    private int layerIndex; // index of the layer

    public Layer(int layerIndex) {
        this.cells = new ArrayList<LayerCell>();
        this.next = new ArrayList<Integer>();
        this.layerIndex = layerIndex;
    }

    public int getIndex() {
        return this.layerIndex;
    }

    public int getNext(int i) {
        return this.next.get(i);
    }

    public void setCells(List<LayerCell> cells) {
        this.cells = cells;
        this.next.clear();
        this.next.add(0);
        for (int i = 1; i < cells.size(); i++) {
            LayerCell cell = cells.get(i - 1);
            this.next.add(cell.getMaxE() + next.get(next.size() - 1));
        }
    }

    public void addCells(List<LayerCell> newCells) {
        int initTail = cells.size() - 1;
        for (int i = 0; i < newCells.size(); i++) {
            this.cells.add(newCells.get(i));
        }
        for (int i = initTail + 1; i < cells.size(); i++) {
            LayerCell cell = cells.get(i - 1);
            this.next.add(cell.getMaxE() + next.get(next.size() - 1));
        }
    }

    public String toString() {
        String maxE = "[";
        String next = "[";
        for (int i = 0; i < cells.size(); i++) {
            maxE += "(" + i + ", " + cells.get(i).getMaxE() + ") ";
            next += "(" + i + ", " + this.next.get(i) + ") ";
        }

        return "maxE = " + maxE + "] \nnext = " + next + "]";
    }

    public void debugCells(HTNProblem problem) {
        for (int i = 0; i < this.cells.size(); i++) {
            LayerCell c = this.cells.get(i);
            System.out.println("Cell " + i + "; Layer " + this.getIndex());
            System.out.println("Methods:");
            for (Method m : c.getMethods()) {
                System.out.println(UtilFunctions.debug_methodToString(m, problem));
            }
            System.out.println("Actions:");
            for (Action t : c.getPrimitiveTasks()) {

                System.out.println(UtilFunctions.taskToStringDebug(problem.getTasks().indexOf(t), problem));
            }
            System.out.println("Noop:" + c.getNoop());
            System.out.println("MaxE:" + c.getMaxE());
            System.out.println("---");
        }
    }

    public List<LayerCell> getCells() {
        return this.cells;
    }

    // decomposeLayer expands layer L into layer L+1. Return null if cannot be
    // decomposed further (no methods)
    public Layer decomposeLayer(HTNProblem problem) {
        Scanner userInput2 = new Scanner(System.in);
        String input2;
        Layer nextLayer = new Layer(this.layerIndex + 1);

        boolean canBeDecomposed = false;
        // check if there is any sense in expanding layer
        /*
         * System.out.println("DECOMPOSING INTO LAYER "+(layerIndex+1));
         * System.out.println("initLayer cells: " + this.getCells().size());
         * System.out.println("initLayer cellsNext: " + this.next);
         * System.out.println("initLayer toString: " + this.toString());
         */
        for (LayerCell c : this.cells) {
            if (c.getMethods().size() > 0) {
                canBeDecomposed = true;
                break;
            }

            /*
             * System.out.println(c.getMethods());
             * System.out.println("proceed ?");
             * input2 = userInput2.nextLine();
             */
        }

        if (!canBeDecomposed) {
            System.out.println("% NOTHING TO DECOMPOSE IN THIS LAYER. Duplicating original layer");
            return null;
        }
        // Decompose every cell into a set of cells
        for (int i = 0; i < this.cells.size(); i++) {
            LayerCell initCell = this.cells.get(i);
            // expand the cell - !!REMEMBER!! every cell contains the possible
            // actions/methods executable from this cell.
            List<LayerCell> expansion = new ArrayList<>();
            // first we populate expansion with maxE empty cells
            for (int j = 0; j < initCell.getMaxE(); j++) {
                expansion.add(new LayerCell());
            }
            // if init cell was a noop cell, expanded cell is a noop as well
            if (initCell.getNoop()) {
                expansion.get(expansion.size() - 1).addNoop();
            }
            // every possible primitive action is executed in cell 0. The rest of the cells
            // are populated with noop.
            for (int k = 0; k < initCell.getPrimitiveTasks().size(); k++) {
                // System.out.println("expanded primitive: " +
                // initCell.getPrimitiveTasks().get(k));
                Action t = initCell.getPrimitiveTasks().get(k);

                int tIndex = initCell.getPrimitiveTasksIndex().get(k);
                expansion.get(0).addTask(t, tIndex);
                if (expansion.size() > 1) {
                    for (int j = 1; j < expansion.size(); j++) {
                        expansion.get(j).addNoop();
                    }
                }
            }
            // treating methods
            for (Method m : initCell.getMethods()) {
                // first transform the subnetwork into a totally ordered list
                List<Integer> tasks_totallyOrdered = UtilFunctions
                        .totallyOrderedList(m.getSubTasks(),
                                m.getOrderingConstraints());
                // System.out.println("Method children: " + tasks_totallyOrdered.size());
                // fill the expansion with the items from the totally ordered list, fill the
                // "leftovers" with noop
                for (int j = 0; j < expansion.size(); j++) {
                    if (j < tasks_totallyOrdered.size()) {
                        Task t = problem.getTasks().get(tasks_totallyOrdered.get(j));
                        // if task is primitive, add it to the expansion
                        if (t.isPrimtive()) {
                            Action a = UtilFunctions.taskToAction_DEPRECATED(t, problem);
                            expansion.get(j).addTask(a, tasks_totallyOrdered.get(j));
                        }
                        // if task is non-primitive, find all of its methods, and add them to the cell
                        else {
                            for (int k = 0; k < problem.getMethods().size(); k++) {
                                Method m_prime = problem.getMethods().get(k);
                                if (m_prime.getTask() == tasks_totallyOrdered.get(j)) {
                                    expansion.get(j).addMethod(m_prime, k);
                                }
                            }
                        }
                    } else {
                        expansion.get(j).addNoop();
                    }
                }
            }

            // append the expanded cell to the new layer
            if (nextLayer.getCells().isEmpty()) {
                nextLayer.setCells(expansion);
            } else {
                nextLayer.addCells(expansion);
            }

        }

        return nextLayer;
    }

}
