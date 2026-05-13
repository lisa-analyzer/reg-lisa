package it.unipr.analysis;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.MathNumber;

/**
 * A non-relational value domain that approximates numeric values as
 * {@link DecimalInterval}s, supporting both integer and floating-point
 * constants (unlike LiSA's built-in {@code Interval} which only handles
 * {@code Integer} constants).
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class DecimalDomain
		implements
		BaseNonRelationalValueDomain<DecimalInterval> {

	@Override
	public DecimalInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		Object val = constant.getValue();
		double d;
		if (val instanceof Integer)
			d = ((Integer) val).doubleValue();
		else if (val instanceof Double)
			d = (Double) val;
		else if (val instanceof Float)
			d = ((Float) val).doubleValue();
		else if (val instanceof Long)
			d = ((Long) val).doubleValue();
		else
			return DecimalInterval.TOP;
		return new DecimalInterval(new MathNumber(d), new MathNumber(d));
	}

	@Override
	public DecimalInterval evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (pushAny instanceof PushIntv)
			return new DecimalInterval(new MathNumber(0.0), new MathNumber(1.0));
		return DecimalInterval.TOP;
	}

	@Override
	public DecimalInterval evalUnaryExpression(
			UnaryExpression expression,
			DecimalInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == NumericNegation.INSTANCE) {
			if (arg.isTop())
				return DecimalInterval.TOP;
			return arg.mul(DecimalInterval.MINUS_ONE);
		}
		return DecimalInterval.TOP;
	}

	@Override
	public DecimalInterval evalBinaryExpression(
			BinaryExpression expression,
			DecimalInterval left,
			DecimalInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator op = expression.getOperator();
		if (op instanceof NumericMax)
			return new DecimalInterval(left.getLow().max(right.getLow()), left.getHigh().max(right.getHigh()));
		if (!(op instanceof DivisionOperator) && (left.isTop() || right.isTop()))
			return DecimalInterval.TOP;
		if (op instanceof AdditionOperator)
			return left.plus(right);
		if (op instanceof SubtractionOperator)
			return left.diff(right);
		if (op instanceof MultiplicationOperator) {
			if (left.is(0.0) || right.is(0.0))
				return DecimalInterval.ZERO;
			return left.mul(right);
		}
		if (op instanceof DivisionOperator) {
			if (right.is(0.0))
				return DecimalInterval.BOTTOM;
			if (left.is(0.0))
				return DecimalInterval.ZERO;
			if (left.isTop() || right.isTop())
				return DecimalInterval.TOP;
			return left.div(right, false, false);
		}
		return DecimalInterval.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			DecimalInterval left,
			DecimalInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator op = expression.getOperator();
		if (op == ComparisonEq.INSTANCE) {
			DecimalInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			if (left.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (op == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(
					expression.withOperator(ComparisonLe.INSTANCE), right, left, pp, oracle);
		else if (op == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(
					expression.withOperator(ComparisonLt.INSTANCE), right, left, pp, oracle);
		else if (op == ComparisonLe.INSTANCE) {
			DecimalInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) <= 0);
			if (glb.isSingleton() && left.getHigh().compareTo(right.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (op == ComparisonLt.INSTANCE) {
			DecimalInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) < 0);
			return Satisfiability.UNKNOWN;
		} else if (op == ComparisonNe.INSTANCE) {
			DecimalInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ValueEnvironment<DecimalInterval> assumeBinaryExpression(
			ValueEnvironment<DecimalInterval> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		Identifier id;
		DecimalInterval eval;
		boolean rightIsExpr;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			eval = eval(environment, right, src, oracle);
			id = (Identifier) left;
			rightIsExpr = true;
		} else if (right instanceof Identifier) {
			eval = eval(environment, left, src, oracle);
			id = (Identifier) right;
			rightIsExpr = false;
		} else
			return environment;

		DecimalInterval starting = environment.getState(id);
		if (eval.isBottom() || starting.isBottom())
			return environment.bottom();

		DecimalInterval update = updateValue(expression.getOperator(), rightIsExpr, starting, eval);
		if (update == null)
			return environment;
		else if (update.isBottom())
			return environment.bottom();
		else
			return environment.putState(id, update);
	}

	/**
	 * Computes the refined value for an identifier given a comparison
	 * constraint. Returns {@code null} if no update is needed,
	 * {@link DecimalInterval#BOTTOM} if the constraint cannot hold, or a new
	 * interval otherwise.
	 *
	 * @param operator    the comparison operator
	 * @param rightIsExpr {@code true} if the form is {@code id op expr}
	 * @param idValue     current value of the identifier
	 * @param exprValue   value of the expression
	 *
	 * @return refined interval or {@code null}
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	public static DecimalInterval updateValue(
			BinaryOperator operator,
			boolean rightIsExpr,
			DecimalInterval idValue,
			DecimalInterval exprValue)
			throws SemanticException {
		boolean exprLowIsMinInf = exprValue.lowIsMinusInfinity();
		DecimalInterval low_inf = new DecimalInterval(exprValue.getLow(), MathNumber.PLUS_INFINITY);
		DecimalInterval lowp1_inf = new DecimalInterval(
				exprValue.getLow().add(MathNumber.ONE), MathNumber.PLUS_INFINITY);
		DecimalInterval inf_high = new DecimalInterval(MathNumber.MINUS_INFINITY, exprValue.getHigh());
		DecimalInterval inf_highm1 = new DecimalInterval(
				MathNumber.MINUS_INFINITY,
				exprValue.getHigh().subtract(MathNumber.ONE));

		DecimalInterval update = null;
		if (operator == ComparisonEq.INSTANCE)
			update = idValue.glb(exprValue);
		else if (operator == ComparisonGe.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? null : idValue.glb(low_inf);
			else
				update = idValue.glb(inf_high);
		else if (operator == ComparisonGt.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? null : idValue.glb(lowp1_inf);
			else
				update = exprLowIsMinInf ? exprValue : idValue.glb(inf_highm1);
		else if (operator == ComparisonLe.INSTANCE)
			if (rightIsExpr)
				update = idValue.glb(inf_high);
			else
				update = exprLowIsMinInf ? null : idValue.glb(low_inf);
		else if (operator == ComparisonLt.INSTANCE)
			if (rightIsExpr)
				update = exprLowIsMinInf ? exprValue : idValue.glb(inf_highm1);
			else
				update = exprLowIsMinInf ? null : idValue.glb(lowp1_inf);
		return update;
	}

	@Override
	public DecimalInterval top() {
		return DecimalInterval.TOP;
	}

	@Override
	public DecimalInterval bottom() {
		return DecimalInterval.BOTTOM;
	}
}
