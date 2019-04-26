package repairta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import repairta.process.RepairTimedAutomata;

public class Imitator {
	public static Logger logger = Logger.getLogger("Imitator");
	
	public static int MAX_BAD_TRACES_IN_ONE_FILE = 100;
	
	public static Set<String> mp, mnp;
	
	public static Set<String> getParameterConstraints(String ptaPath, Set<String> mpNew, Set<String> mnpNew) throws IOException, InterruptedException {
		mp = mpNew;
		mnp = mnpNew;
		System.out.println("Traces to imitator: "+mp.size()+" "+mnp.size());
		Set<String> constraints = new HashSet<>();
		for (int i=-1; i<mp.size(); i++) {
			if (i<2) System.out.println("Getting parameter constraints "+(i==-1?"negative":"positive")+" case...");
			createImitatorInput(ptaPath, i);
			constraints.add((i>=0 ? "not " : "") + "(" + getImitatorOutput()+ ")");
		}
		return constraints;
	}
	
	public static String getImitatorOutput() throws IOException, InterruptedException {
		String[] cmd = new String[] {"./imitator","temp/temp.imi","-mode","EF"};
		if (RepairTimedAutomata.USE_NOAH) cmd = new String[] {RepairTimedAutomata.NOAH_PATH,"./runImitatorForNoahOnMac.sh"};
		if (RepairTimedAutomata.LOG_IMITATOR) logger.log(Level.INFO, Arrays.toString(cmd));
		ProcessBuilder ps = new ProcessBuilder(cmd);
		ps.directory(new File("files/"));
		ps.redirectErrorStream(true);
		Process pr = ps.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = null, constraints=null;
		if (RepairTimedAutomata.USE_NOAH) {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(pr.getOutputStream()));
			out.append("\n");
			out.flush();
		}
		while ((line=in.readLine())!=null) {
			if (RepairTimedAutomata.LOG_IMITATOR) {
				logger.log(Level.INFO, line);
			}
			if (line.contains("(sound and complete)")) {
				break;
			}
			if (constraints!=null) constraints += line;
			if (line.contains("Final constraint such that the system is correct:")) {
				constraints="";
			}
		}
		in.close();
		if (!pr.waitFor(60000, TimeUnit.MILLISECONDS)) {	// wait for completion
			pr.destroyForcibly();
			System.out.println("MFS Timed Out");
		}
		assert constraints!=null : "Constraints null";
		if (RepairTimedAutomata.LOG_IMITATOR) logger.log(Level.INFO, "ConstraintsBefore: "+constraints);
		constraints = constraints.replace("[0m","").replace("[92;40m","").replace("&", "and").replace(" OR ", " or ").replace(" >", ">").replace("  "," ").replace("= ", "=").replace(" <", "<").replace("> ", ">").replace("< ", "<").replace(" =","==").trim();
		//logger.log(Level.INFO, "Constraints: "+constraints);
		return constraints;
	}
	
	public static void parseTraces(List<String> mp, List<String> mnp) throws IOException {
//		mp = new ArrayList<>();
//		mnp = new ArrayList<>();
		assert mp!=null && mnp!=null;
		BufferedReader fin = new BufferedReader(new FileReader("files/temp/classifiedTests.tatrace"));
		boolean isMP = true;
		String line = null;
		while ((line=fin.readLine())!=null) {
			if (line.contains("//")) line = line.substring(0,line.indexOf("//"));
			if (line.trim().equalsIgnoreCase("mnp:")) isMP=false;
			else if (line.trim().equalsIgnoreCase("mp:")) isMP=true;
			else if (line.trim().length()>0) {
				if (isMP) mp.add(line.trim());
				else mnp.add(line.trim());
			}
		}
		fin.close();
	}
	
	public static void createImitatorInput(String ptaPath, int traceId) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(ptaPath));
		PrintWriter fout = new PrintWriter(new FileWriter("files/temp/temp.imi"));
		String line = null;
		List<String> goodTraces = new ArrayList<>(mp);
		while ((line=fin.readLine())!=null) {
			fout.println(line);
			if (line.contains("(* BEGIN GENERATED *)")) {
				if (traceId<0) { // MNP (bad traces)
					fout.println("(* bad traces *)");
					fout.println("urgent loc l0: invariant True");
					fout.println("\t(* Non-deterministic choice *)");
					for (int bt=0; bt<mnp.size(); bt++) {
						fout.println("\twhen True goto bt"+(bt+1)+"_l1;");
					}
					List<String> badTraces = new ArrayList<>(mnp);
					for (int bt=0; bt<badTraces.size(); bt++) {
						String trace = badTraces.get(bt);
						fout.println("(* bad trace "+trace+" *)");
						String[] st = trace.split(" ");
						for (int i=0; i<st.length-1; i+=2) {
							fout.println("\tloc bt"+(bt+1)+"_l"+ ((i/2) +1)+": invariant xabs <= " + st[i+1]);
							fout.println("\t\twhen xabs = "+st[i+1]+" sync "+ st[i] +" goto "+ (i==st.length-2 ? "finished" : ("bt"+(bt+1)+"_l"+ ((i/2) +2)))+";" );
						}
					}			
				} else { // MP (only one good trace)
					String trace = goodTraces.get(traceId);
					fout.println("(* good trace "+trace+" *)");
					fout.println("urgent loc l0: invariant True");
					fout.println("\t(* Non-deterministic choice *)");
					fout.println("\twhen True goto w1;");
					String[] st = trace.split(" ");
					for (int i=0; i<st.length-1; i+=2) {
						fout.println("\tloc w"+ ((i/2) +1)+": invariant xabs <= " + st[i+1]);
						fout.println("\t\twhen xabs = "+st[i+1]+" sync "+ st[i] +" goto "+ (i==st.length-2 ? "finished" : ("w" +((i/2) +2)))+";" );
					}
				}
			}
		}
		fin.close();
		fout.close();
	}
}
