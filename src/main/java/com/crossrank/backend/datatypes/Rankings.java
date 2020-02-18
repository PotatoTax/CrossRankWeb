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

package com.crossrank.backend.datatypes;

import com.crossrank.LoadingBar;
import com.crossrank.backend.serialization.RankingsSerializer;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

public class Rankings implements Serializable {
    @Getter
    private List<Person> runners;

    @Getter
    private Map<String, Integer> runnerDirectory;

    @Getter
    private Map<Double, String> sortedRankingsBoys;
    @Getter
    private Map<Double, String> sortedRankingsGirls;

    public Rankings() {
        runners = new ArrayList<>();

        runnerDirectory = new TreeMap<>();
    }

    public void ScoreRaces(List<Race> races) {
        System.out.println("SCORING MEETS");

        LoadingBar loadingBar = new LoadingBar(50,  races.size());

        for (Race race : races) {
            ScoreRace(race);
            loadingBar.updateProgress();
        }

        loadingBar.end();
    }

    /**
     * Performs the ELO Rating algorithm on a race's results.
     * Updates each individuals rating.
     * @param race - a Race object to be scored.
     */
    public void ScoreRace(Race race) {

        List<Person> raceParticipants = new ArrayList<>();
        for (Result result : race.getResults()) {
            Person runner = getPerson(result);

            if (runner == null) {
                runner = new Person(result);
                runnerDirectory.put(runner.getFullName(), runner.getId());
                runners.add(runner);
            } else {
                runner.addResult(result);
            }

            raceParticipants.add(runner);
        }

        raceParticipants.sort(Comparator.comparing(Person::getRecentMark));

        for (Person raceParticipant : raceParticipants) {
            raceParticipant.addRace();
        }

        for (int i = 0; i < raceParticipants.size(); i++) {
            Person current = raceParticipants.get(i);

            for (int j = 0; j < i; j++) {
                Person other = raceParticipants.get(j);

                newRankings(other, current);
            }

            for (int j = i+1; j < raceParticipants.size(); j++) {
                Person other = raceParticipants.get(j);
                newRankings(current, other);
            }
        }

        for (Person participant : raceParticipants) {
            double currentRating = participant.getRanking();
            int lastResult = participant.getResults().size() - 1;
            participant.getResults().get(lastResult).setRating(currentRating);
        }

        runnerDirectory = new TreeMap<>(runnerDirectory);
    }

    /**
     * Updates the two Person's ratings based on the difference in initial rating.
     * @param winner A Person object for the individual who placed higher
     *               in the race.
     * @param loser A Person object for the lower placing individual.
     */
    public static void newRankings(Person winner, Person loser) {
        double Pb = 1.0f * 1.0f / (1 + 1.0f * (float)(Math.pow(10, 1.0f * (winner.getRanking() - loser.getRanking()) / 400)));
        double Pa = 1 - Pb;

        float winnerK = 100f / winner.getRaces();
        float loserK = 100f / loser.getRaces();

        double Ra = winner.getRanking() + winnerK * (1 - Pa);
        double Rb = loser.getRanking() + loserK * (-Pb);

        winner.setRanking(Ra);
        loser.setRanking(Rb);
    }

    /**
     * Places runners into sorted TreeMaps by gender
     * Used for quickly retrieving rankings.
     */
    private void CreateSortedRankings() {
        sortedRankingsBoys = new TreeMap<>();
        sortedRankingsGirls = new TreeMap<>();

        for (Person runner : runners) {
            if (runner.getGenderName().equals("Boys")) {
                sortedRankingsBoys.put(runner.getRanking(), runner.getFullName());
            } else {
                sortedRankingsGirls.put(runner.getRanking(), runner.getFullName());
            }
        }
    }

    /**
     * Given the section and sex, returns the names and ratings of all runners
     * who fall in the area.
     * @param page Integer denoting the section the rankings will come from.
     * @param pageLength Integer denoting the number of runners on each page.
     * @param sex String denoting the sex to be ranked
     * @return A TreeMap with Double ratings and String names.
     */
    public static Map<Double, String> GetRankings(int page, int pageLength, String sex) {
        Map<Double, String> results = RankingsSerializer.LoadSortedRankings(sex);

        List<Double> ratings = new ArrayList<>(results.keySet());

        Collections.reverse(ratings);

        Map<Double, String> product = new TreeMap<>();

        int endIndex = page * pageLength;

        if (endIndex > ratings.size()) {
            endIndex = ratings.size();
        }
        for (int i = pageLength * page - pageLength; i < endIndex; i++) {
            product.put(ratings.get(i), results.get(ratings.get(i)));
        }

        return product;
    }

    private Person getPerson(Result result) {
        for (Person p : runners) {
            if (p.getFullName().equals(result.getFullName()) && p.getGenderName().equalsIgnoreCase(result.getGenderName())) {
                return p;
            }
        }
        return null;
    }
}
