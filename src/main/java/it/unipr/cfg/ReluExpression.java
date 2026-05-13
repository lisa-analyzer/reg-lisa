package it.unipr.cfg;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;

/**
 * Represents a {@code ReLu(a)} call: the rectified linear unit function,
 * defined as {@code max(0, a)}. The expression is translated to a symbolic
 * {@code max(0, arg)} expression so that abstract domains that handle
 * {@link NumericMax} (e.g., interval analysis) can evaluate it precisely.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class ReluExpression
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression {

	public ReluExpression(CFG cfg, CodeLocation location, Expression arg) {
		super(cfg, location, "ReLu", arg);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Constant zero = new Constant(expr.getStaticType(), 0, getLocation());
		BinaryExpression maxExpr = new BinaryExpression(
				expr.getStaticType(), zero, expr, NumericMax.INSTANCE, getLocation());
		if (expr instanceof Identifier)
			return analysis.assign(state, (Identifier) expr, maxExpr, this);
		return analysis.smallStepSemantics(state, maxExpr, this);
	}
}
