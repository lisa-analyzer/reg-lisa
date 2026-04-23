package it.unipr.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import it.unive.lisa.type.Type;
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
				newPathCondition = new BinaryExpression(Untyped.INSTANCE, this.pathCondition, constraint,
						LogicalAnd.INSTANCE, SyntheticLocation.INSTANCE);
		}

		return new SymbolicAbstractDomain(newPathCondition, cpy);
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
	 * Represents a linear combination of symbolic variables with integer
	 * coefficients plus an integer constant term:
	 * {@code c1*x1 + c2*x2 + ... + cn*xn + k}. Zero-coefficient entries are
	 * never stored in the coefficients map.
	 *
	 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
	 */
	private static final class LinearCombination {

		/**
		 * Maps each {@link Variable} to its non-zero integer coefficient.
		 */
		final Map<Variable, Integer> coefficients;

		/**
		 * The integer constant term {@code k} of the linear combination.
		 */
		final int constantTerm;

		/**
		 * Builds a linear combination with the given coefficients map and
		 * constant term.
		 *
		 * @param coefficients the map from variables to their coefficients
		 * @param constantTerm the integer constant term
		 */
		private LinearCombination(
				Map<Variable, Integer> coefficients,
				int constantTerm) {
			this.coefficients = coefficients;
			this.constantTerm = constantTerm;
		}

		/**
		 * Builds a linear combination representing the pure integer constant
		 * {@code k}.
		 *
		 * @param k the constant value
		 *
		 * @return a new {@link LinearCombination} with no variable terms and
		 *             constant term {@code k}
		 */
		static LinearCombination ofConstant(int k) {
			return new LinearCombination(new LinkedHashMap<>(), k);
		}

		/**
		 * Builds a linear combination representing the single variable
		 * {@code v} with coefficient {@code 1}.
		 *
		 * @param v the variable
		 *
		 * @return a new {@link LinearCombination} equal to {@code 1 * v}
		 */
		static LinearCombination ofVariable(Variable v) {
			Map<Variable, Integer> m = new LinkedHashMap<>();
			m.put(v, 1);
			return new LinearCombination(m, 0);
		}

		/**
		 * Yields {@code true} if this linear combination has no variable terms
		 * (i.e., it is a pure integer constant).
		 *
		 * @return whether this linear combination is a pure constant
		 */
		boolean isConstant() {
			return coefficients.isEmpty();
		}

		/**
		 * Returns a new {@link LinearCombination} equal to
		 * {@code this + other}.
		 *
		 * @param other the addend
		 *
		 * @return the sum of this combination and {@code other}
		 */
		LinearCombination add(LinearCombination other) {
			Map<Variable, Integer> merged = new LinkedHashMap<>(this.coefficients);
			for (Map.Entry<Variable, Integer> e : other.coefficients.entrySet())
				merged.merge(e.getKey(), e.getValue(), Integer::sum);
			merged.entrySet().removeIf(e -> e.getValue() == 0);
			return new LinearCombination(merged, this.constantTerm + other.constantTerm);
		}

		/**
		 * Returns a new {@link LinearCombination} equal to
		 * {@code this - other}.
		 *
		 * @param other the subtrahend
		 *
		 * @return the difference of this combination and {@code other}
		 */
		LinearCombination sub(LinearCombination other) {
			Map<Variable, Integer> merged = new LinkedHashMap<>(this.coefficients);
			for (Map.Entry<Variable, Integer> e : other.coefficients.entrySet())
				merged.merge(e.getKey(), -e.getValue(), Integer::sum);
			merged.entrySet().removeIf(e -> e.getValue() == 0);
			return new LinearCombination(merged, this.constantTerm - other.constantTerm);
		}

		/**
		 * Returns a new {@link LinearCombination} equal to
		 * {@code this * factor}.
		 *
		 * @param factor the integer scalar
		 *
		 * @return this combination scaled by {@code factor}
		 */
		LinearCombination scale(int factor) {
			if (factor == 0)
				return ofConstant(0);
			Map<Variable, Integer> scaled = new LinkedHashMap<>();
			for (Map.Entry<Variable, Integer> e : this.coefficients.entrySet())
				scaled.put(e.getKey(), e.getValue() * factor);
			return new LinearCombination(scaled, this.constantTerm * factor);
		}

		/**
		 * Returns a new {@link LinearCombination} equal to
		 * {@code this / divisor} using integer (truncating) division on each
		 * coefficient and on the constant term.
		 *
		 * @param divisor the non-zero integer divisor
		 *
		 * @return this combination divided by {@code divisor}
		 */
		LinearCombination divideBy(int divisor) {
			Map<Variable, Integer> divided = new LinkedHashMap<>();
			for (Map.Entry<Variable, Integer> e : this.coefficients.entrySet()) {
				int newCoeff = e.getValue() / divisor;
				if (newCoeff != 0)
					divided.put(e.getKey(), newCoeff);
			}
			return new LinearCombination(divided, this.constantTerm / divisor);
		}

		/**
		 * Reconstructs a canonical {@link SymbolicExpression} tree from this
		 * linear combination. Variable terms are emitted in alphabetical order
		 * by their string representation to ensure a deterministic output.
		 *
		 * <ul>
		 * <li>Coefficient {@code 1}: emit the variable directly.
		 * <li>Coefficient {@code > 1}: emit {@code coeff * var}.
		 * <li>Coefficient {@code -1}: emit as {@code result - var}.
		 * <li>Coefficient {@code < -1}: emit as
		 * {@code result - (|coeff| * var)}.
		 * <li>Positive constant term: appended via addition.
		 * <li>Negative constant term: appended via subtraction of its absolute
		 * value.
		 * </ul>
		 *
		 * @param type the static type of the resulting expression
		 * @param loc  the code location to attach to the synthesised nodes
		 *
		 * @return the canonical {@link SymbolicExpression} for this linear
		 *             combination
		 */
		SymbolicExpression toExpression(
				Type type,
				it.unive.lisa.program.cfg.CodeLocation loc) {
			List<Map.Entry<Variable, Integer>> entries = new ArrayList<>(coefficients.entrySet());

			// Deterministic ordering: sort by variable string representation
			entries.sort((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()));

			if (entries.isEmpty())
				return new Constant(Untyped.INSTANCE, constantTerm, SyntheticLocation.INSTANCE);

			// Build the first term
			SymbolicExpression result = buildTerm(entries.get(0).getKey(), entries.get(0).getValue(), type, loc);

			// Chain remaining variable terms
			for (int i = 1; i < entries.size(); i++) {
				Variable var = entries.get(i).getKey();
				int coeff = entries.get(i).getValue();
				if (coeff > 0) {
					SymbolicExpression term = buildTerm(var, coeff, type, loc);
					result = new BinaryExpression(type, result, term, NumericNonOverflowingAdd.INSTANCE, loc);
				} else {
					// Negative coefficient: subtract the positive-magnitude term
					SymbolicExpression term = buildTerm(var, -coeff, type, loc);
					result = new BinaryExpression(type, result, term, NumericNonOverflowingSub.INSTANCE, loc);
				}
			}

			// Append the constant term
			if (constantTerm > 0) {
				Constant k = new Constant(Untyped.INSTANCE, constantTerm, SyntheticLocation.INSTANCE);
				result = new BinaryExpression(type, result, k, NumericNonOverflowingAdd.INSTANCE, loc);
			} else if (constantTerm < 0) {
				Constant k = new Constant(Untyped.INSTANCE, -constantTerm, SyntheticLocation.INSTANCE);
				result = new BinaryExpression(type, result, k, NumericNonOverflowingSub.INSTANCE, loc);
			}

			return result;
		}

		/**
		 * Builds the expression tree for a single term {@code coeff * var},
		 * collapsing to just {@code var} when {@code coeff == 1}. The caller
		 * is responsible for passing the positive (absolute) value of the
		 * coefficient.
		 *
		 * @param var   the variable
		 * @param coeff the positive integer coefficient
		 * @param type  the static type of the resulting expression
		 * @param loc   the code location to attach to the synthesised node
		 *
		 * @return the expression {@code coeff * var}, or just {@code var} when
		 *             {@code coeff == 1}
		 */
		private SymbolicExpression buildTerm(
				Variable var,
				int coeff,
				Type type,
				it.unive.lisa.program.cfg.CodeLocation loc) {
			if (coeff == 1)
				return var;
			Constant c = new Constant(Untyped.INSTANCE, coeff, SyntheticLocation.INSTANCE);
			return new BinaryExpression(type, c, var, NumericNonOverflowingMul.INSTANCE, loc);
		}
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
			return set.elements.stream()
					.findFirst()
					.get();
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
		return new SymbolicAbstractDomain(this.pathCondition, symbolicState.lub(other.symbolicState));
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
	 * Returns the {@link ExpressionSet} associated with {@code id} in this
	 * symbolic state, or {@code null} if {@code id} is not tracked.
	 *
	 * @param id the identifier to look up
	 *
	 * @return the expression set for {@code id}, or {@code null}
	 */
	ExpressionSet getExpressionSet(Identifier id) {
		if (symbolicState.function == null || !symbolicState.function.containsKey(id))
			return null;
		return symbolicState.function.get(id);
	}
}
