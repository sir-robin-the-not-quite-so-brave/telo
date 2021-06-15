# telo - Team-based Elo

`telo` is a tool to calculate team-based Elo ratings for ***Chivalry: Medieval
Warfare***.

## Overview

The tool supports both 1v1 and team-based ratings from playing Last Team
Standing, or Team Objective games. The teams do not need to stay consistent and
players can freely move between teams. The rating is computed for the players,
not for the teams.

The algorithm is based on this [blog
post](https://ryanmadden.net/posts/Adapting-Elo) with some modifications:
* Currently only the player scores are taken into account. They already depend
  on number of kills, deaths, assists, team-kills, and "doing objective", so it
  seems like they are a good overall performance measure
* We ignore the margin of the score difference. For example in an FT10 game it
  doesn't matter whether the player won 10 to 0, or 10 to 9
* The algorithm as described in the blog post has maximum score that can be won
  or lost depends on the team sizes. In 1v1 duels it's capped to 20, but in
  teams with N players, it's (2 * N - 1) * 20. This implementation chooses to
  instead the cap on per game basis.

## Results format

`telo` expects the game results in a CSV format, which looks like this:

``` csv
Name,Score,Kills,Deaths,Assists
player 1,240,10,4,6
player 2,200,8,6,7
,,,,
Name,Score,Kills,Deaths,Assists
player 2,140,10,4,6
player 1,135,8,6,7
```

Each recorded game should start with its own header line and the players from
both teams go in a single table.
