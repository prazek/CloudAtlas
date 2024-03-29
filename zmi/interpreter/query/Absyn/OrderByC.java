package interpreter.query.Absyn; // Java Package generated by the BNF Converter.

public class OrderByC extends OrderBy {
  public final ListOrderItem listorderitem_;

  public OrderByC(ListOrderItem p1) { listorderitem_ = p1; }

  public <R,A> R accept(interpreter.query.Absyn.OrderBy.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof interpreter.query.Absyn.OrderByC) {
      interpreter.query.Absyn.OrderByC x = (interpreter.query.Absyn.OrderByC)o;
      return this.listorderitem_.equals(x.listorderitem_);
    }
    return false;
  }

  public int hashCode() {
    return this.listorderitem_.hashCode();
  }


}
