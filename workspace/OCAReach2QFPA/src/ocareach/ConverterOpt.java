package ocareach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.IntExpr;

import automata.State;
import automata.counter.OCA;
import graph.directed.DGPath;
import graph.directed.DGVertex;
import graph.directed.DGraph;
import graph.directed.SDGVertex;
import graph.directed.abs.ASDGPath;
import graph.directed.abs.ASDGVertex;
import graph.directed.abs.ASDGraph;
import graph.directed.abs.BorderEdge;

public class ConverterOpt extends Converter {

	public ConverterOpt(OCA oca) {
		super(oca);
		// TODO Auto-generated constructor stub
	}
	
	// ALGORITHM
		@Override
		public String convert() {
			return this.convert(this.oca.getInitState(), this.oca.getTargetState());
		}
		@Override
		public String convert(State startState, State endState) {
			assert(this.getOca().containsState(startState) && this.getOca().containsState(endState));
			// set starting and ending vertex in DG
			this.getDgraph().setStartVertexIndex(startState.getIndex());
			this.getDgraph().setEndingVertexIndex(endState.getIndex());
			// run tarjan and get SCC marks
			this.getSdg().tarjan();
			// construct abstract SDG
			this.asdg = new ASDGraph(this.getSdg());
			
			ASDGVertex absStart = this.getAsdg().getVertex(this.getSdg().getStartingVertex().getSccMark());
			ASDGVertex absEnd = this.getAsdg().getVertex(this.getSdg().getEndingVertex().getSccMark());
			// get all the possible abstract path
			List<ASDGPath> paths = this.getAsdg().DFSFindAbsPaths(absStart.getSccIndex(), absEnd.getSccIndex());
			List<BoolExpr> formulae = new ArrayList<BoolExpr>();
			IntExpr sVar = this.getQfpaGen().mkVariableInt("xs");
			IntExpr tVar = this.getQfpaGen().mkVariableInt("xt");
			for(ASDGPath p : paths) {
				for(ASDGVertex v : p.getPath()) {
					System.out.print(v.getSccIndex());
				}
				System.out.println();
				
				// there is no cycle in  SCCs (trivial case: every SCC is a concrete vertex)
				boolean trivial = !p.containsCycledVertex();
				// the counter automata is flat which can be optimised
				boolean isFlat = p.isFlatPath();
				// there might be type-1 certificate
				boolean type1 = true;
				// there might be type-1 . type-2/ type-2 certificate
				boolean type12 = p.containsPosTagVertex();
				// there might be type-1 . type-3 . type-2/ type-1 . type-3/ type-3 . type-2/ type-3 certificate
				boolean type132 = p.containsNegTagVertex() && p.containsPosTagVertex();
				BoolExpr trivialForm = this.getQfpaGen().mkFalse();
				BoolExpr flatForm = this.getQfpaGen().mkFalse();
				BoolExpr type1Form = this.getQfpaGen().mkFalse();
				BoolExpr type12Form = this.getQfpaGen().mkFalse();
				BoolExpr type132Form = this.getQfpaGen().mkFalse();
				if(trivial) {
					trivialForm = this.genTrivialFormula(p);
				}
				if(isFlat && !trivial) {
					System.out.println("FLAT FORMULA CONFIRM");
					flatForm = this.genFlatFormulae(p, startState.getIndex(), endState.getIndex(), sVar, tVar);
					
				}
				if(type1 && !trivial && !isFlat) {
					type1Form = this.genType1Formulae(p, startState.getIndex(), endState.getIndex(), sVar, tVar, false);
				}
				if(type12 && !trivial && !isFlat) {
					type12Form = this.genType12Formulae(p, startState.getIndex(), endState.getIndex(), sVar, tVar);
				}
				if(type132 && !trivial && !isFlat) {
					type132Form = this.genType132Formulae(p, startState.getIndex(), endState.getIndex(), sVar, tVar);
				}
				BoolExpr temp = (trivial)? trivialForm : 
								(isFlat) ? flatForm    : this.combineAllFormlae(type1Form, type12Form, type132Form);
				formulae.add(temp);
			}
			
			
			String result = null;
			BoolExpr resultExpr = this.getQfpaGen().mkFalse();
			for(BoolExpr formula : formulae) {
				resultExpr = this.getQfpaGen().mkOrBool(resultExpr, formula);
			}
			BoolExpr xsXtPosRequirements = this.getQfpaGen().mkAndBool(
						this.getQfpaGen().mkRequireNonNeg(sVar),
						this.getQfpaGen().mkRequireNonNeg(tVar)
			);
			resultExpr = this.getQfpaGen().mkAndBool(resultExpr, xsXtPosRequirements);	
			String solveResult = null;result = resultExpr.toString();
			/*// ----------------------EQUIV DEBUG-----------------------
			resultExpr = this.equivDebug(sVar, tVar, resultExpr);
			
			result = resultExpr.simplify().toString();
			Solver solver = this.getQfpaGen().getCtx().mkSolver();
			solver.add((BoolExpr)resultExpr.simplify());
			if(solver.check() == Status.UNSATISFIABLE) {
				solveResult = "\n UNSAT";
			} else {
				solveResult = "\n SAT \n" + solver.getModel().toString();
			}
			// --------------------------------------------------------*/
			
			return (solveResult == null) ? result : result + solveResult;
		}
		
