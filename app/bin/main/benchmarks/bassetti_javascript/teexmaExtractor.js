//PROBLEM CONFIG
let resNum = 20; //number of resources
let taskNum = 100; //number of tasks
let slotsNum = 100; //number of timeslots


//STEP 0: GET RESOURCES
let resList = ganttManager.txGantt.resources.resourcesStore.getItems()
      .map((a) => ({sort: Math.random(), value: a}))
      .sort((a, b) => a.sort - b.sort)
      .map((a) => a.value).slice(0,resNum)

//note that this is better than using RES_LIST.sort(() => .5 - Math.random()).slice(0,resNum), as the latter doesn't have even probability for all values. See Microsoft Ballot issue for details, as well as the StackOverflow question for more info: https://stackoverflow.com/questions/19269545/how-to-get-a-number-of-random-elements-from-an-array



//STEP 1: GET TASKS
let taskList = gantt.getTaskByTime()
      .map((a) => ({sort: Math.random(), value: a}))
      .sort((a, b) => a.sort - b.sort)
      .map((a) => a.value).slice(0,taskNum)


//STEP 2: GENERATE .hddl PROBLEM FACTS
//2.1 - RESOURCES
let resFacts = "";

for(let i = 0; i<resNum;i++){
resFacts += " res_"+resList[i].id;
}
resFacts += " - resource";

//2.2 - TASKS
let taskFacts = "";

for(let i = 0; i<taskNum;i++){
taskFacts += " task_"+taskList[i].id;
}
taskFacts += " - task";

//2.3 - TIMESLOTS
let slotsFacts = "";

for(let i = 0; i<slotsNum;i++){
slotsFacts += " t"+i;
}
slotsFacts += " - timeslot";


//STEP 3: GENERATE .hddl PROBLEM INITIAL NETWORK + GOAL
let initialNetowrk = "";
let goal = "";

for (let i = 0; i < taskNum; i++){
    initialNetowrk += "     (task" + i + "(do_task task_" + taskList[i].id + "))\n";
    goal += "   (task_performed task_" + taskList[i].id + ")\n";
}


//STEP 4: GENERATE .hddl RESOURCE AVAILABILITY
//default - available all the time
let resInitPredicates = "";
for (let i = 0; i < resNum; i++){
    for (let j = 0; j < slotsNum; j++){
        resInitPredicates += "  (resource_available res_" + resList[i].id + " t" + j + ")\n";
    }
}


//GENERATE .hddl RESOURCE-TASK COMPATIBILITY
//default - init test - only one resource is compatible with tasks
let compatInitPredicates = "";
for (let i = 0; i < taskNum; i++){
        compatInitPredicates += "  (compatible task_" + taskList[i].id + " res_" + resList[0].id + ")\n";
}

//STEP 5: GENERATE FILE
let output = "";
output += "(define (problem test)\n";
output += "(:domain bassetti-javascript)\n"
output += "\n;RESOURCES: " + resNum+"\n";
output += "\n;TASKS: " + taskNum+"\n";
output += "\n;TIMESLOTS: " + slotsNum+"\n";

output += "\n;---------------- Facts -----------------------\n";
output += "(:objects\n";
output += slotsFacts +"\n";
output += resFacts +"\n";
output += taskFacts + "\n)\n\n";
output += ";--------------- Initial State -----------------\n";
output += " (:htn\n";
output += "     :parameters()\n";
output += "     :subtasks (and\n";
output += initialNetowrk + "\n      )\n   )\n";
output += "(:init\n";
output += " ;task-resource compatibility\n";
output += compatInitPredicates + "\n";
output += " ;resource properties\n";
output += resInitPredicates + ")\n\n";
output += "(:goal\n";
output += "  (and\n";
output += goal + "\n  )\n)";
output += "\n\n)";



//STEP X: SAVE TO FILE
var textFile = null,
  makeTextFile = function (text) {
    var data = new Blob([text], {type: 'text/plain'});

    // If we are replacing a previously generated file we need to
    // manually revoke the object URL to avoid memory leaks.
    if (textFile !== null) {
      window.URL.revokeObjectURL(textFile);
    }

    textFile = window.URL.createObjectURL(data);

    // returns a URL you can use as a href
    return textFile;
    };

makeTextFile(output);
  