package graph.directed.abs;

import java.util.ArrayList;
import java.util.List;

import graph.directed.DGraph;
import graph.directed.SDGVertex;
 
public class ASDGPath {
	private List<ASDGVertex> path;
	private ASDGraph g;
	
	public ASDGPath(ASDGVertex startVertex) {
		this.path = new ArrayList<ASDGVertex>();
		this.path.add(startVertex);
		this.setG(startVertex.getGraph());
	}
	
	//basic operations
	public void concatVertex(ASDGVertex vertex) {
		ASDGVertex v = this.getLastVertex();
		if(v.checkAbsEdge(vertex.getSccIndex())) {
			this.getPath().add(vertex);
			return;
		}
		System.out.println("ERROR: append not valid, abstract edge not exists scc: " + this.getLastVertex().getSccIndex() + " --> " + vertex.getSccIndex());
	}
	
	public ASDGVertex getLastVertex() {
		return this.getPath().get(this.getPath().size() - 1);
	}
	
	public void removeLastVertex() {
		if(this.getPath().size() == 1) {
			System.out.println("WARNING: AbsPath is already empty");
			return;
		}
		this.getPath().remove(this.getPath().size() - 1);
	}
	
	public ASDGVertex getVertex(int position) {
		return this.getPath().get(position);
	}
	
	public ASDGVertex getInit() {
		return this.getPath().get(0);
	}
	
	public ASDGPath concatPath(ASDGPath suffix) {
		ASDGPath p = new ASDGPath(this.getInit());
		for(int i = 1; i <= this.length(); i++) {
			p.getPath().add(p.getVertex(i));
		}
		if(suffix.getInit() == p.getLastVertex()) {
			for(int i = 1; i <= suffix.length(); i++) {
				p.getPath().add(suffix.getVertex(i));
			}
			return p;
		}
		System.out.println("ERROR: abspath concat error");
		return null;
	}
	
	public boolean containsPosTagVertex() {
		for(ASDGVertex v : this.getPath()) {
			if(v.getLoopTag() == LoopTag.Pos || v.getLoopTag() == LoopTag.PosNeg) {
				return true;
			} 
		}
		return false;
	}
	
	public boolean containsNegTagVertex() {
		for(ASDGVertex v : this.getPath()) {
			if(v.getLoopTag() == LoopTag.Neg || v.getLoopTag() == LoopTag.PosNeg) {
				return true;
			} 
		}
		return false;
	}
	
	public boolean containsCycledVertex() {
		for(ASDGVertex v : this.getPath()) {
			if(v.getLoopTag() != LoopTag.None) {
				return true;
			} 
		}
		return false;
	}
	
	public boolean containsVertex(ASDGVertex v) {
		for(ASDGVertex ve : this.getPath()) {
			if(ve == v) {
				return true;
			}
		}
		return false;
	}
	
	
	public int length() {
		return this.getPath().size() - 1;
	}
	
	public boolean isFlatPath() {
		for(ASDGVertex v : this.getPath()) {
			DGraph sccGraph = v.getConcreteDGraph();
			if(!sccGraph.isFlatSCCGraph()) {
				return false;
			}
		}
		return true;
	}
	
	// algorithm
	public ASDGPath getSkewPath() {
		ASDGraph skewASDG = this.getG().getSkewTranspose();
		ASDGPath skewP = new ASDGPath(skewASDG.getVertex(this.getLastVertex().getSccIndex()));
		skewP.setG(skewASDG);
		
		for(int i = this.getPath().size()-2; i >= 0; i--) {
			skewP.concatVertex(skewASDG.getVertex(this.getVertex(i).getSccIndex()));
		}
		return skewP;
	}

