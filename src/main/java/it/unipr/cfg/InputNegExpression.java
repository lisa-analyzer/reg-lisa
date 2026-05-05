package it.unipr.cfg;

import it.unipr.analysis.PushNeg;
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
 * Represents an {@code inputNeg()} call: a non-deterministic input that is
 * constrained to be strictly negative. The symbolic domain records
 * {@code x_sym < 0} in the path condition when this expression is assigned.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class InputNegExpression extends NaryExpression {

	public InputNegExpression(CFG cfg, CodeLocation location) {
		super(cfg, location, "inputNeg", new Expression[0]);
	}

	@Override
	public String toString() {
		return "inputNeg()";
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
				new PushNeg(getProgram().getTypes().getIntegerType(), getLocation()), this);
	}
}
