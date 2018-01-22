package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.InstanceDataSet;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;


/**
 * @author iisaev
 */
public class DataSetTransformer {
    private final Random random = new Random();

    public String nextAttrName() {
        return new BigInteger(130, random).toString(32).substring(0, 8);
    }

    public Instances toInstances(InstanceDataSet ds) {
        ArrayList<Attribute> attrInfo = new ArrayList<>();
        attrInfo.add(new Attribute("Classes", Arrays.asList("0", "1")));
        ds.getInstances().get(0).getValues().forEach(val -> attrInfo.add(new Attribute(nextAttrName())));
        Instances result = new Instances(ds.getName(), attrInfo, ds.getInstances().size());
        result.setClassIndex(0);
        ds.getInstances().forEach(inst -> {
            Instance instance = new DenseInstance(inst.getValues().size() + 1);
            instance.setDataset(result);
            instance.setValue(0, String.valueOf(inst.getClazz()));
            IntStream.range(0, inst.getValues().size()).forEach(i -> {
                instance.setValue(i + 1, (double) inst.getValues().get(i));
            });
            result.add(instance);
        });
        return result;
    }

    public Instance toInstance(DataInstance instance) {
        Instance result = new DenseInstance(instance.getValues().size() + 1);
        result.setMissing(0);
        for (int i = 0; i < instance.getValues().size(); ++i) {
            result.setValue(i + 1, instance.getValues().get(i));
        }
        return result;
    }

}