	public ASDGPath[] splitPathAt(ASDGVertex v) {
		// return a pair of abstract paths, the first is type-1 and the second is type1 in G^{op}
		// v cannot be a repeat vertex by definition
		ASDGPath[] paths = new ASDGPath[2];
		int breakPoint = 0;
		for(int i = 0; i <= this.length(); i++) {
			if(this.getVertex(i) == v) {
				breakPoint = i;
			}
		}
		 
		if(breakPoint == 0) {
			paths[0] = null;
			paths[1] = new ASDGPath(this.getVertex(breakPoint));
			for(int i = breakPoint+1; i < this.getPath().size(); i ++) {
				paths[1].concatVertex(this.getVertex(i));
			}
		} else {
			paths[0] = new ASDGPath(this.getVertex(0));
			for(int i = 1; i < breakPoint; i ++) {
				paths[0].concatVertex(this.getVertex(i));
			}
			paths[1] = new ASDGPath(this.getVertex(breakPoint));
			for(int i = breakPoint+1; i < this.getPath().size(); i ++) {
				paths[1].concatVertex(this.getVertex(i));
			}
		}
		return paths;
	}
	
	public List<List<SDGVertex>> inportsOutportsCartesianProduct(SDGVertex start, SDGVertex end, boolean isSkew) {
		List<List<SDGVertex>> list = new ArrayList<List<SDGVertex>>();
		List<SDGVertex> startList = new ArrayList<SDGVertex>();
		startList.add(start);
		list.add(startList);
		ASDGraph g = this.getPath().get(0).getGraph();
		for(int i = 1; i <= this.length(); i++) {
			List<List<SDGVertex>> connect = new ArrayList<List<SDGVertex>>();
			if(!isSkew) {
				for(SDGVertex lastOut : this.getPath().get(i-1).getOutports()) {
					for(SDGVertex nextIn : this.getPath().get(i).getInports()) {
						if(g.containsBorderEdge(lastOut, nextIn)) {
							//System.out.println(this.getPath().get(i-1).getOutports().size() + " " + this.getPath().get(i).getInports().size());
							//System.out.println("lastOut: " + lastOut.getVertexIndex() + " nextIn: " + nextIn.getVertexIndex());
							List<SDGVertex> newCon = new ArrayList<SDGVertex>();
							newCon.add(lastOut);
							newCon.add(nextIn);
							connect.add(newCon);
						}
					}
				}
			} else {
				// inports and outports are exchanged here
				for(SDGVertex lastOut : this.getPath().get(i-1).getOutports()) {
					for(SDGVertex nextIn : this.getPath().get(i).getInports()) {
						if(g.containsBorderEdge(lastOut, nextIn)) {
							List<SDGVertex> newCon = new ArrayList<SDGVertex>();
							newCon.add(lastOut);
							newCon.add(nextIn);
							connect.add(newCon);
						}
					}
				}
			}
			list = this.connectIOSequence(list, connect);
		}
		
		for(List<SDGVertex> l : list) {
			l.add(end);
		}
		return list;
	}
	
	private List<List<SDGVertex>> connectIOSequence(List<List<SDGVertex>> list, List<List<SDGVertex>> connect){
		if(list.size() == 0) {
			return connect;
		}
		List<List<SDGVertex>> newList = new ArrayList<List<SDGVertex>>();
		for(List<SDGVertex> l : connect) {
			for(List<SDGVertex> pre : list) {
				List<SDGVertex> newSeq = new ArrayList<SDGVertex>();
				for(SDGVertex p : pre) {
					newSeq.add(p);
					
				}
				for(SDGVertex v : l) {
					newSeq.add(v);
				}
				newList.add(newSeq);
			}
		}
		return newList;
	}
	
	public List<ASDGVertex> getAllType12Split(){
		List<ASDGVertex> list = new ArrayList<ASDGVertex>();
		for(ASDGVertex v : this.getPath()) {
			// add all the possible split points
			if(v.getLoopTag() == LoopTag.PosNeg || v.getLoopTag() == LoopTag.Pos) {
				list.add(v);
			}
		}
		return list;
	}
	
	public List<ASDGVertex[]> getAllType132Split(){
		List<ASDGVertex[]> list = new ArrayList<ASDGVertex[]>();
		for(int i = 0; i < this.getPath().size(); i++) {
			if(this.getVertex(i).getLoopTag() == LoopTag.Pos || this.getVertex(i).getLoopTag() == LoopTag.PosNeg) {
				for(int j = i; j < this.getPath().size(); j ++) {
					if(this.getVertex(j).getLoopTag() == LoopTag.PosNeg || this.getVertex(j).getLoopTag() == LoopTag.Neg) {
						ASDGVertex[] pair = new ASDGVertex[2];
						pair[0] = this.getVertex(i);
						pair[1] = this.getVertex(j);
						list.add(pair);
					}
				}
			}
		}
		return list;
	}
	

