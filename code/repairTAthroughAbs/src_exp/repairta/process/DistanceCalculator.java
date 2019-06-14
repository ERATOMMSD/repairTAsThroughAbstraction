package repairta.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import parser.TAParserFromImitator;
import repairta.ClassifyTests;
import repairta.DistanceEvaluator;
import repairta.TestGenerator;
import repairta.TestGeneratorFromStateFile;
import repairta.TestGeneratorJava.Mode;
import ta.TA;

public class DistanceCalculator {
	
	public boolean PRINT_TESTS = false;
	
	public double[] computeSemanticDistance(String tempTaPath, String ptaPath, String ptaPathOracle, String ptaPathTransformedForEvaluation, String ptaPathTransformed, int depth, Map<String,Double> assignment1, Map<String,Double> assignment2) throws Exception {
		TestGenerator generator = TestGeneratorFromStateFile.instance;
		generator.mode = Mode.MINUS1_EQUAL_PLUS1_MIDDLE;
		// tests from oracle
		Map<String, Double> initialParamValues=RepairTimedAutomata.getParameterAssignmentsFromPTA(ptaPath);
		RepairTimedAutomata.printToFile(tempTaPath, RepairTimedAutomata.instantiateTA(ptaPathOracle, assignment2));		
		Set<String> generatedTests = generator.generateTests(tempTaPath, null, ptaPathTransformedForEvaluation, 1, new ArrayList<>(), depth, false, null);
		if (PRINT_TESTS) System.out.println(generatedTests);
		
		// add generated tests from model
		RepairTimedAutomata.printToFile(tempTaPath, RepairTimedAutomata.instantiateTA(ptaPath, initialParamValues));		
		generatedTests.addAll(generator.generateTests(tempTaPath, null, ptaPathTransformed, 1, new ArrayList<>(), depth, false, null));
		if (PRINT_TESTS) System.out.println(generatedTests);
		
		// classifyTests
		Map<String,Double> oracleAssignments = assignment2; // RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(ptaPath);
		TA taoParsed = TAParserFromImitator.instance.loadModel(RepairTimedAutomata.instantiateTA(ptaPathOracle, oracleAssignments));
		
		//Map<String,Double> assignments = getParameterValuesFromString(assignment1);
		TA taParsed = TAParserFromImitator.instance.loadModel(RepairTimedAutomata.instantiateTA(ptaPath, assignment1));
		
		//System.out.println(taParsed);
		//System.out.println(taoParsed);
		//System.out.println("TA assignments: "+assignments);
		int unconformantCount = ClassifyTests.classifyTests(taParsed, taoParsed, generatedTests, null, null, false);
		double[] res = new double[3];
		res[0]= unconformantCount;
		res[1]= generatedTests.size();
		res[2]= (1- (double)unconformantCount / (double) generatedTests.size())*100.0;
		return res;
	}
	
	@org.junit.Test
	public void addDistancesToStatsFile() throws Exception {
		BufferedReader fin = new BufferedReader(new FileReader("files/stats.csv"));
		String line = "";
		Map<Benchmark,Set<String>> testsToEvaluateEachBenchmark = new HashMap<>(); // tests generated given taInit and oracle
		Map<String,double[]> ratios = new HashMap<>(); // COFFEE_MOMUT2 {p1=1.0, p2=2.0, p3=8.0, p4=10.0} -> 0.67
		Map<String,Double> syntacticDistance = new HashMap<>();
		StringBuilder file = new StringBuilder();
		while ((line = fin.readLine())!=null) {
			file.append(line+"\n");
			if (line.startsWith("benchmark,generator")) continue;
			//String[] csv = line.split("\"");
			String parameters = line.split("\"")[1].split("\"")[0];// csv[csv.length-1].replace("\"", "");
			String benchmarkName = line.split(",")[1];
			Benchmark b = getBenchmarkFromString(benchmarkName);
			System.out.println(b+" "+parameters);
			testsToEvaluateEachBenchmark.put(b, new HashSet<>());
			ratios.put(b+" "+parameters, null);
			syntacticDistance.put(b+" "+parameters, null);
		}
		fin.close();

		//TestGenerator generator = TestGeneratorFromStateFile.instance;
		for (String be : ratios.keySet()) {
			String s = be.split(" ")[0];
			System.out.println(s);
			Benchmark b = getBenchmarkFromString(s);
			System.out.println(b);
			System.out.println(be);
			String parameterValues = be.split(" \\{")[1];
			
			Map<String,Double> paramValues = getParameterValuesFromString(parameterValues);
			Map<String,Double> oracleAssignments = RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(b.ptaPath);
			double[] ratio = computeSemanticDistance(b.getTempTaPath(), b.ptaPath, b.ptaPathOracle, b.ptaPathTransformedForEvaluation, b.ptaPathTransformed, b.depths.get(0), paramValues, oracleAssignments);
			ratios.put(be, ratio);
			
			double distFinalTa = DistanceEvaluator.instance.euclideanDistance(oracleAssignments, paramValues);
			syntacticDistance.put(be, distFinalTa);
		}
		System.out.println(ratios);
		
		// update output stats
		PrintWriter fout = new PrintWriter(new FileWriter("files/stats.txt"));
		//System.out.println(file);
		String[] lines = file.toString().split("\n");
		for (String l : lines) {
			if (l.startsWith("benchmark,generator")) {
				if (!l.endsWith("semanticDistance")) l=l+",distFinal,semFail,semTotal,semanticDistance";
			}
			else {
				String key = getBenchmarkFromString(l.split(",")[1]).name()+" "+l.split("\"")[1].split("\"")[0];
				System.out.println(key);
				double[] d = ratios.get(key);
				double syntDist = syntacticDistance.get(key);
				String toAdd = ","+syntDist+","+d[0]+","+d[1]+","+d[2];
				if (l.endsWith("\"")) l=l+toAdd;
				else {
					for (int i=0; i<4; i++) {
						l=l.substring(0,l.lastIndexOf(','));
					}
					l=l+toAdd;
				}
			}
			System.out.println(l);
			fout.println(l);
		}
		fout.close();
	}
	
	public Benchmark getBenchmarkFromString(String benchmarkName) {
		for (Benchmark b : Benchmark.values()) if (b.modelName.equals(benchmarkName) || b.name().equals(benchmarkName)) return b;
		return null;
	}
	
	public Map<String, Double> getParameterValuesFromString(String parameterValues) {
		Map<String,Double> res = new HashMap<>();
		String[] st = parameterValues.replace("{", "").replace("}", "").split(", ");
		System.out.println(Arrays.toString(st)+" "+st.length);
		for (String s : st) {
			s = s.replace(",", "");
			//System.out.println(s);
			res.put(s.split("=")[0],Double.parseDouble(s.split("=")[1]));
		}
		return res;
	}
	
}
