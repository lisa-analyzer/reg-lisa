package it.unipr.analysis;

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
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;
import java.util.function.Predicate;

public class CombinationDomainLattice<V extends Lattice<V>> implements ValueLattice<CombinationDomainLattice<V>> {

	/**
	 * Strategy for deriving an abstract value of type {@code V} from a symbolic
	 * expression. Implementations handle all expression shapes: {@link Constant},
	 * {@link SymbolicVariable}, {@link Identifier} (looked up in {@code env}),
	 * and {@link BinaryExpression} (evaluated recursively using the domain's
	 * arithmetic).
	 *
	 * @param <V> the abstract value type
	 */
	@FunctionalInterface
	public interface ExpressionEvaluator<V extends Lattice<V>> {

		/**
		 * Derives the abstract value for {@code expr}.
		 *
		 * @param expr the symbolic expression to evaluate
		 * @param env  the current abstract environment, used to resolve plain
		 *                 {@link Identifier} nodes
		 * @param sym  the symbolic domain state, used to resolve
		 *                 {@link SymbolicVariable} nodes via path-condition
		 *                 constraints; may be {@code null}
		 *
		 * @return the derived abstract value, or {@code env.lattice.top()} when
		 *             the expression cannot be evaluated precisely
		 */
		V evaluate(SymbolicExpression expr, ValueEnvironment<V> env, SymbolicDomainLattice sym);
	}

	/**
	 * The symbolic component, tracking exact linear combinations of
	 * {@link SymbolicVariable} instances for each program variable.
	 */
	private final SymbolicDomainLattice symbolic;

	/**
	 * The value environment component, recording the abstract value (e.g.,
	 * sign, interval) of each program variable. Kept consistent with the
	 * symbolic component via {@link #refineEnvFromSymbolic}.
	 */
	private final ValueEnvironment<V> env;

	/**
	 * Auxiliary value environment accumulated from {@code assume} calls. Unlike
	 * {@link #env}, this field is NOT reset to top by assignments; it is used
	 * exclusively by {@link #popScope} to reconstruct the full environment at
	 * the return node. Intentionally excluded from {@link #equals},
	 * {@link #hashCode}, and {@link #lessOrEqual}.
	 */
	private final ValueEnvironment<V> savedEnv;

	/**
	 * The expression evaluator strategy that derives values of type {@code V}
	 * from symbolic expressions. Excluded from {@link #equals} and
	 * {@link #hashCode}.
	 */
	private final ExpressionEvaluator<V> evaluator;

	/**
	 * Builds a {@link CombinationDomainLattice} whose symbolic and value
	 * components are both top.
	 *
	 * @param evaluator       the expression evaluator strategy
	 * @param latticeTemplate a representative instance used to seed empty
	 *                            {@link ValueEnvironment}s
	 */
	public CombinationDomainLattice(ExpressionEvaluator<V> evaluator, V latticeTemplate) {
		this(new SymbolicDomainLattice(),
				new ValueEnvironment<>(latticeTemplate),
				new ValueEnvironment<>(latticeTemplate),
				evaluator);
	}

	/**
	 * Builds a combination domain from symbolic and value environment
	 * components, with {@code savedEnv} initialised to top.
	 *
	 * @param symbolic  the symbolic component
	 * @param env       the value environment component
	 * @param evaluator the expression evaluator strategy
	 */
	public CombinationDomainLattice(SymbolicDomainLattice symbolic, ValueEnvironment<V> env,
			ExpressionEvaluator<V> evaluator) {
		this(symbolic, env, new ValueEnvironment<>(env.lattice), evaluator);
	}

	/**
	 * Builds a combination domain from all three components.
	 *
	 * @param symbolic  the symbolic component
	 * @param env       the value environment component
	 * @param savedEnv  the auxiliary environment accumulated from assume calls
	 *                      (used only at {@link #popScope})
	 * @param evaluator the expression evaluator strategy
	 */
	public CombinationDomainLattice(SymbolicDomainLattice symbolic, ValueEnvironment<V> env,
			ValueEnvironment<V> savedEnv, ExpressionEvaluator<V> evaluator) {
		this.symbolic = symbolic;
		this.env = env;
		this.savedEnv = savedEnv;
		this.evaluator = evaluator;
	}

