package it.unipr.analysis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.numeric.SignLattice;
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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
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
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
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
					float k = ((Number) ((Constant) bin.getRight()).getValue()).floatValue();

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
						if (entry[1] == null || k < ((Number) ((Constant) entry[1]).getValue()).floatValue())
							entry[1] = bin.getRight();
					} else {
						// upper bound: keep the strictest (smallest k)
						if (entry[2] == null || k < ((Number) ((Constant) entry[2]).getValue()).floatValue())
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
				float lbVal = ((Number) lb.getValue()).floatValue();
				float ubVal = ((Number) ub.getValue()).floatValue();
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
			return Optional.of(LinearCombination.ofConstant(((Number) val).floatValue()));
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
	 * Simplifies a fully evaluated symbolic expression. First attempts to
	 * reduce it to a canonical linear form via
	 * {@link #toLinearCombination(SymbolicExpression)}. When that fails,
	 * applies relu-specific simplifications for {@code max(0, inner)}
	 * expressions:
	 * <ul>
	 * <li>if {@code inner} is a non-negative constant, returns
	 * {@code inner};</li>
	 * <li>if {@code inner} is a negative constant, returns {@code 0};</li>
	 * <li>if {@code inner} is itself a relu expression (always &ge; 0), strips
	 * the outer {@code max(0, ...)}, returning {@code inner};</li>
	 * <li>if {@code state} is provided and the path condition implies
	 * {@code inner &ge; 0}, returns {@code inner};</li>
	 * <li>otherwise rebuilds {@code max(0, simplified_inner)}.</li>
	 * </ul>
	 *
	 * @param expr  the symbolic expression to simplify
	 * @param state the current symbolic state used to query the path condition
	 *                  for sign information; may be {@code null}
	 *
	 * @return the simplified expression
	 */
	private SymbolicExpression simplify(SymbolicExpression expr, SymbolicDomainLattice state) {
		Optional<LinearCombination> lc = toLinearCombination(expr);
		if (lc.isPresent())
			return lc.get().toExpression(expr.getStaticType(), expr.getCodeLocation());

		// relu simplification: max(0, inner)
		SymbolicExpression inner = extractReluInner(expr);
		if (inner != null) {
			SymbolicExpression simplifiedInner = simplify(inner, state);

			// max(0, constant) → evaluate statically
			if (simplifiedInner instanceof Constant) {
				Object val = ((Constant) simplifiedInner).getValue();
				if (val instanceof Number) {
					if (((Number) val).doubleValue() >= 0.0)
						return simplifiedInner;
					return new Constant(expr.getStaticType(), 0, expr.getCodeLocation());
				}
			}

			// max(0, relu_expr) → relu_expr, since relu_expr is already >= 0
			if (extractReluInner(simplifiedInner) != null)
				return simplifiedInner;

			// max(0, e) → e when the path condition implies e >= 0
			if (state != null) {
				SignLattice sign = state.getSignOfExpr(simplifiedInner);
				if (sign.isPositive() || sign.isZero())
					return simplifiedInner;
			}

			// Rebuild with simplified inner if it changed
			if (simplifiedInner != inner) {
				BinaryExpression bin = (BinaryExpression) expr;
				SymbolicExpression newLeft = isZeroConstant(bin.getLeft()) ? bin.getLeft() : simplifiedInner;
				SymbolicExpression newRight = isZeroConstant(bin.getLeft()) ? simplifiedInner : bin.getRight();
				return new BinaryExpression(expr.getStaticType(), newLeft, newRight,
						NumericMax.INSTANCE, expr.getCodeLocation());
			}
		}

		// For any other binary expression, recursively simplify the children.
		// This propagates path-condition-based simplifications (e.g. stripping
		// max wrappers) through compound expressions such as 2.0 * max(0, e).
		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			SymbolicExpression newLeft = simplify(bin.getLeft(), state);
			SymbolicExpression newRight = simplify(bin.getRight(), state);
			if (newLeft != bin.getLeft() || newRight != bin.getRight()) {
				BinaryExpression rebuilt = new BinaryExpression(
						bin.getStaticType(), newLeft, newRight,
						bin.getOperator(), bin.getCodeLocation());
				// Retry on the rebuilt expression — now linear-combination
				// simplification may succeed where it previously failed.
				return simplify(rebuilt, state);
			}
		}

		return expr;
	}

	/**
	 * Returns {@code true} if {@code expr} is a numeric zero constant.
	 */
	private static boolean isZeroConstant(SymbolicExpression expr) {
		if (!(expr instanceof Constant))
			return false;
		Object val = ((Constant) expr).getValue();
		return val instanceof Number && ((Number) val).doubleValue() == 0.0;
	}

	/**
	 * If {@code expr} matches the relu pattern {@code max(0, inner)} or
	 * {@code max(inner, 0)}, returns {@code inner}; otherwise returns
	 * {@code null}.
	 */
	private static SymbolicExpression extractReluInner(SymbolicExpression expr) {
		if (!(expr instanceof BinaryExpression))
			return null;
		BinaryExpression bin = (BinaryExpression) expr;
		if (!(bin.getOperator() instanceof NumericMax))
			return null;
		if (isZeroConstant(bin.getLeft()))
			return bin.getRight();
		if (isZeroConstant(bin.getRight()))
			return bin.getLeft();
		return null;
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
	 * simplified to canonical form via
	 * {@link #simplify(SymbolicExpression, SymbolicDomainLattice)}.
	 * </ol>
	 *
	 * @param state the current symbolic domain state, used to resolve
	 *                  identifiers and apply path-condition-based
	 *                  simplifications
	 * @param expr  the symbolic expression to evaluate
	 *
	 * @return the evaluated and simplified symbolic expression
	 */
	public SymbolicExpression eval(SymbolicDomainLattice state, SymbolicExpression expr) {
		if (expr instanceof Identifier) {
			ExpressionSet set = state.getSymbolicState().getState((Identifier) expr);
			if (set == null || set.elements.isEmpty())
				return expr;
			return set.elements.iterator().next();
		}

		if (expr instanceof PushIntv)
			return new IntvSymbolicVariable(expr.getStaticType(),
					expr.getCodeLocation().toString(), expr.getCodeLocation());

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
			return simplify(evaluated, state);
		}

		return expr;
	}

	/**
	 * Handles an assignment {@code id := expression}. The right-hand side is
	 * evaluated via {@link #eval} to produce a symbolic expression, which is
	 * stored in the symbolic state under {@code id}. When the result is a typed
	 * symbolic variable ({@link PushPos}, {@link PushNeg}, or
	 * {@link PushIntv}), the corresponding range constraint is added to the
	 * path condition. When the result is a {@code max(0, inner)} ReLU
	 * expression, path-condition sign information is used to simplify it where
	 * possible.
	 */
	@Override
	public SymbolicDomainLattice assign(SymbolicDomainLattice state, Identifier id, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		SymbolicExpression v = eval(state, expression);
		SymbolicExpression newPathCondition = state.getPathCondition();

		// Relu simplification: if v is max(0, inner), try to simplify using
		// the sign of inner recorded in the path condition.
		SymbolicExpression reluInner = extractReluInner(v);
		if (reluInner != null) {
			SignLattice innerSign = state.getSignOfExpr(reluInner);
			if (innerSign.isPositive() || innerSign.isZero()) {
				// inner >= 0, so max(0, inner) = inner
				v = reluInner;
			} else if (innerSign.isNegative()) {
				// inner < 0, so max(0, inner) = 0
				v = new Constant(v.getStaticType(), 0, v.getCodeLocation());
			} else {
				// Sign unknown: add 'inner >= 0' to the path condition, then
				// immediately re-simplify v with the updated PC so that
				// max(0, inner) collapses to inner on the spot.
				Constant zero = new Constant(Untyped.INSTANCE, 0, SyntheticLocation.INSTANCE);
				SymbolicExpression geZero = new BinaryExpression(
						Untyped.INSTANCE, reluInner, zero,
						ComparisonGe.INSTANCE, SyntheticLocation.INSTANCE);
				newPathCondition = simplifyPathCondition(new BinaryExpression(
						Untyped.INSTANCE, newPathCondition, geZero,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE));
				// Re-simplify with the updated path condition: now
				// getSignOfExpr(inner) returns ZERO, so max(0, inner) → inner.
				SymbolicDomainLattice tempState = new SymbolicDomainLattice(
						newPathCondition, state.getSymbolicState());
				v = simplify(v, tempState);
			}
		}

		GenericMapLattice<Identifier, ExpressionSet> cpy = state.getSymbolicState().putState(id, new ExpressionSet(v));

		// If the RHS is a typed input, record the constraint(s) in the path
		// condition.
		if (v instanceof SymbolicVariable) {
			Constant zero = new Constant(Untyped.INSTANCE, 0, SyntheticLocation.INSTANCE);
			Constant one = new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
			SymbolicExpression constraint = null;
			if (expression instanceof PushPos)
				constraint = new BinaryExpression(Untyped.INSTANCE, v, zero, ComparisonGt.INSTANCE,
						SyntheticLocation.INSTANCE);
			else if (expression instanceof PushNeg)
				constraint = new BinaryExpression(Untyped.INSTANCE, v, zero, ComparisonLt.INSTANCE,
						SyntheticLocation.INSTANCE);
			else if (expression instanceof PushIntv) {
				// v >= 0
				SymbolicExpression geZero = new BinaryExpression(Untyped.INSTANCE, v, zero,
						ComparisonGe.INSTANCE, SyntheticLocation.INSTANCE);
				// v <= 1
				SymbolicExpression leOne = new BinaryExpression(Untyped.INSTANCE, v, one,
						ComparisonLe.INSTANCE, SyntheticLocation.INSTANCE);
				constraint = new BinaryExpression(Untyped.INSTANCE, geZero, leOne,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
			}
			if (constraint != null)
				newPathCondition = simplifyPathCondition(new BinaryExpression(
						Untyped.INSTANCE, newPathCondition, constraint,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE));
		}

		return new SymbolicDomainLattice(newPathCondition, cpy);
	}

	/**
	 * Advances the symbolic state past a non-assignment expression. The
	 * symbolic domain does not track expression evaluations directly, so the
	 * state is returned unchanged.
	 */
	@Override
	public SymbolicDomainLattice smallStepSemantics(SymbolicDomainLattice state, ValueExpression expression,
			ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		return state;
	}

	/**
	 * Assumes that {@code expression} holds. The symbolic domain does not
	 * perform path-splitting, so the state is returned unchanged; branch
	 * conditions are handled at the {@link CombinationDomain} level.
	 */
	@Override
	public SymbolicDomainLattice assume(SymbolicDomainLattice state, ValueExpression expression, ProgramPoint src,
			ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		return state;
	}

	/**
	 * Returns the initial (top) symbolic lattice element.
	 */
	@Override
	public SymbolicDomainLattice makeLattice() {
		return new SymbolicDomainLattice();
	}
}
