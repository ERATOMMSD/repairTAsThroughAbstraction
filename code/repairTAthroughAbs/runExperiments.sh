#!/bin/bash
rm files/stats.txt 
java -cp .:bin:libs/* repairta.process.RepairTimedAutomata