	@Override
	public CombinationDomainLattice<V> store(Identifier target, Identifier source) throws SemanticException {
		return new CombinationDomainLattice<>(
				symbolic.store(target, source),
				env.store(target, source),
				savedEnv.store(target, source),
				evaluator);
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		return symbolic.knowsIdentifier(id) || env.knowsIdentifier(id);
	}

	@Override
	public CombinationDomainLattice<V> forgetIdentifier(Identifier id, ProgramPoint pp) throws SemanticException {
		return new CombinationDomainLattice<>(
				symbolic.forgetIdentifier(id, pp),
				env.forgetIdentifier(id, pp),
				savedEnv.forgetIdentifier(id, pp),
				evaluator);
	}

	@Override
	public CombinationDomainLattice<V> forgetIdentifiersIf(Predicate<Identifier> test, ProgramPoint pp)
			throws SemanticException {
		return new CombinationDomainLattice<>(
				symbolic.forgetIdentifiersIf(test, pp),
				env.forgetIdentifiersIf(test, pp),
				savedEnv.forgetIdentifiersIf(test, pp),
				evaluator);
	}

	@Override
	public CombinationDomainLattice<V> forgetIdentifiers(Iterable<Identifier> ids, ProgramPoint pp)
			throws SemanticException {
		return new CombinationDomainLattice<>(
				symbolic.forgetIdentifiers(ids, pp),
				env.forgetIdentifiers(ids, pp),
				savedEnv.forgetIdentifiers(ids, pp),
				evaluator);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		String sym = symbolic == null ? "<null>" : symbolic.representation().toString();
		String values = env == null ? "<null>" : env.toString();
		return new StringRepresentation("Symbolic:\n" + sym + "\nSigns:\n" + values);
	}

