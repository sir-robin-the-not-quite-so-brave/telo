package telo;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

@Command(name = "telo", mixinStandardHelpOptions = true, version = "0.1", synopsisHeading = "%nUsage: ",
        header = "Team-based Elo rating for Chivalry",
        description = {"%nTool to compute team-based Elo ratings for Chivalry: Medieval Warfare.%n"})
public class Telo implements Callable<Integer> {

    @Option(names = {"-k", "--max-score"}, defaultValue = "20.0", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            description = {"Maximum points that can be gained or lost."})
    private double maxScore;

    @Option(names = {"--per-num-players"}, defaultValue = "false",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            description = {"Set the maximum points computation:",
                    "true - The maximum points are per number of players.",
                    "false - The maximum points are per game."})
    private boolean perNumPlayers;

    @Option(names = {"-r", "--default-rating"}, defaultValue = "0",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            description = {"The Elo rating assigned to new players."})
    private int defaultRating;

    @Parameters(arity = "1", paramLabel = "<results.csv>",
            description = {"CSV file containing the results. Each game should have its own header row. " +
                                   "The results for both teams go together.",
            "The headers are:",
            "Name - the player name",
            "Score - the game score",
            "Kills - number of kills",
            "Deaths - number of deaths",
            "Assists - number of assists"})
    private Path resultsPath;

    public static void main(String[] args) {
        final CommandLine commandLine = new CommandLine(new Telo());
        final int status = commandLine.execute(args);
        System.exit(status);
    }

    @Override
    public Integer call() {
        final ResultsParser resultsParser = new ResultsParser(resultsPath);
        final List<List<ResultsParser.Result>> games = resultsParser.parse();
        final List<Map<String, Integer>> ratingHistory = new ArrayList<>();
        Map<String, Integer> playerRatings = new TreeMap<>();
        for (var game : games) {
            playerRatings = updateRatings(playerRatings, game);
            ratingHistory.add(playerRatings);
            System.out.println("------------");
            printRatings(playerRatings);
        }
        System.out.println();
        System.out.println("Rating history:");
        printRatingsHistory(ratingHistory);
        return 0;
    }

    private void printRatingsHistory(List<Map<String, Integer>> ratingHistory) {
        final int size = ratingHistory.size();
        if (size == 0)
            return;

        final Set<String> allPlayers = ratingHistory.get(size - 1).keySet();
        allPlayers.forEach(name -> printRatingsHistory(name, ratingHistory));
    }

    private void printRatingsHistory(String name, List<Map<String, Integer>> ratingHistory) {
        System.out.print(name);
        ratingHistory.stream()
                     .map(table -> table.getOrDefault(name, defaultRating))
                     .forEach(rating -> System.out.printf(",%d", rating));
        System.out.println();
    }

    private void printRatings(Map<String, Integer> playerRatings) {
        playerRatings.entrySet().stream()
                     .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                     .forEach(e -> System.out.printf("%s,%d%n", e.getKey(), e.getValue()));
    }

    private Map<String, Integer> updateRatings(Map<String, Integer> playerRatings, List<ResultsParser.Result> game) {
        final Map<String, Integer> updatedRatings = new TreeMap<>(playerRatings);
        for (int i = 0; i < game.size(); i++) {
            final int newRating = updateRating(playerRatings, game, i);
            updatedRatings.put(game.get(i).name, newRating);
        }
        return updatedRatings;
    }

    private int updateRating(Map<String, Integer> playerRatings, List<ResultsParser.Result> game, int idx) {
        if (game.size() < 2)
            throw new IllegalArgumentException("Cannot have a game with less than 2 players.");

        final ResultsParser.Result player = game.get(idx);

        final int rating = playerRatings.getOrDefault(player.name, defaultRating);
        double delta = 0;
        for (int i = 0; i < game.size(); i++) {
            if (i == idx)
                continue;

            final ResultsParser.Result opponent = game.get(i);

            final double actualWin = actualWin(player.score, opponent.score);

            final int opponentRating = playerRatings.getOrDefault(opponent.name, defaultRating);
            final double expectedWin = expectedWin(rating, opponentRating);

            delta += (actualWin - expectedWin);
        }

        int teamSize = perNumPlayers ? 1 : game.size() - 1;
        return (int) Math.round(rating + maxScore * delta / teamSize);
    }

    private double actualWin(int playerScore, int opponentScore) {
        if (playerScore < opponentScore)
            return 0;
        if (playerScore == opponentScore)
            return 0.5;
        return 1;
    }

    private double expectedWin(int playerRating, int opponentRating) {
        final double power = (opponentRating - playerRating) / 400.;
        return 1.0 / (1 + Math.pow(10., power));
    }
}
