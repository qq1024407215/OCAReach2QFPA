package graph.directed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import graph.directed.abs.LoopTag;
import table.dwt.DWTEntry;
import table.dwt.DWTable;
import table.dwt.DWTableImpl;
import table.dwt.DWTuple;

public class DGraph implements Graph{
	private List<DGVertex> vertices;
	private int startVertexIndex;
	private int endingVertexIndex;
	private DWTable table;
	private LoopTag tag;
	private int maxAbsValue;
	public DGraph(int maxAbsValue) {
		this.setVertices(new ArrayList<DGVertex>());
		this.setStartVertexIndex(0);
		this.setEndingVertexIndex(0);
		this.table = null;
		this.tag = null;
		this.maxAbsValue = maxAbsValue;
	}
	
	
	// basic operations
	public void addVertex(int index) {
		for(DGVertex v : this.getVertices()) {
			if(v.getIndex() == index) {
				System.out.println("ERROR: Index repeat");
			}
		}
		this.vertices.add(new DGVertex(index));
	}
	
	public void addVertex(DGVertex v) {
		for(DGVertex ve : this.getVertices()) {
			if(ve.getIndex() == v.getIndex()) {
				System.out.println("ERROR: Index repeat");
				return;
			}
		}
		this.getVertices().add(v);
	}
	
	public void delVertex(int index) {
		for(DGVertex v : this.getVertices()) {
			if(v.getIndex() == index) {
				this.getVertices().remove(v);
				return;
			}
		}
		System.out.println("ERROR: Vertex not exist");
	}
	
	public DGVertex getVertex(int index) {
		for(DGVertex v : this.getVertices()) {
			if(v.getIndex() == index) {
				return v;
			}
		}
		System.out.println("ERROR: Vertex " +  index + " not found");
		return null;
	}
	
	
	
	public void setVertex(int index, DGVertex v) {
		for(DGVertex ve : this.getVertices()) {
			if(ve.getIndex() == index) {
				ve = v;
			}
		}
	}
	
	public int size() {
		return this.getVertices().size();
	}
	
	// mark: generalize here, we require the weight of an edge is fixed. 
	// there does not exist an edge with different weight
	public void addEdge(int fromIndex, int toIndex, int weight) {
		assert(this.containsVertex(fromIndex) && this.containsVertex(toIndex));
		if(this.containsEdge(fromIndex, toIndex)) {
			System.out.println("ERROR: Add edge error, Edge already exists");
			return;
		} 
		this.getVertex(fromIndex).addEdge(this.getVertex(toIndex), weight);
	}
	
	public void delEdge(int fromIndex, int toIndex) {
		if(!this.containsEdge(fromIndex, toIndex)) {
			System.out.println("ERROR: Del edge error, edge does not exists");
			return;
		}
		this.getVertex(fromIndex).delEdge(toIndex);
	}
	
