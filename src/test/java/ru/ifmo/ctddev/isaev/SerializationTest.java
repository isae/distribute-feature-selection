package ru.ifmo.ctddev.isaev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.ifmo.ctddev.isaev.classifier.Classifiers;
import ru.ifmo.ctddev.isaev.dataset.Feature;
import ru.ifmo.ctddev.isaev.executable.Pr;
import ru.ifmo.ctddev.isaev.feature.measure.*;
import ru.ifmo.ctddev.isaev.result.Point;
import ru.ifmo.ctddev.isaev.result.RunStats;
import ru.ifmo.ctddev.isaev.result.SelectionResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author iisaev
 */
public class SerializationTest {
    private static ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSerialization() throws JsonProcessingException {
        RunStats runStats = new RunStats(new AlgorithmConfig(0.0, Classifiers.WEKA_HOEFD, new RelevanceMeasure[]{
                new VDM(),
                new SpearmanRankCorrelation(),
                new SymmetricUncertainty(),
                new FitCriterion()
        }));
        runStats.setStartTime(LocalDateTime.now());
        runStats.updateBestResult(
                new SelectionResult(
                        Arrays.asList(
                                new Feature("1", Collections.singletonList(1))
                        ),
                        new Point(1.0, 1.0, 1.0, 1.0),
                        0.5
                )
        );
        runStats.setFinishTime(LocalDateTime.now());
        String result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(Arrays.asList(new Pr<>(runStats, runStats)));
    }
}
