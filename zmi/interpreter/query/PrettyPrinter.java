package interpreter.query;
import interpreter.query.Absyn.*;

public class PrettyPrinter
{
  //For certain applications increasing the initial size of the buffer may improve performance.
  private static final int INITIAL_BUFFER_SIZE = 128;
  //You may wish to change the parentheses used in precedence.
  private static final String _L_PAREN = new String("(");
  private static final String _R_PAREN = new String(")");
  //You may wish to change render
  private static void render(String s)
  {
    if (s.equals("{"))
    {
       buf_.append("\n");
       indent();
       buf_.append(s);
       _n_ = _n_ + 2;
       buf_.append("\n");
       indent();
    }
    else if (s.equals("(") || s.equals("["))
       buf_.append(s);
    else if (s.equals(")") || s.equals("]"))
    {
       backup();
       buf_.append(s);
       buf_.append(" ");
    }
    else if (s.equals("}"))
    {
       _n_ = _n_ - 2;
       backup();
       backup();
       buf_.append(s);
       buf_.append("\n");
       indent();
    }
    else if (s.equals(","))
    {
       backup();
       buf_.append(s);
       buf_.append(" ");
    }
    else if (s.equals(";"))
    {
       backup();
       buf_.append(s);
       buf_.append("\n");
       indent();
    }
    else if (s.equals("")) return;
    else
    {
       buf_.append(s);
       buf_.append(" ");
    }
  }