	@Override
	public CombinationDomainLattice<V> pushScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		return new CombinationDomainLattice<>(
				symbolic.pushScope(token, pp),
				env.pushScope(token, pp),
				savedEnv.pushScope(token, pp),
				evaluator);
	}

	@Override
	public CombinationDomainLattice<V> popScope(ScopeToken token, ProgramPoint pp) throws SemanticException {
		SymbolicDomainLattice newSymbolic = symbolic.popScope(token, pp);
		ValueEnvironment<V> newEnv = refineEnvFromSymbolic(symbolic, savedEnv, evaluator).popScope(token, pp);
		return new CombinationDomainLattice<>(newSymbolic, newEnv, evaluator);
	}

	@Override
	public boolean lessOrEqual(CombinationDomainLattice<V> other) throws SemanticException {
		if (other.isTop())
			return true;
		else if (isTop())
			return false;
		else if (isBottom())
			return true;
		else if (other.isBottom())
			return false;
		return symbolic.lessOrEqual(other.symbolic) && env.lessOrEqual(other.env);
	}

	/**
	 * Computes the least upper bound of this element and {@code other}.
	 * <p>
	 * <strong>Symbolic policy:</strong> {@code this.symbolic} is kept fixed
	 * (pre-loop / block-entry state). {@code other.symbolic} (the block body
	 * summary) is used only for value refinement via the
	 * {@link ExpressionEvaluator}, not stored in the result.
	 * <p>
	 * <strong>Value refinement:</strong> the value environment is joined
	 * pointwise and then refined using {@code other.symbolic}.
	 */
	@Override
	public CombinationDomainLattice<V> lub(CombinationDomainLattice<V> other) throws SemanticException {
		if (this == other || isBottom() || other.isTop() || equals(other))
			return other;
		if (other.isBottom() || isTop())
			return this;
		ValueEnvironment<V> lubEnv = env.lub(other.env);
		ValueEnvironment<V> refined = refineEnvFromSymbolic(other.symbolic, lubEnv, evaluator);
		ValueEnvironment<V> lubSaved = savedEnv.lub(other.savedEnv);
		return new CombinationDomainLattice<>(this.symbolic, refined, lubSaved, evaluator);
	}
	
	@Override
	public CombinationDomainLattice<V> widening(CombinationDomainLattice<V> other) throws SemanticException {
		if (this == other || isBottom() || other.isTop() || equals(other))
			return other;
		if (other.isBottom() || isTop())
			return this;
		ValueEnvironment<V> lubEnv = env.widening(other.env);
		ValueEnvironment<V> refined = refineEnvFromSymbolic(other.symbolic, lubEnv, evaluator);
		ValueEnvironment<V> lubSaved = savedEnv.widening(other.savedEnv);
		return new CombinationDomainLattice<>(this.symbolic, refined, lubSaved, evaluator);
	}

	@Override
	public CombinationDomainLattice<V> top() {
		return new CombinationDomainLattice<>(symbolic.top(), env.top(), evaluator);
	}

	@Override
	public CombinationDomainLattice<V> bottom() {
		return new CombinationDomainLattice<>(symbolic.bottom(), env.bottom(), evaluator);
	}

	@Override
	public boolean isTop() {
		return symbolic.isTop() && env.isTop();
	}

	@Override
	public boolean isBottom() {
		return symbolic.isBottom() || env.isBottom();
	}

	/**
	 * Re-derives the abstract value for each variable tracked in {@code sym}
	 * from its symbolic expression using {@code evaluator}, and overrides the
	 * binding in {@code base} only when the derived value is neither top nor
	 * bottom.
	 *
	 * @param <V>       the abstract value type
	 * @param sym       the symbolic state to use for refinement
	 * @param base      the value environment to refine
	 * @param evaluator the expression evaluator strategy
	 *
	 * @return the refined value environment
	 *
	 * @throws SemanticException if an error occurs during refinement
	 */
	public static <V extends Lattice<V>> ValueEnvironment<V> refineEnvFromSymbolic(
			SymbolicDomainLattice sym,
			ValueEnvironment<V> base,
			ExpressionEvaluator<V> evaluator)
			throws SemanticException {
		if (sym.isTop() || sym.isBottom())
			return base;

		ValueEnvironment<V> result = base;
		for (Identifier id : sym.getKeys()) {
			SymbolicExpression expr = sym.getSymbolicExpression(id);
			if (expr == null)
				continue;
			V derived = evaluator.evaluate(expr, base, sym);
			if (!derived.isTop() && !derived.isBottom())
				result = result.putState(id, derived);
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Sign-specific evaluator
	// -----------------------------------------------------------------------

	/**
	 * Returns an {@link ExpressionEvaluator} that derives {@link SignLattice}
	 * values from symbolic expressions:
	 * <ul>
	 * <li>{@link Constant} → ZERO / POS / NEG by numeric value;</li>
	 * <li>{@link SymbolicVariable} → sign from the path condition (TOP for
	 * plain {@code input()}, POS for {@code inputPos()}, NEG for
	 * {@code inputNeg()});</li>
	 * <li>{@link Identifier} → looked up in the value environment;</li>
	 * <li>{@link BinaryExpression} → evaluated recursively via
	 * {@link Sign#evalBinaryExpression}.</li>
	 * </ul>
	 *
	 * @return the sign evaluator
	 */
	public static ExpressionEvaluator<SignLattice> signEvaluator() {
		return CombinationDomainLattice::deriveSign;
	}

	private static SignLattice deriveSign(
			SymbolicExpression expr,
			ValueEnvironment<SignLattice> env,
			SymbolicDomainLattice sym) {
		if (expr instanceof Constant) {
			Object val = ((Constant) expr).getValue();
			if (val instanceof Number) {
				float v = ((Number) val).floatValue();
				return v > 0 ? SignLattice.POS : v < 0 ? SignLattice.NEG : SignLattice.ZERO;
			}
			return SignLattice.TOP;
		}

		if (expr instanceof IntvSymbolicVariable)
			return SignLattice.TOP;

		if (expr instanceof SymbolicVariable) {
			if (sym != null)
				return sym.getSignOf((SymbolicVariable) expr);
			return SignLattice.POS;
		}

		if (expr instanceof Identifier) {
			if (env != null)
				return env.getState((Identifier) expr);
			return SignLattice.TOP;
		}

		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			SignLattice left = deriveSign(bin.getLeft(), env, sym);
			SignLattice right = deriveSign(bin.getRight(), env, sym);
			if (bin.getOperator() instanceof NumericMax) {
				if (left.isZero()) {
					if (right.isNegative() || right.isZero())
						return SignLattice.ZERO;
					if (right.isPositive())
						return SignLattice.POS;
					return SignLattice.TOP;
				}
				if (right.isZero()) {
					if (left.isNegative() || left.isZero())
						return SignLattice.ZERO;
					if (left.isPositive())
						return SignLattice.POS;
					return SignLattice.TOP;
				}
				if (left.isPositive() && right.isPositive())
					return SignLattice.POS;
				return SignLattice.TOP;
			}
			// FIXME: LiSA Bug in multiplication
			if (bin.getOperator() instanceof MultiplicationOperator) {
				if ((left.isTop() && right.isNegative()) || (right.isTop() && left.isNegative()))
					return SignLattice.TOP;
				if ((left.isTop() && right.isPositive()) || (right.isTop() && left.isPositive()))
					return SignLattice.TOP;
			}
			return new Sign().evalBinaryExpression(bin, left, right, null, null);
		}

		return SignLattice.TOP;
	}

	// -----------------------------------------------------------------------
	// Interval evaluator
	// -----------------------------------------------------------------------

	/**
	 * Returns an {@link ExpressionEvaluator} that derives {@link DecimalInterval}
	 * values from symbolic expressions:
	 * <ul>
	 * <li>{@link Constant} → singleton interval {@code [n, n]}, supporting
	 * {@code Integer}, {@code Double}, {@code Float}, and {@code Long}
	 * constants;</li>
	 * <li>{@link SymbolicVariable} → interval derived from the path condition:
	 * {@code [1, +∞]} for {@code inputPos()}, {@code [-∞, -1]} for
	 * {@code inputNeg()}, {@code [0, 0]} for zero-constrained, TOP otherwise;</li>
	 * <li>{@link Identifier} → looked up in the value environment;</li>
	 * <li>{@link BinaryExpression} → evaluated recursively via
	 * {@link DecimalDomain#evalBinaryExpression}.</li>
	 * </ul>
	 *
	 * @return the decimal interval evaluator
	 */
	public static ExpressionEvaluator<DecimalInterval> intervalEvaluator() {
		return CombinationDomainLattice::deriveDecimalInterval;
	}

	private static DecimalInterval deriveDecimalInterval(
			SymbolicExpression expr,
			ValueEnvironment<DecimalInterval> env,
			SymbolicDomainLattice sym) {
		if (expr instanceof Constant)
			return new DecimalDomain().evalConstant((Constant) expr, null, null);

		if (expr instanceof IntvSymbolicVariable)
			return new DecimalInterval(0.0, 1.0);

		if (expr instanceof SymbolicVariable) {
			if (sym != null)
				return signToDecimalInterval(sym.getSignOf((SymbolicVariable) expr));
			return DecimalInterval.TOP;
		}

		if (expr instanceof Identifier) {
			if (env != null)
				return env.getState((Identifier) expr);
			return DecimalInterval.TOP;
		}

		if (expr instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expr;
			DecimalInterval left = deriveDecimalInterval(bin.getLeft(), env, sym);
			DecimalInterval right = deriveDecimalInterval(bin.getRight(), env, sym);
			return new DecimalDomain().evalBinaryExpression(bin, left, right, null, null);
		}

		return DecimalInterval.TOP;
	}

	private static DecimalInterval signToDecimalInterval(SignLattice sign) {
		if (sign.isBottom())
			return DecimalInterval.BOTTOM;
		if (sign.isPositive())
			return new DecimalInterval(new MathNumber(1.0), MathNumber.PLUS_INFINITY);
		if (sign.isNegative())
			return new DecimalInterval(MathNumber.MINUS_INFINITY, new MathNumber(-1.0));
		if (sign.isZero())
			return DecimalInterval.ZERO;
		return DecimalInterval.TOP;
	}

	// -----------------------------------------------------------------------
	// Accessors
	// -----------------------------------------------------------------------

	public SymbolicDomainLattice getSymbolic() {
		return symbolic;
	}

	public ValueEnvironment<V> getEnv() {
		return env;
	}

	public ValueEnvironment<V> getSavedEnv() {
		return savedEnv;
	}

	public ExpressionEvaluator<V> getEvaluator() {
		return evaluator;
	}

	@Override
	public int hashCode() {
		return Objects.hash(env, symbolic);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CombinationDomainLattice<?> other = (CombinationDomainLattice<?>) obj;
		return Objects.equals(env, other.env)
				&& Objects.equals(symbolic, other.symbolic);
	}
}