		@Override
		public BoolExpr equivDebug(IntExpr sVar, IntExpr tVar, BoolExpr tempResult) {
			BoolExpr resultExpr = null;
			IntExpr iVar = this.getQfpaGen().mkVariableInt("i");
			IntExpr jVar = this.getQfpaGen().mkVariableInt("j");
			List<IntExpr> sum = new ArrayList<IntExpr>(3);
			sum.add(null);
			sum.add(null);
			sum.add(null);
			sum.set(0, this.getQfpaGen().mkScalarTimes(this.getQfpaGen().mkConstantInt(2), iVar));
			sum.set(1, this.getQfpaGen().mkScalarTimes(this.getQfpaGen().mkConstantInt(-4), jVar));
			sum.set(2, sVar);
			IntExpr[] bounds = new IntExpr[2];
			bounds[0] = iVar;
			bounds[1] = jVar;
			/*BoolExpr equiv = (BoolExpr) this.getQfpaGen().mkExistsQuantifier(bounds,
						this.getQfpaGen().mkAndBool(
								this.getQfpaGen().mkEqBool(tVar, this.getQfpaGen().mkSubInt(this.getQfpaGen().sumUpVars(sum), this.getQfpaGen().mkConstantInt(2))),
						this.getQfpaGen().mkRequireNonNeg(iVar),
						this.getQfpaGen().mkRequireNonNeg(tVar),
						this.getQfpaGen().mkRequireNonNeg(sVar)
						//this.getQfpaGen().mkGeBool(sVar, this.getQfpaGen().mkConstantInt(1))
					));
			*/
			BoolExpr equiv = this.getQfpaGen().mkAndBool(
				this.getQfpaGen().mkGeBool(sVar, this.getQfpaGen().mkConstantInt(2)),
				this.getQfpaGen().mkGeBool(tVar, this.getQfpaGen().mkConstantInt(0)),
				this.getQfpaGen().mkRequireNonNeg(tVar),
				this.getQfpaGen().mkGeBool(this.getQfpaGen().mkSubInt(sVar, this.getQfpaGen().mkConstantInt(2)), tVar)
					//this.getQfpaGen().mkGeBool(tVar, this.getQfpaGen().mkSubInt(sVar, this.getQfpaGen().mkConstantInt(2)))
			);
			resultExpr = this.getQfpaGen().mkAndBool(this.getQfpaGen().getCtx().mkImplies(tempResult, equiv), this.getQfpaGen().getCtx().mkImplies(equiv, tempResult));
			resultExpr = this.getQfpaGen().mkNotBool(resultExpr);
			return resultExpr;
		}
		
		
		public BoolExpr genFlatFormulae(ASDGPath p, int startIndex, int endIndex, IntExpr sVar, IntExpr tVar) {
			BoolExpr sVarTVarNonNeg = this.getQfpaGen().mkAndBool(
					this.getQfpaGen().mkRequireNonNeg(tVar),
					this.getQfpaGen().mkRequireNonNeg(sVar)
				);
			
			List<IntExpr> varList = new ArrayList<IntExpr>();
			// remember the last borderedge 
			BorderEdge lastOutEdge = null;
			IntExpr lastOutVar = null;
			List<List<SDGVertex>> allPossibleInOut;
			IntExpr[] midVars = new IntExpr[2*p.getPath().size()];
			for(int i = 0; i <= p.length(); i++) {
				midVars[2*i] = this.getQfpaGen().mkVariableInt("v_i_" + p.getVertex(i).getSccIndex());
				midVars[2*i + 1] = this.getQfpaGen().mkVariableInt("v_o_" + p.getVertex(i).getSccIndex());
			}
			allPossibleInOut = p.inportsOutportsCartesianProduct(p.getG().getSdg().getVertex(startIndex), 
																 p.getG().getSdg().getVertex(endIndex), false);
			BoolExpr resultForm = this.getQfpaGen().mkFalse();
			for(List<SDGVertex> list : allPossibleInOut) {
				AtomicInteger loopNum = new AtomicInteger();
				loopNum.set(0);
				BoolExpr pathForm = this.getQfpaGen().mkTrue();
				for(int i = 0; i < p.getPath().size(); i++) {
					IntExpr thisInVar, thisOutVar;
					if(i == 0) {
						lastOutEdge = null;
						lastOutVar = null;
						thisInVar = sVar;
						thisOutVar = midVars[2*i + 1];
					} else {
						lastOutEdge = p.getG().getBorderEdgeByInportOutport(
								p.getG().getSdg().getVertex(list.get(2*i-1).getVertexIndex()),
								p.getG().getSdg().getVertex(list.get(2*i).getVertexIndex())
						);
						lastOutVar = midVars[2*i - 1];
						thisInVar = midVars[2*i];
						if(i == p.length()) {
							thisOutVar = tVar;
						} else {
							thisOutVar = midVars[2*i + 1];
						}
					}
					DGraph g = p.getVertex(i).getConcreteDGraph();
					pathForm = this.getQfpaGen().mkAndBool(
						pathForm,
						this.genFlatBorderDropAndWeightRequirements(g, 
								lastOutEdge, 
								list.get(2*i).getVertexIndex(), 
								list.get(2*i+1).getVertexIndex(),
								lastOutVar, 
								thisInVar, thisOutVar, 
								loopNum, varList)
					);
				}
				resultForm = this.getQfpaGen().mkOrBool(resultForm, pathForm);
			}
			
			return resultForm;
		}
		
