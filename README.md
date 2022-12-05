
# HTN CSP Planner README
### Data Structures
For easy generation of minizinc constraints, I have created the following two data structures:

**1. Layer**
Layer is a collection of cells. And the tree is a list of layers.

```java
public  class  Layer {
    private  List<LayerCell> cells;
    private  List<Integer> next;
    private  int layerIndex;
}
``` 

The variable `next` indicates the start index of each cell in the lower layer. For example, if we have cells **A**, **B** in layer 1, and **A** can be reduced into a sequence of 3 cells in layer 2, then `next` for **B** will be 4.

The variable `layerIndex` indicates the depth of the current layer in the tree.

**2. Layer Cell**
A cell contains all the methods and primitive tasks that can be possibly executed in this cell.
```java
public  class  LayerCell {
    private  List<Task> primitiveTasks;
    private  List<Method> methods;
    private  List<Integer> primitiveTasksIndex;
    private  List<Integer> methodsIndex;
    private  boolean hasNoop;
    private  int maxE;
}
```

`hasNoop` indicates if the cell can be empty.
`maxE` indicates the maximum number of cells that the current cell can be reduced to. If the cell has 2 methods **A** and **B** of length 3 and 5 respectively, `maxE = 5`.

### Benchmarking
The execution times for the `CHILDSNACK, TRANSPORT, SATELLITE, ROVER, FACTORIES, DEPOTS, BLOCKSWORLD ` benchmarks  can be found here: 
https://docs.google.com/spreadsheets/d/1ZqlqHV4ZCYHVv72KFkKGbzqhbpnesn2gfeIepcVeNbc/edit?usp=sharing

Of all the benchmarks, Java-part of the planner takes most time in `Childsnack`. This may be explained by the fact that `Childsnack` has only 2 methods - `serve with gluten` and `serve without gluten`. So there are only 2 layers in the problem, and the cells of the first layer contain *all the methods*, and the cells of the second layer contain *all the possible tasks*. 

