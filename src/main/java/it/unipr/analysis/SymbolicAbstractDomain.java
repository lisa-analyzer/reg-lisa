package it.unipr.analysis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A symbolic abstract domain that tracks, for each program variable, a symbolic
 * expression representing its value in terms of fresh symbolic variables
 * introduced at {@code input()} call sites. The symbolic state is stored as a
 * functional lattice mapping each {@link Identifier} to an
 * {@link ExpressionSet} containing its symbolic representative. Arithmetic
 * expressions are simplified to a canonical linear-combination form via
 * {@link #simplify(SymbolicExpression)}.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class SymbolicAbstractDomain implements ValueDomain<SymbolicDomainLattice> {

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
			return SymbolicDomainLattice.TRUE;

		List<SymbolicExpression> result = simplifyComparisons(flat);
		if (result == null)
			return new Constant(Untyped.INSTANCE, false, SyntheticLocation.INSTANCE);
		if (result.isEmpty())
			return SymbolicDomainLattice.TRUE;

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
	 * the same variable. Returns {@code null} to signal a contradiction. For
	 * two lower-bound constraints on the same variable ({@code var > k1} and
	 * {@code var > k2}), the weaker one (smallest {@code k}) is kept. For two
	 * upper-bound constraints ({@code var < k1} and {@code var < k2}), the
	 * strictest one (smallest {@code k}) is kept.
	 *
	 * @param constraints the flat list of atomic constraints
	 *
	 * @return the simplified list, or {@code null} if a contradiction is found
	 */
	private List<SymbolicExpression> simplifyComparisons(List<SymbolicExpression> constraints) {
		// Each entry: [varExpr, lowerBoundConst-or-null,
		// upperBoundConst-or-null]
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
						if (((SymbolicExpression) e[0]).equals(var)) {
							entry = e;
							break;
						}
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
					return null; // contradiction: var > lb && var < ub with lb
									// >= ub
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
	 * @param state
	 * @param expr  the symbolic expression to evaluate
	 *
	 * @return the evaluated symbolic expression
	 */
	public SymbolicExpression eval(SymbolicDomainLattice state, SymbolicExpression expr) {
		if (expr instanceof Identifier) {
			ExpressionSet set = state.getSymbolicState().getState((Identifier) expr);
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
			// Recursively evaluate children so all identifiers/PushAny are
			// resolved
			SymbolicExpression left = eval(state, bin.getLeft());
			SymbolicExpression right = eval(state, bin.getRight());
			// Rebuild the tree with evaluated leaves, then simplify
			BinaryExpression evaluated = new BinaryExpression(
					bin.getStaticType(), left, right,
					bin.getOperator(), bin.getCodeLocation());
			return simplify(evaluated);
		}

		return expr;
	}

	@Override
	public SymbolicDomainLattice assign(SymbolicDomainLattice state, Identifier id, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		SymbolicExpression v = eval(state, expression);
		GenericMapLattice<Identifier, ExpressionSet> cpy = state.getSymbolicState().putState(id, new ExpressionSet(v));

		// If the RHS is a signed input, record the constraint in the path
		// condition.
		SymbolicExpression newPathCondition = state.getPathCondition();
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
						Untyped.INSTANCE, state.getPathCondition(), constraint,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE));
		}

		return new SymbolicDomainLattice(newPathCondition, cpy);
	}

	@Override
	public SymbolicDomainLattice smallStepSemantics(SymbolicDomainLattice state, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return state;
	}

	@Override
	public SymbolicDomainLattice assume(SymbolicDomainLattice state, ValueExpression expression, ProgramPoint src,
			ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		return state;
	}

	@Override
	public SymbolicDomainLattice makeLattice() {
		return new SymbolicDomainLattice();
	}
}
