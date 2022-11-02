# HTN CSP Planner README

### Structure 
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

### Performance evaluation
 Because `Transport` benchmark domain is very intuitive, and the first couple of problems are very simple, I found it great for evaluating the overall functionality of the system, and it's my go-to benchmark whenever I encounter bugs.
 
 `Childsnack` benchmarkhas much more methods, actions, and facts, compared to `Transport`. Thus, I use it to evaluate the efficiency of the system.

Below, you can find the time tables for problems 1-5 for each benchmark:
| TRANSPORT | 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time |	100% |	100% |	100% |	100% |	100% |
PDDL4J |	33%	 |17%	 |27% |	8% |	10% |
Minizinc total |	60%	 |69% |	57%	 |80% |	78%
Java encoding |	3% |	7% |	12% |	8% |	6%
Solve time |	0% |	0% |	0% |	0% |	0%


| CHILDSNACK | 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time | 	100% | 	100% | 	100% | 	- | 	-
PDDL4J | 	12% | 	10%	 | 7% | 	- | 	-
Minizinc total | 	64% | 	58% | 	68%	 | -	 | -
Java encoding | 	18% | 	22% | 	18% | - | 	-
Solve time | 	0% | 	0% | 	0% | 	- | 	-

Childsnack non-SAS not included for problems 4-5 because total execution takes more than 2 minutes.

CHILDSNACK SAS	| 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time | 	100% | 	100% | 	100% | 	100% | 	100%
PDDL4J | 	3% | 	2% | 	3% | 	2% | 	3%
Minizinc total | 	19% | 	20% | 	25%	 | 28%	 | 22%
Java encoding | 	76% | 	75% | 	70%	 | 66% | 	74%
Solve time | 	0% | 	0%	 | 0% | 	0% | 	0%

or in milliseconds:
| TRANSPORT | 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time | 	1224 | 	3209 | 	1939 | 	6166 | 	4386
PDDL4J | 	414 | 	575 | 	535 | 	507 | 	479
Minizinc total | 	737 | 	2219 | 	1113 | 	4975 | 	3430
Java encoding | 	43 | 	254 | 	246 | 	508 | 	285
Solve time | 	0 | 	12 | 	5 | 	28 | 	29

| CHILDSNACK | 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time | 	9236 | 	16006 | 	14490 | 	- | 	-
PDDL4J | 	1129 | 	1621 | 	1110 | 	- | 	-
Minizinc total | 	6002 | 	9312 | 	9926 | 	- | 	-
Java encoding | 	1675 | 	3596 | 	2640 | 	- | 	-
Solve time | 	57 | 	84 | 	88 | 	- | 	-

CHILDSNACK SAS| 1 | 2 | 3| 4 | 5|
| -- | -- | -- | -- | -- | -- |
System time	 | 39138 | 	40016 | 	36554 | 	70557 | 	92030
PDDL4J | 	1241 | 	986 | 	1140 | 	2047 | 	2798
Minizinc total | 	7705 | 	8191 | 	9236 | 	20054 | 	20759
Java encoding | 	29800 | 	30363 | 	25671 | 	47098 | 	67254
Solve time | 	139 | 	82 | 	104	 | 208 | 	83

**`Notice the difference`** between Childsnack with and without SAS. SAS takes additional time, but produces more compact encoding, which is vital for the current system (minizinc parses large files inefficiently). Without SAS encoding, Childsnack problem 4 takes ~18 minutes, with 99% of time spent in minizinc. With SAS, Childsnack problem 4 takes ~1 minute, with 28% time spent in minizinc.
