package it.unipr.analysis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;

/**
 * A combination domain that pairs a {@link SymbolicAbstractDomain} with a SignLattice
 * analysis ({@link ValueEnvironment}{@code <}{@link SignLattice}{@code >}), using the
 * symbolic component to <em>refine</em> the SignLattice component rather than running
 * the two analyses fully independently in parallel.
 * <p>
 * <strong>Basic-block refinement.</strong> Within a straight-line sequence of
 * asSignLatticements, each {@code asSignLattice} updates the symbolic state and then derives
 * the SignLattice of the asSignLatticeed variable directly from the resulting symbolic
 * expression, treating every {@link SymbolicVariable} (the result of an
 * {@code input()} call) as {@link SignLattice#POS}. For example:
 * 
 * <pre>
 *   x := input()   → symbolic: x = x_sym         → SignLattice: x = +
 *   y := x + 1     → symbolic: y = x_sym + 1      → SignLattice: y = + (POS + POS)
 * </pre>
 * 
 * This avoids the SignLattice domain's own, sometimes less precise, inference.
 * <p>
 * <strong>Join-point refinement.</strong> At loop heads and if-then-else merge
 * points, {@link #lub} joins both components pointwise and then refines the
 * SignLattice environment using the joined symbolic state: if the symbolic join still
 * carries a concrete expression for a variable, the derived SignLattice replaces the
 * (potentially imprecise) SignLattice join for that variable. When the symbolic join
 * loses information (the expression set contains multiple alternatives after a
 * join), the refinement evaluates the SignLattice of each alternative and takes their
 * join, still potentially providing a precise result. Only when no concrete
 * SignLattice can be derived is the SignLattice join used as a fallback.
 * <p>
 * For example, consider:
 * 
 * <pre>
 *   x := input(); y := x + 1;
 *   if (x &lt;= 100) { x := x + 1; y := x - 1; }
 *   z := x * y;
 * </pre>
 * 
 * After the body of the if, symbolic gives {@code x = x_sym + 1},
 * {@code y = x_sym} (both positive). At the join with the pre-if state
 * {@code x = x_sym, y = x_sym + 1} (both positive), the symbolic join yields an
 * expression set with two alternatives for each variable, but evaluating the
 * SignLattice of each alternative still gives {@link SignLattice#POS}. The SignLattice analysis thus
 * converges in one pass without losing precision.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class CombinationDomain implements ValueDomain<CombinationDomainLattice> {

	/**
	 * Returns the conjunction of the satisfiability results from both
	 * components.
	 *
	 * @param expression the expression to check
	 * @param pp         the program point
	 * @param oracle     the semantic oracle
	 *
	 * @return the combined satisfiability result
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public Satisfiability satisfies(CombinationDomainLattice state, ValueExpression expression, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		return new SymbolicAbstractDomain().satisfies(state.getSymbolic(), expression, pp, oracle)
				.and(new Sign().satisfies(state.getSignLatticeEnv(), expression, pp, oracle));
	}
	
	/**
	 * Updates the symbolic state with the asSignLatticeed expression and sets the SignLattice
	 * of {@code id} to {@link SignLattice#TOP}. SignLattice precision is recovered at guards
	 * via {@link #assume}, which uses the symbolic summary to refine the SignLattice
	 * environment. The SignLattice domain is never consulted during asSignLatticements.
	 *
	 * @param id         the identifier being asSignLatticeed
	 * @param expression the right-hand side expression
	 * @param pp         the program point of the asSignLatticement
	 * @param oracle     the semantic oracle
	 *
	 * @return the updated combination domain
	 *
	 * @throws SemanticException if an error occurs during the update
	 */
	@Override
	public CombinationDomainLattice assign(CombinationDomainLattice state, Identifier id, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return new CombinationDomainLattice(
				new SymbolicAbstractDomain().assign(state.getSymbolic(), id, expression, pp, oracle),
				state.getSignLatticeEnv().top(),
				state.getSavedSignLatticeEnv());
	}

	/**
	 * Computes the small-step semantics of {@code expression}. Both components
	 * are updated independently; the symbolic component only changes state on
	 * asSignLatticements.
	 *
	 * @param expression the expression whose semantics is computed
	 * @param pp         the program point
	 * @param oracle     the semantic oracle
	 *
	 * @return the updated combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomainLattice smallStepSemantics(CombinationDomainLattice state, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (pp instanceof Ret) {
			// At a return node, we want to keep the SignLattice information refined by
			// guards in the caller.
			// Since asSignLattice resets SignLatticeEnv to top, we use savedSignLatticeEnv
			// (accumulated from assume calls)
			// as the base for refinement so that guard-refined SignLattices are
			// visible at the return node.
			ValueEnvironment<SignLattice> refined = CombinationDomainLattice.refineSignLatticeFromSymbolic(state.getSymbolic(), state.getSavedSignLatticeEnv());
			return new CombinationDomainLattice(
					state.getSymbolic().top(),
					refined,
					state.getSavedSignLatticeEnv());
		}

		ValueEnvironment<SignLattice> newSignLattice = CombinationDomainLattice.refineSignLatticeFromSymbolic(state.getSymbolic(), state.getSignLatticeEnv());
		// Reset symbolic to top only at guard points (comparison expressions).
		// For sub-expressions (identifiers, constants, arithmetic) the symbolic
		// must remain intact so that the guard expression itself can still use
		// it for refinement. smallStepSemantics is called for every sub-node,
		// so
		// resetting unconditionally would kill the symbolic before the
		// comparison
		// is evaluated.
		boolean isGuard = expression instanceof BinaryExpression
				&& ((BinaryExpression) expression).getOperator() instanceof ComparisonOperator;

		return new CombinationDomainLattice(isGuard ? state.getSymbolic().top() : state.getSymbolic(), newSignLattice, state.getSavedSignLatticeEnv());
	}

	/**
	 * Refines the domain by assuming {@code expression} holds on the edge from
	 * {@code src} to {@code dest}. This is the point at which the symbolic
	 * component acts as a proof oracle for the SignLattice component:
	 * <ol>
	 * <li>The SignLattice domain's own {@code assume} is applied first for
	 * condition-driven SignLattice constraints (e.g., {@code x > 0} → x is
	 * positive).</li>
	 * <li>The current symbolic state is then used to further refine the SignLattice
	 * environment via {@link #refineSignLatticeFromSymbolic}: any variable whose
	 * symbolic expression evaluates to a concrete SignLattice (given that all
	 * {@link SymbolicVariable}s are positive) has its SignLattice overridden.</li>
	 * <li>After refinement the symbolic component is reset to
	 * {@link SymbolicAbstractDomain#top() top}: it has served its purpose for
	 * this block. The SignLattice environment carries the refined information
	 * forward.</li>
	 * </ol>
	 * <p>
	 * This implements the "blocks output symbolic things used by guards to
	 * refine SignLattices" contract: a block builds up exact symbolic expressions; the
	 * guard (assume) consumes them to improve SignLattice precision; and the symbolic
	 * is discarded so the next block starts fresh.
	 *
	 * @param expression the assumed boolean expression
	 * @param src        the source program point of the guarded edge
	 * @param dest       the destination program point of the guarded edge
	 * @param oracle     the semantic oracle
	 *
	 * @return the refined combination domain with symbolic reset to top
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomainLattice assume(CombinationDomainLattice state, ValueExpression expression, ProgramPoint src,
			ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		ValueEnvironment<SignLattice> refinedSignLattices = CombinationDomainLattice.refineSignLatticeFromSymbolic(state.getSymbolic(), state.getSignLatticeEnv());
		ValueEnvironment<SignLattice> assumedSignLattices = new Sign().assume(refinedSignLattices, expression, src, dest, oracle);
		// Save the assumed SignLattices so popScope can reconstruct the full SignLattice env
		// at the return node.
		return new CombinationDomainLattice(state.getSymbolic().top(), assumedSignLattices, assumedSignLattices);
	}

	@Override
	public CombinationDomainLattice makeLattice() {
		return new CombinationDomainLattice();
	}
}
