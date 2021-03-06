/*
 * Copyright 2020 Conor Gregg Escalante
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.crossrank.backend;

import com.crossrank.LoadingBar;
import com.crossrank.backend.datatypes.*;
import com.crossrank.backend.serialization.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

public class CrossRank implements Serializable {
    @Getter
    private final List<Person> runners;

    private MeetIndex meetIndex;

    private MeetResults meetResults;

    private Rankings rankings;

    @Getter
    private Map<String, Long> runnerDirectory;

    private long raceIdCounter;

    public CrossRank() {
        runners = new ArrayList<>();

        runnerDirectory = new TreeMap<>();
    }


    /**
     * If there exists a saved MeetIndex, loads that, otherwise generates
     * a new MeetIndex
     */
    private void BuildIndex() {
        meetIndex = MeetIndexSerializer.LoadMeetIndex();

        if (meetIndex == null) {
            System.out.println("NO MEET INDEX SAVED");

            meetIndex = new MeetIndex();
            meetIndex.CompileSeason(2019);

            MeetIndexSerializer.SaveMeetIndex(meetIndex);
        } else {
            System.out.println("EXISTING MEET INDEX LOADED");
        }
    }


    /**
     * Loads the results for all meets in the meetIndex.
     * Sorts the list into chronological order.
     */
    private void GetResults() {
        meetResults = MeetResultsSerializer.LoadMeetResults();

        if (meetResults == null) {
            System.out.println("LOADING RESULTS");

            meetResults = new MeetResults();

            LoadingBar loadingBar = new LoadingBar(50, meetIndex.getMeets().size());

            for (Map.Entry<Integer, List<Integer>> entry : meetIndex.getMeets().entrySet()) {
                List<Race> newRaces = MeetResults.GetRaces(entry.getKey(), entry.getValue(), raceIdCounter);

                meetResults.getRaces().addAll(newRaces);
                raceIdCounter += newRaces.size();

                loadingBar.updateProgress();
            }

            loadingBar.end();

            meetResults.getRaces().sort(Comparator.comparing(Race::getMeetDate));

            MeetResultsSerializer.SaveMeetResults(meetResults);
        } else {
            System.out.println("EXISTING MEET RESULTS LOADED");
        }
    }


    /**
     * Scores each saved meet.
     */
    private void ScoreMeets() {
        rankings = new Rankings();

        rankings.ScoreRaces(meetResults.getRaces());
    }

    public static Person GetPerson(String name) {

        Map<String, Integer> runnerDirectory = RankingsSerializer.LoadRunnerDirectory();

        try {
            if (runnerDirectory != null) {
                int id = runnerDirectory.get(name);
                return PersonSerializer.LoadRunner(id);
            }
        } catch (NullPointerException e) {
            return new Person();
        }

        return new Person();
    }

    public void UpdateRankings() {
        BuildIndex();
        GetResults();
        ScoreMeets();
    }

    public void SaveRankings() {
        System.out.println("SAVING RANKINGS");

        PersonSerializer.SaveRunners(rankings.getRunners());

        RaceSerializer.SaveRaces(meetResults.getRaces());

        RankingsSerializer.SaveRankings(rankings);
        RankingsSerializer.SaveRunnerDirectory(rankings.getRunnerDirectory());
        RankingsSerializer.SaveSortedRankings(rankings.getSortedRankingsBoys(), rankings.getSortedRankingsGirls());
    }

    public static void main(String[] args) {
        CrossRank crossRank = new CrossRank();

        crossRank.UpdateRankings();

        crossRank.SaveRankings();
    }
}
