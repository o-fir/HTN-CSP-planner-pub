package uga.lig.csp_clean_wStaticFluents;

import java.util.ArrayList;
import java.util.List;

import fr.uga.pddl4j.problem.HTNProblem;
import fr.uga.pddl4j.problem.Task;
import fr.uga.pddl4j.problem.operator.Method;

public class LayerCell {
    private List<Task> primitiveTasks;
    private List<Method> methods;
    private List<Integer> primitiveTasksIndex;
    private List<Integer> methodsIndex;
    private boolean hasNoop;
    private int maxE;

    public LayerCell(){
        this.primitiveTasks = new ArrayList<Task>();
        this.methods = new ArrayList<Method>();
        this.primitiveTasksIndex = new ArrayList<Integer>();
        this.methodsIndex = new ArrayList<Integer>();
        this.maxE = 0;
        hasNoop = false;
    }

    public void addTask(Task task, int index) {
        if (!this.primitiveTasks.contains(task)) {
            this.primitiveTasks.add(task);
            this.primitiveTasksIndex.add(index);
            if (maxE < 1) {
                maxE = 1;
            }
        }
    }
    
    public void addMethod(Method method, int index) {
        if (!this.methods.contains(method)) {
            this.methods.add(method);
            this.methodsIndex.add(index);
            if (maxE < method.getSubTasks().size()) {
                maxE = method.getSubTasks().size();
            }
        }
    }

    public void addNoop() {
        this.hasNoop = true;
        if(this.maxE < 1){
            this.maxE = 1;
        }
    }

    public boolean getNoop() {
        return this.hasNoop;
    }

    public String toString(HTNProblem problem) {
        String sortie = "";
        for (Task t : primitiveTasks) {
            sortie += (problem.getTasks().indexOf(t) + 1) + " ";
        }
        for (Method m : methods) {
            sortie += "("+((problem.getMethods().indexOf(m) + 1) * -1) + " "+ m.getName()+")" + " | ";
        }
        return sortie;
    }

    public Integer getMaxE() {
        return this.maxE;
    }

    public List<Task> getPrimitiveTasks() {
        return this.primitiveTasks;
    }

    public List<Method> getMethods() {
        return this.methods;
    }
    public List<Integer> getMethodsIndex() {
        return this.methodsIndex;
    }
    public List<Integer> getPrimitiveTasksIndex() {
        return this.primitiveTasksIndex;
    }
    
}
