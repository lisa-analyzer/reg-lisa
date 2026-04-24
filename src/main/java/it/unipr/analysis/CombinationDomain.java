package it.unipr.analysis;

import java.util.Objects;
import java.util.function.Predicate;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A combination domain that pairs a {@link SymbolicAbstractDomain} with a sign
 * analysis ({@link ValueEnvironment}{@code <}{@link Sign}{@code >}), using the
 * symbolic component to <em>refine</em> the sign component rather than running
 * the two analyses fully independently in parallel.
 *
 * <p><strong>Basic-block refinement.</strong> Within a straight-line sequence
 * of assignments, each {@code assign} updates the symbolic state and then
 * derives the sign of the assigned variable directly from the resulting
 * symbolic expression, treating every {@link SymbolicVariable} (the result of
 * an {@code input()} call) as {@link Sign#POS}. For example:
 * <pre>
 *   x := input()   → symbolic: x = x_sym         → sign: x = +
 *   y := x + 1     → symbolic: y = x_sym + 1      → sign: y = + (POS + POS)
 * </pre>
 * This avoids the sign domain's own, sometimes less precise, inference.
 *
 * <p><strong>Join-point refinement.</strong> At loop heads and
 * if-then-else merge points, {@link #lub} joins both components pointwise
 * and then refines the sign environment using the joined symbolic state: if
 * the symbolic join still carries a concrete expression for a variable, the
 * derived sign replaces the (potentially imprecise) sign join for that
 * variable. When the symbolic join loses information (the expression set
 * contains multiple alternatives after a join), the refinement evaluates the
 * sign of each alternative and takes their join, still potentially providing
 * a precise result. Only when no concrete sign can be derived is the sign
 * join used as a fallback.
 *
 * <p>For example, consider:
 * <pre>
 *   x := input(); y := x + 1;
 *   if (x &lt;= 100) { x := x + 1; y := x - 1; }
 *   z := x * y;
 * </pre>
 * After the body of the if, symbolic gives {@code x = x_sym + 1},
 * {@code y = x_sym} (both positive). At the join with the pre-if state
 * {@code x = x_sym, y = x_sym + 1} (both positive), the symbolic join yields
 * an expression set with two alternatives for each variable, but evaluating
 * the sign of each alternative still gives {@link Sign#POS}. The sign
 * analysis thus converges in one pass without losing precision.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class CombinationDomain implements ValueDomain<CombinationDomain> {

	/**
	 * The symbolic component, tracking exact linear combinations of
	 * {@link SymbolicVariable} instances for each program variable.
	 */
	private final SymbolicAbstractDomain symbolic;

	/**
	 * The sign component, recording the sign (positive, negative, zero) of
	 * each program variable. It is kept consistent with the symbolic component
	 * via the refinement operators in {@link #assign} and {@link #lub}.
	 */
	private final ValueEnvironment<Sign> signEnv;

	/**
	 * Builds the top element of this combination domain.
	 */
	public CombinationDomain() {
		this(new SymbolicAbstractDomain(), new ValueEnvironment<Sign>(new Sign()));
	}

	/**
	 * Builds a combination domain from the given components.
	 *
	 * @param symbolic the symbolic component
	 * @param signEnv  the sign environment component
	 */
	public CombinationDomain(SymbolicAbstractDomain symbolic, ValueEnvironment<Sign> signEnv) {
		this.symbolic = symbolic;
		this.signEnv = signEnv;
	}

	/**
	 * Derives a {@link Sign} value from a symbolic expression.
	 * {@link SymbolicVariable} nodes (results of {@code input()},
	 * {@code inputPos()}, or {@code inputNeg()}) are resolved by querying
	 * {@code sym}'s path condition: if {@code x_sym > 0} is recorded, the sign
	 * is {@link Sign#POS}; if {@code x_sym < 0}, it is {@link Sign#NEG};
	 * otherwise (plain {@code input()}) the sign defaults to {@link Sign#POS}.
	 * Plain {@link Identifier} nodes are resolved via {@code signEnv}.
	 * {@link BinaryExpression} nodes are evaluated recursively using
	 * {@link Sign#evalBinaryExpression}.
	 *
	 * @param expr    the symbolic expression to evaluate
	 * @param signEnv the sign environment used to resolve plain identifiers;
	 *                    may be {@code null}
	 * @param sym     the symbolic state whose path condition constrains
	 *                    {@link SymbolicVariable} signs; may be {@code null}
	 *
	 * @return the derived sign, or {@link Sign#TOP} if it cannot be determined
	 */
	private static Sign deriveSignFromExpr(
			SymbolicExpression expr,
			ValueEnvironment<Sign> signEnv,
			SymbolicAbstractDomain sym) {
		if (expr instanceof Constant) {
			Object val = ((Constant) expr).getValue();
			if (val instanceof Number) {
				int v = ((Number) val).intValue();
				return v > 0 ? Sign.POS : v < 0 ? Sign.NEG : Sign.ZERO;
			}
			return Sign.TOP;
		}
		if (expr instanceof SymbolicVariable) {
			if (sym != null) {
				Sign s = sym.getSignOf((SymbolicVariable) expr);
				// Default to POS for plain input() which has no path-condition constraint.
				return s.isTop() ? Sign.POS : s;
			}
			return Sign.POS;
		}
		if (expr instanceof Identifier) {
			if (signEnv != null)
				return signEnv.getState((Identifier) expr);
			return Sign.TOP;
		}
		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			Sign left = deriveSignFromExpr(bin.getLeft(), signEnv, sym);
			Sign right = deriveSignFromExpr(bin.getRight(), signEnv, sym);
			return Sign.TOP.evalBinaryExpression(bin.getOperator(), left, right, null, null);
		}
		return Sign.TOP;
	}

	/**
	 * Refines {@code base} by re-deriving the sign of each variable tracked in
	 * {@code sym} from its symbolic expression set. For variables whose
	 * expression set contains multiple alternatives (as can happen after a
	 * {@link #lub}), the refinement sign is the join of the signs of all
	 * alternatives. Plain {@link Identifier} nodes inside symbolic expressions
	 * (ordinary program variables that appear after an {@code assume} reset) are
	 * resolved by looking up their sign in {@code base}. If the resulting sign
	 * is top (no improvement over the fallback sign join), or bottom (empty
	 * expression set), the binding in {@code base} is left unchanged.
	 *
	 * @param sym  the symbolic state to use for refinement
	 * @param base the sign environment to refine (typically the result of a
	 *                 sign join); also used to resolve plain identifier signs
	 *
	 * @return the refined sign environment
	 *
	 * @throws SemanticException if an error occurs during the refinement
	 */
	private static ValueEnvironment<Sign> refineSignFromSymbolic(
			SymbolicAbstractDomain sym,
			ValueEnvironment<Sign> base)
			throws SemanticException {
		if (sym.isTop() || sym.isBottom())
			return base;

		ValueEnvironment<Sign> result = base;
		for (Identifier id : sym.getKeys()) {
			SymbolicExpression expr = sym.getSymbolicExpression(id);
			if (expr == null)
				continue;

			Sign derived = deriveSignFromExpr(expr, base, sym);

			// Only override the sign join if we obtained something more concrete.
			if (!derived.isTop() && !derived.isBottom())
				result = result.putState(id, derived);
		}
		return result;
	}

	/**
	 * Updates the symbolic state with the assigned expression and sets the sign
	 * of {@code id} to {@link Sign#TOP}. Sign precision is recovered at guards
	 * via {@link #assume}, which uses the symbolic summary to refine the sign
	 * environment. The sign domain is never consulted during assignments.
	 *
	 * @param id         the identifier being assigned
	 * @param expression the right-hand side expression
	 * @param pp         the program point of the assignment
	 * @param oracle     the semantic oracle
	 *
	 * @return the updated combination domain
	 *
	 * @throws SemanticException if an error occurs during the update
	 */
	@Override
	public CombinationDomain assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new CombinationDomain(
				symbolic.assign(id, expression, pp, oracle),
				signEnv.top());
	}

	/**
	 * Computes the small-step semantics of {@code expression}. Both components
	 * are updated independently; the symbolic component only changes state on
	 * assignments.
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
	public CombinationDomain smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {		
		ValueEnvironment<Sign> newSign = refineSignFromSymbolic(symbolic, signEnv);
		// Reset symbolic to top only at guard points (comparison expressions).
		// For sub-expressions (identifiers, constants, arithmetic) the symbolic
		// must remain intact so that the guard expression itself can still use
		// it for refinement. smallStepSemantics is called for every sub-node, so
		// resetting unconditionally would kill the symbolic before the comparison
		// is evaluated.
		boolean isGuard = expression instanceof BinaryExpression
				&& ((BinaryExpression) expression).getOperator() instanceof ComparisonOperator;
		return new CombinationDomain(isGuard ? symbolic.top() : symbolic, newSign);
	}

	/**
	 * Refines the domain by assuming {@code expression} holds on the edge from
	 * {@code src} to {@code dest}. This is the point at which the symbolic
	 * component acts as a proof oracle for the sign component:
	 * <ol>
	 * <li>The sign domain's own {@code assume} is applied first for
	 * condition-driven sign constraints (e.g., {@code x > 0} → x is
	 * positive).</li>
	 * <li>The current symbolic state is then used to further refine the sign
	 * environment via {@link #refineSignFromSymbolic}: any variable whose
	 * symbolic expression evaluates to a concrete sign (given that all
	 * {@link SymbolicVariable}s are positive) has its sign overridden.</li>
	 * <li>After refinement the symbolic component is reset to
	 * {@link SymbolicAbstractDomain#top() top}: it has served its purpose for
	 * this block. The sign environment carries the refined information
	 * forward.</li>
	 * </ol>
	 *
	 * <p>This implements the "blocks output symbolic things used by guards to
	 * refine signs" contract: a block builds up exact symbolic expressions;
	 * the guard (assume) consumes them to improve sign precision; and the
	 * symbolic is discarded so the next block starts fresh.
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
	public CombinationDomain assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<Sign> refinedSigns = refineSignFromSymbolic(symbolic, signEnv);
		ValueEnvironment<Sign> assumedSigns = refinedSigns.assume(expression, src, dest, oracle);
		return new CombinationDomain(symbolic.top(), assumedSigns);
	}

	/**
	 * Returns {@code true} if either the symbolic or the sign component tracks
	 * a binding for {@code id}.
	 *
	 * @param id the identifier to query
	 *
	 * @return whether any component knows about {@code id}
	 */
	@Override
	public boolean knowsIdentifier(Identifier id) {
		return symbolic.knowsIdentifier(id) || signEnv.knowsIdentifier(id);
	}

	/**
	 * Removes the binding for {@code id} from both components.
	 *
	 * @param id the identifier to forget
	 *
	 * @return the updated combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomain forgetIdentifier(Identifier id) throws SemanticException {
		return new CombinationDomain(
				symbolic.forgetIdentifier(id),
				signEnv.forgetIdentifier(id));
	}

	/**
	 * Removes all bindings whose identifiers satisfy {@code test} from both
	 * components.
	 *
	 * @param test the predicate selecting identifiers to forget
	 *
	 * @return the updated combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomain forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return new CombinationDomain(
				symbolic.forgetIdentifiersIf(test),
				signEnv.forgetIdentifiersIf(test));
	}

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
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return symbolic.satisfies(expression, pp, oracle)
				.and(signEnv.satisfies(expression, pp, oracle));
	}

	/**
	 * Returns a structured representation of this domain, printing the
	 * symbolic state and the sign environment on separate labelled sections.
	 *
	 * @return the structured representation
	 */
	@Override
	public StructuredRepresentation representation() {
		String sym = symbolic == null ? "<null>" : symbolic.representation().toString();
		String signs = signEnv == null ? "<null>" : signEnv.representation().toString();
		return new StringRepresentation("Symbolic:\n" + sym + "\nSigns:\n" + signs);
	}

	/**
	 * Pushes a new interprocedural scope identified by {@code token} onto both
	 * components.
	 *
	 * @param token the scope token identifying the call site
	 *
	 * @return the scoped combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomain pushScope(ScopeToken token) throws SemanticException {
		return new CombinationDomain(
				symbolic.pushScope(token),
				signEnv.pushScope(token));
	}

	/**
	 * Pops the interprocedural scope identified by {@code token} from both
	 * components.
	 *
	 * @param token the scope token to pop
	 *
	 * @return the unscoped combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomain popScope(ScopeToken token) throws SemanticException {
		return new CombinationDomain(
				symbolic.popScope(token),
				signEnv.popScope(token));
	}

	/**
	 * Returns {@code true} if both the symbolic and sign components are less
	 * than or equal to the corresponding components of {@code other}.
	 *
	 * @param other the element to compare against
	 *
	 * @return whether this element is below {@code other} in the partial order
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public boolean lessOrEqual(CombinationDomain other) throws SemanticException {
		if (other.isTop())
			return true;
		else if (isTop())
			return false;
		else if (isBottom())
			return true;
		else if (other.isBottom())
			return false;
		return symbolic.lessOrEqual(other.symbolic) && signEnv.lessOrEqual(other.signEnv);
	}

	/**
	 * Computes the least upper bound of this element and {@code other}.
	 *
	 * <p><strong>Symbolic state policy.</strong> The symbolic component of the
	 * result is kept as <em>this</em> element's symbolic state, not a join of
	 * both sides. At a loop head, {@code this} carries the fixed pre-loop entry
	 * state (the exact linear relationships in terms of the original symbolic
	 * input variables). {@code other} carries the post-body symbolic state,
	 * which is the body's output derived from exactly that same pre-loop state.
	 * Joining the two symbolic states would cause the expression sets to grow
	 * without bound across iterations (e.g.
	 * {@code x_sym}, {@code x_sym+1}, {@code x_sym+2}, …), preventing
	 * convergence. By keeping {@code this.symbolic} fixed, the symbolic stays
	 * stable across all loop iterations.
	 *
	 * <p><strong>Sign refinement.</strong> The sign environment is joined
	 * normally (pointwise {@link Sign} join), and then refined using
	 * {@code other.symbolic} — the block body's symbolic summary. This yields
	 * a more precise sign than the sign domain's own join would produce,
	 * because the symbolic summary carries exact linear-form information. For
	 * programs where every body expression has a stable sign (e.g. all
	 * expressions remain positive), the refined sign at the loop head equals
	 * the pre-loop sign, and the fixpoint is reached in one pass.
	 *
	 * @param other the element to join with
	 *
	 * @return the joined and refined combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomain lub(CombinationDomain other) throws SemanticException {
		if (this == other || isBottom() || other.isTop() || equals(other))
			return other;
		if (other.isBottom() || isTop())
			return this;
		// Keep THIS symbolic fixed (pre-loop / block-entry state).
		// Use OTHER symbolic (block body summary) only for sign refinement.
		ValueEnvironment<Sign> lubSigns = signEnv.lub(other.signEnv);
		ValueEnvironment<Sign> refined = refineSignFromSymbolic(other.symbolic, lubSigns);
		return new CombinationDomain(this.symbolic, refined);
	}

	/**
	 * Returns the top element of this domain: both components at top.
	 *
	 * @return the top element
	 */
	@Override
	public CombinationDomain top() {
		return new CombinationDomain(symbolic.top(), signEnv.top());
	}

	/**
	 * Returns the bottom element of this domain: both components at bottom.
	 *
	 * @return the bottom element
	 */
	@Override
	public CombinationDomain bottom() {
		return new CombinationDomain(symbolic.bottom(), signEnv.bottom());
	}

	/**
	 * Returns {@code true} if both components are top (no information).
	 *
	 * @return whether this is the top element
	 */
	@Override
	public boolean isTop() {
		return symbolic.isTop() && signEnv.isTop();
	}

	/**
	 * Returns {@code true} if either component is bottom (unreachable on
	 * either side implies global unreachability).
	 *
	 * @return whether this is the bottom element
	 */
	@Override
	public boolean isBottom() {
		return symbolic.isBottom() || signEnv.isBottom();
	}

	@Override
	public int hashCode() {
		return Objects.hash(symbolic, signEnv);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		CombinationDomain other = (CombinationDomain) obj;
		return Objects.equals(symbolic, other.symbolic) && Objects.equals(signEnv, other.signEnv);
	}
}
