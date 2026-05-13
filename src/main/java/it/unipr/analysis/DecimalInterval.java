package it.unipr.analysis;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;

/**
 * An interval with decimal (floating-point) bounds. Unlike
 * {@link it.unive.lisa.util.numeric.IntInterval}, bounds are never rounded to
 * integers, making this suitable for analyses that involve non-integer
 * constants.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class DecimalInterval
		implements
		Comparable<DecimalInterval>,
		BaseLattice<DecimalInterval> {

	/**
	 * The interval {@code [-Inf, +Inf]}.
	 */
	public static final DecimalInterval INFINITY = new DecimalInterval();

	/**
	 * The interval {@code [0.0, 0.0]}.
	 */
	public static final DecimalInterval ZERO = new DecimalInterval(0.0, 0.0);

	/**
	 * The interval {@code [1.0, 1.0]}.
	 */
	public static final DecimalInterval ONE = new DecimalInterval(1.0, 1.0);

	/**
	 * The interval {@code [-1.0, -1.0]}.
	 */
	public static final DecimalInterval MINUS_ONE = new DecimalInterval(-1.0, -1.0);

	/**
	 * The interval {@code [NaN, NaN]}, denoting undefined results of
	 * computations.
	 */
	public static final DecimalInterval NaN = new DecimalInterval(MathNumber.NaN, MathNumber.NaN);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final DecimalInterval TOP = INFINITY;

	/**
	 * The abstract bottom element.
	 */
	public static final DecimalInterval BOTTOM = new DecimalInterval((Double) null, null);

	private final MathNumber low;
	private final MathNumber high;

	private DecimalInterval() {
		this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
	}

	/**
	 * Builds a new interval from two double bounds. Order of the bounds is
	 * adjusted if needed.
	 *
	 * @param low  the lower bound
	 * @param high the upper bound
	 */
	public DecimalInterval(
			double low,
			double high) {
		this(new MathNumber(low), new MathNumber(high));
	}

	/**
	 * Builds a new interval from nullable Double bounds. If both are
	 * {@code null}, the bottom element is created.
	 *
	 * @param low  the lower bound ({@code null} → -inf)
	 * @param high the upper bound ({@code null} → +inf)
	 */
	public DecimalInterval(
			Double low,
			Double high) {
		this(handleNulls(low, high, true), handleNulls(low, high, false));
	}

	private static MathNumber handleNulls(
			Double low,
			Double high,
			boolean isLowBound) {
		if (low == null && high == null)
			return null;
		if (isLowBound)
			return low == null ? MathNumber.MINUS_INFINITY : new MathNumber(low);
		else
			return high == null ? MathNumber.PLUS_INFINITY : new MathNumber(high);
	}

	/**
	 * Builds a new interval from two {@link MathNumber} bounds. Order of the
	 * bounds is adjusted if needed. If both are {@code null}, the bottom
	 * element is created.
	 *
	 * @param low  the lower bound
	 * @param high the upper bound
	 */
	public DecimalInterval(
			MathNumber low,
			MathNumber high) {
		if (low == null && high == null) {
			this.low = null;
			this.high = null;
		} else {
			Objects.requireNonNull(low, "Low bound must not be null");
			Objects.requireNonNull(high, "High bound must not be null");
			if (low.isNaN() || high.isNaN()) {
				this.low = MathNumber.NaN;
				this.high = MathNumber.NaN;
			} else if (low.compareTo(high) <= 0) {
				this.low = low;
				this.high = high;
			} else {
				this.low = high;
				this.high = low;
			}
		}
	}

	/**
	 * Yields the upper bound of this interval.
	 *
	 * @return the upper bound
	 */
	public MathNumber getHigh() {
		return high;
	}

	/**
	 * Yields the lower bound of this interval.
	 *
	 * @return the lower bound
	 */
	public MathNumber getLow() {
		return low;
	}

	/**
	 * Yields {@code true} if the lower bound is minus infinity.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean lowIsMinusInfinity() {
		return !isBottom() && low.isMinusInfinity();
	}

	/**
	 * Yields {@code true} if the upper bound is plus infinity.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean highIsPlusInfinity() {
		return !isBottom() && high.isPlusInfinity();
	}

	/**
	 * Yields {@code true} if at least one bound is infinite.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean isInfinite() {
		return !isBottom() && (this == INFINITY || highIsPlusInfinity() || lowIsMinusInfinity());
	}

	/**
	 * Yields {@code true} if both bounds are finite.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean isFinite() {
		return !isBottom() && !isInfinite();
	}

	/**
	 * Yields {@code true} if this is the exact infinity interval
	 * {@code [-Inf, +Inf]}.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean isInfinity() {
		return this == INFINITY;
	}

	/**
	 * Yields {@code true} if both bounds are equal.
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean isSingleton() {
		return isFinite() && low.equals(high);
	}

	/**
	 * Yields {@code true} if this is a singleton containing only {@code n}.
	 *
	 * @param n the value to test
	 *
	 * @return {@code true} if that condition holds
	 */
	public boolean is(
			double n) {
		return !isBottom() && isSingleton() && low.equals(new MathNumber(n));
	}

	private static DecimalInterval cache(
			DecimalInterval i) {
		if (i.isBottom() || i.isTop())
			return i;
		if (i.is(0.0))
			return ZERO;
		if (i.is(1.0))
			return ONE;
		if (i.is(-1.0))
			return MINUS_ONE;
		return i;
	}

	/**
	 * Performs interval addition.
	 *
	 * @param other the other interval
	 *
	 * @return {@code this + other}
	 */
	public DecimalInterval plus(
			DecimalInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (isInfinity() || other.isInfinity())
			return INFINITY;
		return cache(new DecimalInterval(low.add(other.low), high.add(other.high)));
	}

	/**
	 * Performs interval subtraction.
	 *
	 * @param other the other interval
	 *
	 * @return {@code this - other}
	 */
	public DecimalInterval diff(
			DecimalInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (isInfinity() || other.isInfinity())
			return INFINITY;
		return cache(new DecimalInterval(low.subtract(other.high), high.subtract(other.low)));
	}

	private static MathNumber min(
			MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");
		MathNumber min = nums[0];
		for (int i = 1; i < nums.length; i++)
			min = min.min(nums[i]);
		return min;
	}

	private static MathNumber max(
			MathNumber... nums) {
		if (nums.length == 0)
			throw new IllegalArgumentException("No numbers provided");
		MathNumber max = nums[0];
		for (int i = 1; i < nums.length; i++)
			max = max.max(nums[i]);
		return max;
	}

	/**
	 * Performs interval multiplication.
	 *
	 * @param other the other interval
	 *
	 * @return {@code this * other}
	 */
	public DecimalInterval mul(
			DecimalInterval other) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (is(0.0) || other.is(0.0))
			return ZERO;
		if (isInfinity() || other.isInfinity())
			return INFINITY;
		if (low.compareTo(MathNumber.ZERO) >= 0 && other.low.compareTo(MathNumber.ZERO) >= 0)
			return cache(new DecimalInterval(low.multiply(other.low), high.multiply(other.high)));
		MathNumber ll = low.multiply(other.low);
		MathNumber lh = low.multiply(other.high);
		MathNumber hl = high.multiply(other.low);
		MathNumber hh = high.multiply(other.high);
		return cache(new DecimalInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
	}

	/**
	 * Performs interval division.
	 *
	 * @param other       the other interval
	 * @param ignoreZero  if {@code true}, ignore the fact that {@code other}
	 *                        might contain zero
	 * @param errorOnZero if {@code true}, throw when {@code other} contains
	 *                        zero
	 *
	 * @return {@code this / other}
	 */
	public DecimalInterval div(
			DecimalInterval other,
			boolean ignoreZero,
			boolean errorOnZero) {
		if (isBottom() || other.isBottom())
			return BOTTOM;
		if (errorOnZero && (other.is(0.0) || other.includes(ZERO)))
			throw new ArithmeticException("DecimalInterval divide by zero");
		if (is(0.0))
			return ZERO;
		if (other.is(0.0))
			return TOP;
		if (!other.includes(ZERO))
			return mul(new DecimalInterval(MathNumber.ONE.divide(other.high), MathNumber.ONE.divide(other.low)));
		else if (other.high.isZero())
			return mul(new DecimalInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
		else if (other.low.isZero())
			return mul(new DecimalInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));
		else if (ignoreZero)
			return mul(new DecimalInterval(MathNumber.ONE.divide(other.low), MathNumber.ONE.divide(other.high)));
		else {
			DecimalInterval lower = mul(
					new DecimalInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
			DecimalInterval higher = mul(
					new DecimalInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));
			if (lower.includes(higher))
				return lower;
			else if (higher.includes(lower))
				return higher;
			else
				return cache(new DecimalInterval(
						lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low,
						lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
		}
	}

	/**
	 * Yields {@code true} if this interval includes {@code other}.
	 *
	 * @param other the other interval
	 *
	 * @return {@code true} if included
	 */
	public boolean includes(
			DecimalInterval other) {
		if (isBottom() || other.isBottom())
			return false;
		return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
	}

	/**
	 * Yields {@code true} if this interval intersects {@code other}.
	 *
	 * @param other the other interval
	 *
	 * @return {@code true} if they intersect
	 */
	public boolean intersects(
			DecimalInterval other) {
		if (isBottom() || other.isBottom())
			return false;
		return includes(other)
				|| other.includes(this)
				|| (high.compareTo(other.low) >= 0 && high.compareTo(other.high) <= 0)
				|| (other.high.compareTo(low) >= 0 && other.high.compareTo(high) <= 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((high == null) ? 0 : high.hashCode());
		result = prime * result + ((low == null) ? 0 : low.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DecimalInterval other = (DecimalInterval) obj;
		if (high == null) {
			if (other.high != null)
				return false;
		} else if (!high.equals(other.high))
			return false;
		if (low == null) {
			if (other.low != null)
				return false;
		} else if (!low.equals(other.low))
			return false;
		return true;
	}

	@Override
	public int compareTo(
			DecimalInterval o) {
		if (isBottom())
			return o.isBottom() ? 0 : -1;
		if (isTop())
			return o.isTop() ? 0 : 1;
		if (o.isBottom())
			return 1;
		if (o.isTop())
			return -1;
		int cmp;
		if ((cmp = low.compareTo(o.low)) != 0)
			return cmp;
		return high.compareTo(o.high);
	}

	@Override
	public DecimalInterval top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return lowIsMinusInfinity() && highIsPlusInfinity();
	}

	@Override
	public DecimalInterval bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return low == null && high == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		return new StringRepresentation("[" + low + ", " + high + "]");
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public DecimalInterval lubAux(
			DecimalInterval other)
			throws SemanticException {
		MathNumber newLow = getLow().min(other.getLow());
		MathNumber newHigh = getHigh().max(other.getHigh());
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new DecimalInterval(newLow, newHigh);
	}

	@Override
	public DecimalInterval glbAux(
			DecimalInterval other) {
		MathNumber newLow = getLow().max(other.getLow());
		MathNumber newHigh = getHigh().min(other.getHigh());
		if (newLow.compareTo(newHigh) > 0)
			return bottom();
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new DecimalInterval(newLow, newHigh);
	}

	@Override
	public DecimalInterval wideningAux(
			DecimalInterval other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		if (other.getHigh().compareTo(getHigh()) > 0)
			newHigh = MathNumber.PLUS_INFINITY;
		else
			newHigh = getHigh();
		if (other.getLow().compareTo(getLow()) < 0)
			newLow = MathNumber.MINUS_INFINITY;
		else
			newLow = getLow();
		return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new DecimalInterval(newLow, newHigh);
	}

	@Override
	public DecimalInterval narrowingAux(
			DecimalInterval other)
			throws SemanticException {
		MathNumber newLow = getLow().isInfinite() ? other.getLow() : getLow();
		MathNumber newHigh = getHigh().isInfinite() ? other.getHigh() : getHigh();
		return new DecimalInterval(newLow, newHigh);
	}

	@Override
	public boolean lessOrEqualAux(
			DecimalInterval other)
			throws SemanticException {
		return other.includes(this);
	}
}
