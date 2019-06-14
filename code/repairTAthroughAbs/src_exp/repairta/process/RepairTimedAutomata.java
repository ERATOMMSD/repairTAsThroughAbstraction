package repairta.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import parser.ParameterAssignmentByChoco;
import parser.TAParserFromImitator;
import repairta.ClassifyTests;
import repairta.DistanceEvaluator;
import repairta.Imitator;
import repairta.TestGenerator;
import repairta.TestGeneratorFromStateFile;
import repairta.TestGeneratorJava.Mode;
import ta.TA;
import tgtlib.definitions.expression.parser.ParseException;

public class RepairTimedAutomata {
	public static Logger logger = Logger.getLogger("RepairTimedAutomata");
	
	public static boolean LOG_MOMUT = false;
	public static boolean LOG_IMITATOR = false;
	public static boolean LOG_PARAMETER_ASSIGNMENT = true;
	
	public static boolean CHECK_WITH_ALLOWED_TRACES = false;
	public static String WINE_PATH = System.getProperty("os.name").toLowerCase().contains("mac") ? "/usr/local/Cellar/wine/4.0/bin/wine" : "wine";
	public static String NOAH_PATH = "/usr/local/Cellar/noah/0.5.1/bin/noah";
	public static boolean USE_NOAH = System.getProperty("os.name").toLowerCase().contains("mac");
	public static String JAVA_PATH = "C:\\Program Files\\Java\\jdk1.8.0_201\\bin\\java.exe";
	public static String LOG_PATH = "files/log.txt";
	public static String STAT_PATH = "files/stats.csv";
	public static final boolean EVALUATE_ONLY_NEW_TESTS=true;
	public static final boolean USE_UPPAAL_MODEL = false;
	public static TestGenerator[] generators = new TestGenerator[] {
			//TestGeneratorJava.instance,
			TestGeneratorFromStateFile.instance,
	};
	public static Benchmark[] benchmarks = new Benchmark[] {
			Benchmark.COFFEE,
			Benchmark.CAS,
			Benchmark.RUNNING_EXAMPLE,
			Benchmark.RUNNING_EXAMPLE_ALT,
	};
	
	public static Mode[] modes = new Mode[] {
		Mode.MINUS1_EQUAL_PLUS1_QUARTER,
		Mode.MINUS1_EQUAL_PLUS1_MIDDLE,
		Mode.MINUS1_EQUAL_PLUS1,
		Mode.RANDOM,
	};
	
	public static final String SEP = ",";
	public static final String STATS_HEADER = "date,benchmark,generator,mode,depth,step,time,unconformantTests,mp,mnp,timeGeneration,timeClassification,timeImitator,timeChoco,paramValues,distFinal,semFail,semTotal,semanticDistance";
	
	public static void main(String[] args) throws ParseException, IOException, InterruptedException, Exception {
		System.out.println(System.getProperty("os.name"));
		if (args!=null && args.length>=3) {
			String modelName = args[0];
			int depth = Integer.parseInt(args[1]);
			boolean determinize = Boolean.parseBoolean(args[2]);
			repairTimedAutomata(Benchmark.getPtaPath(modelName), Benchmark.getPtaPathMomut(modelName), Benchmark.getPtaPathTransformed(modelName), modelName, depth, determinize, new String[] {"Template"}, generators[0], Mode.MIN_MAX_MIDDLE, Benchmark.getPtaPath(modelName));
		} else {
			for (Benchmark b : benchmarks) {
				for (TestGenerator generator : generators) {
					for (Mode mode : modes) {
						repairTimedAutomata(b, generator, mode);
					}
				}
			}
		}
	}

	static int step;
	
	public static void repairTimedAutomata(Benchmark b, TestGenerator generator, Mode mode) throws ParseException, IOException, InterruptedException, Exception {
		log(b.modelName+" "+generator.getClass().getSimpleName());
		for (int depth : b.depths) {
			repairTimedAutomata(b.ptaPath, null, b.ptaPathTransformed, b.modelName, depth, false, null, generator, mode, b.ptaPathOracle);
		}
	}
	