	public ASDGPath[] getAllType132SplitPaths(ASDGVertex[] splitVertices){
		assert(this.containsVertex(splitVertices[0]) && this.containsVertex(splitVertices[1]));

		ASDGPath[] splittedPaths = new ASDGPath[3];
		if(splitVertices[0] != this.getInit() && splitVertices[1] != this.getLastVertex()) {
			// subtype 132
			// construct type 1 abstract path
			ASDGPath p1 = new ASDGPath(this.getInit());
			int i = 1;
			for(i = 1; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[0].getSccIndex(); i ++) {
				p1.concatVertex(this.getVertex(i));
			}
			// construct type 3  abstract path
			ASDGPath p3 = new ASDGPath(splitVertices[0]);
			for(     ; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[1].getSccIndex(); i++) {
				if(this.getPath().get(i).getSccIndex() != splitVertices[0].getSccIndex()) {
					p3.concatVertex(this.getVertex(i));
				}
			}
			if(i < this.getPath().size()) {
				if(this.getVertex(i).getSccIndex() != splitVertices[0].getSccIndex()) {
					// if not the case that the split vertices are the same one
					p3.concatVertex(this.getVertex(i));
				}
			}
			// construct type2 abstract path
			i = i + 1;
			ASDGPath p2 = new ASDGPath(this.getVertex(i));
			for(     ; i < this.getPath().size(); i++) {
				p2.concatVertex(this.getVertex(i));
			}
			splittedPaths[0] = p1;
			splittedPaths[1] = p3;
			splittedPaths[2] = p2;
		} else if(splitVertices[0] != this.getInit() && splitVertices[1] == this.getLastVertex()) {
			// subtype 13

			ASDGPath p1 = new ASDGPath(this.getInit());
			int i = 1;
			System.out.print(p1.getInit().getSccIndex());
			for(i = 1; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[0].getSccIndex(); i ++) {
				p1.concatVertex(this.getVertex(i));
				System.out.print(this.getVertex(i).getSccIndex());
			}
			
			System.out.println();
			ASDGPath p3 = new ASDGPath(splitVertices[0]);
			System.out.print(p3.getInit().getSccIndex());
			for(     ; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[1].getSccIndex(); i++) {
				if(this.getVertex(i).getSccIndex() != splitVertices[0].getSccIndex()) {
					p3.concatVertex(this.getVertex(i));

					
				}
			}
			
			if(i < this.getPath().size()) {
				// if the last vertex is not added to type 3 path
				p3.concatVertex(this.getVertex(i));
				System.out.print(this.getVertex(i).getSccIndex());
			}
			System.out.println();
			assert(i == this.length());
			ASDGPath p2 = null;
			
			splittedPaths[0] = p1;
			splittedPaths[1] = p3;
			splittedPaths[2] = p2;
		} else if(splitVertices[0] == this.getInit() && splitVertices[1] != this.getLastVertex()) {
			// subtype 32
			ASDGPath p1 = null;
			int i = 0;
			ASDGPath p3 = new ASDGPath(splitVertices[0]);
			for(     ; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[1].getSccIndex(); i++) {
				if(i != 0) {
					p3.concatVertex(this.getVertex(i));
				}
			}
			if(i != 0) {
				p3.concatVertex(this.getVertex(i));
			}
			i = i + 1;
			ASDGPath p2 = new ASDGPath(this.getVertex(i));
			for(i = i + 1; i < this.getPath().size(); i++) {
				p2.concatVertex(this.getVertex(i));
			}
			splittedPaths[0] = p1;
			splittedPaths[1] = p3;
			splittedPaths[2] = p2;
		} else {
			// subtype 3
			ASDGPath p1 = null;
			int i = 1;
			ASDGPath p3 = new ASDGPath(splitVertices[0]);
			for(     ; i < this.getPath().size() && this.getVertex(i).getSccIndex() != splitVertices[1].getSccIndex(); i++) {
				p3.concatVertex(this.getVertex(i));
			}
			if(i < this.getPath().size()) {
				assert(this.getVertex(i).getLoopTag() == LoopTag.Neg || this.getVertex(i).getLoopTag() == LoopTag.PosNeg);
				p3.concatVertex(this.getVertex(i));
			}
			assert(i == this.length());
			ASDGPath p2 = null;
			splittedPaths[0] = p1;
			splittedPaths[1] = p3;
			splittedPaths[2] = p2;
		}
		return splittedPaths;
	}
	
