package com.crossrank.backend;

import com.crossrank.backend.datatypes.Person;
import com.crossrank.backend.datatypes.Race;
import com.crossrank.backend.datatypes.Rankings;
import com.crossrank.backend.datatypes.Result;

import java.io.Serializable;
import java.util.*;

public class CrossRank implements Serializable {
    private final List<Person> runners;
    private final List<Race> races;
    private final List<Integer> scoredMeets;

    private Map<String, Long> runnerDirectory;

    private long runnerIdCounter;
    private long raceIdCounter;

    CrossRank() {
        runners = new ArrayList<>();
        scoredMeets = new ArrayList<>();
        races = new ArrayList<>();

        runnerDirectory = new HashMap<>();
    }

    private void ScanMeets() {
        Map<Integer, List<Integer>> meetData = MeetCompiler.CompileMonth();

        for (Map.Entry<Integer, List<Integer>> entry : meetData.entrySet()) {
            ScoreMeet(entry.getKey(), entry.getValue());
        }
    }

    private void ScoreMeet(int meetId, List<Integer> resultsId) {
        if (scoredMeets.contains(meetId)) {
            return;
        }

        scoredMeets.add(meetId);

        List<Race> newRaces = Fetcher.GetRaces(meetId, resultsId, raceIdCounter);
        raceIdCounter += newRaces.size();

        for (Race race : newRaces) {
            races.add(race);
            List<Person> raceParticipants = new ArrayList<>();
            for (Result result : race.getResults()) {
                Person runner = getPerson(result);

                if (runner == null) {
                    runner = new Person(result, runnerIdCounter);
                    runnerDirectory.put(runner.getFullName(), runner.getId());
                    runnerIdCounter++;
                    runners.add(runner);
                }

                raceParticipants.add(runner);
            }

            for (int i = 0; i < raceParticipants.size(); i++) {
                Person participant = raceParticipants.get(i);
                participant.addRaces(raceParticipants.size()-1);

                double opponentRating = 0;
                for (int j = 0; j < i; j++) {
                    opponentRating += raceParticipants.get(j).getRanking();
                }
                for (int j = i+1; j < raceParticipants.size(); j++) {
                    opponentRating += raceParticipants.get(j).getRanking();
                }

                participant.addOpponentRatings(opponentRating);
                participant.addWinLoss(raceParticipants.size() - i - 1);

                participant.updateRanking();
            }

            for (Person p : raceParticipants) {
                p.finalizeRanking();
            }
        }

        runnerDirectory = new TreeMap<>(runnerDirectory);
    }

    public static Rankings GetRankings(int page, int pageLength, String sex) {
        CrossRank crossRank = CrossRankSerializer.LoadRankings();

        List<Person> sorted = new ArrayList<>();
        Rankings rankings = new Rankings();

        crossRank.runners.sort(Comparator.comparing(Person::getRanking).reversed());
        for (Person p : crossRank.runners) {
            if (p.getGenderName().equalsIgnoreCase(sex)) {
                sorted.add(p);
            }
        }

        for (int i = pageLength * page - pageLength; i < pageLength * page; i++) {
            rankings.addRunner(sorted.get(i));
        }

        return rankings;
    }

    private Person getPerson(Result result) {
        for (Person p : runners) {
            if (p.getFullName().equals(result.getFullName()) && p.getGenderName().equalsIgnoreCase(result.getGenderName())) {
                p.addResults(result);
                return p;
            }
        }
        return null;
    }

    public static Person GetPerson(String name) {

        Map<String, Long> runnerDirectory = CrossRankSerializer.LoadRunnerDirectory();

        try {
            if (runnerDirectory != null) {
                long id = runnerDirectory.get(name);
                return CrossRankSerializer.LoadRunner(id);
            }
        } catch (NullPointerException e) {
            return new Person();
        }

        return new Person();
    }

    @SuppressWarnings("unused")
    public List<Person> getRunners() {
        return runners;
    }

    @SuppressWarnings("unused")
    public Map<String, Long> getRunnerDirectory() {
        return runnerDirectory;
    }

    public static void main(String[] args) {
        CrossRank crossRank = CrossRankSerializer.LoadRankings();
        crossRank.ScanMeets();
        CrossRankSerializer.SaveRankings(crossRank);
        CrossRankSerializer.SaveRunners(crossRank.runners);
        CrossRankSerializer.SaveRunnerDirectory(crossRank.runnerDirectory);
        CrossRankSerializer.SaveRaces(crossRank.races);
    }
}
