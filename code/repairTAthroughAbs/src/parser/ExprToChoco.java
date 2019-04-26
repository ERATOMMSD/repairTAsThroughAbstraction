package parser;

import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;

import tgtlib.definitions.expression.AndExpression;
import tgtlib.definitions.expression.BinaryExpression;
import tgtlib.definitions.expression.CaseExpression;
import tgtlib.definitions.expression.CondExpression;
import tgtlib.definitions.expression.DivExpression;
import tgtlib.definitions.expression.EqualsExpression;
import tgtlib.definitions.expression.ExpressionVisitor;
import tgtlib.definitions.expression.FunctionTerm;
import tgtlib.definitions.expression.GreaterEqualExpression;
import tgtlib.definitions.expression.GreaterThanExpression;
import tgtlib.definitions.expression.IdExpression;
import tgtlib.definitions.expression.ImpliesExpression;
import tgtlib.definitions.expression.LessEqualExpression;
import tgtlib.definitions.expression.LessThanExpression;
import tgtlib.definitions.expression.MinusExpression;
import tgtlib.definitions.expression.ModuloExpression;
import tgtlib.definitions.expression.MultExpression;
import tgtlib.definitions.expression.NegExpression;
import tgtlib.definitions.expression.NextExpression;
import tgtlib.definitions.expression.NotEqualsExpression;
import tgtlib.definitions.expression.NotExpression;
import tgtlib.definitions.expression.OrExpression;
import tgtlib.definitions.expression.PlusExpression;
import tgtlib.definitions.expression.PrimedIdExpression;
import tgtlib.definitions.expression.XOrExpression;
import tgtlib.definitions.expression.type.BoolType;

/** converts an expression to the corresponding Choco data structure
 * 
 * @author radavelli
 *
 */
public class ExprToChoco implements ExpressionVisitor<ArExpression> {
	Map<IdExpression, ? extends ArExpression> idYices;
	Model model;

	/**
	 * 
	 * @param yices
	 * @param context
	 *            the context
	 * @param idYices
	 *            the ids already created in the context
	 */
	public ExprToChoco(Model model, Map<IdExpression, ? extends ArExpression> idYices) {
		this.idYices = idYices;
		this.model = model;
	}

	@Override
	public ArExpression forAndExpression(AndExpression andExpression) {
		ArExpression[] p = getPointerArray(andExpression);
		//PointerByReference pr = new PointerByReference(p[0]);
		//return yices.yices_mk_and(context, pr, 2);
		return ((ReExpression)p[0]).and((ReExpression)p[1]);
	}

	@Override
	public ArExpression forOrExpression(OrExpression orExpression) {
		ArExpression[] p = getPointerArray(orExpression);
		//PointerByReference pr = new PointerByReference(p[0]);
		//return yices.yices_mk_or(context, pr, 2);
		return ((ReExpression)p[0]).or((ReExpression)p[1]);
	}

	@Override
	public ArExpression forXOrExpression(XOrExpression xOrExpression) {
		ArExpression[] p = getPointerArray(xOrExpression);
		return p[0].ne(p[1]);
	}

	private ArExpression[] getPointerArray(BinaryExpression binExpr) {
		ArExpression p1 = binExpr.getFirstOperand().accept(this);
		ArExpression p2 = binExpr.getSecondOperand().accept(this);
		ArExpression p[] = { p1, p2 };
		return p;
	}

	@Override
	public ArExpression forNotExpression(NotExpression notExpression) {
		ArExpression p = notExpression.getOperand().accept(this);
		return ((ReExpression)p).not();
	}

	@Override
	public ArExpression forEqualsExpression(EqualsExpression equalsExpression) {
		ArExpression p1 = equalsExpression.getFirstOperand().accept(this);
		ArExpression p2 = equalsExpression.getSecondOperand().accept(this);
		return p1.eq(p2);
	}
	
	@Override
	public ArExpression forNotEqualsExpression(
			NotEqualsExpression notEqualsExpression) {
		ArExpression[] p = getPointerArray(notEqualsExpression);
		return p[0].ne(p[1]);
	}

	@Override
	public ArExpression forIdExpression(IdExpression idExpression) {
		if (idExpression == BoolType.TRUE_CONST)
			return model.boolVar(true);// model.trueConstraint();
		if (idExpression == BoolType.FALSE_CONST)
			return model.boolVar(false); //model.falseConstraint();
		ArExpression idExpr = idYices.get(idExpression);
		return idExpr;
	}

	@Override
	public ArExpression forGreaterEqualExpression(
			GreaterEqualExpression greaterEqualExpression) {
		ArExpression[] p = getPointerArray(greaterEqualExpression);
		return p[0].ge(p[1]);
	}

	@Override
	public ArExpression forDivExpression(DivExpression divExpression) {
		ArExpression[] p = getPointerArray(divExpression);
		return p[0].div(p[1]);
	}

	@Override
	public ArExpression forPlusExpression(PlusExpression plusExpression) {
		ArExpression[] p = getPointerArray(plusExpression);
		return p[0].add(p[1]);
	}

	@Override
	public ArExpression forMinusExpression(MinusExpression minusExpression) {
		ArExpression[] p = getPointerArray(minusExpression);
		return p[0].sub(p[1]);
	}

	@Override
	public ArExpression forGreaterThanExpression(
			GreaterThanExpression greaterThanExpression) {
		ArExpression[] p = getPointerArray(greaterThanExpression);
		return p[0].gt(p[1]);
	}

	@Override
	public ArExpression forLessEqualExpression(
			LessEqualExpression lessEqualExpression) {
		ArExpression[] p = getPointerArray(lessEqualExpression);
		return p[0].le(p[1]);
	}

	@Override
	public ArExpression forLessThanExpression(LessThanExpression lessThanExpression) {
		ArExpression[] p = getPointerArray(lessThanExpression);
		return p[0].lt(p[1]);
	}

	@Override
	public ArExpression forImpliesExpression(ImpliesExpression impliesExpression) {
		ArExpression[] p = getPointerArray(impliesExpression);
		// p[0] => p[1]
		// convert to not p[0] or p[1]  
		p[0] = ((ReExpression)p[0]).not();
		return ((ReExpression)p[0]).or((ReExpression)p[1]);
	}

	@Override
	public ArExpression forMultExpression(MultExpression multExpression) {
		ArExpression[] p = getPointerArray(multExpression);
		return p[0].mul(p[1]);
	}

	@Override
	public ArExpression forNegExpression(NegExpression negExpression) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ArExpression forNextExpression(NextExpression nextExpression) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ArExpression forPrimedIdExpression(PrimedIdExpression primedIdExpression) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ArExpression forModuloExpression(ModuloExpression moduloExpression) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ArExpression forFunctionTerm(FunctionTerm ft) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ArExpression forCaseExpression(CaseExpression caseExpression) {
		throw new RuntimeException("not implemented");
	}

	public Map<IdExpression, ? extends ArExpression> getIdYices() {
		return idYices;
	}

	@Override
	public ArExpression forConditionalExpression(CondExpression cond) {
		throw new RuntimeException("not implemented yet");
	}
}