	public static void createFolderIfNotExists(String directory) {
		File dir = new File(directory);
	    if (!dir.exists()) dir.mkdirs();
	}
	
	public static void repairTimedAutomata(String ptaPath, String ptaPathMomut, String ptaPathTransformed, String modelName, int depth, boolean determinize, String[] automatonNames, TestGenerator generator, Mode mode, String ptaPathOracle) throws Exception {
		long timeTestGeneration=Calendar.getInstance().getTimeInMillis(), 
				timeClassification=0,
				timeConstraintGeneration=0, 
				timeChoco=0;
		initLogger();
		long time = Calendar.getInstance().getTimeInMillis();
		createFolderIfNotExists("files/temp");
		step = 1;
		List<String> allConstraints = new ArrayList<>();
		Map<String, Double> initialParamValues=getParameterAssignmentsFromPTA(ptaPath);
		Map<String, Double> paramValues=initialParamValues;
		logger.log(Level.INFO, "Initial parameter assignments: "+paramValues);
		String prevTaPath = "files/temp/"+modelName+"Initial.imi";
		printToFile(prevTaPath, instantiateTA(ptaPath, paramValues));
		String prevTaPathMomut = null; 
		if (USE_UPPAAL_MODEL) {
			prevTaPathMomut = "files/temp/"+modelName+"Initial.xml";
			printToFile(prevTaPathMomut, instantiateTAMomut(ptaPathMomut, paramValues));
		}
		String taoPath = "files/temp/"+modelName+"Oracle.imi";
		Map<String,Double> oracleAssignments = getOracleParameterAssignmentsFromPTA(ptaPathOracle);
		String oracleModelAsString = instantiateTA(ptaPathOracle, oracleAssignments);
		printToFile(taoPath, oracleModelAsString);
		List<String> allGeneratedTests = new ArrayList<>();
		//double distBenchmark = DistanceEvaluator.instance.euclideanDistance(oracleAssignments, initialParamValues);
		TA taoParsed = TAParserFromImitator.instance.loadModel(oracleModelAsString);
		double distConstraintsAvg=0, distConstraintsVar=0, distFinalTa=0;
		while (!stoppingCondition()) {
			System.out.println("Iteration "+step);
			TA taParsed = TAParserFromImitator.instance.loadModel(new File(prevTaPath));
			System.out.println(taParsed);
			// 1. test generation
			//Set<String> generatedTests = TestGeneratorMomut.generateTests(ptaPath, prevTaPathMomut, step, allGeneratedTests, depth, determinize, automatonNames);
			//List<Trace> generatedTests = TestGenerator.generateTests(ptaPath, prevTaPathMomut, step, allGeneratedTests, depth, determinize, automatonNames);
			generator.mode = mode;
			Set<String> generatedTests = generator.generateTests(prevTaPath, null, ptaPathTransformed, step, allGeneratedTests, depth, determinize, automatonNames);
			timeTestGeneration = Calendar.getInstance().getTimeInMillis() - timeTestGeneration;
			
			timeClassification = Calendar.getInstance().getTimeInMillis();
			printToFile("files/temp/"+modelName+"_"+mode+"_generatedTestsStep"+step+".txt", generatedTests.toString().replaceAll(",","\n"));
			System.out.print("Prev. tests: "+allGeneratedTests.size()+" + Generated tests: "+generatedTests.size());
			allGeneratedTests.addAll(generatedTests);
			// 2. IMITATOR
			// 2a. Classify tests (conformance check)
			int unconformantCount = ClassifyTests.classifyTests(taParsed, taoParsed, generatedTests, prevTaPath, taoPath, step>1);
			System.out.println(" - Unconformant count: "+unconformantCount+"/"+(ClassifyTests.mp.size()+ClassifyTests.mnp.size()));
			timeClassification = Calendar.getInstance().getTimeInMillis() - timeClassification;
			
			if (unconformantCount>0) {
				timeConstraintGeneration = Calendar.getInstance().getTimeInMillis();
				Set<String> constraints = Imitator.getParameterConstraints(ptaPath, EVALUATE_ONLY_NEW_TESTS ? ClassifyTests.mpNew : ClassifyTests.mp, EVALUATE_ONLY_NEW_TESTS ? ClassifyTests.mnpNew : ClassifyTests.mnp);
				//String constraints = "(p1>3 and p2>1 or  p1>1 and p2>1 and 3>p1 or  p1>=0 and p2>1 and 1>p1) and not (p1>2 and p2>=0 or  2>p1 and p1>=0 and p2>=0) and not (2*p1>3 and 2*p2>3 or  p1>=0 and 2*p2>3 and 3>2*p1) and not (p1>=0 and p2>4)";
				//allConstraints = (allConstraints == null ? "" : allConstraints + " and ") + constraints;
				timeConstraintGeneration = Calendar.getInstance().getTimeInMillis() - timeConstraintGeneration;
				
				printToFile("files/logConstraints.txt", modelName+SEP+generator.getClass().getSimpleName()+SEP+mode+SEP+depth+SEP+step+SEP+(Calendar.getInstance().getTimeInMillis()-time)+SEP+unconformantCount+SEP+ClassifyTests.mp.size()+SEP+ClassifyTests.mnp.size()+SEP+"\""+constraints+"\"", true);
				
				allConstraints.addAll(constraints);
				log("Step "+step+" - "+ClassifyTests.mpNew.size()+" "+ClassifyTests.mnpNew.size());
				// 3a. use SMT for getting parameter values
				//paramValues = ParameterAssignmentBySMT.getParameterValues(allConstraints, paramValues);
				// 3b. use Choco (search-based) for getting parameter values
				timeChoco = Calendar.getInstance().getTimeInMillis();
				paramValues = ParameterAssignmentByChoco.getParameterValues(allConstraints, paramValues);
				timeChoco = Calendar.getInstance().getTimeInMillis() - timeChoco;
				
				log("Parameter values: " + paramValues);
				System.out.println(paramValues);
				// 4. instantiate PTA and MOMUT model (or model used for generation)
				String ta = instantiateTA(ptaPath, paramValues);
				prevTaPath = "files/temp/"+modelName+"Step" + step + ".imi";
				printToFile(prevTaPath, ta);
				// logger.log(Level.INFO, "TA:\n" + ta);
				if (USE_UPPAAL_MODEL) {
					String taMomut = instantiateTAMomut(ptaPathMomut, paramValues);
					prevTaPathMomut = "files/temp/"+modelName+"Step.xml";// + step + ".xml";
					printToFile(prevTaPathMomut, taMomut);
				}
			}
			stat(modelName+SEP+generator.getClass().getSimpleName()+SEP+mode+SEP+depth+SEP+step+SEP+(Calendar.getInstance().getTimeInMillis()-time)+SEP+unconformantCount+SEP+ClassifyTests.mp.size()+SEP+ClassifyTests.mnp.size()+SEP+timeTestGeneration+SEP+timeClassification+SEP+timeConstraintGeneration+SEP+timeChoco+SEP+"\""+paramValues+"\"");
			step++;
			time = Calendar.getInstance().getTimeInMillis();
			if (unconformantCount==0 || generator instanceof TestGeneratorFromStateFile) break;
		}
		log("All constraints: "+allConstraints.toString().replace(",", "\n"));
		log("Final paramValues: "+paramValues);
		log("Constraints contain oracle: "+DistanceEvaluator.instance.containsSolution(oracleAssignments, allConstraints, initialParamValues));
		log("Final distance constraints: "+distConstraintsAvg+" "+distConstraintsVar);
		log("Final distance final ta: "+distFinalTa);
	}

