package repairta.process;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

import parser.TAParserFromImitator;
import repairta.DistanceEvaluator;
import ta.TA;

public class BenchmarksStats {
	
	@org.junit.Test
	public void printBenchmarksStats() throws Exception {
		PrintWriter fout = new PrintWriter(new FileWriter("files/benchmarkStats.csv"));
		fout.println("benchmark,locations,transitions,assignments,distance");
		StringBuilder sb = new StringBuilder();
		for (Benchmark b : Benchmark.values()) {
			RepairTimedAutomata.createFolderIfNotExists("files/temp");
			TA pta = TAParserFromImitator.instance.loadModel(new File(b.ptaPath));
			//System.out.println(pta);
			
			Map<String, Double> initialAssignments = RepairTimedAutomata.getParameterAssignmentsFromPTA(b.ptaPath);
			Map<String, Double> oracleAssignments=RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(b.ptaPathOracle);
			double distanceFinalTa = DistanceEvaluator.instance.euclideanDistance(oracleAssignments, initialAssignments);
			double[] semanticDistance = new DistanceCalculator().computeSemanticDistance(b.getTempTaPath(), b.ptaPath, b.ptaPathOracle, b.ptaPathTransformedForEvaluation, b.ptaPathTransformed, b.depths.get(0), b.getAssignmentsInitial(), b.getAssignmentsOracle());
			
			String SEP = ",";
			String csvLine = b.modelName+SEP+(pta.locations.size()-1)+SEP+pta.getTotalTransitions()+SEP+initialAssignments.size()+SEP+distanceFinalTa+SEP+Arrays.toString(semanticDistance);
			fout.println(csvLine);

			SEP = " & ";
			String tableLine = b.modelName+SEP+(pta.locations.size()-1)+SEP+pta.getTotalTransitions()+SEP+initialAssignments.size()+SEP+distanceFinalTa+SEP+Arrays.toString(semanticDistance)+" \\\\";
			sb.append(tableLine+"\n");
		}
		System.out.println(sb);
		fout.close();
	}
	
	public TA getInitialTA(Benchmark b) throws Exception {
		Map<String, Double> paramValues=RepairTimedAutomata.getParameterAssignmentsFromPTA(b.ptaPath);
		return TAParserFromImitator.instance.loadModel(RepairTimedAutomata.instantiateTA(b.ptaPath, paramValues));
	}

	public TA getOracleTA(Benchmark b) throws Exception {
		Map<String, Double> paramValues=RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(b.ptaPath);
		return TAParserFromImitator.instance.loadModel(RepairTimedAutomata.instantiateTA(b.ptaPath, paramValues));
	}

}
