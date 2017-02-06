package me.dags.plotsweb;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dags <dags@dags.me>
 */
class TemplateParser {

    private final String in;
    private int pos = -1;

    TemplateParser(String input) {
        this.in = input;
    }

    List<Template.Element> parse() {
        List<Template.Element> elements = new ArrayList<>();
        while (hasNext()) {
            elements.add(nextElement());
        }
        return elements;
    }

    private boolean hasNext() {
        return pos + 1 < in.length();
    }

    private char peek(int num) {
        if (pos + num < in.length()) {
            return in.charAt(pos + num);
        }
        throw new UnsupportedOperationException("End handle text");
    }

    private void skip(int num) {
        pos += num;
    }

    private char next() {
        if (hasNext()) {
            return in.charAt(++pos);
        }
        throw new UnsupportedOperationException("End handle text");
    }

    private Template.Element nextElement() {
        if (peek(1) == '$' && peek(2) == '{') {
            skip(2);
            return parseArg();
        }
        return parseText();
    }

    private Template.Element parseText() {
        StringBuilder builder = new StringBuilder();
        while (hasNext()) {
            if (peek(1) == '$' && peek(2) == '{') {
                break;
            }
            builder.append(next());
        }
        return new Template.Element(builder.toString(), false);
    }

    private Template.Element parseArg() {
        StringBuilder builder = new StringBuilder();
        while (hasNext()) {
            char c = next();
            if (c == '}') {
                break;
            }
            builder.append(c);
        }
        return new Template.Element(builder.toString(), true);
    }
}
