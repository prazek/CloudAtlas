package interpreter.query.Absyn; // Java Package generated by the BNF Converter.

public abstract class Order implements java.io.Serializable {
  public abstract <R,A> R accept(Order.Visitor<R,A> v, A arg);
  public interface Visitor <R,A> {
    public R visit(interpreter.query.Absyn.AscOrderC p, A arg);
    public R visit(interpreter.query.Absyn.DescOrderC p, A arg);
    public R visit(interpreter.query.Absyn.NoOrderC p, A arg);

  }

}
