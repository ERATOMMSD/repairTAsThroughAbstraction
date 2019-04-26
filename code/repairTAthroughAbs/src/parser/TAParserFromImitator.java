package parser;

import ta.Location;
import ta.TA;
import ta.Transition;

public class TAParserFromImitator extends TAParser {
	
	public static TAParserFromImitator instance = new TAParserFromImitator();
	
	private TAParserFromImitator() {}
	
	@Override
	public TA loadModel(String model) {
		TA ta = new TA();
		boolean initial=false;
		String automatonName="";
		String prevLine="";
		for (String line : model.split("\n")) {
			if (line.contains(": clock;")) {
				String clocks = (prevLine + line.split(": clock;")[0]).trim();
				for (String clock : clocks.split(",")) {
					ta.addClock(clock.trim());
				}
			}
			if (line.contains("automaton ")) {
				automatonName = line.split("automaton ")[1];
				if (automatonName.trim().equals("trace")) break;
			}
			if (line.contains("synclabs:")) initial = true;
			if (line.startsWith("loc ") && line.contains(": invariant ")) {
				String locName = line.split(": invariant ")[0].split(" ")[1];
				String locOps = line.split(": invariant ")[1].replace(" ", "");
				ta.addLocation(automatonName, new Location(locName, locOps, automatonName), initial);
				if (initial) initial=false;
			}
			prevLine = line;
		}
		Location currentLoc = null;
		for (String line : model.split("\n")) {
			if (line.contains("automaton trace")) return ta;
			if (line.startsWith("loc ") && line.contains(": invariant ")) {
				String locName = line.split(": invariant ")[0].split(" ")[1];
				currentLoc = ta.locations.get(locName);
			} if (line.contains("when ") && line.contains(" sync ") && line.contains(" goto ")) {
				String name = line.split(" sync ")[1].split(" ")[0];
				Location destination = ta.locations.get(line.split(" goto ")[1].split(";")[0]);
				String constraint = line.contains(" do ")&&line.indexOf(" do ")<line.indexOf(" sync ") ? line.split("when ")[1].split(" do ")[0] : line.split("when ")[1].split(" sync ")[0];
				constraint = constraint.replace(" ", "");
				String assignment = line.contains("{") ? line.split("\\{")[1].split("\\}")[0] : "";
				assignment = assignment.replace(" ", "");
				Transition t = new Transition(name, currentLoc, destination, constraint, assignment);
				ta.addTransition(t);
			}
		}
		return ta;
	}
}
