#!/bin/bash
rm files/stats.csv 
java -cp .:bin:libs/* repairta.process.RepairTimedAutomata
# compute statistcs
java -cp .:bin:libs/* org.junit.runner.JUnitCore repairta.process.DistanceCalculator 
Rscript stats.R
