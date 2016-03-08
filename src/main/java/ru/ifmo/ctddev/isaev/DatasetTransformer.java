package ru.ifmo.ctddev.isaev;

import ru.ifmo.ctddev.isaev.dataset.DataInstance;
import ru.ifmo.ctddev.isaev.dataset.FeatureDataSet;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


/**
 * @author iisaev
 */
public class DatasetTransformer {
    public Instances toInstances(FeatureDataSet ds) {
        FastVector attrInfo = new FastVector();
        FastVector classesVector = new FastVector();
        ds.getClasses().forEach(classesVector::addElement);
        attrInfo.addElement(new Attribute("Classes", classesVector));
        ds.getFeatures().forEach(feature -> {
            FastVector featureVector = new FastVector(feature.getValues().size());
            feature.getValues().forEach(featureVector::addElement);
            attrInfo.addElement(new Attribute(feature.getName(), featureVector));
        });
        Instances result = new Instances(ds.getName(), attrInfo, ds.getFeatures().size());
        result.setClassIndex(0);
        return result;
    }

    public Instance toInstance(DataInstance instance) {
        Instance result = new Instance(instance.getValues().size() + 1);
        result.setMissing(0);
        for (int i = 1; i < instance.getValues().size(); ++i) {
            result.setValue(i, instance.getValues().get(i - 1));
        }
        result.setMissing(0);
        return result;
    }

}
