package com.arizen.launcher.tools;

import android.content.Context;

public class CalculatorTool implements ArizenTool {
    private final Context context;
    public CalculatorTool(Context context) { this.context = context; }

    @Override
    public void execute(String params) {
        try {
            double result = evaluate(params.trim());
            context.getSharedPreferences("arizen_calc", Context.MODE_PRIVATE).edit()
                .putString("result", params + " = " + formatResult(result))
                .apply();
        } catch (Exception e) {
            context.getSharedPreferences("arizen_calc", Context.MODE_PRIVATE).edit()
                .putString("result", "Error: " + e.getMessage())
                .apply();
        }
    }

    private String formatResult(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long)v);
        return String.format("%.6f", v).replaceAll("0+$","").replaceAll("\\.$","");
    }

    // Simple expression evaluator (handles +,-,*,/,^,parentheses)
    private int pos = -1, ch;
    private String expr;

    private double evaluate(String expression) {
        this.expr = expression.replaceAll("\\s+","").replaceAll(",",".");
        this.pos = -1;
        nextChar();
        double x = parseExpression();
        if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char)ch);
        return x;
    }

    private void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) { nextChar(); return true; }
        return false;
    }

    private double parseExpression() {
        double x = parseTerm();
        for (;;) {
            if      (eat('+')) x += parseTerm();
            else if (eat('-')) x -= parseTerm();
            else return x;
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        for (;;) {
            if      (eat('*')) x *= parseFactor();
            else if (eat('/')) x /= parseFactor();
            else return x;
        }
    }

    private double parseFactor() {
        if (eat('+')) return parseFactor();
        if (eat('-')) return -parseFactor();
        double x;
        int startPos = pos;
        if (eat('(')) {
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(expr.substring(startPos, pos));
        } else {
            // functions: sqrt, sin, cos, tan, log
            while (ch >= 'a' && ch <= 'z') nextChar();
            String func = expr.substring(startPos, pos);
            eat('('); double arg = parseExpression(); eat(')');
            switch (func) {
                case "sqrt": x = Math.sqrt(arg); break;
                case "sin":  x = Math.sin(Math.toRadians(arg)); break;
                case "cos":  x = Math.cos(Math.toRadians(arg)); break;
                case "tan":  x = Math.tan(Math.toRadians(arg)); break;
                case "log":  x = Math.log10(arg); break;
                case "ln":   x = Math.log(arg); break;
                default: throw new RuntimeException("Fungsi tidak dikenal: " + func);
            }
        }
        if (eat('^')) x = Math.pow(x, parseFactor());
        return x;
    }

    @Override
    public String describe() {
        return "CalculatorTool: evaluate math expressions. Usage: [CalculatorTool:2+2] or [CalculatorTool:sqrt(144)]";
    }
}
