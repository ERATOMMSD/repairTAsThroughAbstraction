*** STRUCTURE ***
The artificat is composed by:
- The Eclipse Java Project of the code, in the folders:
	- src		routines and data structures
	- test_src	some unit tests
	- src_exp	containing classes with code to run experiments
	- libs		libraries
	- files		input and output files for the experiments
- stats.R	script that reads from files/stats.txt, and produces the statistics for the experiments evaluation (for Table 3 in the paper)

*** SET UP ***
On Linux, run: compile.sh

*** HOW TO REPLICATE RESULTS ***
To obtain benchmarks statistics (Table 2), run benchmarksStats.sh
To execute experiments and produce files/stats.csv with the evaluation metrics in output, and the numbers in Table 3 to the standard output, run runExperiments.sh . Obs: R is needed in the machine.
