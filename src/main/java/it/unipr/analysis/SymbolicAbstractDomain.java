package it.unipr.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A symbolic abstract domain that tracks, for each program variable, a
 * symbolic expression representing its value in terms of fresh symbolic
 * variables introduced at {@code input()} call sites. The symbolic state is
 * stored as a functional lattice mapping each {@link Identifier} to an
 * {@link ExpressionSet} containing its symbolic representative. Arithmetic
 * expressions are simplified to a canonical linear-combination form via
 * {@link #simplify(SymbolicExpression)}.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class SymbolicAbstractDomain implements ValueDomain<SymbolicAbstractDomain> {

	/** 
	 * A synthetic {@link Constant} representing the boolean value {@code true},
	 * used as the default path condition (i.e., no constraint).
	 */
	private static final Constant TRUE = new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE);

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
	public SymbolicAbstractDomain() {
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
	private SymbolicAbstractDomain(
			SymbolicExpression pathCondition,
			GenericMapLattice<Identifier, ExpressionSet> symbolicState) {
		this.pathCondition = pathCondition;
		this.symbolicState = symbolicState;
	}

	/**
	 * Updates the symbolic state by assigning the symbolic evaluation of
	 * {@code expression} to {@code id}. The expression is evaluated against
	 * the current symbolic state via {@link #eval(SymbolicExpression)}, and the
	 * resulting symbolic representative is stored in the functional map.
	 *
	 * @param id         the identifier being assigned
	 * @param expression the right-hand side expression
	 * @param pp         the program point where the assignment occurs
	 * @param oracle     the semantic oracle for additional queries
	 *
	 * @return a new {@link SymbolicAbstractDomain} with the updated binding for
	 *             {@code id}
	 *
	 * @throws SemanticException if an error occurs during evaluation
	 */
	@Override
	public SymbolicAbstractDomain assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		SymbolicExpression v = eval(expression);
		GenericMapLattice<Identifier, ExpressionSet> cpy = this.symbolicState.putState(id, new ExpressionSet(v));

		// If the RHS is a signed input, record the constraint in the path condition.
		SymbolicExpression newPathCondition = this.pathCondition;
		if (v instanceof SymbolicVariable) {
			Constant zero = new Constant(Untyped.INSTANCE, 0, SyntheticLocation.INSTANCE);
			SymbolicExpression constraint = null;
			if (expression instanceof PushPos)
				constraint = new BinaryExpression(Untyped.INSTANCE, v, zero, ComparisonGt.INSTANCE,
						SyntheticLocation.INSTANCE);
			else if (expression instanceof PushNeg)
				constraint = new BinaryExpression(Untyped.INSTANCE, v, zero, ComparisonLt.INSTANCE,
						SyntheticLocation.INSTANCE);
			if (constraint != null)
				newPathCondition = simplifyPathCondition(new BinaryExpression(
						Untyped.INSTANCE, this.pathCondition, constraint,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE));
		}

		return new SymbolicAbstractDomain(newPathCondition, cpy);
	}

	/**
	 * Simplifies a path condition expression by:
	 * <ul>
	 * <li>removing {@code true} leaves from conjunctions;</li>
	 * <li>for each variable, keeping only the weakest lower bound
	 * ({@code var > k} with smallest {@code k}) and the strictest upper bound
	 * ({@code var < k} with smallest {@code k});</li>
	 * <li>returning a {@code false} constant when a contradiction is detected
	 * ({@code var > lb} and {@code var < ub} with {@code lb >= ub}).</li>
	 * </ul>
	 *
	 * @param pc the raw path condition to simplify
	 *
	 * @return the simplified path condition
	 */
	private SymbolicExpression simplifyPathCondition(SymbolicExpression pc) {
		List<SymbolicExpression> flat = new ArrayList<>();
		flattenAnd(pc, flat);

		// Remove "true" leaves
		flat.removeIf(c -> c instanceof Constant && Boolean.TRUE.equals(((Constant) c).getValue()));

		if (flat.isEmpty())
			return TRUE;

		List<SymbolicExpression> result = simplifyComparisons(flat);
		if (result == null)
			return new Constant(Untyped.INSTANCE, false, SyntheticLocation.INSTANCE);
		if (result.isEmpty())
			return TRUE;

		// Rebuild conjunction in deterministic order
		result.sort((a, b) -> a.toString().compareTo(b.toString()));
		SymbolicExpression out = result.get(0);
		for (int i = 1; i < result.size(); i++)
			out = new BinaryExpression(Untyped.INSTANCE, out, result.get(i),
					LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
		return out;
	}

	/**
	 * Flattens a left- or right-associative chain of {@link LogicalAnd}
	 * conjunctions into a flat list of atomic constraints.
	 *
	 * @param expr the expression to flatten
	 * @param out  the list to accumulate atomic constraints into
	 */
	private static void flattenAnd(SymbolicExpression expr, List<SymbolicExpression> out) {
		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			if (bin.getOperator() instanceof LogicalAnd) {
				flattenAnd(bin.getLeft(), out);
				flattenAnd(bin.getRight(), out);
				return;
			}
		}
		out.add(expr);
	}

	/**
	 * Processes a flat list of constraints, merging comparison constraints on
	 * the same variable. Returns {@code null} to signal a contradiction.
	 * For two lower-bound constraints on the same variable ({@code var > k1}
	 * and {@code var > k2}), the weaker one (smallest {@code k}) is kept.
	 * For two upper-bound constraints ({@code var < k1} and {@code var < k2}),
	 * the strictest one (smallest {@code k}) is kept.
	 *
	 * @param constraints the flat list of atomic constraints
	 *
	 * @return the simplified list, or {@code null} if a contradiction is found
	 */
	private List<SymbolicExpression> simplifyComparisons(List<SymbolicExpression> constraints) {
		// Each entry: [varExpr, lowerBoundConst-or-null, upperBoundConst-or-null]
		List<Object[]> varEntries = new ArrayList<>();
		List<SymbolicExpression> other = new ArrayList<>();

		for (SymbolicExpression c : constraints) {
			if (c instanceof BinaryExpression) {
				BinaryExpression bin = (BinaryExpression) c;
				BinaryOperator op = bin.getOperator();
				if ((op instanceof ComparisonGt || op instanceof ComparisonLt)
						&& bin.getRight() instanceof Constant
						&& ((Constant) bin.getRight()).getValue() instanceof Number) {
					SymbolicExpression var = bin.getLeft();
					int k = ((Number) ((Constant) bin.getRight()).getValue()).intValue();

					// Find existing entry for this variable
					Object[] entry = null;
					for (Object[] e : varEntries)
						if (((SymbolicExpression) e[0]).equals(var)) { entry = e; break; }
					if (entry == null) {
						entry = new Object[] { var, null, null };
						varEntries.add(entry);
					}

					if (op instanceof ComparisonGt) {
						// lower bound: keep the weakest (smallest k)
						if (entry[1] == null || k < ((Number) ((Constant) entry[1]).getValue()).intValue())
							entry[1] = bin.getRight();
					} else {
						// upper bound: keep the strictest (smallest k)
						if (entry[2] == null || k < ((Number) ((Constant) entry[2]).getValue()).intValue())
							entry[2] = bin.getRight();
					}
					continue;
				}
			}
			other.add(c);
		}

		List<SymbolicExpression> result = new ArrayList<>(other);
		for (Object[] entry : varEntries) {
			SymbolicExpression var = (SymbolicExpression) entry[0];
			Constant lb = (Constant) entry[1];
			Constant ub = (Constant) entry[2];

			if (lb != null && ub != null) {
				int lbVal = ((Number) lb.getValue()).intValue();
				int ubVal = ((Number) ub.getValue()).intValue();
				if (lbVal >= ubVal)
					return null; // contradiction: var > lb && var < ub with lb >= ub
			}
			if (lb != null)
				result.add(new BinaryExpression(Untyped.INSTANCE, var, lb,
						ComparisonGt.INSTANCE, SyntheticLocation.INSTANCE));
			if (ub != null)
				result.add(new BinaryExpression(Untyped.INSTANCE, var, ub,
						ComparisonLt.INSTANCE, SyntheticLocation.INSTANCE));
		}
		return result;
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
	 * @return {@link it.unive.lisa.analysis.numeric.Sign#POS} if {@code var > 0}
	 *             is in the path condition, {@link it.unive.lisa.analysis.numeric.Sign#NEG}
	 *             if {@code var < 0} is, or {@link it.unive.lisa.analysis.numeric.Sign#TOP}
	 *             if no constraint was found
	 */
	it.unive.lisa.analysis.numeric.Sign getSignOf(SymbolicVariable var) {
		return extractSign(pathCondition, var);
	}

	private static it.unive.lisa.analysis.numeric.Sign extractSign(
			SymbolicExpression pc, SymbolicVariable var) {
		if (!(pc instanceof BinaryExpression))
			return it.unive.lisa.analysis.numeric.Sign.TOP;
		BinaryExpression bin = (BinaryExpression) pc;
		BinaryOperator op = bin.getOperator();
		if (op instanceof LogicalAnd) {
			it.unive.lisa.analysis.numeric.Sign l = extractSign(bin.getLeft(), var);
			if (!l.isTop())
				return l;
			return extractSign(bin.getRight(), var);
		}
		if (bin.getLeft().equals(var)) {
			if (op instanceof ComparisonGt)
				return it.unive.lisa.analysis.numeric.Sign.POS;
			if (op instanceof ComparisonLt)
				return it.unive.lisa.analysis.numeric.Sign.NEG;
		}
		return it.unive.lisa.analysis.numeric.Sign.TOP;
	}

	/**
	 * Computes the small-step semantics of {@code expression} at program point
	 * {@code pp}. This domain only updates its state through assignments; all
	 * other expressions leave the state unchanged.
	 *
	 * @param expression the expression whose semantics is computed
	 * @param pp         the program point where the expression is evaluated
	 * @param oracle     the semantic oracle for additional queries
	 *
	 * @return this domain unchanged
	 *
	 * @throws SemanticException if an error occurs during evaluation
	 */
	@Override
	public SymbolicAbstractDomain smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// nothing to do: this domain is non-relational and only updates state on assign()
		return this;
	}

	/**
	 * Tries to reduce {@code expr} to a {@link LinearCombination}. Returns
	 * {@link Optional#empty()} whenever the expression is non-linear (e.g.,
	 * {@code x * y}), in which case the caller should fall back to returning
	 * the expression unchanged. This method expects a fully evaluated
	 * expression tree whose leaves are either {@link Constant} or
	 * {@link Variable} (including {@link SymbolicVariable}); it must not be
	 * called with unresolved {@link Identifier} or {@link PushAny} nodes.
	 *
	 * @param expr the symbolic expression to reduce
	 *
	 * @return an {@link Optional} containing the {@link LinearCombination}, or
	 *             {@link Optional#empty()} if the expression is non-linear or
	 *             unsupported
	 */
	private Optional<LinearCombination> toLinearCombination(SymbolicExpression expr) {
		if (expr instanceof Constant) {
			Object val = ((Constant) expr).getValue();
			if (!(val instanceof Number))
				return Optional.empty();
			return Optional.of(LinearCombination.ofConstant(((Number) val).intValue()));
		}

		if (expr instanceof Variable)
			return Optional.of(LinearCombination.ofVariable((Variable) expr));

		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			Optional<LinearCombination> left = toLinearCombination(bin.getLeft());
			Optional<LinearCombination> right = toLinearCombination(bin.getRight());

			if (left.isEmpty() || right.isEmpty())
				return Optional.empty();

			BinaryOperator op = bin.getOperator();
			LinearCombination l = left.get();
			LinearCombination r = right.get();

			if (op == NumericNonOverflowingAdd.INSTANCE)
				return Optional.of(l.add(r));

			if (op == NumericNonOverflowingSub.INSTANCE)
				return Optional.of(l.sub(r));

			if (op == NumericNonOverflowingMul.INSTANCE) {
				if (l.isConstant())
					return Optional.of(r.scale(l.constantTerm));
				if (r.isConstant())
					return Optional.of(l.scale(r.constantTerm));
				// Non-linear (variable * variable): give up
				return Optional.empty();
			}

			if (op == NumericNonOverflowingDiv.INSTANCE) {
				if (r.isConstant() && r.constantTerm != 0)
					return Optional.of(l.divideBy(r.constantTerm));
				// Division by zero or by a non-constant: give up
				return Optional.empty();
			}
		}

		return Optional.empty();
	}

	/**
	 * Simplifies a fully evaluated symbolic expression to its canonical linear
	 * form via {@link #toLinearCombination(SymbolicExpression)}. Returns the
	 * expression unchanged when it is non-linear.
	 *
	 * @param expr the symbolic expression to simplify
	 *
	 * @return the canonical linear-form expression, or {@code expr} if it
	 *             cannot be reduced to a linear combination
	 */
	private SymbolicExpression simplify(SymbolicExpression expr) {
		return toLinearCombination(expr)
				.map(lc -> lc.toExpression(expr.getStaticType(), expr.getCodeLocation()))
				.orElse(expr);
	}

	/**
	 * Evaluates a symbolic expression against the current symbolic state:
	 * <ol>
	 * <li>{@link Identifier} references are resolved by looking up their
	 * symbolic representative in {@link #symbolicState}; if no information is
	 * available the identifier is returned as-is.
	 * <li>{@link PushAny} nodes produce a fresh {@link SymbolicVariable} whose
	 * name is the string representation of the call-site code location.
	 * <li>{@link Constant} nodes are returned unchanged.
	 * <li>{@link BinaryExpression} nodes are evaluated recursively and then
	 * simplified to canonical linear form via
	 * {@link #simplify(SymbolicExpression)}.
	 * </ol>
	 *
	 * @param expr the symbolic expression to evaluate
	 *
	 * @return the evaluated symbolic expression
	 */
	public SymbolicExpression eval(SymbolicExpression expr) {
		if (expr instanceof Identifier) {
			ExpressionSet set = this.symbolicState.getState((Identifier) expr);
			if (set == null || set.elements.isEmpty())
				return expr;
			return set.elements.iterator().next();
		}

		if (expr instanceof PushAny)
			return new SymbolicVariable(expr.getStaticType(),
					expr.getCodeLocation().toString(), expr.getCodeLocation());

		if (expr instanceof Constant)
			return expr;

		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			// Recursively evaluate children so all identifiers/PushAny are resolved
			SymbolicExpression left = eval(bin.getLeft());
			SymbolicExpression right = eval(bin.getRight());
			// Rebuild the tree with evaluated leaves, then simplify
			BinaryExpression evaluated = new BinaryExpression(
					bin.getStaticType(), left, right,
					bin.getOperator(), bin.getCodeLocation());
			return simplify(evaluated);
		}

		return expr;
	}

	/**
	 * Refines the symbolic state under the assumption that {@code expression}
	 * holds on the edge from {@code src} to {@code dest}. Currently returns
	 * this state unchanged (no constraint propagation is implemented).
	 *
	 * @param expression the boolean expression assumed to hold
	 * @param src        the source program point of the guarded edge
	 * @param dest       the destination program point of the guarded edge
	 * @param oracle     the semantic oracle for additional queries
	 *
	 * @return this domain unchanged
	 *
	 * @throws SemanticException if an error occurs during assumption
	 */
	@Override
	public SymbolicAbstractDomain assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * Returns {@code true} if this symbolic state holds a binding for
	 * {@code id}, i.e., the identifier is tracked in the functional map.
	 * Always returns {@code false} for top and bottom elements.
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
	public SymbolicAbstractDomain forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null || !symbolicState.function.containsKey(id))
			return this;
		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(symbolicState.function, true);
		newMap.remove(id);
		return new SymbolicAbstractDomain(this.pathCondition,
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
	public SymbolicAbstractDomain forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;
		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(symbolicState.function, true);
		newMap.keySet().removeIf(test);
		return new SymbolicAbstractDomain(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
	}

	/**
	 * Checks whether {@code expression} is satisfied in this symbolic state.
	 * Currently always returns {@link Satisfiability#UNKNOWN} as no constraint
	 * solving is implemented.
	 *
	 * @param expression the expression to check for satisfiability
	 * @param pp         the program point where the check is performed
	 * @param oracle     the semantic oracle for additional queries
	 *
	 * @return {@link Satisfiability#UNKNOWN}
	 *
	 * @throws SemanticException if an error occurs during the check
	 */
	@Override
	public Satisfiability satisfies(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return Satisfiability.UNKNOWN;
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
		return new StringRepresentation("Path condition: " + pathCondition.toString() + ", \n" + "Symbolic state: " + symbolicState.toString());
	}

	/**
	 * Pushes a new interprocedural scope identified by {@code token} onto the
	 * symbolic state. Each currently tracked identifier {@code id} is replaced
	 * by {@code id.pushScope(token)}, which wraps it in an
	 * {@link it.unive.lisa.symbolic.value.OutOfScopeIdentifier} so that the
	 * binding is preserved but hidden during the callee's execution. Identifiers
	 * for which {@code pushScope} returns {@code null} are dropped. On key
	 * collision the associated {@link ExpressionSet} values are joined via
	 * {@link ExpressionSet#lub}.
	 *
	 * @param token the scope token identifying the call site
	 *
	 * @return a new {@link SymbolicAbstractDomain} with all bindings scoped
	 *             under {@code token}
	 *
	 * @throws SemanticException if an error occurs while pushing the scope
	 */
	@Override
	public SymbolicAbstractDomain pushScope(ScopeToken token) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;

		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(null, false);
		for (Identifier id : symbolicState.getKeys()) {
			Identifier scoped = (Identifier) id.pushScope(token);
			if (scoped != null) {
				ExpressionSet val = symbolicState.getState(id);
				if (!newMap.containsKey(scoped))
					newMap.put(scoped, val);
				else
					newMap.put(scoped, val.lub(newMap.get(scoped)));
			}
		}

		return new SymbolicAbstractDomain(this.pathCondition,
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
	public SymbolicAbstractDomain popScope(ScopeToken token) throws SemanticException {
		if (isTop() || isBottom())
			return this;
		if (symbolicState.function == null)
			return this;

		Map<Identifier, ExpressionSet> newMap = symbolicState.mkNewFunction(null, false);
		for (Identifier id : symbolicState.getKeys()) {
			Identifier unscoped = (Identifier) id.popScope(token);
			if (unscoped != null) {
				ExpressionSet val = symbolicState.getState(id);
				if (!newMap.containsKey(unscoped))
					newMap.put(unscoped, val);
				else
					newMap.put(unscoped, val.lub(newMap.get(unscoped)));
			}
		}

		return new SymbolicAbstractDomain(this.pathCondition,
				new GenericMapLattice<>(symbolicState.lattice, newMap));
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
	public boolean lessOrEqual(SymbolicAbstractDomain other) throws SemanticException {
		if (isBottom() || other.isTop())
			return true;
		if (isTop() || other.isBottom())
			return false;
		return symbolicState.lessOrEqual(other.symbolicState);
	}

	/**
	 * Computes the least upper bound of this abstract element and {@code other}.
	 * The join is delegated to the underlying {@link GenericMapLattice}, which
	 * performs a pointwise join: for every identifier tracked in either
	 * element, the resulting symbolic expression set is the join of the two
	 * corresponding sets (missing entries are treated as top).
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
	public SymbolicAbstractDomain lub(SymbolicAbstractDomain other) throws SemanticException {
		if (isBottom() || other.isTop())
			return other;
		if (other.isBottom() || isTop())
			return this;
		SymbolicExpression mergedPC = Objects.equals(pathCondition, other.pathCondition)
				? pathCondition : TRUE;
		return new SymbolicAbstractDomain(mergedPC, symbolicState.lub(other.symbolicState));
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
	 * Returns the top element of this domain: a fresh instance whose symbolic
	 * state is top and whose path condition is {@code true}.
	 *
	 * @return the top element
	 */
	@Override
	public SymbolicAbstractDomain top() {
		return new SymbolicAbstractDomain(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}

	/**
	 * Returns the bottom element of this domain: a fresh instance whose
	 * symbolic state is bottom and whose path condition is {@code true}.
	 *
	 * @return the bottom element
	 */
	@Override
	public SymbolicAbstractDomain bottom() {
		return new SymbolicAbstractDomain(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).bottom());
	}

	/**
	 * Returns the hash code of this element, computed from its path condition
	 * and symbolic state.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return Objects.hash(pathCondition, symbolicState);
	}

	/**
	 * Returns {@code true} if {@code obj} is a {@link SymbolicAbstractDomain}
	 * with the same path condition and symbolic state as this element.
	 *
	 * @param obj the object to compare
	 *
	 * @return whether this element equals {@code obj}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolicAbstractDomain other = (SymbolicAbstractDomain) obj;
		return Objects.equals(pathCondition, other.pathCondition) && Objects.equals(symbolicState, other.symbolicState);
	}

	/**
	 * Returns the set of identifiers currently tracked in this symbolic state.
	 * Returns an empty set when the state is top, bottom, or the functional map
	 * is uninitialised.
	 *
	 * @return the set of tracked identifiers (never {@code null})
	 */
	Set<Identifier> getKeys() {
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
	SymbolicExpression getSymbolicExpression(Identifier id) {
		if (symbolicState.function == null || !symbolicState.function.containsKey(id))
			return null;
		ExpressionSet set = symbolicState.function.get(id);
		if (set == null || set.elements.isEmpty())
			return null;
		else if (set.size() > 1)
			throw new IllegalStateException("Multiple symbolic expressions for identifier " + id);
		return set.elements.iterator().next();
	}
}
