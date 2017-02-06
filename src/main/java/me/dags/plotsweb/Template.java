package me.dags.plotsweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author dags <dags@dags.me>
 */
class Template {

    private final List<Element> elements = new ArrayList<>();

    private Template(List<Element> elements) {
        this.elements.addAll(elements);
    }

    Instance with(String key, Object arg) {
        return new Instance(this).with(key, arg);
    }

    public Instance get() {
        return new Instance(this);
    }

    private String apply(Map<String, Object> args) {
        StringBuilder builder = new StringBuilder();
        for (Element element : elements) {
            if (element.isArg) {
                builder.append(args.get(element.value));
            } else {
                builder.append(element.value);
            }
        }
        return builder.toString();
    }

    static Template parse(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return parse(inputStream);
        } catch (IOException e) {
            return new Template(Collections.emptyList());
        }
    }

    private static Template parse(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(builder.length() > 0 ? "\n" : "").append(line);
            }
            List<Element> elements = new TemplateParser(builder.toString()).parse();
            return new Template(elements);
        } catch (IOException e) {
            return new Template(Collections.emptyList());
        }
    }

    public static Template parse(String in) {
        List<Element> elements = new TemplateParser(in).parse();
        return new Template(elements);
    }

    static class Element {

        private final String value;
        private final boolean isArg;

        Element(String value, boolean isArg) {
            this.value = value;
            this.isArg = isArg;
        }
    }

    static class Instance {

        private final Map<String, Object> args = new HashMap<>();
        private final Template template;

        private Instance(Template template) {
            this.template = template;
        }

        Instance with(String key, Object arg) {
            args.put(key, arg);
            return this;
        }

        String apply() {
            return template.apply(args);
        }
    }
}
