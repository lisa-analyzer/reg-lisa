package it.unipr.analysis;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a linear combination of symbolic variables with integer
 * coefficients plus an integer constant term:
 * {@code c1*x1 + c2*x2 + ... + cn*xn + k}. Zero-coefficient entries are never
 * stored in the coefficients map.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
class LinearCombination {

	/**
	 * Maps each {@link Variable} to its non-zero integer coefficient.
	 */
	final Map<Variable, Float> coefficients;

	/**
	 * The integer constant term {@code k} of the linear combination.
	 */
	final float constantTerm;

	/**
	 * Builds a linear combination with the given coefficients map and constant
	 * term.
	 *
	 * @param coefficients the map from variables to their coefficients
	 * @param constantTerm the integer constant term
	 */
	private LinearCombination(
			Map<Variable, Float> coefficients,
			float constantTerm) {
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
	static LinearCombination ofConstant(float k) {
		return new LinearCombination(new LinkedHashMap<>(), k);
	}

	/**
	 * Builds a linear combination representing the single variable {@code v}
	 * with coefficient {@code 1}.
	 *
	 * @param v the variable
	 *
	 * @return a new {@link LinearCombination} equal to {@code 1 * v}
	 */
	static LinearCombination ofVariable(Variable v) {
		Map<Variable, Float> m = new LinkedHashMap<>();
		m.put(v, (float) 1);
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
	 * Returns a new {@link LinearCombination} equal to {@code this + other}.
	 *
	 * @param other the addend
	 *
	 * @return the sum of this combination and {@code other}
	 */
	LinearCombination add(LinearCombination other) {
		Map<Variable, Float> merged = new LinkedHashMap<>(this.coefficients);
		for (Map.Entry<Variable, Float> e : other.coefficients.entrySet())
			merged.merge(e.getKey(), e.getValue(), Float::sum);
		merged.entrySet().removeIf(e -> e.getValue() == 0);
		return new LinearCombination(merged, this.constantTerm + other.constantTerm);
	}

	/**
	 * Returns a new {@link LinearCombination} equal to {@code this - other}.
	 *
	 * @param other the subtrahend
	 *
	 * @return the difference of this combination and {@code other}
	 */
	LinearCombination sub(LinearCombination other) {
		Map<Variable, Float> merged = new LinkedHashMap<>(this.coefficients);
		for (Map.Entry<Variable, Float> e : other.coefficients.entrySet())
			merged.merge(e.getKey(), -e.getValue(), Float::sum);
		merged.entrySet().removeIf(e -> e.getValue() == 0);
		return new LinearCombination(merged, this.constantTerm - other.constantTerm);
	}

	/**
	 * Returns a new {@link LinearCombination} equal to {@code this * factor}.
	 *
	 * @param factor the integer scalar
	 *
	 * @return this combination scaled by {@code factor}
	 */
	LinearCombination scale(float factor) {
		if (factor == 0)
			return ofConstant(0);
		Map<Variable, Float> scaled = new LinkedHashMap<>();
		for (Map.Entry<Variable, Float> e : this.coefficients.entrySet())
			scaled.put(e.getKey(), e.getValue() * factor);
		return new LinearCombination(scaled, this.constantTerm * factor);
	}

	/**
	 * Returns a new {@link LinearCombination} equal to {@code this / divisor}
	 * using integer (truncating) division on each coefficient and on the
	 * constant term.
	 *
	 * @param divisor the non-zero integer divisor
	 *
	 * @return this combination divided by {@code divisor}
	 */
	LinearCombination divideBy(float divisor) {
		Map<Variable, Float> divided = new LinkedHashMap<>();
		for (Map.Entry<Variable, Float> e : this.coefficients.entrySet()) {
			float newCoeff = e.getValue() / divisor;
			if (newCoeff != 0)
				divided.put(e.getKey(), newCoeff);
		}
		return new LinearCombination(divided, this.constantTerm / divisor);
	}

	/**
	 * Reconstructs a canonical {@link SymbolicExpression} tree from this linear
	 * combination. Variable terms are emitted in alphabetical order by their
	 * string representation to ensure a deterministic output.
	 * <ul>
	 * <li>Coefficient {@code 1}: emit the variable directly.
	 * <li>Coefficient {@code > 1}: emit {@code coeff * var}.
	 * <li>Coefficient {@code -1}: emit as {@code result - var}.
	 * <li>Coefficient {@code < -1}: emit as {@code result - (|coeff| * var)}.
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
		List<Map.Entry<Variable, Float>> entries = new ArrayList<>(coefficients.entrySet());

		// Deterministic ordering: sort by variable string representation
		entries.sort((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()));

		if (entries.isEmpty())
			return new Constant(Untyped.INSTANCE, constantTerm, SyntheticLocation.INSTANCE);

		// Build the first term
		SymbolicExpression result = buildTerm(entries.get(0).getKey(), entries.get(0).getValue(), type, loc);

		// Chain remaining variable terms
		for (int i = 1; i < entries.size(); i++) {
			Variable var = entries.get(i).getKey();
			float coeff = entries.get(i).getValue();
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
	 * collapsing to just {@code var} when {@code coeff == 1}. The caller is
	 * responsible for passing the positive (absolute) value of the coefficient.
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
			float coeff,
			Type type,
			it.unive.lisa.program.cfg.CodeLocation loc) {
		if (coeff == 1)
			return var;
		Constant c = new Constant(Untyped.INSTANCE, coeff, SyntheticLocation.INSTANCE);
		return new BinaryExpression(type, c, var, NumericNonOverflowingMul.INSTANCE, loc);
	}
}
