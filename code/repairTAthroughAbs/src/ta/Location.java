package ta;

public class Location {
	public final String name;
	public String invariant;
	/** the name of the automata to which it belongs */
	public final String automata;
	
	public Location(String name, String operations, String automata) {
		this.name = name;
		this.invariant = operations;
		this.automata = automata;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String toStringComplete() {
		return name+":"+invariant+":"+automata;
	}
}