  //  print and show methods are defined for each category.
  public static String print(interpreter.query.Absyn.ListStatement foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.ListStatement foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.ListOrderItem foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.ListOrderItem foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.ListSelItem foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.ListSelItem foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.ListCondExpr foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.ListCondExpr foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.Program foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.Program foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.Statement foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.Statement foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.Where foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.Where foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.OrderBy foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.OrderBy foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.OrderItem foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.OrderItem foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.Order foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.Order foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.Nulls foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.Nulls foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.SelItem foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.SelItem foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.BoolExpr foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.BoolExpr foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.CondExpr foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.CondExpr foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.BasicExpr foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.BasicExpr foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String print(interpreter.query.Absyn.RelOp foo)
  {
    pp(foo, 0);
    trim();
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  public static String show(interpreter.query.Absyn.RelOp foo)
  {
    sh(foo);
    String temp = buf_.toString();
    buf_.delete(0,buf_.length());
    return temp;
  }
  /***   You shouldn't need to change anything beyond this point.   ***/

  private static void pp(interpreter.query.Absyn.ListStatement foo, int _i_)
  {
     for (java.util.Iterator<Statement> it = foo.iterator(); it.hasNext();)
     {
       pp(it.next(), 0);
       if (it.hasNext()) {
         render(";");
       } else {
         render("");
       }
     }
  }

  private static void pp(interpreter.query.Absyn.ListOrderItem foo, int _i_)
  {
     for (java.util.Iterator<OrderItem> it = foo.iterator(); it.hasNext();)
     {
       pp(it.next(), 0);
       if (it.hasNext()) {
         render(",");
       } else {
         render("");
       }
     }
  }

  private static void pp(interpreter.query.Absyn.ListSelItem foo, int _i_)
  {
     for (java.util.Iterator<SelItem> it = foo.iterator(); it.hasNext();)
     {
       pp(it.next(), 0);
       if (it.hasNext()) {
         render(",");
       } else {
         render("");
       }
     }
  }

  private static void pp(interpreter.query.Absyn.ListCondExpr foo, int _i_)
  {
     for (java.util.Iterator<CondExpr> it = foo.iterator(); it.hasNext();)
     {
       pp(it.next(), 0);
       if (it.hasNext()) {
         render(",");
       } else {
         render("");
       }
     }
  }

  private static void pp(interpreter.query.Absyn.Program foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.ProgramC)
    {
       interpreter.query.Absyn.ProgramC _programc = (interpreter.query.Absyn.ProgramC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_programc.liststatement_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.Statement foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.StatementC)
    {
       interpreter.query.Absyn.StatementC _statementc = (interpreter.query.Absyn.StatementC) foo;
       if (_i_ > 0) render(_L_PAREN);
       render("SELECT");
       pp(_statementc.listselitem_, 0);
       pp(_statementc.where_, 0);
       pp(_statementc.orderby_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.Where foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.NoWhereC)
    {
       if (_i_ > 0) render(_L_PAREN);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.WhereC)
    {
       interpreter.query.Absyn.WhereC _wherec = (interpreter.query.Absyn.WhereC) foo;
       if (_i_ > 0) render(_L_PAREN);
       render("WHERE");
       pp(_wherec.condexpr_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.OrderBy foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.NoOrderByC)
    {
       if (_i_ > 0) render(_L_PAREN);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.OrderByC)
    {
       interpreter.query.Absyn.OrderByC _orderbyc = (interpreter.query.Absyn.OrderByC) foo;
       if (_i_ > 0) render(_L_PAREN);
       render("ORDER");
       render("BY");
       pp(_orderbyc.listorderitem_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.OrderItem foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.OrderItemC)
    {
       interpreter.query.Absyn.OrderItemC _orderitemc = (interpreter.query.Absyn.OrderItemC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_orderitemc.condexpr_, 0);
       pp(_orderitemc.order_, 0);
       pp(_orderitemc.nulls_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.Order foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.AscOrderC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("ASC");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.DescOrderC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("DESC");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.NoOrderC)
    {
       if (_i_ > 0) render(_L_PAREN);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.Nulls foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.NoNullsC)
    {
       if (_i_ > 0) render(_L_PAREN);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.NullFirstsC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("NULLS");
       render("FIRST");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.NullsLastC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("NULLS");
       render("LAST");
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.SelItem foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.SelItemC)
    {
       interpreter.query.Absyn.SelItemC _selitemc = (interpreter.query.Absyn.SelItemC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_selitemc.condexpr_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.AliasedSelItemC)
    {
       interpreter.query.Absyn.AliasedSelItemC _aliasedselitemc = (interpreter.query.Absyn.AliasedSelItemC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_aliasedselitemc.condexpr_, 0);
       render("AS");
       pp(_aliasedselitemc.qident_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.BoolExpr foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.BoolExprCmpC)
    {
       interpreter.query.Absyn.BoolExprCmpC _boolexprcmpc = (interpreter.query.Absyn.BoolExprCmpC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_boolexprcmpc.basicexpr_1, 0);
       pp(_boolexprcmpc.relop_, 0);
       pp(_boolexprcmpc.basicexpr_2, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BoolExprRegExpC)
    {
       interpreter.query.Absyn.BoolExprRegExpC _boolexprregexpc = (interpreter.query.Absyn.BoolExprRegExpC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_boolexprregexpc.basicexpr_, 0);
       render("REGEXP");
       printQuoted(_boolexprregexpc.string_);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BoolExprBasicExprC)
    {
       interpreter.query.Absyn.BoolExprBasicExprC _boolexprbasicexprc = (interpreter.query.Absyn.BoolExprBasicExprC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_boolexprbasicexprc.basicexpr_, 0);
       if (_i_ > 0) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.CondExpr foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.CondExprOrC)
    {
       interpreter.query.Absyn.CondExprOrC _condexprorc = (interpreter.query.Absyn.CondExprOrC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_condexprorc.condexpr_1, 0);
       render("OR");
       pp(_condexprorc.condexpr_2, 1);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.CondExprAndC)
    {
       interpreter.query.Absyn.CondExprAndC _condexprandc = (interpreter.query.Absyn.CondExprAndC) foo;
       if (_i_ > 1) render(_L_PAREN);
       pp(_condexprandc.condexpr_1, 1);
       render("AND");
       pp(_condexprandc.condexpr_2, 2);
       if (_i_ > 1) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.CondExprNotC)
    {
       interpreter.query.Absyn.CondExprNotC _condexprnotc = (interpreter.query.Absyn.CondExprNotC) foo;
       if (_i_ > 2) render(_L_PAREN);
       render("NOT");
       pp(_condexprnotc.condexpr_, 2);
       if (_i_ > 2) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.CondExprBoolExprC)
    {
       interpreter.query.Absyn.CondExprBoolExprC _condexprboolexprc = (interpreter.query.Absyn.CondExprBoolExprC) foo;
       if (_i_ > 2) render(_L_PAREN);
       pp(_condexprboolexprc.boolexpr_, 0);
       if (_i_ > 2) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.BasicExpr foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.BasicExprAddC)
    {
       interpreter.query.Absyn.BasicExprAddC _basicexpraddc = (interpreter.query.Absyn.BasicExprAddC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_basicexpraddc.basicexpr_1, 0);
       render("+");
       pp(_basicexpraddc.basicexpr_2, 1);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BasicExprSubC)
    {
       interpreter.query.Absyn.BasicExprSubC _basicexprsubc = (interpreter.query.Absyn.BasicExprSubC) foo;
       if (_i_ > 0) render(_L_PAREN);
       pp(_basicexprsubc.basicexpr_1, 0);
       render("-");
       pp(_basicexprsubc.basicexpr_2, 1);
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BasicExprMulC)
    {
       interpreter.query.Absyn.BasicExprMulC _basicexprmulc = (interpreter.query.Absyn.BasicExprMulC) foo;
       if (_i_ > 1) render(_L_PAREN);
       pp(_basicexprmulc.basicexpr_1, 1);
       render("*");
       pp(_basicexprmulc.basicexpr_2, 2);
       if (_i_ > 1) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BasicExprDivC)
    {
       interpreter.query.Absyn.BasicExprDivC _basicexprdivc = (interpreter.query.Absyn.BasicExprDivC) foo;
       if (_i_ > 1) render(_L_PAREN);
       pp(_basicexprdivc.basicexpr_1, 1);
       render("/");
       pp(_basicexprdivc.basicexpr_2, 2);
       if (_i_ > 1) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BasicExprModC)
    {
       interpreter.query.Absyn.BasicExprModC _basicexprmodc = (interpreter.query.Absyn.BasicExprModC) foo;
       if (_i_ > 1) render(_L_PAREN);
       pp(_basicexprmodc.basicexpr_1, 1);
       render("%");
       pp(_basicexprmodc.basicexpr_2, 2);
       if (_i_ > 1) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.BasicExprNegC)
    {
       interpreter.query.Absyn.BasicExprNegC _basicexprnegc = (interpreter.query.Absyn.BasicExprNegC) foo;
       if (_i_ > 2) render(_L_PAREN);
       render("-");
       pp(_basicexprnegc.basicexpr_, 2);
       if (_i_ > 2) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EBoolC)
    {
       interpreter.query.Absyn.EBoolC _eboolc = (interpreter.query.Absyn.EBoolC) foo;
       if (_i_ > 3) render(_L_PAREN);
       pp(_eboolc.qbool_, 0);
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EIdentC)
    {
       interpreter.query.Absyn.EIdentC _eidentc = (interpreter.query.Absyn.EIdentC) foo;
       if (_i_ > 3) render(_L_PAREN);
       pp(_eidentc.qident_, 0);
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EFunC)
    {
       interpreter.query.Absyn.EFunC _efunc = (interpreter.query.Absyn.EFunC) foo;
       if (_i_ > 3) render(_L_PAREN);
       pp(_efunc.qident_, 0);
       render("(");
       pp(_efunc.listcondexpr_, 0);
       render(")");
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EStrC)
    {
       interpreter.query.Absyn.EStrC _estrc = (interpreter.query.Absyn.EStrC) foo;
       if (_i_ > 3) render(_L_PAREN);
       printQuoted(_estrc.string_);
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EIntC)
    {
       interpreter.query.Absyn.EIntC _eintc = (interpreter.query.Absyn.EIntC) foo;
       if (_i_ > 3) render(_L_PAREN);
       pp(_eintc.qinteger_, 0);
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EDblC)
    {
       interpreter.query.Absyn.EDblC _edblc = (interpreter.query.Absyn.EDblC) foo;
       if (_i_ > 3) render(_L_PAREN);
       pp(_edblc.qdouble_, 0);
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.ECondExprC)
    {
       interpreter.query.Absyn.ECondExprC _econdexprc = (interpreter.query.Absyn.ECondExprC) foo;
       if (_i_ > 3) render(_L_PAREN);
       render("(");
       pp(_econdexprc.condexpr_, 0);
       render(")");
       if (_i_ > 3) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.EStmtC)
    {
       interpreter.query.Absyn.EStmtC _estmtc = (interpreter.query.Absyn.EStmtC) foo;
       if (_i_ > 3) render(_L_PAREN);
       render("(");
       pp(_estmtc.statement_, 0);
       render(")");
       if (_i_ > 3) render(_R_PAREN);
    }
  }

  private static void pp(interpreter.query.Absyn.RelOp foo, int _i_)
  {
    if (foo instanceof interpreter.query.Absyn.RelOpGtC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render(">");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.RelOpEqC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("=");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.RelOpNeC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("<>");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.RelOpLtC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("<");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.RelOpLeC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render("<=");
       if (_i_ > 0) render(_R_PAREN);
    }
    else     if (foo instanceof interpreter.query.Absyn.RelOpGeC)
    {
       if (_i_ > 0) render(_L_PAREN);
       render(">=");
       if (_i_ > 0) render(_R_PAREN);
    }
  }


  private static void sh(interpreter.query.Absyn.ListStatement foo)
  {
     for (java.util.Iterator<Statement> it = foo.iterator(); it.hasNext();)
     {
       sh(it.next());
       if (it.hasNext())
         render(",");
     }
  }

  private static void sh(interpreter.query.Absyn.ListOrderItem foo)
  {
     for (java.util.Iterator<OrderItem> it = foo.iterator(); it.hasNext();)
     {
       sh(it.next());
       if (it.hasNext())
         render(",");
     }
  }

  private static void sh(interpreter.query.Absyn.ListSelItem foo)
  {
     for (java.util.Iterator<SelItem> it = foo.iterator(); it.hasNext();)
     {
       sh(it.next());
       if (it.hasNext())
         render(",");
     }
  }

  private static void sh(interpreter.query.Absyn.ListCondExpr foo)
  {
     for (java.util.Iterator<CondExpr> it = foo.iterator(); it.hasNext();)
     {
       sh(it.next());
       if (it.hasNext())
         render(",");
     }
  }

  private static void sh(interpreter.query.Absyn.Program foo)
  {
    if (foo instanceof interpreter.query.Absyn.ProgramC)
    {
       interpreter.query.Absyn.ProgramC _programc = (interpreter.query.Absyn.ProgramC) foo;
       render("(");
       render("ProgramC");
       render("[");
       sh(_programc.liststatement_);
       render("]");
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.Statement foo)
  {
    if (foo instanceof interpreter.query.Absyn.StatementC)
    {
       interpreter.query.Absyn.StatementC _statementc = (interpreter.query.Absyn.StatementC) foo;
       render("(");
       render("StatementC");
       render("[");
       sh(_statementc.listselitem_);
       render("]");
       sh(_statementc.where_);
       sh(_statementc.orderby_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.Where foo)
  {
    if (foo instanceof interpreter.query.Absyn.NoWhereC)
    {
       render("NoWhereC");
    }
    if (foo instanceof interpreter.query.Absyn.WhereC)
    {
       interpreter.query.Absyn.WhereC _wherec = (interpreter.query.Absyn.WhereC) foo;
       render("(");
       render("WhereC");
       sh(_wherec.condexpr_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.OrderBy foo)
  {
    if (foo instanceof interpreter.query.Absyn.NoOrderByC)
    {
       render("NoOrderByC");
    }
    if (foo instanceof interpreter.query.Absyn.OrderByC)
    {
       interpreter.query.Absyn.OrderByC _orderbyc = (interpreter.query.Absyn.OrderByC) foo;
       render("(");
       render("OrderByC");
       render("[");
       sh(_orderbyc.listorderitem_);
       render("]");
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.OrderItem foo)
  {
    if (foo instanceof interpreter.query.Absyn.OrderItemC)
    {
       interpreter.query.Absyn.OrderItemC _orderitemc = (interpreter.query.Absyn.OrderItemC) foo;
       render("(");
       render("OrderItemC");
       sh(_orderitemc.condexpr_);
       sh(_orderitemc.order_);
       sh(_orderitemc.nulls_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.Order foo)
  {
    if (foo instanceof interpreter.query.Absyn.AscOrderC)
    {
       render("AscOrderC");
    }
    if (foo instanceof interpreter.query.Absyn.DescOrderC)
    {
       render("DescOrderC");
    }
    if (foo instanceof interpreter.query.Absyn.NoOrderC)
    {
       render("NoOrderC");
    }
  }

  private static void sh(interpreter.query.Absyn.Nulls foo)
  {
    if (foo instanceof interpreter.query.Absyn.NoNullsC)
    {
       render("NoNullsC");
    }
    if (foo instanceof interpreter.query.Absyn.NullFirstsC)
    {
       render("NullFirstsC");
    }
    if (foo instanceof interpreter.query.Absyn.NullsLastC)
    {
       render("NullsLastC");
    }
  }

  private static void sh(interpreter.query.Absyn.SelItem foo)
  {
    if (foo instanceof interpreter.query.Absyn.SelItemC)
    {
       interpreter.query.Absyn.SelItemC _selitemc = (interpreter.query.Absyn.SelItemC) foo;
       render("(");
       render("SelItemC");
       sh(_selitemc.condexpr_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.AliasedSelItemC)
    {
       interpreter.query.Absyn.AliasedSelItemC _aliasedselitemc = (interpreter.query.Absyn.AliasedSelItemC) foo;
       render("(");
       render("AliasedSelItemC");
       sh(_aliasedselitemc.condexpr_);
       sh(_aliasedselitemc.qident_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.BoolExpr foo)
  {
    if (foo instanceof interpreter.query.Absyn.BoolExprCmpC)
    {
       interpreter.query.Absyn.BoolExprCmpC _boolexprcmpc = (interpreter.query.Absyn.BoolExprCmpC) foo;
       render("(");
       render("BoolExprCmpC");
       sh(_boolexprcmpc.basicexpr_1);
       sh(_boolexprcmpc.relop_);
       sh(_boolexprcmpc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BoolExprRegExpC)
    {
       interpreter.query.Absyn.BoolExprRegExpC _boolexprregexpc = (interpreter.query.Absyn.BoolExprRegExpC) foo;
       render("(");
       render("BoolExprRegExpC");
       sh(_boolexprregexpc.basicexpr_);
       sh(_boolexprregexpc.string_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BoolExprBasicExprC)
    {
       interpreter.query.Absyn.BoolExprBasicExprC _boolexprbasicexprc = (interpreter.query.Absyn.BoolExprBasicExprC) foo;
       render("(");
       render("BoolExprBasicExprC");
       sh(_boolexprbasicexprc.basicexpr_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.CondExpr foo)
  {
    if (foo instanceof interpreter.query.Absyn.CondExprOrC)
    {
       interpreter.query.Absyn.CondExprOrC _condexprorc = (interpreter.query.Absyn.CondExprOrC) foo;
       render("(");
       render("CondExprOrC");
       sh(_condexprorc.condexpr_1);
       sh(_condexprorc.condexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.CondExprAndC)
    {
       interpreter.query.Absyn.CondExprAndC _condexprandc = (interpreter.query.Absyn.CondExprAndC) foo;
       render("(");
       render("CondExprAndC");
       sh(_condexprandc.condexpr_1);
       sh(_condexprandc.condexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.CondExprNotC)
    {
       interpreter.query.Absyn.CondExprNotC _condexprnotc = (interpreter.query.Absyn.CondExprNotC) foo;
       render("(");
       render("CondExprNotC");
       sh(_condexprnotc.condexpr_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.CondExprBoolExprC)
    {
       interpreter.query.Absyn.CondExprBoolExprC _condexprboolexprc = (interpreter.query.Absyn.CondExprBoolExprC) foo;
       render("(");
       render("CondExprBoolExprC");
       sh(_condexprboolexprc.boolexpr_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.BasicExpr foo)
  {
    if (foo instanceof interpreter.query.Absyn.BasicExprAddC)
    {
       interpreter.query.Absyn.BasicExprAddC _basicexpraddc = (interpreter.query.Absyn.BasicExprAddC) foo;
       render("(");
       render("BasicExprAddC");
       sh(_basicexpraddc.basicexpr_1);
       sh(_basicexpraddc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BasicExprSubC)
    {
       interpreter.query.Absyn.BasicExprSubC _basicexprsubc = (interpreter.query.Absyn.BasicExprSubC) foo;
       render("(");
       render("BasicExprSubC");
       sh(_basicexprsubc.basicexpr_1);
       sh(_basicexprsubc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BasicExprMulC)
    {
       interpreter.query.Absyn.BasicExprMulC _basicexprmulc = (interpreter.query.Absyn.BasicExprMulC) foo;
       render("(");
       render("BasicExprMulC");
       sh(_basicexprmulc.basicexpr_1);
       sh(_basicexprmulc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BasicExprDivC)
    {
       interpreter.query.Absyn.BasicExprDivC _basicexprdivc = (interpreter.query.Absyn.BasicExprDivC) foo;
       render("(");
       render("BasicExprDivC");
       sh(_basicexprdivc.basicexpr_1);
       sh(_basicexprdivc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BasicExprModC)
    {
       interpreter.query.Absyn.BasicExprModC _basicexprmodc = (interpreter.query.Absyn.BasicExprModC) foo;
       render("(");
       render("BasicExprModC");
       sh(_basicexprmodc.basicexpr_1);
       sh(_basicexprmodc.basicexpr_2);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.BasicExprNegC)
    {
       interpreter.query.Absyn.BasicExprNegC _basicexprnegc = (interpreter.query.Absyn.BasicExprNegC) foo;
       render("(");
       render("BasicExprNegC");
       sh(_basicexprnegc.basicexpr_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EBoolC)
    {
       interpreter.query.Absyn.EBoolC _eboolc = (interpreter.query.Absyn.EBoolC) foo;
       render("(");
       render("EBoolC");
       sh(_eboolc.qbool_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EIdentC)
    {
       interpreter.query.Absyn.EIdentC _eidentc = (interpreter.query.Absyn.EIdentC) foo;
       render("(");
       render("EIdentC");
       sh(_eidentc.qident_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EFunC)
    {
       interpreter.query.Absyn.EFunC _efunc = (interpreter.query.Absyn.EFunC) foo;
       render("(");
       render("EFunC");
       sh(_efunc.qident_);
       render("[");
       sh(_efunc.listcondexpr_);
       render("]");
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EStrC)
    {
       interpreter.query.Absyn.EStrC _estrc = (interpreter.query.Absyn.EStrC) foo;
       render("(");
       render("EStrC");
       sh(_estrc.string_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EIntC)
    {
       interpreter.query.Absyn.EIntC _eintc = (interpreter.query.Absyn.EIntC) foo;
       render("(");
       render("EIntC");
       sh(_eintc.qinteger_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EDblC)
    {
       interpreter.query.Absyn.EDblC _edblc = (interpreter.query.Absyn.EDblC) foo;
       render("(");
       render("EDblC");
       sh(_edblc.qdouble_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.ECondExprC)
    {
       interpreter.query.Absyn.ECondExprC _econdexprc = (interpreter.query.Absyn.ECondExprC) foo;
       render("(");
       render("ECondExprC");
       sh(_econdexprc.condexpr_);
       render(")");
    }
    if (foo instanceof interpreter.query.Absyn.EStmtC)
    {
       interpreter.query.Absyn.EStmtC _estmtc = (interpreter.query.Absyn.EStmtC) foo;
       render("(");
       render("EStmtC");
       sh(_estmtc.statement_);
       render(")");
    }
  }

  private static void sh(interpreter.query.Absyn.RelOp foo)
  {
    if (foo instanceof interpreter.query.Absyn.RelOpGtC)
    {
       render("RelOpGtC");
    }
    if (foo instanceof interpreter.query.Absyn.RelOpEqC)
    {
       render("RelOpEqC");
    }
    if (foo instanceof interpreter.query.Absyn.RelOpNeC)
    {
       render("RelOpNeC");
    }
    if (foo instanceof interpreter.query.Absyn.RelOpLtC)
    {
       render("RelOpLtC");
    }
    if (foo instanceof interpreter.query.Absyn.RelOpLeC)
    {
       render("RelOpLeC");
    }
    if (foo instanceof interpreter.query.Absyn.RelOpGeC)
    {
       render("RelOpGeC");
    }
  }


  private static void pp(String s, int _i_) { buf_.append(s); buf_.append(" "); }
  private static void sh(String s) { printQuoted(s); }
  private static void printQuoted(String s) { render("\"" + s + "\""); }
  private static void indent()
  {
    int n = _n_;
    while (n > 0)
    {
      buf_.append(" ");
      n--;
    }
  }
  private static void backup()
  {
     if (buf_.charAt(buf_.length() - 1) == ' ') {
      buf_.setLength(buf_.length() - 1);
    }
  }
  private static void trim()
  {
     while (buf_.length() > 0 && buf_.charAt(0) == ' ')
        buf_.deleteCharAt(0); 
    while (buf_.length() > 0 && buf_.charAt(buf_.length()-1) == ' ')
        buf_.deleteCharAt(buf_.length()-1);
  }
  private static int _n_ = 0;
  private static StringBuilder buf_ = new StringBuilder(INITIAL_BUFFER_SIZE);
}