	private static boolean stoppingCondition() {
		return false;
		// if iterative, to place the max number of iterations (like: return step > 3;)
	}

	private static void initLogger() {
		for (Logger logger : new Logger[] { RepairTimedAutomata.logger }) {
			logger.setUseParentHandlers(false);
			ConsoleHandler handler = new ConsoleHandler();
			handler.setFormatter(new SimpleFormatter() {

				@Override
				public synchronized String format(LogRecord lr) {
					return lr.getMessage() + "\n";
				}
			});
			logger.addHandler(handler);
		}
	}

	public static String instantiateTA(String ptaPath, Map<String, Double> paramValues) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<String> lines = Files.readAllLines(Paths.get(ptaPath), StandardCharsets.UTF_8);
		for (String line : lines) {
			if (!line.startsWith("(* Parameters *)"))
				for (Entry<String, Double> e : paramValues.entrySet()) {
					line = line.replaceAll(e.getKey(), e.getValue().toString());
				}
			sb.append(line).append("\n");
		}
		return sb.toString();
	}
	
	static String instantiateTAMomut(String ptaPathMomut, Map<String, Double> paramValues) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<String> lines = Files.readAllLines(Paths.get(ptaPathMomut), StandardCharsets.UTF_8);
		for (String line : lines) {
			for (Entry<String, Double> e : paramValues.entrySet()) {
				if (line.contains("("+e.getKey()+")")) {					
					line = line.replace("("+e.getKey()+")", "("+e.getValue().intValue()+")");
				}
			}
			sb.append(line).append("\n");
		}
		return sb.toString();
	}

	public static void copyFile(String pathFrom, String pathTo) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(pathFrom));
		PrintWriter fout = new PrintWriter(new FileWriter(pathTo));
		String line = null;
		while ((line = fin.readLine()) != null) {
			fout.println(line);
		}
		fin.close();
		fout.close();
	}

	public static void printToFile(String pathTo, String string) throws IOException {
		printToFile(pathTo, string, false);
	}
	
	public static void printToFile(String pathTo, String string, boolean append) throws IOException {
		PrintWriter fout = new PrintWriter(new FileWriter(pathTo, append));
		fout.println(string);
		fout.close();
	}
		
	public static void log(String string) throws IOException {
		string = (new Date())+" "+string;
		System.out.println(string);
		printToFile(LOG_PATH, string, true);
	}
	
	public static String getFirstLineOfFile(String filePath) throws IOException {
		File f = new File(filePath);
		if (!f.exists()) return null;
		BufferedReader fin = new BufferedReader(new FileReader(f));
		String line = fin.readLine();
		fin.close();
		return line;
	}
	
	public static void stat(String string) throws IOException {
		string = (new Date())+SEP+string;
		System.out.println(string);
		if (!STATS_HEADER.equals(getFirstLineOfFile(STAT_PATH))) {
			printToFile(STAT_PATH, STATS_HEADER, false);
		}
		printToFile(STAT_PATH, string, true);
	}
	
	public static Map<String, Double> getParameterAssignmentsFromPTA(String taPath, String description) throws IOException {
		Map<String, Double> res = new HashMap<>();
		BufferedReader fin = new BufferedReader(new FileReader(taPath));
		String line = null;
		while ((line = fin.readLine()) != null) {
			if (line.contains(description)) {
				String[] st = line.split(description)[1].split("\\*\\)")[0].split(",");
				for (String assignment : st) {
					res.put(assignment.split("=")[0].trim(), Double.parseDouble(assignment.split("=")[1]));
				}
			}
		}
		fin.close();
		return res;
	}
	
	public static Map<String, Double> getParameterAssignmentsFromPTA(String taPath) throws IOException {
		return getParameterAssignmentsFromPTA(taPath, "Parameter assignments ta: ");
	}
	
	public static Map<String, Double> getOracleParameterAssignmentsFromPTA(String taPath) throws IOException {
		return getParameterAssignmentsFromPTA(taPath, "Parameter assignments oracle: ");
	}

}
