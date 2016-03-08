package ru.ifmo.ctddev.isaev;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class DatasetReaderTest {
    private DatasetReader datasetReader = new DatasetReader();

    @Test
    public void testDatasetReader() {
        Dataset dataset = datasetReader.readCsv("src/test/resources/datasets/simpleDataset.csv");
        assertEquals("5 classes", 5, dataset.getClasses().size());
        List<Integer> classes = dataset.getClasses();
        assertArrayEquals("Classes", new Object[] {0, 0, 1, 1, 1}, classes.toArray());
        assertArrayEquals("Feature 1", new Object[] {0.39992, 0.038909, 0.41751, -0.11239, 0.285}, dataset.getFeatures().get(0).toArray());
        assertArrayEquals("Feature 2", new Object[] {-0.12397, -0.12242, -0.098427, -0.058646, -0.034064}, dataset.getFeatures().get(1).toArray());
    }
}
