package interpreter.query.Absyn; // Java Package generated by the BNF Converter.

public class AscOrderC extends Order {

  public AscOrderC() { }

  public <R,A> R accept(interpreter.query.Absyn.Order.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof interpreter.query.Absyn.AscOrderC) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return 37;
  }


}
