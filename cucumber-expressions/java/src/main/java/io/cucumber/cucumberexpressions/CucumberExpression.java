package io.cucumber.cucumberexpressions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CucumberExpression implements Expression {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^\\}:]+)(:([^\\}]+))?\\}");
    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("\\(([^\\)]+)\\)");

    private final Pattern pattern;
    private final List<Transform<?>> transforms = new ArrayList<>();
    private final String expression;

    public CucumberExpression(final String expression, List<Class<?>> types, TransformLookup transformLookup) {
        this.expression = expression;
        String expressionWithOptionalGroups = OPTIONAL_PATTERN.matcher(expression).replaceAll("(?:$1)?");
        Matcher matcher = VARIABLE_PATTERN.matcher(expressionWithOptionalGroups);

        StringBuffer regexp = new StringBuffer();
        regexp.append("^");
        int typeIndex = 0;
        while (matcher.find()) {
            Class<?> type = types.size() <= typeIndex ? null : types.get(typeIndex++);
            String argumentName = matcher.group(1);
            String typeName = matcher.group(3);

            Transform<?> transform = null;
            if (type != null) {
                transform = transformLookup.lookupByType(type);
            }
            if (transform == null && typeName != null) {
                transform = transformLookup.lookupByType(typeName, false);
            }
            if (transform == null) {
                transform = transformLookup.lookupByType(argumentName, true);
            }
            if (transform == null && type != null) {
                transform = new ConstructorTransform<>(type);
            }
            if (transform == null) {
                transform = new ConstructorTransform<>(String.class);
            }
            transforms.add(transform);

            matcher.appendReplacement(regexp, Matcher.quoteReplacement("(" + transform.getCaptureGroupRegexps().get(0) + ")"));
        }
        matcher.appendTail(regexp);
        regexp.append("$");

        pattern = Pattern.compile(regexp.toString());
    }

    @Override
    public List<Argument> match(String text) {
        return ArgumentMatcher.matchArguments(pattern, text, transforms);
    }

    @Override
    public String getSource() {
        return expression;
    }

    Pattern getPattern() {
        return pattern;
    }
}
