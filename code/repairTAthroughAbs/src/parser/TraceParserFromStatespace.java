package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ta.Location;
import ta.TA;
import ta.Transition;

/** 
 * parses the statefile from imitator, to give traces with constraints over feasible values to times
 * @author marcoradavelli
 */
public class TraceParserFromStatespace {
	
	/** @return the transitions guards in the TA represent the symbolic admissible times
	 */
	public static TA getTA(File stateFile) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(stateFile));
		StringBuilder sb = new StringBuilder();
		String line ="";
		while ((line=fin.readLine())!=null) {
			sb.append(line+"\n");
		}
		fin.close();
		return getTA(sb.toString());
	}
	
	public static TA getTA(String stateSpaceInput) {
		TA ta = new TA();
		String stateName ="";
		int id=0;
		String[] lines = stateSpaceInput.split("\n");
		boolean initial=false;
		boolean copyState = false;
		boolean readConstraints = false;
		String constraints = "";
		Set<Integer> initialStateIds = new HashSet<>();
		Map<Integer,String> mapIdName = new HashMap<>();
		Map<String,Integer> mapNameId = new HashMap<>();
		for (String line : lines) {
			if (line.contains("INITIAL")) initial=true;
			if (line.contains("STATE ")) {
				id = Integer.parseInt(line.split("STATE ")[1].split(":")[0]);
				if (initial) {
					initialStateIds.add(id);
					initial=false;
				}
			}
			if (line.contains("pta: ")) {
				copyState = line.contains("copy");
				stateName=line.split("pta:")[1].split("==>")[0].replace("copy", "").trim();
				if (!copyState) {
					mapIdName.put(id, stateName);
					mapNameId.put(stateName, id);
					ta.addLocation("Template",new Location("s_"+id,"","Template"), initialStateIds.contains(id));
				}
			}
			if (copyState && line.contains("Projection onto selected")) {
				readConstraints=true;
			} else if (copyState && readConstraints) {
				if (line.contains("pabs")) {
					constraints += " " +line.trim();
				} else {
					constraints=constraints.trim();
					ta.locations.get("s_"+mapNameId.get(stateName)).invariant=constraints;
					constraints = "";
					readConstraints=false;
				}
			}
			if (line.contains(" -> ") && line.contains(" via ")) {
				String[] st = line.trim().split(" ");
				String l1 = st[0];
				String l2 = st[2];
				String transitionName = st[4].replace("\"", "");
				Location origin = ta.locations.get(l1);
				Location destination = ta.locations.get(l2);
				if (origin!=null && destination!=null) {
					ta.addTransition(new Transition(transitionName, origin, destination, destination.invariant, ""));					
				}
			}
		}
		for (Location l : ta.locations.values()) l.invariant=""; // reset the invariants?!? (only the guards on the transitions are enough)
		ta.addClock("pabs");
		return ta;
	}
}
