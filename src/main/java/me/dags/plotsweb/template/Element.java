package me.dags.plotsweb.template;

/**
 * @author dags <dags@dags.me>
 */
class Element {

    final String value;
    final boolean isArg;

    Element(String value, boolean isArg) {
        this.value = value;
        this.isArg = isArg;
    }
}
