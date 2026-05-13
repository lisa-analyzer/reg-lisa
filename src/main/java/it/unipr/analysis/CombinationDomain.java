package it.unipr.analysis;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;

/**
 * A combination domain that pairs a {@link SymbolicAbstractDomain} with any
 * non-relational value analysis ({@link ValueEnvironment}{@code <V>}), using
 * the symbolic component to <em>refine</em> the value environment rather than
 * running the two analyses fully independently in parallel.
 * <p>
 * Use {@link #forSign()} to obtain a sign-analysis instance and
 * {@link #forInterval()} for an interval-analysis instance.
 *
 * @param <V> the abstract value type stored in the value environment
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class CombinationDomain<V extends Lattice<V>> implements ValueDomain<CombinationDomainLattice<V>> {

	private final BaseNonRelationalValueDomain<V> domain;
	private final CombinationDomainLattice.ExpressionEvaluator<V> evaluator;

	/**
	 * Builds a combination domain with the given value-analysis domain and
	 * expression evaluator.
	 *
	 * @param domain    the non-relational value domain used for
	 *                      {@code satisfies} and {@code assume}
	 * @param evaluator the strategy for deriving abstract values from symbolic
	 *                      expressions (used for environment refinement)
	 */
	public CombinationDomain(
			BaseNonRelationalValueDomain<V> domain,
			CombinationDomainLattice.ExpressionEvaluator<V> evaluator) {
		this.domain = domain;
		this.evaluator = evaluator;
	}

	// -----------------------------------------------------------------------
	// Static factories
	// -----------------------------------------------------------------------

	/**
	 * Returns a {@link CombinationDomain} backed by sign analysis.
	 *
	 * @return the sign-based combination domain
	 */
	public static CombinationDomain<SignLattice> forSign() {
		return new CombinationDomain<>(new Sign(), CombinationDomainLattice.signEvaluator());
	}

	/**
	 * Returns a {@link CombinationDomain} backed by decimal interval analysis.
	 * Unlike LiSA's built-in integer interval domain, this variant supports
	 * non-integer constants (e.g., {@code Double}, {@code Float}).
	 *
	 * @return the decimal-interval-based combination domain
	 */
	public static CombinationDomain<DecimalInterval> forInterval() {
		return new CombinationDomain<>(new DecimalDomain(), CombinationDomainLattice.intervalEvaluator());
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private CombinationDomainLattice<V> newLattice(
			SymbolicDomainLattice symbolic, ValueEnvironment<V> env) {
		return new CombinationDomainLattice<>(symbolic, env, evaluator);
	}

	private ValueEnvironment<V> refine(
			SymbolicDomainLattice symbolic, ValueEnvironment<V> base)
			throws SemanticException {
		return CombinationDomainLattice.refineEnvFromSymbolic(symbolic, base, evaluator);
	}

	// -----------------------------------------------------------------------
	// ValueDomain operations
	// -----------------------------------------------------------------------

	@Override
	public Satisfiability satisfies(CombinationDomainLattice<V> state, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return new SymbolicAbstractDomain().satisfies(state.getSymbolic(), expression, pp, oracle)
				.and(domain.satisfies(state.getEnv(), expression, pp, oracle));
	}

	@Override
	public CombinationDomainLattice<V> assign(CombinationDomainLattice<V> state, Identifier id,
			ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		boolean isJoinPoint = pp.getCFG().getIngoingEdges((Statement) pp).size() > 1;

		if (isJoinPoint) {
			return newLattice(
					new SymbolicAbstractDomain().assign(state.getSymbolic().top(), id, expression, pp, oracle),
					state.getEnv());
		} else {
			return newLattice(
					new SymbolicAbstractDomain().assign(state.getSymbolic(), id, expression, pp, oracle),
					state.getEnv());
		}
	}

	@Override
	public CombinationDomainLattice<V> smallStepSemantics(CombinationDomainLattice<V> state,
			ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

		boolean isGuard = expression instanceof BinaryExpression
				&& ((BinaryExpression) expression).getOperator() instanceof ComparisonOperator;

		boolean isJoinPoint = pp.getCFG().getIngoingEdges((Statement) pp).size() > 1;

		if (pp instanceof Ret) {
			if (isJoinPoint) {
				return newLattice(state.getSymbolic().top(), state.getEnv());
			} else {
				ValueEnvironment<V> refined = refine(state.getSymbolic(), state.getEnv());
				return newLattice(state.getSymbolic().top(), refined);
			}
		}

		if (isJoinPoint && !isGuard) {
			return newLattice(state.getSymbolic().top(), state.getEnv());
		} else if (!isGuard) {
			return state;
		}

		ValueEnvironment<V> newEnv = refine(state.getSymbolic(), state.getEnv());
		return newLattice(state.getSymbolic().top(), newEnv);
	}

	@Override
	public CombinationDomainLattice<V> assume(CombinationDomainLattice<V> state,
			ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<V> refined = refine(state.getSymbolic(), state.getEnv());
		ValueEnvironment<V> assumed = domain.assume(refined, expression, src, dest, oracle);
		return newLattice(state.getSymbolic().top(), assumed);
	}

	@Override
	public CombinationDomainLattice<V> makeLattice() {
		return new CombinationDomainLattice<>(evaluator, domain.top());
	}
}
