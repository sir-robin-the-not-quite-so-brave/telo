package telo;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ResultsParser {

    private final Path results;

    public ResultsParser(Path results) {
        this.results = results;
    }

    public List<List<Result>> parse() {
        final CsvParserSettings settings = new CsvParserSettings();
        final CsvParser parser = new CsvParser(settings);
        parser.beginParsing(results.toFile());
        final List<List<Result>> all = new ArrayList<>();
        List<Result> game = null;
        Record record;
        while ((record = parser.parseNextRecord()) != null) {
            if (record.getString(0) == null)
                continue;

            if ("score".equalsIgnoreCase(record.getString(1))) {
                if (game != null)
                    all.add(game);
                game = new ArrayList<>();
                continue;
            }
            final Result result = parse(record);
            if (game == null)
                throw new IllegalStateException("Missing CSV headers");
            game.add(result);
        }
        if (game != null && !game.isEmpty())
            all.add(game);

        return all;
    }

    private Result parse(Record record) {
        try {
            final String name = record.getString(0).trim().toLowerCase();
            final Integer score = record.getInt(1);
            final Integer kills = record.getInt(2);
            final Integer deaths = record.getInt(3);
            final Integer assists = record.getInt(4);
            return new Result(name, score, kills, deaths, assists);
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Can't parse record: " + record);
        }
    }

    public static class Result {
        public final String name;
        public final int score;
        public final int kills;
        public final int deaths;
        public final int assists;

        public Result(String name, int score, int kills, int deaths, int assists) {
            this.name = name;
            this.score = score;
            this.kills = kills;
            this.deaths = deaths;
            this.assists = assists;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Result.class.getSimpleName() + "[", "]")
                           .add("name='" + name + "'")
                           .add("score=" + score)
                           .add("kills=" + kills)
                           .add("deaths=" + deaths)
                           .add("assists=" + assists)
                           .toString();
        }
    }
}