After integrating memory profiling into the planner, and launching it on **Childsnack Problem 15**, we obtain the following picture for the Java part of the execution:
![](https://i.ibb.co/0C63sM2/heap-Evolution.png)
The majority of time is spent on 2 blocks of code.
**P2** is a part of instantiation of the initial layer, which generates a `List<LayerCell> cells`, and corresponds to the following chunk of code:
```java
List<LayerCell> cells = new ArrayList<>();
          // 2.1 Transform every task into a cell
          for (int i = 0; i < tasks_totallyOrdered.size(); i++) {
            LayerCell cell = new LayerCell();
            Task t = problem.getTasks().get(tasks_totallyOrdered.get(i));
            if (t.isPrimtive()) {
              // if task is primitive - add it to the cell
              cell.addTask(t, tasks_totallyOrdered.get(i));
            } else {
              // if task is non-primitive, add all methods, applicable to this
              // task, to the cell
              for (int j = 0; j < problem.getMethods().size(); j++) {
                Method m = problem.getMethods().get(j);
                if (m.getTask() == tasks_totallyOrdered.get(i)) {
                  cell.addMethod(m, j);
                }
              }
            }
            cells.add(cell);
          }
```

**frameAxioms for layer 2** goes through every action of every cell, and checks which predicates can be changed at each cell (and by which action). The problem section is:
```java
    // these 2 lists indicate which tasks of the current cell affect current fluent
    List<Integer> negTaskIds = new ArrayList<Integer>();
    List<Integer> posTaskIds = new ArrayList<Integer>();

    LayerCell cell = layer.getCells().get(i);
    //go through every primitive task
    for (int k = 0; k < cell.getPrimitiveTasks().size(); k++) {
        int taskIndex = cell.getPrimitiveTasksIndex().get(k);
                    
        Task t = cell.getPrimitiveTasks().get(k);
        //convert task to action to get the effects
        Action a = UtilFunctions.taskToAction(t, problem);
                    
        // TODO: DOES NOT SUPPORT CONDITIONAL EFFECTS!!!!!!
        if (a.getUnconditionalEffect().getNegativeFluents().get(predId)) {
            negTaskIds.add(taskIndex + 1);
        } else if (a.getUnconditionalEffect().getPositiveFluents()
                            .get(predId)) {
            posTaskIds.add(taskIndex + 1);
        }
    }
```
80% of the time is taken by the `taskToAction` function:
```java
    // find the action that corresponds to a task
    // this function is inefficient and can take over 10% of total exec time
    public static Action taskToAction(Task t, HTNProblem problem) {
        List<Integer> taskResolvers = problem.getTaskResolvers().get(problem.getTasks().indexOf(t));
        if (taskResolvers.isEmpty()) {
            return null;
        } else {
            Action a = problem.getActions().get(taskResolvers.get(0));
            return a;
        }

    }
```
And 15% of the time is taken by:
```java
// TODO: DOES NOT SUPPORT CONDITIONAL EFFECTS!!!!!!
if (a.getUnconditionalEffect().getNegativeFluents().get(predId)) {
    negTaskIds.add(taskIndex + 1);
} else if (a.getUnconditionalEffect().getPositiveFluents()
                            .get(predId)) {
    posTaskIds.add(taskIndex + 1);
}
```
So, for **Childsnack 15**, for example, it took 47 seconds to run all the `taskToAction`s, and 10s to populate the `negTaskIds` and `posTaskIds`. While the rest of the code (getting tasks, writing to file etc) took 1.5 seconds in total. 

### 🔴🔴🔴 Benchmarking (VERY BRIEFLY)🔴🔴🔴
I have prepared the benchmark execution times in the following table:
https://docs.google.com/spreadsheets/d/1ZqlqHV4ZCYHVv72KFkKGbzqhbpnesn2gfeIepcVeNbc/edit?usp=sharing

After running the planner on **Childsnack** (domain has 2 methods - `serve with/without gluten`, therefore the tree will have only 2 layers) **Problem 15**, I get the following memory profile:
![](https://i.ibb.co/0C63sM2/heap-Evolution.png)

Problem in **P2** happens when I'm instantiating the first layer data structure, and essentially corresponds to
```java
for (int j = 0; j < problem.getMethods().size(); j++) {
                Method m = problem.getMethods().get(j);
                if (m.getTask() == tasks_totallyOrdered.get(i)) {
                  cell.addMethod(m, j);
                }
              }
```
where `cell` is a data structure containing possible actions and methods executable in this cell.

Problem in **frameAxioms for layer 2** essentially corresponds to a utility function that transforms tasks into actions (to get the effects for the frame axioms):
```java
    // find the action that corresponds to a task
    // this function is inefficient and can take over 10% of total exec time
    public static Action taskToAction(Task t, HTNProblem problem) {
        List<Integer> taskResolvers = problem.getTaskResolvers().get(problem.getTasks().indexOf(t));
        if (taskResolvers.isEmpty()) {
            return null;
        } else {
            Action a = problem.getActions().get(taskResolvers.get(0));
            return a;
        }

    }
```



### File Structure 
| File |Function|
| ------ | ------ |
| **CoreMain.java** | Main file. PDDL4J parsing and encoding functions are called from here |
| **Layer.java** | Defines ```Layer``` data structure. Contains method `expandLayer` that generates layer `L+1` from layer `L` |
| **LayerCell.java** | Layers are composed of cells. `LayerCell` data structure manages information relative to a single cell |
| **Strips2SasPlus.java** | Manages STRIPS to SAS+ translation process |
| **GeneratorFunctions.java** | Currently, a collection of methods that write encoding rules to a MiniZinc file. In theory, to switch from MiniZinc to some Java API one needs only to edit `GeneratorFunctions.java`. |
| **UtilFunctions.java** | Contains a number of utility, mainly debug, functions. |
|**Validator.java** and **ValidatorItem.java**|Translates MiniZinc output into a format used by the plan validator from PANDA Framework|

### Usage
In **`CoreMain.java`**:
1. Edit `benchmarkPath` variable to point to the benchmark folder
2. Edit `outputPath` variable to point to some work directory - upon execution, the program will generate 3 files inside of it: (1) encoded problem without the final layer rule, (2) encoded problem with the final layer rule, and (3) solution. 
3. Edit `domainPath` and `problemPath` to point to your benchmark  
3. You can enable/disable SAS+ via `skipSAS` boolean

Once the `benchmarkPath` and `outputPath` are edited - compile. Currently, the program needs to be compiled for each execution.


### Console output
While the program writes encoding to file, we also output information in console to track the execution process.
A layer is encoded via a number of rules, when we begin encoding a certain layer, a message `Rules for layer XXX` will appear. `r XXX encoded` indicates that the rule has been written to a file. `r XXX redundant` means that the rule has been skipped, as it's redundant in CSP. Thus, if, for example, program is stuck after `r 5 encoded`, we know that `rule 6` is taking a long time to encode. 

Note, that unlike the rest of the layers, the last layer does not need **rules 10-15**, which "establish links" between layers **L** and **L+1**. Therefore, when we expand layers after the problem has been found `UNSAT`, we "append" these rules to what was previously last layer. In this case, in the console log you will see `Appending rules to layer XXX`.

When the problem is solved, **cumulative execution times** will be outputted. Meaning that, for example, `solve time` will indicate a sum of solve times of all solving attempts.

| Indicator |Meaning|
| ------ | ------ |
| Total time spent| Time since the start of the execution, until the very end|
| PDDL4J| Time from parsing the file to instantiating HTN problem |
|Minizinc total | The time spent since calling the minizinc command in the command line, until its finished. This means, this time includes reading the text file, flattening the problem (simplifying it and formatting for a specific solver), solving the problem, and outputting the solution.|
| Java encoding| The time to process the HTN problem and write encoding rules to a file|
| Solve time only| Time spent by the solver to solve the problem |
Java encoding is further decomposed into:
| Indicator |Meaning|
| ------ | ------ |
|SAS encoding | Time spent on the translation process |
|decomposeLayer() | Time spent to generate `Layer` data structure for L+1 from layer L |
|getCellDomain() | Time to write cell variables (cells indicate which action/method has been selected) in MiniZinc file |
|getLayerFacts() | Time to write fact variables for a layer |
|rule XXX | Time to write encoding rule |
|file copy | As we expand the problem, the definition of the last layer changes, thus some encoding rules specific only for the last layer must be edited to fit the new last layer. Current approach maintains a "universal" file without last layer rules encoded, and creates a copy with last layer rules appended, when needed. This way we don't need to rewrite the whole encoding every time. |
