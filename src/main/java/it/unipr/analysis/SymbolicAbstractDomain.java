package it.unipr.analysis;

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
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class SymbolicAbstractDomain implements ValueDomain<SymbolicAbstractDomain> {

	private static final Constant TRUE = new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE);

	/**
	 * The top abstract element.
	 */
//	private static final SymbolicAbstractDomain TOP = new SymbolicAbstractDomain();
	
	/**
	 * The bottom abstract element.
	 */
//	private static final SymbolicAbstractDomain BOTTOM = new SymbolicAbstractDomain(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
//			new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).bottom());
//	
	
	/**
	 * The path condition.
	 */
	private final SymbolicExpression pathCondition;
	
	/**
	 * The symbolic state.
	 */
	private final GenericMapLattice<Identifier, ExpressionSet> symbolicState;
	
	
	public SymbolicAbstractDomain() {
		this(TRUE, new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}

	private SymbolicAbstractDomain(SymbolicExpression pathCondition,
			GenericMapLattice<Identifier, ExpressionSet> symbolicState) {
		this.pathCondition = pathCondition;
		this.symbolicState = symbolicState;
	}

	@Override
	public SymbolicAbstractDomain assign(Identifier id, ValueExpression expression, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		SymbolicExpression v = eval(expression);
		GenericMapLattice<Identifier, ExpressionSet> cpy = this.symbolicState.putState(id, new ExpressionSet(v));
		return new SymbolicAbstractDomain(this.pathCondition, cpy);
	}

	@Override
	public SymbolicAbstractDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// nothing to do: this domain is non-relational and only updates state on assign()
		return this;
	}

	/**
	 * Represents a linear combination of symbolic variables and an integer
	 * constant: c1*x1 + c2*x2 + ... + cn*xn + k.
	 *
	 * <p>Zero-coefficient entries are never stored in the map.
	 */
	private static final class LinearCombination {

		/** Maps each variable to its non-zero integer coefficient. */
		final Map<Variable, Integer> coefficients;

		/** The integer constant term. */
		final int constantTerm;

		private LinearCombination(Map<Variable, Integer> coefficients, int constantTerm) {
			this.coefficients = coefficients;
			this.constantTerm = constantTerm;
		}

		static LinearCombination ofConstant(int k) {
			return new LinearCombination(new LinkedHashMap<>(), k);
		}

		static LinearCombination ofVariable(Variable v) {
			Map<Variable, Integer> m = new LinkedHashMap<>();
			m.put(v, 1);
			return new LinearCombination(m, 0);
		}

		/** True when there are no variable terms (pure constant). */
		boolean isConstant() {
			return coefficients.isEmpty();
		}

		/** Returns a new LinearCombination equal to {@code this + other}. */
		LinearCombination add(LinearCombination other) {
			Map<Variable, Integer> merged = new LinkedHashMap<>(this.coefficients);
			for (Map.Entry<Variable, Integer> e : other.coefficients.entrySet())
				merged.merge(e.getKey(), e.getValue(), Integer::sum);
			merged.entrySet().removeIf(e -> e.getValue() == 0);
			return new LinearCombination(merged, this.constantTerm + other.constantTerm);
		}

		/** Returns a new LinearCombination equal to {@code this - other}. */
		LinearCombination sub(LinearCombination other) {
			Map<Variable, Integer> merged = new LinkedHashMap<>(this.coefficients);
			for (Map.Entry<Variable, Integer> e : other.coefficients.entrySet())
				merged.merge(e.getKey(), -e.getValue(), Integer::sum);
			merged.entrySet().removeIf(e -> e.getValue() == 0);
			return new LinearCombination(merged, this.constantTerm - other.constantTerm);
		}

		/** Returns a new LinearCombination equal to {@code this * factor}. */
		LinearCombination scale(int factor) {
			if (factor == 0)
				return ofConstant(0);
			Map<Variable, Integer> scaled = new LinkedHashMap<>();
			for (Map.Entry<Variable, Integer> e : this.coefficients.entrySet())
				scaled.put(e.getKey(), e.getValue() * factor);
			return new LinearCombination(scaled, this.constantTerm * factor);
		}

		/**
		 * Returns a new LinearCombination equal to {@code this / divisor}
		 * using integer (truncating) division on each coefficient.
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
		 * linear combination.
		 *
		 * <ul>
		 * <li>Coefficient 1: emit the variable directly (no multiplication).
		 * <li>Coefficient &gt; 1: emit {@code coeff * var}.
		 * <li>Coefficient -1: emit as subtraction {@code result - var}.
		 * <li>Coefficient &lt; -1: emit as subtraction {@code result - (|coeff| * var)}.
		 * <li>Constant term appended last via addition (positive) or
		 *     subtraction (negative).
		 * </ul>
		 */
		SymbolicExpression toExpression(Type type, it.unive.lisa.program.cfg.CodeLocation loc) {
			List<Map.Entry<Variable, Integer>> entries = new ArrayList<>(coefficients.entrySet());

			// Ensure deterministic ordering of variable terms: sort by variable string
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
		 * Builds a single term {@code coeff * var}, collapsing to just
		 * {@code var} when {@code coeff == 1}.
		 * Caller is responsible for passing a positive (absolute) coefficient.
		 */
		private SymbolicExpression buildTerm(Variable var, int coeff,
				Type type, it.unive.lisa.program.cfg.CodeLocation loc) {
			if (coeff == 1)
				return var;
			Constant c = new Constant(Untyped.INSTANCE, coeff, SyntheticLocation.INSTANCE);
			return new BinaryExpression(type, c, var, NumericNonOverflowingMul.INSTANCE, loc);
		}
	}

	/**
	 * Tries to reduce {@code expr} to a {@link LinearCombination}.
	 *
	 * <p>Returns {@link Optional#empty()} for non-linear sub-expressions
	 * (e.g. {@code x * y}), in which case the caller falls back to returning
	 * the expression unchanged.
	 *
	 * <p>This method expects a fully evaluated expression tree whose leaves are
	 * either {@link Constant} or {@link Variable} (including
	 * {@link SymbolicVariable}). It must not be called with unresolved
	 * {@link Identifier} or {@link PushAny} nodes.
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
	 * form. Falls back to returning the expression unchanged when it is
	 * non-linear.
	 */
	private SymbolicExpression simplify(SymbolicExpression expr) {
		return toLinearCombination(expr)
				.map(lc -> lc.toExpression(expr.getStaticType(), expr.getCodeLocation()))
				.orElse(expr);
	}

	/**
	 * Evaluates a symbolic expression by:
	 * <ol>
	 * <li>Resolving {@link Identifier} references from the symbolic state.
	 * <li>Creating fresh {@link SymbolicVariable}s for {@link PushAny} inputs.
	 * <li>Recursively evaluating {@link BinaryExpression} children, then
	 *     simplifying the result to canonical linear form via
	 *     {@link #simplify(SymbolicExpression)}.
	 * </ol>
	 */
	public SymbolicExpression eval(SymbolicExpression expr) {
		if (expr instanceof Identifier) {
			ExpressionSet set = this.symbolicState.getState((Identifier) expr);
			if (set == null || set.elements.isEmpty())
				return expr;
			// ExpressionSet.elements may be an unordered collection; pick a deterministic
			// representative by ordering by the expression's string representation.
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

	@Override
	public SymbolicAbstractDomain assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public SymbolicAbstractDomain forgetIdentifier(Identifier id) throws SemanticException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SymbolicAbstractDomain forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return Satisfiability.UNKNOWN;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(this.symbolicState.toString());
	}

	@Override
	public SymbolicAbstractDomain pushScope(ScopeToken token) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicAbstractDomain popScope(ScopeToken token) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean lessOrEqual(SymbolicAbstractDomain other) throws SemanticException {
		return true;
	}

	@Override
	public SymbolicAbstractDomain lub(SymbolicAbstractDomain other) throws SemanticException {
		return null;
	}

	@Override
	public boolean isTop() {
		return this.symbolicState.isTop();
	}
	
	@Override
	public boolean isBottom() {
		return this.symbolicState.isBottom();
	}
	
	@Override
	public SymbolicAbstractDomain top() {
		return new SymbolicAbstractDomain(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}

	@Override
	public SymbolicAbstractDomain bottom() {
		return new SymbolicAbstractDomain(new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE),
				new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).bottom());
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
		SymbolicAbstractDomain other = (SymbolicAbstractDomain) obj;
		return Objects.equals(pathCondition, other.pathCondition) && Objects.equals(symbolicState, other.symbolicState);
	}
}
