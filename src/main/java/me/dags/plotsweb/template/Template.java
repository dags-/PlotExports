package me.dags.plotsweb.template;

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
public class Template {

    private final List<Element> elements = new ArrayList<>();

    private Template(List<Element> elements) {
        this.elements.addAll(elements);
    }

    public Instance with(String key, Object arg) {
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

    public static Template parse(Path path) {
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

    public static class Instance {

        private final Map<String, Object> args = new HashMap<>();
        private final Template template;

        private Instance(Template template) {
            this.template = template;
        }

        public Instance with(String key, Object arg) {
            args.put(key, arg);
            return this;
        }

        public String apply() {
            return template.apply(args);
        }
    }
}
