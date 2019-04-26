package repairta.process;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum Benchmark {
	
	COFFEE("coffee", Arrays.asList(new Integer[] {4})),
	CAS("carAlarmSystem", Arrays.asList(new Integer[] {4})),
	RUNNING_EXAMPLE("runningExample",Arrays.asList(new Integer[] {4})),
	RUNNING_EXAMPLE_ALT("runningExampleAlt", Arrays.asList(new Integer[] {4}), "runningExampleNewOracle", "runningExampleNewOracle");
	;
	
	private Benchmark(String modelName, List<Integer> depths) {
		this(modelName, depths, modelName);
	}
	
	private Benchmark(String modelName, List<Integer> depths, String modelNameEpzg) {
		this(modelName, depths, modelName, modelNameEpzg);
	}

	private Benchmark(String modelName, List<Integer> depths, String modelNameOracle, String modelNameEpzg) {
		this(getPtaPath(modelName), getPtaPath(modelNameOracle), getPtaPathTransformed(modelNameEpzg), 
				getPtaPathTransformedForEvaluation(modelNameEpzg), modelName, depths);
	}

	private Benchmark(String ptaPath, String ptaPathOracle, String ptaPathTransformed, 
			String ptaPathTransformedForEvaluation, String modelName, List<Integer> depths) {
		this.ptaPath = ptaPath;
		this.ptaPathOracle = ptaPathOracle;
		this.ptaPathTransformed = ptaPathTransformed;
		this.modelName = modelName;
		this.depths = depths;
		this.ptaPathTransformedForEvaluation = ptaPathTransformedForEvaluation;
	}

	@Deprecated
	private Benchmark(String ptaPath, String ptaPathMomut, String ptaPathTransformed, 
			String ptaPathTransformedForEvaluation, String modelName, List<Integer> depths, 
			boolean determinize, String... automatonNames) {
		this.ptaPath = ptaPath;
		this.ptaPathMomut = ptaPathMomut;
		this.ptaPathTransformed = ptaPathTransformed;
		this.modelName = modelName;
		this.depths = depths;
		this.deteminize=determinize;
		this.automatonNames=automatonNames;
		this.ptaPathTransformedForEvaluation = ptaPathTransformedForEvaluation;
	}
	
	public String ptaPath;
	public String modelName;
	String ptaPathTransformed;
	String ptaPathTransformedForEvaluation;
	List<Integer> depths;
	public String ptaPathOracle;

	/** should be needed only for MoMuT */
	@Deprecated String ptaPathMomut;
	@Deprecated boolean deteminize;
	@Deprecated String[] automatonNames;
	
	/*private Benchmark(String modelName, int depth, boolean determinize) {
		this(modelName,Utils.makeListBetweenAndIncluded(1, depth),determinize);
	}
	private Benchmark(String modelName, List<Integer> depths, boolean determinize) {
		this(modelName,depths,determinize,"Template");
	}*/

	public static String getPtaPath(String modelName) {
		return "files/"+modelName+".imi";
	}
	public static String getPtaPathMomut(String modelName) {
		return "files/"+modelName+".xml";
	}
	public static String getPtaPathTransformed(String modelName) {
		return "files/"+modelName+"-transformed-statespace.states";
	}
	public static String getPtaPathTransformedForEvaluation(String modelName) {
		return "files/"+modelName+"-transformedForEvaluation-statespace.states";
	}
	
	
	
	public String getTempTaPath() {
		return "files/temp/"+modelName+"Initial.imi";
	}
	
	public Map<String,Double> getAssignmentsInitial() throws IOException {
		return RepairTimedAutomata.getParameterAssignmentsFromPTA(ptaPath);
	}
	public Map<String,Double> getAssignmentsOracle() throws IOException {
		return RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(ptaPath);
	}

}
