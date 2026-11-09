package com.encorecraft.encorecarts.client.model.bb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tiny Molang subset for Blockbench keyframe values: numbers, + - * /, parentheses, unary minus,
 * {@code query.anim_time}, {@code query.life_time} (and {@code q.} aliases) and common {@code math.*} functions
 * (trigonometry in degrees, as in Molang). Unknown identifiers evaluate to 0.
 */
public final class Molang {
    public record Context(double animTime, double lifeTime) {
        public static final Context ZERO = new Context(0, 0);
    }

    @FunctionalInterface
    public interface Expr {
        double eval(Context ctx);

        default boolean isConstant() {
            return false;
        }

        default Expr negate() {
            Expr self = this;
            return ctx -> -self.eval(ctx);
        }
    }

    public record Constant(double value) implements Expr {
        @Override
        public double eval(Context ctx) {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }

        @Override
        public Expr negate() {
            return new Constant(-value);
        }
    }

    public static final Expr ZERO = new Constant(0);

    private Molang() {
    }

    public static Expr constant(double value) {
        return value == 0 ? ZERO : new Constant(value);
    }

    /** Compiles {@code source}; malformed input yields 0 rather than failing the whole model. */
    public static Expr compile(String source) {
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return ZERO;
        }
        try {
            return constant(Double.parseDouble(trimmed));
        } catch (NumberFormatException ignored) {
            // fall through to the parser
        }
        try {
            Parser parser = new Parser(trimmed.toLowerCase(Locale.ROOT).replaceAll("[\\r\\n]", ""));
            Expr expr = parser.expression();
            parser.skipSpaces();
            if (parser.pos < parser.src.length() && parser.src.charAt(parser.pos) == ';') {
                parser.pos++;
            }
            return expr;
        } catch (RuntimeException e) {
            return ZERO;
        }
    }

    private static final class Parser {
        final String src;
        int pos;

        Parser(String src) {
            this.src = src;
        }

        void skipSpaces() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        boolean eat(char c) {
            skipSpaces();
            if (pos < src.length() && src.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        Expr expression() {
            Expr left = term();
            while (true) {
                if (eat('+')) {
                    Expr a = left, b = term();
                    left = ctx -> a.eval(ctx) + b.eval(ctx);
                } else if (eat('-')) {
                    Expr a = left, b = term();
                    left = ctx -> a.eval(ctx) - b.eval(ctx);
                } else {
                    return left;
                }
            }
        }

        Expr term() {
            Expr left = factor();
            while (true) {
                if (eat('*')) {
                    Expr a = left, b = factor();
                    left = ctx -> a.eval(ctx) * b.eval(ctx);
                } else if (eat('/')) {
                    Expr a = left, b = factor();
                    left = ctx -> {
                        double d = b.eval(ctx);
                        return d == 0 ? 0 : a.eval(ctx) / d;
                    };
                } else {
                    return left;
                }
            }
        }

        Expr factor() {
            if (eat('-')) {
                return factor().negate();
            }
            if (eat('+')) {
                return factor();
            }
            if (eat('(')) {
                Expr inner = expression();
                eat(')');
                return inner;
            }
            skipSpaces();
            int start = pos;
            if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                    pos++;
                }
                // Exponent only when digits follow, so "2e" never swallows an identifier.
                if (pos + 1 < src.length() && src.charAt(pos) == 'e') {
                    int exp = pos + 1;
                    if ((src.charAt(exp) == '-' || src.charAt(exp) == '+') && exp + 1 < src.length()) {
                        exp++;
                    }
                    if (Character.isDigit(src.charAt(exp))) {
                        pos = exp;
                        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                            pos++;
                        }
                    }
                }
                return constant(Double.parseDouble(src.substring(start, pos)));
            }
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_' || src.charAt(pos) == '.')) {
                pos++;
            }
            String name = src.substring(start, pos);
            if (name.isEmpty()) {
                throw new IllegalStateException("Unexpected character at " + pos + " in " + src);
            }
            List<Expr> args = new ArrayList<>();
            if (eat('(')) {
                if (!eat(')')) {
                    do {
                        args.add(expression());
                    } while (eat(','));
                    eat(')');
                }
            }
            return identifier(name, args);
        }

        private Expr identifier(String name, List<Expr> args) {
            String n = name.startsWith("q.") ? "query." + name.substring(2) : name;
            Expr a = args.isEmpty() ? ZERO : args.get(0);
            Expr b = args.size() < 2 ? ZERO : args.get(1);
            Expr c = args.size() < 3 ? ZERO : args.get(2);
            return switch (n) {
                case "query.anim_time" -> Context::animTime;
                case "query.life_time" -> Context::lifeTime;
                case "math.pi" -> constant(Math.PI);
                case "math.sin" -> ctx -> Math.sin(Math.toRadians(a.eval(ctx)));
                case "math.cos" -> ctx -> Math.cos(Math.toRadians(a.eval(ctx)));
                case "math.abs" -> ctx -> Math.abs(a.eval(ctx));
                case "math.sqrt" -> ctx -> Math.sqrt(Math.max(0, a.eval(ctx)));
                case "math.floor" -> ctx -> Math.floor(a.eval(ctx));
                case "math.ceil" -> ctx -> Math.ceil(a.eval(ctx));
                case "math.round" -> ctx -> Math.round(a.eval(ctx));
                case "math.trunc" -> ctx -> (long) a.eval(ctx);
                case "math.exp" -> ctx -> Math.exp(a.eval(ctx));
                case "math.pow" -> ctx -> Math.pow(a.eval(ctx), b.eval(ctx));
                case "math.min" -> ctx -> Math.min(a.eval(ctx), b.eval(ctx));
                case "math.max" -> ctx -> Math.max(a.eval(ctx), b.eval(ctx));
                case "math.mod" -> ctx -> {
                    double d = b.eval(ctx);
                    return d == 0 ? 0 : a.eval(ctx) % d;
                };
                case "math.clamp" -> ctx -> Math.max(b.eval(ctx), Math.min(c.eval(ctx), a.eval(ctx)));
                case "math.lerp" -> ctx -> a.eval(ctx) + (b.eval(ctx) - a.eval(ctx)) * c.eval(ctx);
                default -> ZERO;
            };
        }
    }
}
