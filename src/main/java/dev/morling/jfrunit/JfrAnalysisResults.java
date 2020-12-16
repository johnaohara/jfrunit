package dev.morling.jfrunit;

import org.openjdk.jmc.flightrecorder.rules.IResult;

import java.util.List;

public class JfrAnalysisResults {
    private List<IResult> results;

    public JfrAnalysisResults(List<IResult> analysisRecording) {
        this.results = analysisRecording;
    }

    public List<IResult> getResults(){
        return this.results;
    }

    public int size() {
        return  this.results.size();
    }
}
