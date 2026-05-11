package it.unipr.cfg;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.PushAny;

public class InputExpression extends NaryExpression {

	public InputExpression(CFG cfg, CodeLocation location) {
		super(cfg, location, "input", new Expression[0]);
	}

	@Override
	public String toString() {
		return "input()";
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, ExpressionSet[] params,
			StatementStore<A> expressions) throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(getProgram().getTypes().getIntegerType(), getLocation()), this);
	}
}
