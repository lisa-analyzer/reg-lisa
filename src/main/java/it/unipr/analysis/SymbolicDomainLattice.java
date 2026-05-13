package it.unipr.analysis;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class SymbolicDomainLattice implements ValueLattice<SymbolicDomainLattice> {
	/**
	 * A synthetic {@link Constant} representing the boolean value {@code true},
	 * used as the default path condition (i.e., no constraint).
	 */
	public static final Constant TRUE = new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE);

	/**
	 * The path condition of this abstract element, represented as a
	 * {@link SymbolicExpression}. The default value is {@link #TRUE}, meaning
	 * no constraint is imposed on the execution path.
	 */
	private final SymbolicExpression pathCondition;

	/**
	 * The symbolic state: a functional lattice mapping each tracked
	 * {@link Identifier} to an {@link ExpressionSet} containing its symbolic
	 * representative expression.
	 */
	private final GenericMapLattice<Identifier, ExpressionSet> symbolicState;

	/**
	 * Builds the top element of this domain, with the default path condition
	 * ({@link #TRUE}) and a top symbolic state (no information).
	 */
	public SymbolicDomainLattice() {
		this(TRUE, new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}

	/**
	 * Builds a {@link SymbolicAbstractDomain} with the given path condition and
	 * symbolic state.
	 *
	 * @param pathCondition the path condition of this abstract element
	 * @param symbolicState the functional map from identifiers to symbolic
	 *                          expressions
	 */
	public SymbolicDomainLattice(
			SymbolicExpression pathCondition,
			GenericMapLattice<Identifier, ExpressionSet> symbolicState) {
		this.pathCondition = pathCondition;
		this.symbolicState = symbolicState;
	}

	@Override
	public SymbolicDomainLattice store(Identifier target, Identifier source) throws SemanticException {
		return new SymbolicDomainLattice(pathCondition,
				this.symbolicState.putState(target, this.symbolicState.getState(source)));
	}

	/**
	 * Returns {@code true} if this symbolic state holds a binding for
	 * {@code id}, i.e., the identifier is tracked in the functional map. Always
	 * returns {@code false} for top and bottom elements.
	 *
	 * @param id the identifier to look up
	 *
	 * @return whether this state contains information about {@code id}
	 */
	@Override
	public boolean knowsIdentifier(Identifier id) {
		if (isTop() || isBottom())
			return false;
		return symbolicState.function != null && symbolicState.function.containsKey(id);
	}

	/**
	 * Returns a copy of this symbolic state with the binding for {@code id}
	 * removed. If this element is top, bottom, or {@code id} is not tracked,
	 * this state is returned unchanged.
	 *
	 * @param id the identifier whose binding should be removed
	 *
	 * @return a new {@link SymbolicAbstractDomain} without a binding for
	 *             {@code id}
	 *
	 * @throws SemanticException if an error occurs while computing the result
	 */
	@Override
	public SymbolicDomainLattice forgetIdentifier(Identifier id, ProgramPoint pp) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null || !symbolicState.function.containsKey(id))
			return this;
		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(symbolicState.function, true);
		newMap.remove(id);
		return new SymbolicDomainLattice(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
	}

	/**
	 * Returns a copy of this symbolic state with all identifiers satisfying
	 * {@code test} removed. If this element is top, bottom, or the functional
	 * map is empty, this state is returned unchanged.
	 *
	 * @param test the predicate that selects the identifiers to forget
	 *
	 * @return a new {@link SymbolicAbstractDomain} with the matching bindings
	 *             removed
	 *
	 * @throws SemanticException if an error occurs while computing the result
	 */
	@Override
	public SymbolicDomainLattice forgetIdentifiersIf(Predicate<Identifier> test, ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;
		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(symbolicState.function, true);
		newMap.keySet().removeIf(test);
		return new SymbolicDomainLattice(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
	}

	@Override
	public SymbolicDomainLattice forgetIdentifiers(Iterable<Identifier> ids, ProgramPoint pp) throws SemanticException {
		SymbolicDomainLattice result = this;
		for (Identifier id : ids)
			result = result.forgetIdentifier(id, pp);
		return result;
	}

	/**
	 * Checks whether this abstract element is less than or equal to
	 * {@code other} in the symbolic domain's partial order. The comparison is
	 * delegated to the underlying {@link GenericMapLattice}, which performs a
	 * pointwise check: for every tracked identifier {@code k}, the symbolic
	 * expression set held by this element must be less than or equal to the one
	 * held by {@code other}.
	 *
	 * @param other the element to compare against
	 *
	 * @return {@code true} if this element is below {@code other} in the
	 *             partial order
	 *
	 * @throws SemanticException if an error occurs during the comparison
	 */
	@Override
	public boolean lessOrEqual(SymbolicDomainLattice other) throws SemanticException {
		if (isBottom() || other.isTop())
			return true;
		if (isTop() || other.isBottom())
			return false;
		return symbolicState.lessOrEqual(other.symbolicState);
	}

	/**
	 * Computes the least upper bound of this abstract element and
	 * {@code other}. The join is delegated to the underlying
	 * {@link GenericMapLattice}, which performs a pointwise join: for every
	 * identifier tracked in either element, the resulting symbolic expression
	 * set is the join of the two corresponding sets (missing entries are
	 * treated as top).
	 *
	 * @param other the element to join with
	 *
	 * @return a new {@link SymbolicAbstractDomain} whose symbolic state is the
	 *             pointwise join of this element's state and {@code other}'s
	 *             state
	 *
	 * @throws SemanticException if an error occurs during the join
	 */
	@Override
	public SymbolicDomainLattice lub(SymbolicDomainLattice other) throws SemanticException {
		if (isBottom() || other.isTop())
			return other;
		if (other.isBottom() || isTop())
			return this;
		SymbolicExpression mergedPC = Objects.equals(pathCondition, other.pathCondition)
				? pathCondition
				: TRUE;
		return new SymbolicDomainLattice(mergedPC, symbolicState.lub(other.symbolicState));
	}

	/**
	 * Returns the top element of this domain: a fresh instance whose symbolic
	 * state is top and whose path condition is {@code true}.
	 *
	 * @return the top element
	 */
	@Override
	public SymbolicDomainLattice top() {
		return new SymbolicDomainLattice(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}

	/**
	 * Returns the bottom element of this domain: a fresh instance whose
	 * symbolic state is bottom and whose path condition is {@code true}.
	 *
	 * @return the bottom element
	 */
	@Override
	public SymbolicDomainLattice bottom() {
		return new SymbolicDomainLattice(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).bottom());
	}

	/**
	 * Returns {@code true} if this element is the top of the symbolic domain,
	 * i.e., the underlying symbolic state is top (no information).
	 *
	 * @return whether this is the top element
	 */
	@Override
	public boolean isTop() {
		return this.symbolicState.isTop();
	}

	/**
	 * Returns {@code true} if this element is the bottom of the symbolic
	 * domain, i.e., the underlying symbolic state is bottom (unreachable).
	 *
	 * @return whether this is the bottom element
	 */
	@Override
	public boolean isBottom() {
		return this.symbolicState.isBottom();
	}

	/**
	 * Returns a {@link StructuredRepresentation} of this symbolic state as a
	 * string containing the string representation of the underlying functional
	 * lattice.
	 *
	 * @return a {@link StringRepresentation} of the symbolic state
	 */
	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(
				"Path condition: " + pathCondition.toString() + ", \n" + "Symbolic state: " + symbolicState.toString());
	}

	/**
	 * Pushes a new interprocedural scope identified by {@code token} onto the
	 * symbolic state. Each currently tracked identifier {@code id} is replaced
	 * by {@code id.pushScope(token)}, which wraps it in an
	 * {@link it.unive.lisa.symbolic.value.OutOfScopeIdentifier} so that the
	 * binding is preserved but hidden during the callee's execution.
	 * Identifiers for which {@code pushScope} returns {@code null} are dropped.
	 * On key collision the associated {@link ExpressionSet} values are joined
	 * via {@link ExpressionSet#lub}.
	 *
	 * @param token the scope token identifying the call site
	 *
	 * @return a new {@link SymbolicAbstractDomain} with all bindings scoped
	 *             under {@code token}
	 *
	 * @throws SemanticException if an error occurs while pushing the scope
	 */
	@Override
	public SymbolicDomainLattice pushScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;

		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(null, false);
		for (Identifier id : symbolicState.getKeys()) {
			Identifier scoped = (Identifier) id.pushScope(token, pp);
			if (scoped != null) {
				ExpressionSet val = symbolicState.getState(id);
				if (!newMap.containsKey(scoped))
					newMap.put(scoped, val);
				else
					newMap.put(scoped, val.lub(newMap.get(scoped)));
			}
		}

		return new SymbolicDomainLattice(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
	}

	/**
	 * Pops the interprocedural scope identified by {@code token} from the
	 * symbolic state. Each currently tracked identifier {@code id} is
	 * transformed via {@code id.popScope(token)}: identifiers that belong to
	 * the current scope (i.e., regular {@link Variable} instances) return
	 * {@code null} and are dropped; identifiers that were hidden by a matching
	 * {@code pushScope} are restored to their original form. On key collision
	 * the associated {@link ExpressionSet} values are joined via
	 * {@link ExpressionSet#lub}.
	 *
	 * @param token the scope token identifying the call site to pop
	 *
	 * @return a new {@link SymbolicAbstractDomain} with the scope for
	 *             {@code token} removed and previously hidden bindings restored
	 *
	 * @throws SemanticException if an error occurs while popping the scope
	 */
	@Override
	public SymbolicDomainLattice popScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;

		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(null, false);
		for (Identifier id : symbolicState.getKeys()) {
			Identifier unscoped = (Identifier) id.popScope(token, pp);
			if (unscoped != null) {
				ExpressionSet val = symbolicState.getState(id);
				if (!newMap.containsKey(unscoped))
					newMap.put(unscoped, val);
				else
					newMap.put(unscoped, val.lub(newMap.get(unscoped)));
			}
		}

		return new SymbolicDomainLattice(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
	}

	@Override
	public int hashCode() {
		return Objects.hash(pathCondition, symbolicState);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolicDomainLattice other = (SymbolicDomainLattice) obj;
		return Objects.equals(pathCondition, other.pathCondition) && Objects.equals(symbolicState, other.symbolicState);
	}

	public SymbolicExpression getPathCondition() {
		return pathCondition;
	}

	public GenericMapLattice<Identifier, ExpressionSet> getSymbolicState() {
		return symbolicState;
	}

	/**
	 * Returns the set of identifiers currently tracked in this symbolic state.
	 * Returns an empty set when the state is top, bottom, or the functional map
	 * is uninitialised.
	 *
	 * @return the set of tracked identifiers (never {@code null})
	 */
	public Set<Identifier> getKeys() {
		return symbolicState.getKeys();
	}

	/**
	 * Returns the symbolic expression associated with {@code id} in this
	 * symbolic state, or {@code null} if {@code id} is not tracked.
	 *
	 * @param id the identifier to look up
	 *
	 * @return the symbolic expression for {@code id}, or {@code null}
	 */
	public SymbolicExpression getSymbolicExpression(Identifier id) {
		if (symbolicState.function == null || !symbolicState.function.containsKey(id))
			return null;
		ExpressionSet set = symbolicState.function.get(id);
		if (set == null || set.elements.isEmpty())
			return null;
		else if (set.size() > 1)
			throw new IllegalStateException("Multiple symbolic expressions for identifier " + id);
		return set.elements.iterator().next();
	}

	/**
	 * Returns the sign of a {@link SymbolicVariable} as recorded in the path
	 * condition, or {@link it.unive.lisa.analysis.numeric.Sign#TOP} if no
	 * constraint is known. Callers that default positively (e.g., for plain
	 * {@code input()}) should treat a {@code TOP} result as
	 * {@link it.unive.lisa.analysis.numeric.Sign#POS}.
	 *
	 * @param var the symbolic variable to query
	 *
	 * @return {@link it.unive.lisa.analysis.numeric.Sign#POS} if
	 *             {@code var > 0} is in the path condition,
	 *             {@link it.unive.lisa.analysis.numeric.Sign#NEG} if
	 *             {@code var < 0} is, or
	 *             {@link it.unive.lisa.analysis.numeric.Sign#TOP} if no
	 *             constraint was found
	 */
	public SignLattice getSignOf(SymbolicVariable var) {
		return extractSign(pathCondition, var);
	}

	/**
	 * Returns the sign of an arbitrary symbolic expression as recorded in the
	 * path condition, or {@link SignLattice#TOP} if no constraint is known.
	 *
	 * @param expr the symbolic expression to query
	 *
	 * @return the sign derived from the path condition
	 */
	public SignLattice getSignOfExpr(SymbolicExpression expr) {
		return extractSign(pathCondition, expr);
	}

	private static SignLattice extractSign(
			SymbolicExpression pc, SymbolicExpression target) {
		if (!(pc instanceof BinaryExpression))
			return SignLattice.TOP;
		BinaryExpression bin = (BinaryExpression) pc;
		BinaryOperator op = bin.getOperator();
		if (op instanceof LogicalAnd) {
			SignLattice l = extractSign(bin.getLeft(), target);
			if (!l.isTop())
				return l;
			return extractSign(bin.getRight(), target);
		}
		if (bin.getLeft().equals(target)) {
			if (op instanceof ComparisonGt)
				return SignLattice.POS;
			if (op instanceof ComparisonLt)
				return SignLattice.NEG;
			// expr >= 0: non-negative; return ZERO as a proxy so that callers
			// treating ZERO-or-POS as "non-negative" can simplify max(0, expr)
			// → expr.
			if (op instanceof ComparisonGe)
				return SignLattice.ZERO;
			// expr <= k: cannot determine sign from an upper bound alone.
			if (op instanceof ComparisonLe)
				return SignLattice.TOP;
		}
		return SignLattice.TOP;
	}
}
