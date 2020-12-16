package dev.morling.jfrunit;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;


public class JfrAnalysis {

    public static List<IResult> analysisRecording(String fileName, Severity minSeverity) {
        return JfrAnalysis.analysisRecording(fileName,  minSeverity, false);
    }

    public static List<IResult> analysisRecording(String fileName, Severity minSeverity, boolean verbose) {
        try {
            File file = new File(fileName);

            IItemCollection events = null;
            try {
                events = JfrLoaderToolkit.loadEvents(file);
            } catch (IOException | CouldNotLoadRecordingException e) {
                //TODO:: error reporting
                return null;
            }

            // TODO: Provide configuration
            Map<IRule, Future<IResult>> resultFutures = RulesToolkit.evaluateParallel(RuleRegistry.getRules(), events,
                    null, 0);
            List<Map.Entry<IRule, Future<IResult>>> resultFutureList = new ArrayList<>(resultFutures.entrySet());
            Collections.sort(resultFutureList, new Comparator<Map.Entry<IRule, ?>>() {
                @Override
                public int compare(Map.Entry<IRule, ?> o1, Map.Entry<IRule, ?> o2) {
                    return o1.getKey().getId().compareTo(o2.getKey().getId());
                }
            });

            List<IResult> analysisResults = new ArrayList();

            for (Map.Entry<IRule, Future<IResult>> resultEntry : resultFutureList) {
                IResult result = null;
                try {
                    result = resultEntry.getValue().get();
                } catch (Throwable t) {
                    //TODO:: error reporting
                    continue;
                }

                if (result != null && result.getSeverity().compareTo(minSeverity) >= 0) {
                    //TODO: further results processing
                    analysisResults.add(result);
                }
            }
            return analysisResults;
        } catch (Throwable t) {
            System.err.println("Got exception when creating report for " + fileName); //$NON-NLS-1$
            throw t;
        }
    }
}
