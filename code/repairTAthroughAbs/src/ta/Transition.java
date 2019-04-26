package ta;

public class Transition {
	public final String name;
	public final Location origin;
	public final Location destination;
	public final String guard;
	public final String assignment;
	
	public Transition(String name, Location origin, Location destination, String guard, String assignment) {
		this.name = name;
		this.origin = origin;
		this.destination = destination;
		this.guard = guard;
		this.assignment = assignment;
	}
	
	@Override
	public String toString() {
		return name+"("+origin+"-"+destination+"-"+assignment+"-"+guard+")";
	}
}
