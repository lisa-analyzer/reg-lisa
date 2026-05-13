package it.unipr.cfg;

import it.unipr.analysis.PushIntv;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * Represents an {@code inputIntv()} call: a non-deterministic input
 * constrained to the interval {@code [0, 1]}. Sign analysis treats it as
 * {@code TOP}; interval analysis returns {@code [0.0, 1.0]}.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class InputIntvExpression extends NaryExpression {

	public InputIntvExpression(CFG cfg, CodeLocation location) {
		super(cfg, location, "inputIntv", new Expression[0]);
	}

	@Override
	public String toString() {
		return "inputIntv()";
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state,
			it.unive.lisa.lattices.ExpressionSet[] params, StatementStore<A> expressions) throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new PushIntv(getProgram().getTypes().getIntegerType(), getLocation()), this);
	}
}
