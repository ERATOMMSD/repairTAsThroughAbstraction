package repairta;

import java.util.Iterator;

/**
 * It is an iterable over all the possible combinations (n,k)
 * @author marcoradavelli
 *
 */
public class Combination implements Iterable<boolean[]> {
	
	public final int n;
	public final int k;
	
	public Combination(int n, int k) {
		this.n=n;
		this.k=k;
	}

	@Override
	public Iterator<boolean[]> iterator() {
		return new Iterator<boolean[]>() {
			
			boolean[] b;
			
			@Override
			public boolean hasNext() {
				if (b==null) return true;
				for (int i=0; i<k; i++) if (!b[b.length-1-i]) return true;
				return false;
			}
			

			@Override
			public boolean[] next() {
				if (b==null) {
					b = new boolean[n];
					for (int i=0; i<k; i++) b[i]=true;
					return b;
				}
				// shift an element (like a sum)
				for (int i=b.length-2; i>=0; i--) {
					if (b[i] && !b[i+1]) {
						b[i] = false;
						b[i+1] = true;
						int count=0;
						for (int j=i+2; j<b.length; j++) {
							if (b[j]) {
								b[j]=false;
								count++;
							}
						}
						for (int j=0; j<count; j++) {
							b[i+2+j]=true; // the following come just next to the shifted one
						}
						return b;
					}
				}
				return b; // strange case
			}
			
		};
	}

}
