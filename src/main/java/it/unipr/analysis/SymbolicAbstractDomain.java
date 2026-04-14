package it.unipr.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.antlr.v4.parse.ANTLRParser.elementOptions_return;

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
	
	private Constant knownTerm(SymbolicExpression expr, Integer defaultValue) {
		/*
		 * This method attempts to compute a known value for a binary expression
		 * by recursively evaluating its operands.
		 * Only sum and difference are handled for now.
		 */

		BinaryExpression bin = (BinaryExpression) expr;
		SymbolicExpression left = bin.getLeft();
		SymbolicExpression right = bin.getRight();
		BinaryOperator op = bin.getOperator();
		//for sum and difference 
		//const op const -> op and return
		//const op var -> op with default value and recall
		//var op const -> op with default value and recall
		//var op var -> recall for both and combine results
		if (op == NumericNonOverflowingAdd.INSTANCE) {
			if(left instanceof Constant && right instanceof Constant)
				return new Constant(Untyped.INSTANCE, (Integer) ((Constant) left).getValue() + (Integer) ((Constant) right).getValue(), SyntheticLocation.INSTANCE);
			else if (left instanceof Constant)
				return knownTerm(right, (Integer) ((Constant) left).getValue() + defaultValue);
			else if (right instanceof Constant)
				return knownTerm(left, (Integer) ((Constant) right).getValue() + defaultValue);
			else
				return new Constant(Untyped.INSTANCE, (Integer) knownTerm(left, defaultValue).getValue() + (Integer) knownTerm(right, defaultValue).getValue(), SyntheticLocation.INSTANCE);
		}
		if (op == NumericNonOverflowingSub.INSTANCE) {
			if(left instanceof Constant && right instanceof Constant)
				return new Constant(Untyped.INSTANCE, (Integer) ((Constant) left).getValue() - (Integer) ((Constant) right).getValue(), SyntheticLocation.INSTANCE);
			else if (left instanceof Constant)
				return knownTerm(right, defaultValue - (Integer) ((Constant) left).getValue());
			else if (right instanceof Constant)
				return knownTerm(left, defaultValue - (Integer) ((Constant) right).getValue());
			else
				return new Constant(Untyped.INSTANCE, (Integer) knownTerm(left, defaultValue).getValue() - (Integer) knownTerm(right, defaultValue).getValue(), SyntheticLocation.INSTANCE);
		}
		return null;
	}

	private void getVariables(SymbolicExpression expr, List<SymbolicVariable> variables, List<Constant> coeffs) {

		/* This method attempts to extract variables and their coefficients from a binary expression */

		BinaryExpression bin = (BinaryExpression) expr;
		SymbolicExpression left = bin.getLeft();
		SymbolicExpression right = bin.getRight();
		BinaryOperator op = bin.getOperator();

		//
		if (left instanceof Constant && right instanceof SymbolicVariable) {
			Integer idx=variables.indexOf(right);
			if(idx==-1){
				variables.add((SymbolicVariable) right);
				coeffs.add(new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE));
			}
			else{
				if (op == NumericNonOverflowingAdd.INSTANCE)
					coeffs.set(idx, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idx)).getValue()) + 1, SyntheticLocation.INSTANCE));
				else if (op == NumericNonOverflowingSub.INSTANCE)
					coeffs.set(idx, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idx)).getValue()) - 1, SyntheticLocation.INSTANCE));
				else if (op == NumericNonOverflowingMul.INSTANCE)
					coeffs.set(idx, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idx)).getValue()) + ((Integer) ((Constant) left).getValue()), SyntheticLocation.INSTANCE));
			}
			return;
			
		} 
		else if (left instanceof SymbolicVariable && right instanceof Constant) {
			Integer idx=variables.indexOf(right);
			if(idx==-1){
				variables.add((SymbolicVariable) left);
				coeffs.add(new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE));
			}
			//TO DO: add and sub: how do i know the variable coefficient?
			else if (op == NumericNonOverflowingMul.INSTANCE)
				coeffs.set(idx, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idx)).getValue()) * ((Integer) ((Constant) right).getValue()), SyntheticLocation.INSTANCE));
			else if (op == NumericNonOverflowingDiv.INSTANCE) {
				coeffs.set(idx, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idx)).getValue()) / ((Integer) ((Constant) right).getValue()), SyntheticLocation.INSTANCE));
			}
			return;
		} 
		else if (left instanceof SymbolicVariable && right instanceof SymbolicVariable) {
			
			Integer idxLeft=variables.indexOf(left);
			Integer idxRight=variables.indexOf(right);

			if(idxLeft==-1){
				variables.add((SymbolicVariable) left);
				coeffs.add(new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE));
			}

			//TODO: add and sub: how do i know the left variable coefficient?

			if(idxRight==-1){
				variables.add((SymbolicVariable) right);
				coeffs.add(new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE));
			}
			else{
				if (op == NumericNonOverflowingAdd.INSTANCE)
					coeffs.set(idxRight, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idxRight)).getValue()) + 1, SyntheticLocation.INSTANCE));
				else if (op == NumericNonOverflowingSub.INSTANCE)
					coeffs.set(idxRight, new Constant(Untyped.INSTANCE, ((Integer) ((Constant) coeffs.get(idxRight)).getValue()) - 1, SyntheticLocation.INSTANCE));
			}
			return;
		}
		else {
			getVariables(left, variables, coeffs);
			getVariables(right, variables, coeffs);	
		}
	}
	
	public SymbolicExpression eval(SymbolicExpression expr) {
		if (expr instanceof Identifier)
			return this.symbolicState.getState((Identifier) expr).elements.stream().findAny().get();
		else if (expr instanceof PushAny) {
			return new SymbolicVariable(expr.getStaticType(), expr.getCodeLocation().toString(), expr.getCodeLocation());
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
			} 
			else {
				//coeffs*variables + known term
				List<SymbolicVariable> variables = new ArrayList<>();
				List<Constant> coeffs = new ArrayList<>();
				Constant known = knownTerm(expr, 0);
				getVariables(expr, variables, coeffs);
				BinaryExpression newBin = new BinaryExpression(bin.getStaticType(), (SymbolicExpression) ((Constant) coeffs.get(0)), variables.get(0), NumericNonOverflowingMul.INSTANCE, bin.getCodeLocation());
				for (int i = 1; i<variables.size(); i++) {
					BinaryExpression newBinTemp=new BinaryExpression(bin.getStaticType(), (SymbolicExpression) ((Constant) coeffs.get(i)), variables.get(i), NumericNonOverflowingMul.INSTANCE, bin.getCodeLocation());
					newBin = new BinaryExpression(bin.getStaticType(), newBin, newBinTemp, NumericNonOverflowingAdd.INSTANCE, bin.getCodeLocation());
				}

				return new BinaryExpression(bin.getStaticType(), newBin, (SymbolicExpression) known, NumericNonOverflowingAdd.INSTANCE, bin.getCodeLocation());
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
