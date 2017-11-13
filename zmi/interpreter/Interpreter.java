/**
 * Copyright (c) 2014, University of Warsaw
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import interpreter.query.PrettyPrinter;
import interpreter.query.Absyn.AliasedSelItemC;
import interpreter.query.Absyn.AscOrderC;
import interpreter.query.Absyn.BasicExpr;
import interpreter.query.Absyn.BasicExprAddC;
import interpreter.query.Absyn.BasicExprDivC;
import interpreter.query.Absyn.BasicExprModC;
import interpreter.query.Absyn.BasicExprMulC;
import interpreter.query.Absyn.BasicExprNegC;
import interpreter.query.Absyn.BasicExprSubC;
import interpreter.query.Absyn.BoolExpr;
import interpreter.query.Absyn.BoolExprBasicExprC;
import interpreter.query.Absyn.BoolExprCmpC;
import interpreter.query.Absyn.BoolExprRegExpC;
import interpreter.query.Absyn.CondExpr;
import interpreter.query.Absyn.CondExprAndC;
import interpreter.query.Absyn.CondExprBoolExprC;
import interpreter.query.Absyn.CondExprNotC;
import interpreter.query.Absyn.CondExprOrC;
import interpreter.query.Absyn.DescOrderC;
import interpreter.query.Absyn.EBoolC;
import interpreter.query.Absyn.ECondExprC;
import interpreter.query.Absyn.EDblC;
import interpreter.query.Absyn.EFunC;
import interpreter.query.Absyn.EIdentC;
import interpreter.query.Absyn.EIntC;
import interpreter.query.Absyn.EStmtC;
import interpreter.query.Absyn.EStrC;
import interpreter.query.Absyn.NoNullsC;
import interpreter.query.Absyn.NoOrderByC;
import interpreter.query.Absyn.NoOrderC;
import interpreter.query.Absyn.NoWhereC;
import interpreter.query.Absyn.NullFirstsC;
import interpreter.query.Absyn.Nulls;
import interpreter.query.Absyn.NullsLastC;
import interpreter.query.Absyn.Order;
import interpreter.query.Absyn.OrderBy;
import interpreter.query.Absyn.OrderByC;
import interpreter.query.Absyn.OrderItem;
import interpreter.query.Absyn.OrderItemC;
import interpreter.query.Absyn.Program;
import interpreter.query.Absyn.ProgramC;
import interpreter.query.Absyn.RelOp;
import interpreter.query.Absyn.RelOpEqC;
import interpreter.query.Absyn.RelOpGeC;
import interpreter.query.Absyn.RelOpGtC;
import interpreter.query.Absyn.RelOpLeC;
import interpreter.query.Absyn.RelOpLtC;
import interpreter.query.Absyn.RelOpNeC;
import interpreter.query.Absyn.SelItem;
import interpreter.query.Absyn.SelItemC;
import interpreter.query.Absyn.Statement;
import interpreter.query.Absyn.StatementC;
import interpreter.query.Absyn.Where;
import interpreter.query.Absyn.WhereC;
import model.Attribute;
import model.TypePrimitive;
import model.Value;
import model.ValueBoolean;
import model.ValueDouble;
import model.ValueInt;
import model.ValueString;
import model.ZMI;

public class Interpreter {
	private static class ValuesPair {
		public final Result left;
		public final Result right;

		public ValuesPair(Result left, Result right) {
			this.left = left;
			this.right = right;
		}
	}

	private final ZMI zmi;

	public Interpreter(ZMI zmi) {
		this.zmi = zmi;
	}

	private static Boolean getBoolean(Value value) {
		if(value.getType().isCompatible(TypePrimitive.BOOLEAN)) {
			Boolean b = ((ValueBoolean)value).getValue();
			return b == null? false : b.booleanValue();
		}
		throw new InvalidTypeException(TypePrimitive.BOOLEAN, value.getType());
	}

	public List<QueryResult> interpretProgram(Program program) {
		return program.accept(new ProgramInterpreter(), zmi);
	}

	public List<QueryResult> interpretStatement(Statement statement) {
		return statement.accept(new StatementInterpreter(), zmi);
	}

	public class ProgramInterpreter implements Program.Visitor<List<QueryResult>, ZMI> {
		public List<QueryResult> visit(ProgramC program, ZMI zmi) {
			List<QueryResult> results = new ArrayList<QueryResult>();
			for(Statement s : program.liststatement_) {
				try {
					List<QueryResult> l = s.accept(new StatementInterpreter(), zmi);
					for(QueryResult qr : l)
						if(qr.getName() == null)
							throw new IllegalArgumentException("All items in top-level SELECT must be aliased.");
					results.addAll(l);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(s), exception);
				}
			}
			return results;
		}
	}

	public class StatementInterpreter implements Statement.Visitor<List<QueryResult>, ZMI> {
		public List<QueryResult> visit(StatementC statement, ZMI zmi) {
			Table table = new Table(zmi);
			try {
				table = statement.where_.accept(new WhereInterpreter(), table);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(statement.where_), exception);
			}
			try {
				table = statement.orderby_.accept(new OrderByInterpreter(), table);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(statement.orderby_), exception);
			}
			List<QueryResult> ret = new ArrayList<QueryResult>();

			for(SelItem selItem : statement.listselitem_) {
				try {
					QueryResult qr = selItem.accept(new SelItemInterpreter(), table);
					if(qr.getName() != null) {
						for(QueryResult qrRet : ret)
							if(qr.getName().getName().equals(qrRet.getName().getName()))
								throw new IllegalArgumentException("Alias collision.");
					}
					ret.add(qr);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(selItem), exception);
				}
			}

			return ret;
		}
	}

	public class WhereInterpreter implements Where.Visitor<Table, Table> {
		public Table visit(NoWhereC where, Table table) {
			return table;
		}

		public Table visit(WhereC where, Table table) {
			Table result = new Table(table);
			for(TableRow row : table) {
				Environment env = new Environment(row, table.getColumns());
				Value value = where.condexpr_.accept(new CondExprInterpreter(), env).getValue();
				if(getBoolean(value))
					result.appendRow(row);
			}
			return result;
		}
	}

	public class OrderByInterpreter implements OrderBy.Visitor<Table, Table> {
		public Table visit(NoOrderByC orderBy, Table table) {
			return table;
		}

		public Table visit(OrderByC orderBy, Table table) {
			for(OrderItem item : orderBy.listorderitem_) {
				try {
					table = item.accept(new OrderItemInterpreter(), table);
				} catch(Exception exception) {
					throw new InsideQueryException(PrettyPrinter.print(item), exception);
				}
			}
			return table;
		}
	}

	public class OrderItemInterpreter implements OrderItem.Visitor<Table, Table> {
		public Table visit(final OrderItemC orderItem, final Table table) {
			Comparator<TableRow> comparator = new Comparator<TableRow>() {
				@Override
				public int compare(TableRow row1, TableRow row2) {
					Environment env1 = new Environment(row1, table.getColumns());
					Result expr1 = orderItem.condexpr_.accept(new CondExprInterpreter(), env1);
					Environment env2 = new Environment(row2, table.getColumns());
					Result expr2 = orderItem.condexpr_.accept(new CondExprInterpreter(), env2);
					ValuesPair pair = new ValuesPair(expr1, expr2);
					int result = orderItem.nulls_.accept(new NullsInterpreter(), pair);
					if(result == 0)
						result = orderItem.order_.accept(new OrderInterpreter(), pair);
					return result;
				}
			};
			table.sort(comparator);
			return table;
		}
	}

	public class OrderInterpreter implements Order.Visitor<Integer, ValuesPair> {
		private int compareAsc(ValuesPair pair) {
			if(getBoolean(pair.left.isEqual(pair.right).getValue()))
				return 0;
			if(getBoolean(pair.left.isLowerThan(pair.right).getValue()))
				return -1;
			return 1;
		}

		public Integer visit(AscOrderC order, ValuesPair pair) {
			return compareAsc(pair);
		}

		public Integer visit(DescOrderC order, ValuesPair pair) {
			return -compareAsc(pair);
		}

		public Integer visit(NoOrderC order, ValuesPair pair) {
			return compareAsc(pair);
		}
	}

	public class NullsInterpreter implements Nulls.Visitor<Integer, ValuesPair> {
		private Integer nullsFirst(ValuesPair pair) {
			if(pair.left.getValue().isNull()) {
				if(pair.right.getValue().isNull())
					return 0;
				return -1;
			}
			if(pair.right.getValue().isNull())
				return 1;
			return 0;
		}

		public Integer visit(NoNullsC nulls, ValuesPair pair) {
			return nullsFirst(pair);
		}

		public Integer visit(NullFirstsC nulls, ValuesPair pair) {
			return nullsFirst(pair);
		}

		public Integer visit(NullsLastC nulls, ValuesPair pair) {
			return -nullsFirst(pair);
		}
	}

	public class SelItemInterpreter implements SelItem.Visitor<QueryResult, Table> {
		public QueryResult visit(SelItemC selItem, Table table) {
			Environment env = new Environment(table.iterator().next(), table.getColumns()); // TODO
			Result result = selItem.condexpr_.accept(new CondExprInterpreter(), env);
			return new QueryResult(result.getValue());
		}

		public QueryResult visit(AliasedSelItemC selItem, Table table) {
			Environment env = new Environment(table.iterator().next(), table.getColumns()); // TODO
			Result result = selItem.condexpr_.accept(new CondExprInterpreter(), env);
			return new QueryResult(new Attribute(selItem.qident_), result.getValue());
		}
	}

	public class BoolExprInterpreter implements BoolExpr.Visitor<Result, Environment> {
		public Result visit(BoolExprCmpC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return expr.relop_.accept(new RelOpInterpreter(), new ValuesPair(left, right));
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BoolExprRegExpC expr, Environment env) {
			try {
				Result left = expr.basicexpr_.accept(new BasicExprInterpreter(), env);
				return (new ResultSingle(new ValueString(expr.string_))).regExpr(left);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BoolExprBasicExprC expr, Environment env) {
			return expr.basicexpr_.accept(new BasicExprInterpreter(), env);
		}
	}

	public class CondExprInterpreter implements CondExpr.Visitor<Result, Environment> {
		public Result visit(CondExprOrC expr, Environment env) {
			try {
				Result left = expr.condexpr_1.accept(new CondExprInterpreter(), env);
				Result right = expr.condexpr_2.accept(new CondExprInterpreter(), env);
				return left.or(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(CondExprAndC expr, Environment env) {
			try {
				Result left = expr.condexpr_1.accept(new CondExprInterpreter(), env);
				Result right = expr.condexpr_2.accept(new CondExprInterpreter(), env);
				return left.and(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(CondExprNotC expr, Environment env) {
			try {
				return expr.condexpr_.accept(new CondExprInterpreter(), env).negate();
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(CondExprBoolExprC expr, Environment env) {
			return expr.boolexpr_.accept(new BoolExprInterpreter(), env);
		}
	}

	public class BasicExprInterpreter implements BasicExpr.Visitor<Result, Environment> {
		public Result visit(BasicExprAddC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return left.addValue(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BasicExprSubC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return left.subtract(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BasicExprMulC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return left.multiply(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BasicExprDivC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return left.divide(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BasicExprModC expr, Environment env) {
			try {
				Result left = expr.basicexpr_1.accept(new BasicExprInterpreter(), env);
				Result right = expr.basicexpr_2.accept(new BasicExprInterpreter(), env);
				return left.modulo(right);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(BasicExprNegC expr, Environment env) {
			try {
				return expr.basicexpr_.accept(new BasicExprInterpreter(), env).negate();
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public Result visit(EIdentC expr, Environment env) {
			return env.getIdent(expr.qident_);
		}

		public Result visit(EFunC expr, Environment env) {
			try {
				List<Result> arguments = new ArrayList<Result>(expr.listcondexpr_.size());
				for(CondExpr arg : expr.listcondexpr_)
					arguments.add(arg.accept(new CondExprInterpreter(), env));

				return Functions.getInstance().evaluate(expr.qident_, arguments);
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}

		public ResultSingle visit(EStrC expr, Environment env) {
			return new ResultSingle(new ValueString(expr.string_));
		}

		public ResultSingle visit(EBoolC expr, Environment env) {
			ValueBoolean value;
			if(expr.qbool_.compareTo("true") == 0)
				value = new ValueBoolean(true);
			else if(expr.qbool_.compareTo("false") == 0)
				value = new ValueBoolean(false);
			else
				throw new InternalInterpreterException("Incorrect boolean constant: " + PrettyPrinter.print(expr));
			return new ResultSingle(value);
		}

		public ResultSingle visit(EIntC expr, Environment env) {
			try {
				return new ResultSingle(new ValueInt(Long.parseLong(expr.qinteger_)));
			} catch(NumberFormatException exception) {
				throw new InternalInterpreterException(exception.getMessage());
			}
		}

		public ResultSingle visit(EDblC expr, Environment env) {
			try {
				return new ResultSingle(new ValueDouble(Double.parseDouble(expr.qdouble_)));
			} catch(NumberFormatException exception) {
				throw new InternalInterpreterException(exception.getMessage());
			}
		}

		public Result visit(ECondExprC expr, Environment env) {
			return expr.condexpr_.accept(new CondExprInterpreter(), env);
		}

		public ResultSingle visit(EStmtC expr, Environment env) {
			try {
				List<QueryResult> l = expr.statement_.accept(new StatementInterpreter(), zmi);
				if(l.size() != 1)
					throw new IllegalArgumentException("Nested queries must SELECT exactly one item.");
				return new ResultSingle(l.get(0).getValue());
			} catch(Exception exception) {
				throw new InsideQueryException(PrettyPrinter.print(expr), exception);
			}
		}
	}

	public class RelOpInterpreter implements RelOp.Visitor<Result, ValuesPair> {
		public Result visit(RelOpGtC op, ValuesPair pair) {
			// TODO - may be better implemented as: pair.right.isLowerThan(pair.left).negate()
			// Unless we can't do that safely for some reason
			return pair.left.isLowerThan(pair.right).negate().and(pair.left.isEqual(pair.right).negate());
		}

		public Result visit(RelOpEqC op, ValuesPair pair) {
			return pair.left.isEqual(pair.right);
		}

		public Result visit(RelOpNeC op, ValuesPair pair) {
			return pair.left.isEqual(pair.right).negate();
		}

		public Result visit(RelOpLtC op, ValuesPair pair) {
			return pair.left.isLowerThan(pair.right);
		}

		public Result visit(RelOpLeC op, ValuesPair pair) {
			return pair.left.isLowerThan(pair.right).or(pair.left.isEqual(pair.right));
		}

		public Result visit(RelOpGeC op, ValuesPair pair) {
			return pair.left.isLowerThan(pair.right).negate();
		}
	}
}
