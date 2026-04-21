package it.unipr.analysis;

import java.util.function.Predicate;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;

public class CombinationDomain implements ValueDomain<CombinationDomain> {

	private final SymbolicAbstractDomain symbolic;
	private final ValueEnvironment<Sign> signEnv;
	
	public CombinationDomain() {
		this(new SymbolicAbstractDomain(), new ValueEnvironment<Sign>(new Sign()));
	}
	
	/**
	 * Constructs a combination domain with given components.
	 * @param symbolic the symbolic domain component
	 * @param signEnv the numeric sign environment component
	 */
	public CombinationDomain(SymbolicAbstractDomain symbolic, ValueEnvironment<Sign> signEnv) {
		this.symbolic = symbolic;
		this.signEnv = signEnv;
	}
	
	@Override
	public CombinationDomain assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return new CombinationDomain(
				symbolic.assign(id, expression, pp, oracle),
				signEnv.assign(id, expression, pp, oracle));
	}

	@Override
	public CombinationDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return new CombinationDomain(
				symbolic.smallStepSemantics(expression, pp, oracle),
				signEnv.smallStepSemantics(expression, pp, oracle));
	}

	@Override
	public CombinationDomain assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {
		return new CombinationDomain(
				symbolic.assume(expression, src, dest, oracle),
				signEnv.assume(expression, src, dest, oracle));
	}

	@Override
	public boolean knowsIdentifier(Identifier id) {
		return symbolic.knowsIdentifier(id) || signEnv.knowsIdentifier(id);
	}

	@Override
	public CombinationDomain forgetIdentifier(Identifier id) throws SemanticException {
		return new CombinationDomain(
				symbolic.forgetIdentifier(id),
				signEnv.forgetIdentifier(id));
	}

	@Override
	public CombinationDomain forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return new CombinationDomain(
				symbolic.forgetIdentifiersIf(test),
				signEnv.forgetIdentifiersIf(test));
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		return symbolic.satisfies(expression, pp, oracle).and(signEnv.satisfies(expression, pp, oracle));
	}

	@Override
	public StructuredRepresentation representation() {
		// Combine the two component representations into a single string representation.
		String sym = symbolic == null ? "<null>" : symbolic.representation().toString();
		String signs = signEnv == null ? "<null>" : signEnv.representation().toString();
		return new StringRepresentation("Symbolic:\n" + sym + "\nSigns:\n" + signs);
	}

	@Override
	public CombinationDomain pushScope(ScopeToken token) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CombinationDomain popScope(ScopeToken token) throws SemanticException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean lessOrEqual(CombinationDomain other) throws SemanticException {
		return symbolic.lessOrEqual(other.symbolic) && signEnv.lessOrEqual(other.signEnv);
	}

	@Override
	public CombinationDomain lub(CombinationDomain other) throws SemanticException {
		return new CombinationDomain(
				symbolic.lub(other.symbolic),
				signEnv.lub(other.signEnv));
	}

	@Override
	public CombinationDomain top() {
		return new CombinationDomain(
				symbolic.top(),
				signEnv.top());
	}

	@Override
	public CombinationDomain bottom() {
		return new CombinationDomain(
				symbolic.bottom(),
				signEnv.bottom());
	}
}