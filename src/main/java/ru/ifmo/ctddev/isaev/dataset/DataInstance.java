package ru.ifmo.ctddev.isaev.dataset;

import java.util.List;


/**
 * @author iisaev
 */
public class DataInstance {
    private final Integer clazz;

    private final List<Integer> values;

    public DataInstance(Integer clazz, List<Integer> values) {
        this.clazz = clazz;
        this.values = values;
    }

    public Integer getClazz() {
        return clazz;
    }

    public List<Integer> getValues() {
        return values;
    }
}
