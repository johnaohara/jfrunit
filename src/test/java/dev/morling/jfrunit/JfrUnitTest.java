/**
 *  Copyright 2020 The JfrUnit authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.morling.jfrunit;

import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.FullGcRule;
import org.openjdk.jmc.flightrecorder.rules.jdk.memory.HeapDumpRule;

import static dev.morling.jfrunit.ExpectedEvent.event;
import static dev.morling.jfrunit.JfrEventsAssert.assertThat;
import static dev.morling.jfrunit.JfrAnalysisAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

@JfrEventTest
public class JfrUnitTest {

    public JfrEvents jfrEvents = new JfrEvents();

    @Test
    @EnableEvent("jdk.GarbageCollection")
    @EnableEvent("jdk.ThreadSleep")
    public void shouldHaveGcAndSleepEvents() throws Exception {
        System.gc();
        Thread.sleep(1000);

        jfrEvents.awaitEvents();

        assertThat(jfrEvents).contains(event("jdk.GarbageCollection"));
        assertThat(jfrEvents).contains(
                event("jdk.GarbageCollection").with("cause", "System.gc()"));
        assertThat(jfrEvents).contains(
                event("jdk.ThreadSleep").with("time", Duration.ofSeconds(1)));

        assertThat(jfrEvents.filter(event("jdk.GarbageCollection"))).hasSize(1);
    }

    @Test
    @EnableConfiguration("profile")
    public void shouldHaveGcAndSleepEventsWithDefaultConfiguration() throws Exception {
        System.gc();
        Thread.sleep(1000);

        jfrEvents.awaitEvents();

        assertThat(jfrEvents).contains(event("jdk.GarbageCollection"));
        assertThat(jfrEvents).contains(
                event("jdk.GarbageCollection").with("cause", "System.gc()"));
        assertThat(jfrEvents).contains(
                event("jdk.ThreadSleep").with("time", Duration.ofSeconds(1)));

        assertThat(jfrEvents.filter(
                event("jdk.GarbageCollection").with("cause", "System.gc()")))
                .hasSize(1);

        long allocated = jfrEvents.filter(event("jdk.ObjectAllocationInNewTLAB"))
                .mapToLong(e -> e.getLong("tlabSize"))
                .sum();

        assertThat(allocated).isGreaterThan(0);
    }

    @Test
    @EnableConfiguration("profile")
    public void automatedAnalysis() throws Exception {

        System.gc();
        Thread.sleep(1000);

        jfrEvents.awaitEvents();

        JfrAnalysisResults analysisResults = jfrEvents.automaticAnalysis();

        assertThat(analysisResults.size()).isEqualTo(2);

        //Inspect rules that fired
        assertThat(analysisResults).contains(FullGcRule.class);
        assertThat(analysisResults).doesNotContain(HeapDumpRule.class);

        //Inspect severity of rule
        assertThat(analysisResults).hasSeverity(FullGcRule.class, Severity.WARNING);

        //Inspect score of rule
        assertThat(analysisResults)
                .contains(FullGcRule.class)
                .scoresLessThan(80);

    }
}
