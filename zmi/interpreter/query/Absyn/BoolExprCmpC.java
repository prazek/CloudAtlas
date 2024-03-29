package interpreter.query.Absyn; // Java Package generated by the BNF Converter.

public class BoolExprCmpC extends BoolExpr {
  public final BasicExpr basicexpr_1, basicexpr_2;
  public final RelOp relop_;

  public BoolExprCmpC(BasicExpr p1, RelOp p2, BasicExpr p3) { basicexpr_1 = p1; relop_ = p2; basicexpr_2 = p3; }

  public <R,A> R accept(interpreter.query.Absyn.BoolExpr.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof interpreter.query.Absyn.BoolExprCmpC) {
      interpreter.query.Absyn.BoolExprCmpC x = (interpreter.query.Absyn.BoolExprCmpC)o;
      return this.basicexpr_1.equals(x.basicexpr_1) && this.relop_.equals(x.relop_) && this.basicexpr_2.equals(x.basicexpr_2);
    }
    return false;
  }

  public int hashCode() {
    return 37*(37*(this.basicexpr_1.hashCode())+this.relop_.hashCode())+this.basicexpr_2.hashCode();
  }


}
