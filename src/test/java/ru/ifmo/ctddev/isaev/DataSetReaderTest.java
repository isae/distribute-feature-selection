package ru.ifmo.ctddev.isaev;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class DataSetReaderTest {
    private DataSetReader dataSetReader = new DataSetReader();

    @Test
    public void testDatasetReader() {
        FeatureDataSet dataset = dataSetReader.readCsv("src/test/resources/datasets/simpleDataset.csv");
        assertEquals("5 classes", 5, dataset.getClasses().size());
        List<Integer> classes = dataset.getClasses();
        assertArrayEquals("Classes", new Object[] {0, 0, 1, 1, 1}, classes.toArray());
        assertArrayEquals("Feature 1",
                new Object[] {1, 2, 3, 4, 5},
                dataset.getFeatures().get(0).getValues().toArray()
        );
        assertArrayEquals("Feature 2",
                new Object[] {5, 4, 3, 2, 1},
                dataset.getFeatures().get(1).getValues().toArray()
        );
    }
}
