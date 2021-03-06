package graph.directed;

public class DGEdge {
	
	private DGVertex from, to;
	private int weight;
	
	public DGEdge(DGVertex f, DGVertex t, int w) {
		this.from = f;
		this.to= t;
		this.setWeight(w);
	}
	
	//getters and setters
	public DGVertex getFrom() {
		return this.from;
	}
	
	public void setFrom(DGVertex from) {
		this.from = from;
	}
	
	public DGVertex getTo() {
		return this.to;
	}
	
	public void setTo(DGVertex to) {
		this.to = to;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