		public BoolExpr genFlatBorderDropAndWeightRequirements(DGraph g,
															   BorderEdge lastOutEdge,
															   int inportIndex, int outportIndex,
															   IntExpr lastOutVar, 
															   IntExpr thisInVar, IntExpr thisOutVar,
															   AtomicInteger loopNum, List<IntExpr> varList) {
			if(thisInVar == null || thisOutVar == null) {
				System.out.println("ERROR: flat border drop and weight requirements error.");
				return null;
			} else {
				if(!g.containsCycle()) {
					// trivial abstract vertex 
					return this.genFlatTrivialForm(thisInVar, thisOutVar, lastOutVar, lastOutEdge);
				} else {
					loopNum.incrementAndGet();
					IntExpr loopTimeVar = this.getQfpaGen().mkVariableInt("loop_" + loopNum);
					varList.add(loopTimeVar);
					// abstract vertex with loop
					BoolExpr loopRequirement = this.genFlatLoopForm(g, inportIndex, outportIndex, thisInVar, thisOutVar, loopTimeVar);
					BoolExpr otherRequirements = this.getQfpaGen().mkAndBool(
						this.getQfpaGen().mkRequireNonNeg(thisOutVar),
						this.getQfpaGen().mkRequireNonNeg(thisInVar)
					);
					otherRequirements = (lastOutVar == null) ? 
						otherRequirements : 
						this.getQfpaGen().mkAndBool(
							otherRequirements,
							this.getQfpaGen().mkEqBool(
								this.getQfpaGen().mkAddInt(lastOutVar, this.getQfpaGen().mkConstantInt(lastOutEdge.getWeight())), 
								thisInVar)
					);
					return this.getQfpaGen().mkAndBool(loopRequirement, otherRequirements);
				}
			}
			
		}
		
		public BoolExpr genFlatTrivialForm(IntExpr thisInVar, IntExpr thisOutVar,
										   IntExpr lastOutVar, BorderEdge lastOutEdge) {
			if(lastOutVar == null && lastOutEdge == null) {
				// first abstract vertex 
				return this.getQfpaGen().mkAndBool(
						this.getQfpaGen().mkRequireNonNeg(thisInVar),
						this.getQfpaGen().mkEqBool(thisInVar, thisOutVar)
					);
			} else {
				// other abstract vertex
				return this.getQfpaGen().mkAndBool(
						this.getQfpaGen().mkRequireNonNeg(thisInVar),
						this.getQfpaGen().mkEqBool(thisInVar, thisOutVar),
						this.getQfpaGen().mkEqBool(thisInVar, 
							this.getQfpaGen().mkAddInt(lastOutVar, this.getQfpaGen().mkConstantInt(lastOutEdge.getWeight())))
				);
			}
		}
		
