package ru.ifmo.ctddev.isaev;

import j2html.tags.Tag;
import org.junit.Test;
import ru.ifmo.ctddev.isaev.executable.Pr;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

/**
 * @author iisaev
 */
public class TestHtml {

    @Test
    public void testHtml() {
        List<Pr<Double, Double>> abc = Arrays.asList(new Pr(0.4, 0.5), new Pr(0.4, 0.5));
        Tag[] rows = Stream.concat(
                Stream.of(
                        tr().with(
                                th("Basic score"),
                                th("Parallel score")
                        )
                ),
                abc.stream()
                        .map(pr -> {
                            return tr().with(
                                    td(pr.getBasic().toString()),
                                    td(pr.getParallel().toString())
                            );
                        })
        ).collect(Collectors.toList()).toArray(new Tag[0]);
        String htmlString = html().with(
                head().with(
                        title("Experiment results")
                ),
                body().with(
                        h1("Heading!"),
                        table().with(rows)
                )
        ).render();
        System.out.println(htmlString);
    }
}
