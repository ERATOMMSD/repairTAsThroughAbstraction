package ta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Trace extends ArrayList<Transition> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5799164144480374251L;
	
	public List<Double> times = new ArrayList<>();
	
	public Trace() {
		super();
	}
	
	public Trace(Trace t) {
		super(t);
		this.times=new ArrayList<>(t.times);
	}
	
	public Trace(Transition transition, double time) {
		super(Arrays.asList(new Transition[] {transition}));
		this.times = Arrays.asList(new Double[] {time});
	}
	
	public void add(Transition transition, double time) {
		this.add(transition);
		this.times.add(time);
	}
	
	public void add(Trace t) {
		this.addAll(t);
		this.times.addAll(t.times);
	}
	
	public Location getOrigin() {
		return this.size()>0 ? this.get(0).origin : null;
	}
	
	public Location getFinalDestination() {
		return this.isEmpty() ? null : this.get(this.size()-1).destination;
	}
	
	/** @return the timed trace in .tatrace format */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<this.size(); i++) {
			sb.append(this.get(i).name+(times.size()>i?(" "+times.get(i)):"")+(i<this.size()-1?" ":""));
		}
		return sb.toString();
	}
	
	public String toStringComplete() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<this.size(); i++) {
			sb.append(this.get(i)+(times.size()>i ? ("="+times.get(i)) : "")+(i<this.size()-1?", ":""));
		}
		return "{"+sb.toString()+"}";		
	}
	
	public String toStringUntimed() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<this.size(); i++) {
			sb.append(" "+this.get(i).name);
		}
		return sb.toString().substring(1);
	}
}