	public List<SDGVertex[]> getType132LinkInportOutport(ASDGVertex[] splitVertices){
		boolean subType132 = false, subType13 = false, subType32 = false, subType3 = false;
		if(splitVertices[0] == this.getInit() && splitVertices[1] == this.getLastVertex()) {
			subType3 = true;
		} else if(splitVertices[0] == this.getInit()) {
			subType32 = true;
		} else if(splitVertices[1] == this.getLastVertex()) {
			subType13 = true;
		} else {
			subType132 = true;
		}
		List<SDGVertex[]> list = new ArrayList<SDGVertex[]>();
		if(subType132) {
			ASDGVertex last = null;
			ASDGVertex now = this.getInit();
			
			int i = 0;
			for(i = 0; i < this.getPath().size() && now != splitVertices[0]; i++) {
				last = now;
				now = this.getVertex(i+1);
			}
			ASDGVertex last2 = last;
			ASDGVertex now2 = now;
			for(     ; i < this.getPath().size() && last2 != splitVertices[1]; i++) {
				last2 = now2;
				now2 = this.getVertex(i+1);
			}
			for(SDGVertex vo1 : last.getOutports()) {
				for(SDGVertex vi1 : now.getInports()) {
					if(this.getG().containsBorderEdge(vo1, vi1)) {
						for(SDGVertex vo2 : last2.getOutports()) {
							for(SDGVertex vi2 : now2.getInports()) {
								if(this.getG().containsBorderEdge(vo2, vi2)) {
									SDGVertex[] inouts = new SDGVertex[4];
									inouts[0] = vo1;
									inouts[1] = vi1;
									inouts[2] = vo2;
									inouts[3] = vi2;
									list.add(inouts);
								}
							}
						}
					}
				}
			}
		}
		
		if(subType13) {
			ASDGVertex last = null;
			ASDGVertex now = this.getInit();
			
			int i = 0;
			for(i = 0; i < this.getPath().size() && now != splitVertices[0]; i++) {
				last = now;
				now = this.getVertex(i+1);
			}
			for(SDGVertex vo1 : last.getOutports()) {
				for(SDGVertex vi1 : now.getInports()) {
					if(this.getG().containsBorderEdge(vo1, vi1)) {
						SDGVertex[] inouts = new SDGVertex[2];
						inouts[0] = vo1;
						inouts[1] = vi1;
						list.add(inouts);
					}
				}
			}
		}
		
		if(subType32) {
			ASDGVertex last = null;
			ASDGVertex now = this.getInit();
			
			int i = 0;
			for(i = 0; i < this.getPath().size() && last != splitVertices[1]; i++) {
				last = now;
				now = this.getVertex(i+1);
			}
			for(SDGVertex vo2 : last.getOutports()) {
				for(SDGVertex vi2 : now.getInports()) {
					if(this.getG().containsBorderEdge(vo2, vi2)) {
						SDGVertex[] inouts = new SDGVertex[2];
						inouts[0] = vo2;
						inouts[1] = vi2;
						list.add(inouts);
					}
				}
			}
		}
		
		if(subType3) {
			// nothing todo
		}
		
		return list;
	}

	public void print() {
		System.out.println("SCCPath: ");
		for(ASDGVertex v : this.getPath()) {
			System.out.print(v.getSccIndex() + " ");
		}
		System.out.println();
	}
	
	//getters and setters
	public List<ASDGVertex> getPath() {
		return path;
	}

	public void setPath(List<ASDGVertex> path) {
		this.path = path;
	}

	public ASDGraph getG() {
		return g;
	}

	public void setG(ASDGraph g) {
		this.g = g;
	}
}
