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
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
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

	/**
	 * Checks whether {@code expression} is satisfied in {@code state} by
	 * combining the satisfiability results from both the symbolic component
	 * (via {@link SymbolicAbstractDomain}) and the value environment (via the
	 * underlying {@link BaseNonRelationalValueDomain}).
	 */
	@Override
	public Satisfiability satisfies(CombinationDomainLattice<V> state, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return new SymbolicAbstractDomain().satisfies(state.getSymbolic(), expression, pp, oracle)
				.and(domain.satisfies(state.getEnv(), expression, pp, oracle));
	}

	/**
	 * Handles an assignment {@code id := expression}. The symbolic component is
	 * updated with the evaluated symbolic expression; at a loop join point the
	 * pre-loop symbolic state is discarded (reset to top) so that loop-carried
	 * symbolic expressions do not accumulate. The value environment is then
	 * refined using the new symbolic state.
	 */
	@Override
	public CombinationDomainLattice<V> assign(CombinationDomainLattice<V> state, Identifier id,
			ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		boolean isJoinPoint = pp.getCFG().getIngoingEdges((Statement) pp).size() > 1;

		SymbolicDomainLattice rst = new SymbolicAbstractDomain()
				.assign(isJoinPoint ? state.getSymbolic().top() : state.getSymbolic(), id, expression, pp, oracle);
		ValueEnvironment<V> sign = refine(rst, state.getEnv());
		return newLattice(rst, sign);
	}

	/**
	 * Advances the abstract state past {@code expression} without an
	 * assignment.
	 * <ul>
	 * <li>At a {@link Ret} statement: refines the value environment from the
	 * current symbolic state (unless the program point is a join point, in
	 * which case refinement is skipped), then resets the symbolic component to
	 * top.</li>
	 * <li>At a join point with a non-guard expression: resets the symbolic
	 * component to top, preserving the value environment.</li>
	 * <li>At a guard expression (comparison): refines the value environment
	 * from the symbolic state and resets the symbolic component to top.</li>
	 * <li>Otherwise: the state is returned unchanged.</li>
	 * </ul>
	 */
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
		} else {
			ValueEnvironment<V> newEnv = refine(state.getSymbolic(), state.getEnv());
			return newLattice(state.getSymbolic().top(), newEnv);
		}
	}

	/**
	 * Assumes that {@code expression} holds, refining the value environment by
	 * first applying symbolic refinement and then delegating to the underlying
	 * value domain's {@code assume}. The symbolic component is reset to top
	 * afterward.
	 */
	@Override
	public CombinationDomainLattice<V> assume(CombinationDomainLattice<V> state,
			ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<V> refined = refine(state.getSymbolic(), state.getEnv());
		ValueEnvironment<V> assumed = domain.assume(refined, expression, src, dest, oracle);
		return newLattice(state.getSymbolic().top(), assumed);
	}

	/**
	 * Returns the initial (top) lattice element for this domain: a
	 * {@link CombinationDomainLattice} whose symbolic component is top and
	 * whose value environment is seeded from the top element of the underlying
	 * value domain.
	 */
	@Override
	public CombinationDomainLattice<V> makeLattice() {
		return new CombinationDomainLattice<>(evaluator, domain.top());
	}
}
