package frangel.model.expression;

import java.util.Arrays;

import frangel.Settings;
import frangel.model.FunctionData;
import frangel.model.Precedence;
import frangel.utils.Utils;

public class FuncExpression extends Expression {
	public final Expression[] args;
	public final FunctionData data;
	private Expression callee;

	public FuncExpression(Expression[] args, Expression callee, FunctionData data) {
		this.args = args;
		this.callee = callee;
		this.data = data;
        setType(data.returnType);
	}

	@Override
	public Precedence getPrecedence() {
        switch (data.kind) {
			case METHOD:
			case FIELD:
			case ARR_GET:
			case ARR_LEN:
				return Precedence.DOT;
			case CONSTRUCTOR:
				return Precedence.NEW;
			case ARR_SET:
				return Precedence.ASSIGNMENT;
			default:
				return null;
		}
	}

	@Override
	public void toJava(StringBuilder sb) {
		Precedence thisPrecedence = getPrecedence();
		String sep = "";
        switch (data.kind) {
			case METHOD:
				if (callee == null) {
                    sb.append(Utils.getClassName(data.calleeClass));
				} else {
					if (thisPrecedence.parenLeft(callee.getPrecedence())) {
						sb.append('(');
						callee.toJava(sb);
						sb.append(')');
					} else {
						callee.toJava(sb);
					}
				}
				sb.append('.').append(getName()).append('(');
				for (Expression e : args) {
					sb.append(sep);
					e.toJava(sb);
					sep = ", ";
				}
				sb.append(')');
				break;
			case CONSTRUCTOR:
				sb.append("new ").append(getName()).append('(');
				for (Expression e : args) {
					sb.append(sep);
					e.toJava(sb);
					sep = ", ";
				}
				sb.append(')');
				break;
			case FIELD:
                if (data.isStatic) {
                    sb.append(Utils.getClassName(data.calleeClass)).append('.').append(getName());
				} else {
					if (thisPrecedence.parenLeft(callee.getPrecedence())) {
						sb.append('(');
						callee.toJava(sb);
						sb.append(')');
					} else {
						callee.toJava(sb);
					}
					sb.append('.').append(getName());
				}
				break;
			case ARR_GET:
				if (thisPrecedence.parenLeft(args[0].getPrecedence())) {
					sb.append('(');
					args[0].toJava(sb);
					sb.append(')');
				} else {
					args[0].toJava(sb);
				}
				sb.append('[');
				args[1].toJava(sb);
				sb.append(']');
				break;
			case ARR_SET:
				if (thisPrecedence.parenLeft(args[0].getPrecedence())) {
					sb.append('(');
					args[0].toJava(sb);
					sb.append(')');
				} else {
					args[0].toJava(sb);
				}
				sb.append('[');
				args[1].toJava(sb);
				sb.append("] = "); // Only allowed as a Statement
				args[2].toJava(sb);
				break;
			case ARR_LEN:
				if (thisPrecedence.parenLeft(args[0].getPrecedence())) {
					sb.append('(');
					args[0].toJava(sb);
					sb.append(')');
				} else {
					args[0].toJava(sb);
				}
				sb.append(".length");
				break;
		}
	}

	@Override
	public void encode(StringBuilder sb) {
        switch (data.kind) {
			case METHOD:
			case CONSTRUCTOR:
			case FIELD:
				sb.append("f");
				data.encode(sb);
				sb.append(":");
				if (callee != null)
					callee.encode(sb);
				for (Expression e : args)
					e.encode(sb);
				break;
			case ARR_GET:
				sb.append("g");
				args[0].encode(sb);
				args[1].encode(sb);
				break;
			case ARR_SET:
				sb.append("s");
				args[0].encode(sb);
				args[1].encode(sb);
				args[2].encode(sb);
				break;
			case ARR_LEN:
				sb.append("l");
				args[0].encode(sb);
				break;
			default:
                System.err.println("Unknown kind of FunctionData: " + data.kind);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		FuncExpression other = (FuncExpression) obj;
		if (!data.equals(other.data) || !Arrays.equals(args, other.args))
			return false;
		if (callee == null) {
			return other.callee == null;
		} else return callee.equals(other.callee);
	}

	@Override
	public int hashCode() {
		final int prime = 1759;
		int result = 1;
		result = prime * result + (data == null ? 0 : data.hashCode());
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + (callee == null ? 0 : callee.hashCode());
		return prime * result;
	}

	@Override
	public Expression clone() {
		Expression[] newArgs = new Expression[args.length];
		for (int i = 0; i < args.length; i++)
			newArgs[i] = args[i].clone();
		return new FuncExpression(newArgs, callee == null ? null : callee.clone(), data);
	}

	public String getName() {
		return data.name(Settings.USE_SIMPLE_NAME);
	}

	public Expression callee() {
		return callee;
	}

	public void callee(Expression callee) {
		this.callee = callee;
	}

}