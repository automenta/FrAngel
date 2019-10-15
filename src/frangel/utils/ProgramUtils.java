// Contains useful helper functions specifically operating on Programs.

package frangel.utils;

import java.util.*;

import frangel.model.Program;
import frangel.model.expression.*;
import frangel.model.statement.*;

public class ProgramUtils {

    public static List<Expression> getSubExpressions(Expression exp) {
        List<Expression> ans = new ArrayList<>();
        getSubExpressions(exp, ans);
        ans.remove(0); // don't include exp itself
        return ans;
    }

    private static void getSubExpressions(Expression exp, List<Expression> ans) {
        ans.add(exp);
        if (exp instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) exp;
            if (f.callee() != null)
                getSubExpressions(f.callee(), ans);
            for (int i = 0; i < f.args.length; i++)
                getSubExpressions(f.args[i], ans);
        } else if (exp instanceof OpExpression) {
            OpExpression o = (OpExpression) exp;
            if (o.getLeft() != null)
                getSubExpressions(o.getLeft(), ans);
            getSubExpressions(o.getRight(), ans);
        } else if (exp instanceof VarExpression) {
            // nothing else
        } else if (exp instanceof LiteralExpression) {
            // nothing else
        } else {
            System.err.println("Unknown expression class in getSubExpressions()");
        }
    }

    public static List<Statement> getSubStatements(Statement s) {
        List<Statement> ans = new ArrayList<>();
        getSubStatements(s, ans);
        ans.remove(0); // don't include s itself
        return ans;
    }

    private static void getSubStatements(Statement s, List<Statement> ans) {
        ans.add(s);
        if (s instanceof IfStatement) {
            for (Statement inner : ((IfStatement) s).body)
                getSubStatements(inner, ans);
        } else if (s instanceof ForLoop) {
            for (Statement inner : ((ForLoop) s).body)
                getSubStatements(inner, ans);
        } else if (s instanceof ForEachLoop) {
            for (Statement inner : ((ForEachLoop) s).body)
                getSubStatements(inner, ans);
        } else if (s instanceof VarAssignment || s instanceof FuncStatement) {
            // nothing else
        } else {
            System.err.println("Unknown statement class in getSubStatements()");
        }
    }

    public static void getFragments(Program p, List<Expression> expressions, List<Statement> statements) {
        for (Statement s : p.getStatements())
            getFragments(s, expressions, statements);
        if (p.returns())
            getFragments(p.getReturnVal(), expressions);
    }

    private static void getFragments(Statement s, List<Expression> expressions, List<Statement> statements) {
        statements.add(s);
        if (s instanceof VarAssignment) {
            getFragments(((VarAssignment) s).getValue(), expressions);
        } else if (s instanceof FuncStatement) {
            getFragments(((FuncStatement) s).func, expressions); // repetitive
        } else if (s instanceof IfStatement) {
            for (Statement inner : ((IfStatement) s).body)
                getFragments(inner, expressions, statements);
        } else if (s instanceof ForLoop) {
            for (Statement inner : ((ForLoop) s).body)
                getFragments(inner, expressions, statements);
        } else if (s instanceof ForEachLoop) {
            //getFragments(((ForEachLoop) s).getContainer(), expressions); // This doesn't work with map.keySet()
            for (Statement inner : ((ForEachLoop) s).body)
                getFragments(inner, expressions, statements);
        } else {
            System.err.println("Unknown statement class in getFragments()");
        }
    }

    private static void getFragments(Expression exp, List<Expression> expressions) {
        expressions.add(exp);
        if (exp instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) exp;
            if (f.callee() != null)
                getFragments(f.callee(), expressions);
            for (int i = 0; i < f.args.length; i++)
                getFragments(f.args[i], expressions);
        } else if (exp instanceof OpExpression) {
            OpExpression o = (OpExpression) exp;
            if (o.getLeft() != null)
                getFragments(o.getLeft(), expressions);
            getFragments(o.getRight(), expressions);
        } else if (exp instanceof VarExpression) {
            // nothing else
        } else if (exp instanceof LiteralExpression) {
            // nothing else
        } else {
            System.err.println("Unknown expression class in getFragments()");
        }
    }

    public static Set<String> getUsedVars(Program p) {
        return getUsedVars(p, true);
    }

    public static Set<String> getUsedVars(Program p, boolean includeLoopCounter) {
        Set<String> usedVars = new HashSet<>();
        for (Statement s : p.getStatements())
            getUsedVars(s, usedVars, includeLoopCounter);
        if (p.returns())
            getUsedVars(p.getReturnVal(), usedVars);
        return usedVars;
    }

    public static void getUsedVars(Statement s, Set<String> usedVars, boolean includeLoopCounter) {
        if (s instanceof VarAssignment) {
            VarAssignment v = (VarAssignment) s;
            getUsedVars(v.var, usedVars);
            getUsedVars(v.getValue(), usedVars);
        } else if (s instanceof FuncStatement) {
            getUsedVars(((FuncStatement) s).func, usedVars);
        } else if (s instanceof IfStatement) {
            IfStatement i = (IfStatement) s;
            if (!i.isAngelic())
                getUsedVars(i.getCondition(), usedVars);
            for (Statement b : i.body)
                getUsedVars(b, usedVars, includeLoopCounter);
        } else if (s instanceof ForLoop) {
            ForLoop f = (ForLoop) s;
            if (!f.isAngelic())
                getUsedVars(f.getCondition(), usedVars);
            for (Statement b : f.body)
                getUsedVars(b, usedVars, includeLoopCounter);
            if (includeLoopCounter && !f.isWhileLoop())
                usedVars.add(f.getVarName());
        } else if (s instanceof ForEachLoop) {
            ForEachLoop f = (ForEachLoop) s;
            getUsedVars(f.container, usedVars);
            for (Statement b : f.body)
                getUsedVars(b, usedVars, includeLoopCounter);
            usedVars.add(f.getVarName());
        } else {
            System.err.println("Unknown Statement class in getUsedVars()");
        }
    }

    private static void getUsedVars(Expression e, Set<String> usedVars) {
        if (e instanceof LiteralExpression) {
            // do nothing
        } else if (e instanceof VarExpression) {
            usedVars.add(((VarExpression) e).getName());
        } else if (e instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) e;
            if (!f.data.isStatic)
                getUsedVars(f.callee(), usedVars);
            for (Expression a : f.args)
                getUsedVars(a, usedVars);
        } else if (e instanceof OpExpression) {
            OpExpression o = (OpExpression) e;
            if (o.getLeft() != null)
                getUsedVars(o.getLeft(), usedVars);
            getUsedVars(o.getRight(), usedVars);
        } else {
            System.err.println("Unknown Expression class in getUsedVars()");
        }
    }

    public static boolean containsVar(Expression e) {
        if (e instanceof LiteralExpression) {
            return false;
        } else if (e instanceof VarExpression) {
            return true;
        } else if (e instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) e;
            if (!f.data.isStatic)
                if (containsVar(f.callee()))
                    return true;
            for (Expression a : f.args)
                if (containsVar(a))
                    return true;
            return false;
        } else if (e instanceof OpExpression) {
            OpExpression o = (OpExpression) e;
            if (o.getLeft() != null)
                if (containsVar(o.getLeft()))
                    return true;
            return containsVar(o.getRight());
        } else {
            System.err.println("Unknown Expression class in getUsedVars()");
            return false;
        }
    }

    public static void replaceVars(Program p, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();
            Utils.replaceKeyIfPresent(p.getVariables(), oldName, newName);
            Utils.replaceKeyIfPresent(p.getLocalVars(), oldName, newName);
            Utils.replaceIfPresent(p.getLoopVars(), oldName, newName);
            Utils.replaceIfPresent(p.getLoopVarsDeclaredInLoop(), oldName, newName);
            Utils.replaceIfPresent(p.getElemVars(), oldName, newName);
            Utils.replaceIfPresent(p.getInScope(), oldName, newName);
            // Not updating typeToVars (typeToVars isn't relevant anymore)
        }
        replaceVars(p.getStatements(), replacements);
        if (p.returns())
            replaceVars(p.getReturnVal(), replacements);
    }

    private static void replaceVars(List<Statement> block, Map<String, String> replacements) {
        for (Statement s : block)
            replaceVars(s, replacements);
    }

    private static void replaceVars(Statement s, Map<String, String> replacements) {
        if (s instanceof VarAssignment) {
            VarAssignment v = (VarAssignment) s;
            replaceVars(v.var, replacements);
            replaceVars(v.getValue(), replacements);
        } else if (s instanceof FuncStatement) {
            replaceVars(((FuncStatement) s).func, replacements);
        } else if (s instanceof ForLoop) {
            ForLoop f = (ForLoop) s;
            String name = f.getVarName();
            if (replacements.containsKey(name))
                f.setVarName(replacements.get(name));
            if (!f.isAngelic())
                replaceVars(f.getCondition(), replacements);
            if (f.getRememberedCondition() != null)
                replaceVars(f.getRememberedCondition(), replacements);
            replaceVars(f.body, replacements);
        } else if (s instanceof IfStatement) {
            IfStatement is = (IfStatement) s;
            if (!is.isAngelic())
                replaceVars(is.getCondition(), replacements);
            if (is.getRememberedCondition() != null)
                replaceVars(is.getRememberedCondition(), replacements);
            replaceVars(is.body, replacements);
        } else if (s instanceof ForEachLoop) {
            ForEachLoop f = (ForEachLoop) s;
            String name = f.getVarName();
            if (replacements.containsKey(name))
                f.setVarName(replacements.get(name));
            replaceVars(f.body, replacements);
            replaceVars(f.container, replacements);
        } else {
            System.err.println("Unknown statement type in replaceVars: " + s.getClass().getName());
        }
    }

    private static void replaceVars(Expression e, Map<String, String> replacements) {
        if (e instanceof LiteralExpression) {
            // do nothing
        } else if (e instanceof VarExpression) {
            VarExpression v = (VarExpression) e;
            String name = v.getName();
            if (replacements.containsKey(name))
                v.setName(replacements.get(name));
        } else if (e instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) e;
            if (!f.data.isStatic)
                replaceVars(f.callee(), replacements);
            for (Expression a : f.args)
                replaceVars(a, replacements);
        } else if (e instanceof OpExpression) {
            OpExpression o = (OpExpression) e;
            if (o.getLeft() != null)
                replaceVars(o.getLeft(), replacements);
            replaceVars(o.getRight(), replacements);
        } else {
            System.err.println("Unknown expression type in replaceVars: " + e.getClass().getName());
        }
    }

    public static void makeVarsCompatible(Expression e, Map<String, String> replacements, Program p) {
        if (e instanceof LiteralExpression) {
            // do nothing
        } else if (e instanceof VarExpression) {
            VarExpression v = (VarExpression) e;
            String name = v.getName();
            String replacementName = replacements.get(name);
            if (replacementName != null) {
                v.setName(replacementName);
            } else {
                Class<?> type = v.getType();
                Class<?> oldType = p.getVariables().get(name);
                if (oldType == null || (oldType != null && !type.equals(oldType))) {
                    List<String> sameType = new ArrayList<>();
                    for (Map.Entry<String, Class<?>> entry : p.getVariables().entrySet())
                        if (entry.getValue().equals(type))
                            sameType.add(entry.getKey());
                    if (sameType.isEmpty()) {
                        replacementName = p.getFreshLocalVar();
                        v.setName(replacementName);
                        p.addLocalVar(replacementName, type);
                        p.addToScope(replacementName);
                        replacements.put(name, replacementName);
                    } else {
                        replacementName = Utils.randElement(sameType);
                        v.setName(replacementName);
                        replacements.put(name, replacementName);
                    }
                }
            }
        } else if (e instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) e;
            if (!f.data.isStatic)
                makeVarsCompatible(f.callee(), replacements, p);
            for (Expression a : f.args)
                makeVarsCompatible(a, replacements, p);
        } else if (e instanceof OpExpression) {
            OpExpression o = (OpExpression) e;
            if (o.getLeft() != null)
                makeVarsCompatible(o.getLeft(), replacements, p);
            makeVarsCompatible(o.getRight(), replacements, p);
        } else {
            System.err.println("Unknown expression type in makeVarsCompatible: " + e.getClass().getName());
        }
    }

    public static void makeVarsCompatible(Statement s, Map<String, String> replacements, Program p) {
        if (s instanceof VarAssignment) {
            VarAssignment v = (VarAssignment) s;
            makeVarsCompatible(v.var, replacements, p);
            makeVarsCompatible(v.getValue(), replacements, p);
        } else if (s instanceof FuncStatement) {
            makeVarsCompatible(((FuncStatement) s).func, replacements, p);
        } else if (s instanceof ForLoop) {
            ForLoop f = (ForLoop) s;
            String name = f.getVarName();
            if (p.getVariables().containsKey(name)) {
                String replacementName = p.getFreshLoopVar();
                f.setVarName(replacementName);
                p.addLoopVar(replacementName);
                replacements.put(name, replacementName);
            } else {
                p.addLoopVar(name);
            }
            if (!f.isAngelic())
                makeVarsCompatible(f.getCondition(), replacements, p);
            if (f.getRememberedCondition() != null)
                makeVarsCompatible(f.getRememberedCondition(), replacements, p);
            for (Statement inner : f.body)
                makeVarsCompatible(inner, replacements, p);
        } else if (s instanceof IfStatement) {
            IfStatement is = (IfStatement) s;
            if (!is.isAngelic())
                makeVarsCompatible(is.getCondition(), replacements, p);
            if (is.getRememberedCondition() != null)
                makeVarsCompatible(is.getRememberedCondition(), replacements, p);
            for (Statement inner : is.body)
                makeVarsCompatible(inner, replacements, p);
        } else if (s instanceof ForEachLoop) {
            ForEachLoop f = (ForEachLoop) s;
            String name = f.getVarName();
            if (p.getVariables().containsKey(name)) {
                String replacementName = p.getFreshElemVar();
                f.setVarName(replacementName);
                p.addElemVar(replacementName, f.varType);
                replacements.put(name, replacementName);
            } else {
                p.addElemVar(name, f.varType);
            }
            makeVarsCompatible(f.container, replacements, p);
            for (Statement inner : f.body)
                makeVarsCompatible(inner, replacements, p);
        } else {
            System.err.println("Unknown statement type in makeVarsCompatible: " + s.getClass().getName());
        }
    }

    public static int size(Program p) {
        int sum = p.getLocalVars().size(); // Local vars are initialized to default values (don't count size for initial values)
        for (Statement s : p.getStatements())
            sum += size(s);
        if (p.returns())
            sum += size(p.getReturnVal());
        return sum;
    }

    public static int size(Statement s) {
        if (s instanceof VarAssignment) {
            return 2 + size(((VarAssignment) s).getValue()); // 1 for equals sign, 1 for variable name
        } else if (s instanceof FuncStatement) {
            // FuncStatement is just a wrapper around FuncExpression, don't count the FuncStatement
            return size(((FuncStatement) s).func);
        } else if (s instanceof ForLoop) {
            ForLoop f = (ForLoop) s;
            int size = 1;
            if (!f.isAngelic())
                size += size(f.getCondition());
            for (Statement b : f.body)
                size += size(b);
            return size;
        } else if (s instanceof ForEachLoop) {
            ForEachLoop f = (ForEachLoop) s;
            int size = 1;
            size += size(f.container);
            for (Statement b : f.body)
                size += size(b);
            return size;
        } else if (s instanceof IfStatement) {
            IfStatement i = (IfStatement) s;
            int size = 1;
            if (!i.isAngelic())
                size += size(i.getCondition());
            for (Statement b : i.body)
                size += size(b);
            return size;
        } else {
            System.err.println("Unknown statement class in size()");
            return 0;
        }
    }

    public static int size(Expression exp) {
        if (exp instanceof LiteralExpression) {
            return 1;
        } else if (exp instanceof VarExpression) {
            return 1;
        } else if (exp instanceof OpExpression) {
            OpExpression o = (OpExpression) exp;
            return 1 + (o.getLeft() == null ? 0 : size(o.getLeft())) + size(o.getRight());
        } else if (exp instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) exp;
            int size = 1;
            if (f.callee() != null)
                size += size(f.callee());
            for (Expression a : f.args)
                size += size(a);
            return size;
        } else {
            System.err.println("Unknown expression class in size()");
            return 0;
        }
    }

    public static double computeUsefulness(Expression fragment, Program program) {
        int maxMatch = 0;
        for (Statement s : program.getStatements())
            maxMatch = Math.max(maxMatch, searchMatch(fragment, s));
        if (program.returns())
            maxMatch = Math.max(maxMatch, searchMatch(fragment, program.getReturnVal()));
        return maxMatch / (double) size(fragment);
    }

    public static double computeUsefulness(Statement fragment, Program program) {
        int maxMatch = 0;
        for (Statement s : program.getStatements())
            maxMatch = Math.max(maxMatch, searchMatch(fragment, s));
        return maxMatch / (double) size(fragment);
    }

    private static int searchMatch(Statement fragment, Statement s) {
        int max = countMatch(fragment, s);
        if (s instanceof VarAssignment || s instanceof FuncStatement) {
            // nothing else
        } else if (s instanceof ForLoop) {
            for (Statement b : ((ForLoop) s).body)
                max = Math.max(max, searchMatch(fragment, b));
        } else if (s instanceof ForEachLoop) {
            for (Statement b : ((ForEachLoop) s).body)
                max = Math.max(max, searchMatch(fragment, b));
        } else if (s instanceof IfStatement) {
            for (Statement b : ((IfStatement) s).body)
                max = Math.max(max, searchMatch(fragment, b));
            return max;
        } else {
            System.err.println("Unknown statement class in searchMatch(Statement, Statement)");
            return 0;
        }
        return max;
    }

    private static int searchMatch(Expression fragment, Statement s) {
        if (s instanceof VarAssignment) {
            return Math.max(searchMatch(fragment, ((VarAssignment) s).var), searchMatch(fragment, ((VarAssignment) s).getValue()));
        } else if (s instanceof FuncStatement) {
            return searchMatch(fragment, ((FuncStatement) s).func);
        } else if (s instanceof ForLoop) {
            int max = 0;
            ForLoop f = (ForLoop) s;
            if (!f.isAngelic())
                max = Math.max(max, searchMatch(fragment, f.getCondition()));
            for (Statement b : f.body)
                max = Math.max(max, searchMatch(fragment, b));
            return max;
        } else if (s instanceof ForEachLoop) {
            ForEachLoop f = (ForEachLoop) s;
            int max = searchMatch(fragment, f.container);
            for (Statement b : f.body)
                max = Math.max(max, searchMatch(fragment, b));
            return max;
        } else if (s instanceof IfStatement) {
            int max = 0;
            IfStatement i = (IfStatement) s;
            if (!i.isAngelic())
                max = Math.max(max, searchMatch(fragment, i.getCondition()));
            for (Statement b : i.body)
                max = Math.max(max, searchMatch(fragment, b));
            return max;
        } else {
            System.err.println("Unknown statement class in searchMatch(Expression, Statement)");
            return 0;
        }
    }

    private static int searchMatch(Expression fragment, Expression e) {
        int max = countMatch(fragment, e);
        if (e instanceof LiteralExpression || e instanceof VarExpression) {
            // nothing else
        } else if (e instanceof FuncExpression) {
            FuncExpression f = (FuncExpression) e;
            if (!f.data.isStatic)
                max = Math.max(max, searchMatch(fragment, f.callee()));
            for (Expression e2 : f.args)
                max = Math.max(max, searchMatch(fragment, e2));
        } else if (e instanceof OpExpression) {
            OpExpression o = (OpExpression) e;
            if (o.getLeft() != null)
                max = Math.max(max, searchMatch(fragment, o.getLeft()));
            max = Math.max(max, searchMatch(fragment, o.getRight()));
        } else {
            System.err.println("Unknown expression class in searchMatch(Expression, Expression)");
        }
        return max;
    }

    private static int countMatch(Statement fragment, Statement s) {
        if (!fragment.getClass().equals(s.getClass()))
            return 0;
        int sum = 0;
        if (s instanceof VarAssignment) {
            VarAssignment vp = (VarAssignment) fragment;
            VarAssignment vs = (VarAssignment) s;
            return 1 + countMatch(vp.var, vs.var) + countMatch(vp.getValue(), vs.getValue());
        } else if (s instanceof FuncStatement) {
            return countMatch(((FuncStatement) fragment).func, ((FuncStatement) s).func);
        } else if (s instanceof ForLoop) {
            sum++;
            ForLoop fp = (ForLoop) fragment;
            ForLoop fs = (ForLoop) s;
            if (!fp.isAngelic() && !fs.isAngelic())
                sum += countMatch(fp.getCondition(), fs.getCondition());
            for (int i = 0; i < fp.body.size() && i < fs.body.size(); i++)
                sum += countMatch(fp.body.get(i), fs.body.get(i));
        } else if (s instanceof ForEachLoop) {
            sum++;
            ForEachLoop fp = (ForEachLoop) fragment;
            ForEachLoop fs = (ForEachLoop) s;
            sum += countMatch(fp.container, fs.container);
            for (int i = 0; i < fp.body.size() && i < fs.body.size(); i++)
                sum += countMatch(fp.body.get(i), fs.body.get(i));
        } else if (s instanceof IfStatement) {
            sum++;
            IfStatement ip = (IfStatement) fragment;
            IfStatement is = (IfStatement) s;
            if (!ip.isAngelic() && !is.isAngelic())
                sum += countMatch(ip.getCondition(), is.getCondition());
            for (int i = 0; i < ip.body.size() && i < is.body.size(); i++)
                sum += countMatch(ip.body.get(i), is.body.get(i));
        } else {
            System.err.println("Unknown statement class in countMatch(Statement, Statement)");
            return 0;
        }
        return sum;
    }

    private static int countMatch(Expression fragment, Expression e) {
        if (!fragment.getClass().equals(e.getClass()))
            return 0;
        int sum = 0;
        if (e instanceof LiteralExpression || e instanceof VarExpression) {
            return fragment.toJava().equals(e.toJava()) ? 1 : 0;
        } else if (e instanceof FuncExpression) {
            FuncExpression fp = (FuncExpression) fragment;
            FuncExpression fe = (FuncExpression) e;
            if (!fp.data.equals(fe.data))
                return 0;
            sum++;
            if (!fe.data.isStatic)
                sum += countMatch(fp.callee(), fe.callee());
            for (int i = 0; i < fe.args.length; i++)
                sum += countMatch(fp.args[i], fe.args[i]);
        } else if (e instanceof OpExpression) {
            OpExpression op = (OpExpression) fragment;
            OpExpression oe = (OpExpression) e;
            if (op.op() != oe.op())
                return 0;
            sum++;
            if (oe.getLeft() != null)
                sum += countMatch(op.getLeft(), oe.getLeft());
            sum += countMatch(op.getRight(), oe.getRight());
        } else {
            System.err.println("Unknown expression class in countMatch(Expression, Expression)");
        }
        return sum;
    }

    public static int numAngelic(Program p) {
        return numAngelic(p.getStatements());
    }

    private static int numAngelic(List<Statement> list) {
        int num = 0;
        for (Statement s : list)
            num += numAngelic(s);
        return num;
    }

    private static int numAngelic(Statement s) {
        if (s instanceof ForLoop) {
            ForLoop f = (ForLoop) s;
            return numAngelic(f.body) + (f.isAngelic() ? 1 : 0);
        } else if (s instanceof IfStatement) {
            IfStatement i = (IfStatement) s;
            return numAngelic(i.body) + (i.isAngelic() ? 1 : 0);
        } else if (s instanceof ForEachLoop) {
            return numAngelic(((ForEachLoop) s).body);
        }
        return 0;
    }

    public static void resetIndents(Program p) {
        resetIndents(p.getStatements(), 1);
    }

    private static void resetIndents(List<Statement> list, int indent) {
        for (Statement s : list)
            resetIndents(s, indent);
    }

    private static void resetIndents(Statement s, int indent) {
        s.setIndent(indent);
        if (s instanceof ForLoop)
            resetIndents(((ForLoop) s).body, indent + 1);
        else if (s instanceof IfStatement)
            resetIndents(((IfStatement) s).body, indent + 1);
        else if (s instanceof ForEachLoop)
            resetIndents(((ForEachLoop) s).body, indent + 1);
    }
}
