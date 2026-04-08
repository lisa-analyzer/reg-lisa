package it.unipr.analysis;

import java.util.Objects;
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
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class SymbolicAbstractDomain implements ValueDomain<SymbolicAbstractDomain> {
	
	private static final Constant TRUE = new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE);
	
	private final SymbolicExpression pathCondition;
	private final GenericMapLattice<Identifier, ExpressionSet> symbolicState;
	
	public SymbolicAbstractDomain() {
		this(TRUE, new GenericMapLattice<Identifier, ExpressionSet>(new ExpressionSet()).top());
	}
	
	private SymbolicAbstractDomain(SymbolicExpression pathCondition, GenericMapLattice<Identifier, ExpressionSet> symbolicState) {
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
		return this;
	}
	
	
	public SymbolicExpression eval(SymbolicExpression expr) {
		if (expr instanceof Identifier)
			return this.symbolicState.getState((Identifier) expr).elements.stream().findAny().get();
		else if (expr instanceof PushAny) {
			// TODO: handle push any (e.g., generate a new symbolic variable)
			return expr;
		} else if (expr instanceof Constant)
			return expr;
		else if (expr instanceof BinaryExpression) {

			BinaryExpression bin = (BinaryExpression) expr;
			SymbolicExpression left = eval(bin.getLeft());
			SymbolicExpression right = eval(bin.getRight());
			
			if (left instanceof Constant && right instanceof Constant) {
				BinaryOperator op = bin.getOperator();
				Integer leftConst = (Integer) ((Constant) left).getValue();
				Integer rightConst = (Integer) ((Constant) right).getValue();
				
				if (op == NumericNonOverflowingAdd.INSTANCE)
					return new Constant(Untyped.INSTANCE, leftConst + rightConst, SyntheticLocation.INSTANCE);
				else if (op == NumericNonOverflowingSub.INSTANCE)
					return new Constant(Untyped.INSTANCE, leftConst - rightConst, SyntheticLocation.INSTANCE);
				else if (op == NumericNonOverflowingMul.INSTANCE)
					return new Constant(Untyped.INSTANCE, leftConst * rightConst, SyntheticLocation.INSTANCE);
				else if (op == NumericNonOverflowingDiv.INSTANCE)
					return new Constant(Untyped.INSTANCE, leftConst / rightConst, SyntheticLocation.INSTANCE);
				else
					return bin;
			} else {
				// TODO: handle more cases (e.g., other operators, non-constant operands)
			}
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
		return this;
	}

	@Override
	public SymbolicAbstractDomain popScope(ScopeToken token) throws SemanticException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean lessOrEqual(SymbolicAbstractDomain other) throws SemanticException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SymbolicAbstractDomain lub(SymbolicAbstractDomain other) throws SemanticException {
		// TODO Auto-generated method stub
		return other;
	}

	@Override
	public SymbolicAbstractDomain top() {
		// TODO Auto-generated method stub
		return new SymbolicAbstractDomain();
	}

	@Override
	public SymbolicAbstractDomain bottom() {
		// TODO Auto-generated method stub
		return new SymbolicAbstractDomain();
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