	public List<DGEdge> getEdges(){
		List<DGEdge> edgeList = new ArrayList<DGEdge>();
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				edgeList.add(e);
			}
		}
		return edgeList;
	}
	
	public Boolean containsEdge(int fromIndex, int toIndex) {
		for(DGEdge e : this.getEdges()) {
			if(e.getFrom().getIndex() == fromIndex 
			&& e.getTo().getIndex() == toIndex) {
				return true;
			}
		}
		return false;
	}
	
	//Algorithms
	public LoopTag computeLoopTag() {
		if(this.table != null) {
			return this.getTag();
		}
		//System.out.println("compute loop tag");
		DWTable table = new DWTableImpl(this);
		this.table = table;
		for(int i = 0; i <= this.getVertices().size(); i ++) {
			table.increMaxLenUpdate();
		}
		boolean hasPos = false;
		boolean hasNeg = false;
		boolean noCycle = true;
		for(DGVertex v : this.getVertices()) {
			DWTEntry entry = table.getEntry(v.getIndex(), v.getIndex());
			if(entry == null) {
				continue;
			}
			if(entry.getSetOfDWTuples().size() != 0) {
				/*System.out.println("tuple print:");
				for(DWTuple t : entry.getSetOfDWTuples()) {
					t.printTuple();
				}*/
				noCycle = false;
			}
			for(DWTuple t : entry.getSetOfDWTuples()) {
				if(t.getWeight() > 0) {
					hasPos = true;
				} else if(t.getWeight() < 0) {
					hasNeg = true;
				} else {
					
				}
			}
		}
		if(noCycle) {
			this.setTag(LoopTag.None);
			return LoopTag.None;
		} else {
			if(hasPos && hasNeg) {
				this.setTag(LoopTag.PosNeg);
				return LoopTag.PosNeg;
			} else if(hasPos) {
				this.setTag(LoopTag.Pos);
				return LoopTag.Pos;
			} else if(hasNeg) {
				this.setTag(LoopTag.Neg);
				return LoopTag.Neg;
			} else {
				this.setTag(LoopTag.Zero);
				return LoopTag.Zero;
			}
		}      
	}
	
	public List<DGraph> getAllPossibleSupport(int startIndex, int endIndex){
		// find the supports that contains startVertex and endVertex
		// the support also needs to be a strong connect component
		// TODO: find a better implementation 
		// TODO: NEW wait for algorithm
		List<DGraph> graphs = new ArrayList<DGraph>();
		List<DGEdge> edges = new ArrayList<DGEdge>();
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				edges.add(e);
			}
		}
		List<List<DGEdge>> edgePow = DGraphUtil.getPowerSet(edges);
		for(List<DGEdge> list : edgePow) {
			DGraph temp = this.edgeListToGraph(list, startIndex, endIndex);
			if(temp.containsVertex(startIndex) && temp.containsVertex(endIndex)) {
				temp.computeLoopTag();
				graphs.add(temp);
			}
		}
		return graphs;
	}
	
	public DGraph getSkewTranspose() {
		DGraph g = new DGraph(this.getMaxAbsValue());
		for(DGVertex v : this.getVertices()) {
			g.addVertex(v.getIndex());
		}
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				g.addEdge(e.getTo().getIndex(), e.getFrom().getIndex(), -e.getWeight());
			}
		}
		int oldStart = this.getStartVertexIndex();
		g.setStartVertexIndex(this.getEndingVertexIndex());
		g.setEndingVertexIndex(oldStart);
		return g;
	}
	
	public int getMaximalAbsWeight() {
		int max = 1;
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				if(Math.abs(e.getWeight()) > max) {
					max = Math.abs(e.getWeight());
				}
			}
		}
		return max;
	}
	
	public DGraph edgeListToGraph(List<DGEdge> list, int startIndex, int endIndex) {
		DGraph g = new DGraph(this.getMaxAbsValue());
		if(list.size() == 0 && this.getVertices().size() != 0) {
			// trivial case
			if(startIndex == endIndex) {
				g.addVertex(startIndex);
			} 
		}
		for(DGEdge e : list) {
			if(this.containsVertex(e.getTo().getIndex()) && !g.containsVertex(e.getTo().getIndex())) {
				g.addVertex(e.getTo().getIndex());
			}
			if(this.containsVertex(e.getFrom().getIndex()) && !g.containsVertex(e.getFrom().getIndex())) {
				g.addVertex(e.getFrom().getIndex());
			}
			g.addEdge(e.getFrom().getIndex(), e.getTo().getIndex(), e.getWeight());
		}
		g.setStartVertexIndex(startIndex);
		g.setEndingVertexIndex(endIndex);
		return g;
	}
	
	public boolean isConnected() {
		// return true if the graph is a connected graph
		// here we only need the integrity of the graph
		// if there is a vertex not appear in the visited list
		// the graph is not connected
		DGVertex startVertex = this.getVertex(this.getStartVertexIndex());
		Queue<DGVertex> list = new LinkedList<DGVertex>();
		List<DGVertex> visited = new ArrayList<DGVertex>();
		list.add(startVertex);
		this.connectedBFS(list,  visited);
		for(DGVertex v : this.getVertices()) {
			if(!visited.contains(v)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isReachable(int fromIndex, int toIndex) {
		DGVertex fromVertex = this.getVertex(fromIndex);
		Queue<DGVertex> list = new LinkedList<DGVertex>();
		List<DGVertex> visited = new ArrayList<DGVertex>();
		list.add(fromVertex);
		this.connectedBFS(list, visited);
		if(!visited.contains(this.getVertex(toIndex))) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isFlatSCCGraph() {
		// the strongly connected property should be guaranteed
		// if the graph is a trivial state
		if(this.getVertices().size() == 1) {
			return true;
		}
		// otherwise
		for(DGVertex v : this.getVertices()) {
			if(v.getEdges().size() > 1) {
				return false;
			} else if(v.getEdges().size() == 1) {
				continue;
			} else {
				// not possible
				System.out.println("ERROR: DGraph flatness error!!");
			}
		}
		return true;
	}
	
	
	public void connectedBFS(Queue<DGVertex> list, List<DGVertex> visited) {
		// BFS and store all the reached vertices into visited
		while(!list.isEmpty()) {
			visited.add(list.peek());
			for(DGEdge e : list.poll().getEdges()) {
				if(!visited.contains(e.getTo())) {
					list.add(e.getTo());
				}
			}
		}
	}
	
	public boolean isSubgraphOf(DGraph graph) {
		// a graph is a subgraph if the vertices are covered
		for(DGVertex v : this.getVertices()) {
			if(!graph.containsVertex(v.getIndex())) {
				return false;
			}
			
			for(DGEdge e : v.getEdges()) {
				if(!graph.containsEdge(e.getFrom().getIndex(), e.getTo().getIndex())) {
					return false;
				}
			}
		}
		return true;
	}

	public DGraph union(DGraph graph) {
		DGraph newG = new DGraph(this.getMaxAbsValue());
		for(DGVertex v : this.getVertices()) {
			newG.addVertex(v.getIndex());
		}
		for(DGVertex w : graph.getVertices()) {
			if(!newG.containsVertex(w.getIndex())) {
				newG.addVertex(w.getIndex());
			}
		}
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				if(newG.containsVertex(v.getIndex()) && newG.containsVertex(e.getTo().getIndex())) {
					newG.addEdge(v.getIndex(), e.getTo().getIndex(), e.getWeight());
				}
			}
		}
		for(DGVertex w : graph.getVertices()) {
			for(DGEdge e : w.getEdges()) {
				if(newG.containsVertex(w.getIndex()) && newG.containsVertex(e.getTo().getIndex())) {
					newG.addEdge(w.getIndex(), e.getTo().getIndex(), e.getWeight());
				}
			}
		}
		return newG;
	}
	
	public boolean containsCycle() {
		if(this.table == null) {
			this.computeLoopTag();
		}
		if(this.getTag().equals(LoopTag.None)){
			return false;
		} else {
			return true;
		}
	}
	
	public boolean containsVertex(int index) {
		for(DGVertex v : this.getVertices()) {
			if(v.getIndex() == index) {
				return true;
			}
		}
		return false;
	}
	
	public void increaseDWTLenLimit() {
		//TODO: GEN add the absolute value of the weight to the length limit
		// increase the length limit to 2|V|^2 N_A + 1 and
		
		if(this.getTable() == null) {
			this.computeLoopTag();
		}
		for(int i = this.getTable().getMaxLength(); 
				i <= 2 * this.getVertices().size() * this.getVertices().size() * this.getMaxAbsValue() + 1; 
				i = this.getTable().getMaxLength()) {
			this.getTable().increMaxLenUpdate();
		}
	}
	
	
	public int getZeroEdgeNum() {
		int n = 0;
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				if(e.getWeight() == 0) {
					n ++;
				}
			}
		}
		return n;
	}
	
	public DGraph getGraphZeroEdgeRemoved() {
		DGraph graph = new DGraph(this.getMaxAbsValue());
		for(DGVertex v : this.getVertices()) {
			graph.addVertex(v.getIndex());
		}
		for(DGVertex v : this.getVertices()) {
			for(DGEdge e : v.getEdges()) {
				if(e.getWeight() != 0) {
					graph.addEdge(e.getFrom().getIndex(), e.getTo().getIndex(), e.getWeight());
				}
			}
		}
		return graph;
	}
	
	// getters and setters
	public List<DGVertex> getVertices() {
		return vertices;
	}
	
	public void setVertices(List<DGVertex> vertices) {
		this.vertices = vertices;
	}

	public int getStartVertexIndex() {
		return startVertexIndex;
	}

	public void setStartVertexIndex(int startVertexIndex) {
		this.startVertexIndex = startVertexIndex;
	}

	public int getEndingVertexIndex() {
		return endingVertexIndex;
	}

	public void setEndingVertexIndex(int endingVertexIndex) {
		this.endingVertexIndex = endingVertexIndex;
	}

	public DWTable getTable() {
		return table;
	}

	public void setTable(DWTable table) {
		this.table = table;
	}
	
	public LoopTag getTag() {
		return this.tag;
	}
	
	public void setTag(LoopTag tag) {
		this.tag = tag;
	}


	public int getMaxAbsValue() {
		return maxAbsValue;
	}


	public void setMaxAbsValue(int maxAbsValue) {
		this.maxAbsValue = maxAbsValue;
	}
}