		public BoolExpr genFlatLoopForm(DGraph g, int inportIndex, int outportIndex,
												  IntExpr thisInVar, IntExpr thisOutVar,
												  IntExpr loopTimeVar) {
			if(!g.containsVertex(inportIndex) || !g.containsVertex(outportIndex)) {
				System.out.println("ERROR: flat loop form error.");
				return null;
			}
			if(inportIndex != outportIndex) {
				int minDrop = 0;
				DGPath i2oPath = new DGPath(g.getVertex(inportIndex));
				DGVertex currentV = g.getVertex(inportIndex);
				while(currentV.getIndex() != outportIndex) {
					currentV = currentV.getEdges().get(0).getTo();
					i2oPath.concatVertex(currentV);
				}
				DGPath o2oPath = new DGPath(g.getVertex(outportIndex));
				currentV = g.getVertex(outportIndex).getEdges().get(0).getTo();
				o2oPath.concatVertex(currentV);
				while(currentV.getIndex() != outportIndex) {
					currentV = currentV.getEdges().get(0).getTo();
					o2oPath.concatVertex(currentV);
				}
				
				IntExpr dropI2O = this.getQfpaGen().mkConstantInt(Math.min(i2oPath.getDrop(), 0));
				IntExpr dropFirstO2O = this.getQfpaGen().mkConstantInt(Math.min(i2oPath.getWeight() + o2oPath.getDrop(), 0));
				// the drop may be large if the weight of the loop is negative
				IntExpr dropFinal = this.getQfpaGen().mkAddInt(dropFirstO2O,
					this.getQfpaGen().mkScalarTimes(
						this.getQfpaGen().mkSubInt(loopTimeVar, this.getQfpaGen().mkConstantInt(1)), 
						this.getQfpaGen().mkConstantInt(o2oPath.getWeight())
					)
				);
				IntExpr weightI2O = this.getQfpaGen().mkConstantInt(i2oPath.getWeight());
				IntExpr weightO2O = this.getQfpaGen().mkConstantInt(o2oPath.getWeight());
				IntExpr weightLoop = this.getQfpaGen().mkScalarTimes(loopTimeVar, weightO2O);
				IntExpr weightPreAndLoop = this.getQfpaGen().mkAddInt(weightI2O, weightLoop);
				IntExpr con0 = this.getQfpaGen().mkConstantInt(0);
				BoolExpr dropRequirement = this.getQfpaGen().mkOrBool(
					this.getQfpaGen().mkAndBool(
							this.getQfpaGen().mkGeBool(loopTimeVar, this.getQfpaGen().mkConstantInt(1)),
							this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropI2O), con0),
							this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropFirstO2O), con0),
							this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropFinal), con0)
					),
					this.getQfpaGen().mkAndBool(
							this.getQfpaGen().mkEqBool(loopTimeVar, con0),
							this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropI2O), con0)
					)
				);
				
				BoolExpr weightRequirement = this.getQfpaGen().mkOrBool(
						this.getQfpaGen().mkAndBool(
							this.getQfpaGen().mkGeBool(loopTimeVar, this.getQfpaGen().mkConstantInt(1)),
							this.getQfpaGen().mkEqBool(thisOutVar, this.getQfpaGen().mkAddInt(thisInVar, weightPreAndLoop))
						),
						this.getQfpaGen().mkAndBool(
							this.getQfpaGen().mkEqBool(loopTimeVar, con0),
							this.getQfpaGen().mkEqBool(thisOutVar, this.getQfpaGen().mkAddInt(thisInVar, weightI2O))
						)
				);
				
				BoolExpr loopTimeNonNeg = this.getQfpaGen().mkRequireNonNeg(loopTimeVar);
				return this.getQfpaGen().mkAndBool(dropRequirement, weightRequirement, loopTimeNonNeg);
				
			} else {
				DGVertex currentV = g.getVertex(outportIndex);
				DGPath o2oPath = new DGPath(currentV);
				currentV = currentV.getEdges().get(0).getTo();
				while(currentV.getIndex() != outportIndex) {
					o2oPath.concatVertex(currentV);
					currentV = currentV.getEdges().get(0).getTo();
				}
				IntExpr dropFirstO2O = this.getQfpaGen().mkConstantInt(o2oPath.getDrop());
				IntExpr dropFinal = this.getQfpaGen().mkAddInt(
							this.getQfpaGen().mkConstantInt(o2oPath.getDrop()), 
							this.getQfpaGen().mkScalarTimes(loopTimeVar, this.getQfpaGen().mkConstantInt(o2oPath.getWeight()))
				);
				
				IntExpr weightO2O = this.getQfpaGen().mkConstantInt(o2oPath.getWeight());
				IntExpr weightLoop = this.getQfpaGen().mkScalarTimes(loopTimeVar, weightO2O);

				IntExpr con0 = this.getQfpaGen().mkConstantInt(0);
				BoolExpr dropRequirement = this.getQfpaGen().mkAndBool(
					this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropFirstO2O), con0),
					this.getQfpaGen().mkGeBool(this.getQfpaGen().mkAddInt(thisInVar, dropFinal), con0)
				);
				BoolExpr weightRequirement = this.getQfpaGen().mkEqBool(this.getQfpaGen().mkAddInt(thisInVar, weightLoop), thisOutVar);

				BoolExpr otherRequirement = this.getQfpaGen().mkRequireNonNeg(loopTimeVar);
				return this.getQfpaGen().mkAndBool(dropRequirement, weightRequirement, otherRequirement);
			}
		}
}