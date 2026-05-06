package it.unipr.analysis;

import java.util.Objects;
import java.util.function.Predicate;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class CombinationDomainLattice implements ValueLattice<CombinationDomainLattice> {

	/**
	 * The symbolic component, tracking exact linear combinations of
	 * {@link SymbolicVariable} instances for each program variable.
	 */
	private final SymbolicDomainLattice symbolic;

	/**
	 * The SignLattice component, recording the SignLattice (positive, negative, zero) of each
	 * program variable. It is kept consistent with the symbolic component via
	 * the refinement operators in {@link #asSignLattice} and {@link #lub}.
	 */
	private final ValueEnvironment<SignLattice> signEnv;


	/**
	 * Builds the top element of this combination domain.
	 */
	public CombinationDomainLattice() {
		this(new SymbolicDomainLattice(),
				new ValueEnvironment<SignLattice>(new SignLattice()));
	}

	/**
	 * Builds a combination domain from all three components.
	 *
	 * @param symbolic     the symbolic component
	 * @param SignLatticeEnv      the SignLattice environment component
	 * @param savedSignLatticeEnv the auxiliary SignLattice environment accumulated from
	 *                         assume calls (used only at {@link #popScope})
	 */
	public CombinationDomainLattice(
			SymbolicDomainLattice symbolic,
			ValueEnvironment<SignLattice> SignLatticeEnv) {
		this.symbolic = symbolic;
		this.signEnv = SignLatticeEnv;
	}
	
	@Override
	public CombinationDomainLattice store(Identifier target, Identifier source) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns {@code true} if either the symbolic or the SignLattice component tracks
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
	public CombinationDomainLattice forgetIdentifier(Identifier id, ProgramPoint pp) throws SemanticException {
		return new CombinationDomainLattice(
				symbolic.forgetIdentifier(id, pp),
				signEnv.forgetIdentifier(id, pp));
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
	public CombinationDomainLattice forgetIdentifiersIf(Predicate<Identifier> test, ProgramPoint pp) throws SemanticException {
		return new CombinationDomainLattice(
				symbolic.forgetIdentifiersIf(test, pp),
				signEnv.forgetIdentifiersIf(test, pp));
	}
	
	@Override
	public CombinationDomainLattice forgetIdentifiers(Iterable<Identifier> ids, ProgramPoint pp)
			throws SemanticException {
		return new CombinationDomainLattice(
				symbolic.forgetIdentifiers(ids, pp),
				signEnv.forgetIdentifiers(ids, pp));
	}
	
	/**
	 * Returns a structured representation of this domain, printing the symbolic
	 * state and the SignLattice environment on separate labelled sections.
	 *
	 * @return the structured representation
	 */
	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		String sym = symbolic == null ? "<null>" : symbolic.representation().toString();
		String SignLattices = signEnv == null ? "<null>" : signEnv.toString();
		return new StringRepresentation("Symbolic:\n" + sym + "\nSigns:\n" + SignLattices);
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
	public CombinationDomainLattice pushScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		return new CombinationDomainLattice(
				symbolic.pushScope(token, pp),
				signEnv.pushScope(token, pp));
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
	public CombinationDomainLattice popScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		SymbolicDomainLattice newSymbolic = symbolic.popScope(token, pp);
		// Use savedSignLatticeEnv (accumulated from assume calls) as the base so that
		// variables refined by guards (e.g. x:+ from assume(x>100)) are visible
		// at the return node even though SignLatticeEnv was reset to TOP by the last
		// asSignLattice.
		ValueEnvironment<SignLattice> newSignLatticeEnv = refineSignLatticeFromSymbolic(symbolic, signEnv).popScope(token, pp);
		return new CombinationDomainLattice(newSymbolic, newSignLatticeEnv);
	}
	
	/**
	 * Returns {@code true} if both the symbolic and SignLattice components are less
	 * than or equal to the corresponding components of {@code other}.
	 *
	 * @param other the element to compare against
	 *
	 * @return whether this element is below {@code other} in the partial order
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public boolean lessOrEqual(CombinationDomainLattice other) throws SemanticException {
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
	 * <p>
	 * <strong>Symbolic state policy.</strong> The symbolic component of the
	 * result is kept as <em>this</em> element's symbolic state, not a join of
	 * both sides. At a loop head, {@code this} carries the fixed pre-loop entry
	 * state (the exact linear relationships in terms of the original symbolic
	 * input variables). {@code other} carries the post-body symbolic state,
	 * which is the body's output derived from exactly that same pre-loop state.
	 * Joining the two symbolic states would cause the expression sets to grow
	 * without bound across iterations (e.g. {@code x_sym}, {@code x_sym+1},
	 * {@code x_sym+2}, …), preventing convergence. By keeping
	 * {@code this.symbolic} fixed, the symbolic stays stable across all loop
	 * iterations.
	 * <p>
	 * <strong>SignLattice refinement.</strong> The SignLattice environment is joined normally
	 * (pointwise {@link SignLattice} join), and then refined using
	 * {@code other.symbolic} — the block body's symbolic summary. This yields a
	 * more precise SignLattice than the SignLattice domain's own join would produce, because
	 * the symbolic summary carries exact linear-form information. For programs
	 * where every body expression has a stable SignLattice (e.g. all expressions
	 * remain positive), the refined SignLattice at the loop head equals the pre-loop
	 * SignLattice, and the fixpoint is reached in one pass.
	 *
	 * @param other the element to join with
	 *
	 * @return the joined and refined combination domain
	 *
	 * @throws SemanticException if an error occurs
	 */
	@Override
	public CombinationDomainLattice lub(CombinationDomainLattice other) throws SemanticException {
		// Keep THIS symbolic fixed (pre-loop / block-entry state).
		// Use OTHER symbolic (block body summary) only for SignLattice refinement.
		ValueEnvironment<SignLattice> refined = refineSignLatticeFromSymbolic(other.symbolic, other.signEnv);
		ValueEnvironment<SignLattice> lubSaved = refined.lub(this.signEnv);
		return new CombinationDomainLattice(other.symbolic, lubSaved);
	}

	/**
	 * Returns the top element of this domain: both components at top.
	 *
	 * @return the top element
	 */
	@Override
	public CombinationDomainLattice top() {
		return new CombinationDomainLattice(symbolic.top(), signEnv.top());
	}

	/**
	 * Returns the bottom element of this domain: both components at bottom.
	 *
	 * @return the bottom element
	 */
	@Override
	public CombinationDomainLattice bottom() {
		return new CombinationDomainLattice(symbolic.bottom(), signEnv.bottom());
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
	 * Returns {@code true} if either component is bottom (unreachable on either
	 * side implies global unreachability).
	 *
	 * @return whether this is the bottom element
	 */
	@Override
	public boolean isBottom() {
		return symbolic.isBottom() || signEnv.isBottom();
	}
	
	/**
	 * Refines {@code base} by re-deriving the SignLattice of each variable tracked in
	 * {@code sym} from its symbolic expression set. For variables whose
	 * expression set contains multiple alternatives (as can happen after a
	 * {@link #lub}), the refinement SignLattice is the join of the SignLattices of all
	 * alternatives. Plain {@link Identifier} nodes inside symbolic expressions
	 * (ordinary program variables that appear after an {@code assume} reset)
	 * are resolved by looking up their SignLattice in {@code base}. If the resulting
	 * SignLattice is top (no improvement over the fallback SignLattice join), or bottom
	 * (empty expression set), the binding in {@code base} is left unchanged.
	 *
	 * @param sym  the symbolic state to use for refinement
	 * @param base the SignLattice environment to refine (typically the result of a
	 *                 SignLattice join); also used to resolve plain identifier SignLattices
	 *
	 * @return the refined SignLattice environment
	 *
	 * @throws SemanticException if an error occurs during the refinement
	 */
	public static ValueEnvironment<SignLattice> refineSignLatticeFromSymbolic(
			SymbolicDomainLattice sym,
			ValueEnvironment<SignLattice> base)
			throws SemanticException {
		if (sym.isTop() || sym.isBottom())
			return base;

		ValueEnvironment<SignLattice> result = base;
		for (Identifier id : sym.getKeys()) {
			SymbolicExpression expr = sym.getSymbolicExpression(id);
			if (expr == null)
				continue;

			SignLattice derived = deriveSignLatticeFromExpr(expr, base, sym);

			// Only override the SignLattice join if we obtained something more
			// concrete.
//			if (!derived.isTop() && !derived.isBottom())
				result = result.putState(id, derived);
		}
		return result;
	}
	
	/**
	 * Derives a {@link SignLattice} value from a symbolic expression.
	 * {@link SymbolicVariable} nodes (results of {@code input()},
	 * {@code inputPos()}, or {@code inputNeg()}) are resolved by querying
	 * {@code sym}'s path condition: if {@code x_sym > 0} is recorded, the SignLattice
	 * is {@link SignLattice#POS}; if {@code x_sym < 0}, it is {@link SignLattice#NEG};
	 * otherwise (plain {@code input()}) the SignLattice defaults to {@link SignLattice#POS}.
	 * Plain {@link Identifier} nodes are resolved via {@code SignLatticeEnv}.
	 * {@link BinaryExpression} nodes are evaluated recursively using
	 * {@link SignLattice#evalBinaryExpression}.
	 *
	 * @param expr    the symbolic expression to evaluate
	 * @param SignLatticeEnv the SignLattice environment used to resolve plain identifiers;
	 *                    may be {@code null}
	 * @param sym     the symbolic state whose path condition constrains
	 *                    {@link SymbolicVariable} SignLattices; may be {@code null}
	 *
	 * @return the derived SignLattice, or {@link SignLattice#TOP} if it cannot be determined
	 */
	private static SignLattice deriveSignLatticeFromExpr(
			SymbolicExpression expr,
			ValueEnvironment<SignLattice> SignLatticeEnv,
			SymbolicDomainLattice sym) {
		if (expr instanceof Constant) {
			Object val = ((Constant) expr).getValue();
			if (val instanceof Number) {
				int v = ((Number) val).intValue();
				return v > 0 ? SignLattice.POS : v < 0 ? SignLattice.NEG : SignLattice.ZERO;
			}
			return SignLattice.TOP;
		}
		
		if (expr instanceof SymbolicVariable) {
			if (sym != null) {
				SignLattice s = sym.getSignOf((SymbolicVariable) expr);
				return s;
			}
			return SignLattice.POS;
		}
		
		if (expr instanceof Identifier) {
			if (SignLatticeEnv != null)
				return SignLatticeEnv.getState((Identifier) expr);
			return SignLattice.TOP;
		}
		
		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			SignLattice left = deriveSignLatticeFromExpr(bin.getLeft(), SignLatticeEnv, sym);
			SignLattice right = deriveSignLatticeFromExpr(bin.getRight(), SignLatticeEnv, sym);
			
			// FIXME: LiSA Bug in multiplication
			if ((left.isTop() && right.isNegative() || right.isTop() && left.isNegative()) && bin.getOperator() instanceof MultiplicationOperator)
				return SignLattice.TOP;
			else if ((left.isTop() && right.isPositive() || right.isTop() && left.isPositive()) && bin.getOperator() instanceof MultiplicationOperator)
				return SignLattice.TOP;
			return new Sign().evalBinaryExpression(bin, left, right, null, null);
		}
		
		return SignLattice.TOP;
	}

	public SymbolicDomainLattice getSymbolic() {
		return symbolic;
	}

	public ValueEnvironment<SignLattice> getSignLatticeEnv() {
		return signEnv;
	}

	@Override
	public int hashCode() {
		return Objects.hash(signEnv, symbolic);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CombinationDomainLattice other = (CombinationDomainLattice) obj;
		return Objects.equals(signEnv, other.signEnv)
				&& Objects.equals(symbolic, other.symbolic);
	}
}
