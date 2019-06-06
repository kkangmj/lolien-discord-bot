package kr.webgori.lolien.discord.bot.component;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import at.stefangeyer.challonge.Challonge;
import at.stefangeyer.challonge.exception.DataAccessException;
import at.stefangeyer.challonge.model.Credentials;
import at.stefangeyer.challonge.model.Tournament;
import at.stefangeyer.challonge.model.enumeration.TournamentType;
import at.stefangeyer.challonge.model.query.ParticipantQuery;
import at.stefangeyer.challonge.model.query.TournamentQuery;
import at.stefangeyer.challonge.rest.RestClient;
import at.stefangeyer.challonge.rest.retrofit.RetrofitRestClient;
import at.stefangeyer.challonge.serializer.Serializer;
import at.stefangeyer.challonge.serializer.gson.GsonSerializer;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.entity.Champ;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.repository.ChampRepository;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import static kr.webgori.lolien.discord.bot.util.CommonUtil.*;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class TeamGenerateComponent {
  private static final String[] TIER_LIST = {"IRON","BRONZE", "SILVER", "GOLD", "PLATINUM",
          "DIAMOND", "MASTER", "GRANDMASTER", "CHALLENGE"};
  private static final String[] RANK_LIST = {"IV", "III", "II", "I"};
  private static final String CURRENT_SEASON = "S9";
  private static final String DEFAULT_TIER = "UNRANKED";
  private static final int PERIOD_POINT = 5;
  private static final int LOOP_LIMIT_COUNT = 5;
  private static final int FAIL_LIMIT_COUNT = 5;

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LeagueRepository leagueRepository;
  private final CustomGameComponent customGameComponent;
  private final ChampRepository champRepository;

  @Value("${riot.api.key}")
  private String riotApiKey;

  @Value("${challonge.username}")
  private String challongeUserName;

  @Value("${challonge.api.key}")
  private String challongeApiKey;

  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 3) {
      sendSyntax(textChannel);
      return;
    }

    String subCommand = commands.get(1);

    if (!subCommand.equals("밸런스") && !subCommand.equals("랜덤")) {
      sendSyntax(textChannel);
      return;
    }

    StringBuilder entriesBuilder = new StringBuilder();

    for (int i = 2; i < commands.size(); i++) {
      entriesBuilder.append(commands.get(i));
    }

    String[] entries = entriesBuilder.toString().split(",");

    if (entries.length % 5 != 0) {
      sendErrorMessage(textChannel, "참가자가 인원이 잘못되었습니다.", Color.RED);
      return;
    }

    List<List<Object>> generateTeams = generateTeam(textChannel, entries, loLienSummonerRepository);
    StringBuilder message = new StringBuilder();

    for (List<Object> generateTeam : generateTeams) {
      message.append((String) generateTeam.get(0));
      message.append("\n");
      message.append((String) generateTeam.get(1));
      message.append("\n\n");
    }

    int teamSize = generateTeams.size();

    if (teamSize > 2) {
      Credentials credentials = new Credentials(challongeUserName, challongeApiKey);

      Serializer serializer = new GsonSerializer();
      RestClient restClient = new RetrofitRestClient();

      Challonge challonge = new Challonge(credentials, serializer, restClient);

      String uniqueId = RandomStringUtils.randomAlphanumeric(10);
      String tournamentCreatedDate = getTournamentCreatedDate();
      String gameName = String.format("[%s] LoLien League", tournamentCreatedDate);

      TournamentQuery query = TournamentQuery
              .builder()
              .name(gameName)
              .gameName("League of Legends")
              .url(uniqueId)
              .tournamentType(TournamentType.SINGLE_ELIMINATION)
              .build();

      try {
        Tournament tournament = challonge.createTournament(query);
        List<ParticipantQuery> participantQueries = Lists.newArrayList();

        for (int i = 1; i <= teamSize; i++) {
          String name = String.format("%s팀", i);

          ParticipantQuery participantQuery = ParticipantQuery
                  .builder()
                  .name(name)
                  .build();

          participantQueries.add(participantQuery);
        }

        challonge.bulkAddParticipants(tournament, participantQueries);
        String fullChallongeUrl = String.format("대진표: %s", tournament.getFullChallongeUrl());

        message.append(fullChallongeUrl);
      } catch (DataAccessException e) {
        logger.error("{}", e);
      }
    }

    sendMessage(textChannel, message.toString());
  }

  private int getPointByLevel(int level) {
    if (level > 150) {
      return 24;
    } else if (level > 100) {
      return 20;
    } else if (level > 50) {
      return 16;
    } else {
      return 8;
    }
  }

  private List<List<Object>> generateTeam(TextChannel textChannel, String[] entries,
                                    LoLienSummonerRepository loLienSummonerRepository) {
    int totalPoint = 0;
    Map<String, Integer> entriesPointMap = new LinkedHashMap<>();

    for (String summonerName : entries) {
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format("\"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.", summonerName);
        sendErrorMessage(textChannel, errorMessage, Color.BLUE);
        throw new IllegalArgumentException("register summoner first");
      }

      LoLienSummoner loLienSummoner = loLienSummonerRepository.findBySummonerName(summonerName);

      boolean presentCurrentSeason = loLienSummoner
              .getLeagues()
              .stream()
              .anyMatch(l -> l.getSeason().equals(CURRENT_SEASON));

      if (!presentCurrentSeason) {
        String tier = getCurrentSeasonTier(summonerName);

        League league = League
                .builder()
                .loLienSummoner(loLienSummoner)
                .season(CURRENT_SEASON)
                .tier(tier)
                .build();

        leagueRepository.save(league);
      }

      Map<String, Integer> tiers = getTiers();

      List<League> leagues = loLienSummoner.getLeagues();
      leagues.sort(Comparator.comparing(League::getSeason, Comparator.reverseOrder()));
      String tier;

      if (leagues.size() == 1) {
        League league = leagues.get(0);
        tier = league.getTier();
      } else {
        League currentSeasonLeague = leagues.get(0);
        String currentTier = currentSeasonLeague.getTier();

        League prevSeasonLeague = leagues.get(1);
        String prevTier = prevSeasonLeague.getTier();

        if (currentTier.equals(DEFAULT_TIER)) {
          tier = prevTier;
        } else {
          int currentTierPoint = tiers.get(currentTier);
          int prevTierPoint = tiers.get(prevTier);

          tier = currentTier;

          if (prevTierPoint > currentTierPoint) {
            tier = prevTier;
          }
        }
      }

      int point;

      if (tier.equals("UNRANKED")) {
        int summonerLevel = loLienSummoner.getSummonerLevel();
        point = getPointByLevel(summonerLevel);
      } else {
        point = tiers.get(tier);
      }

      totalPoint += point;

      entriesPointMap.put(summonerName, point);
    }

    int teamSize = (entries.length / 5);
    int averageTeamPoint = totalPoint / teamSize;
    List<String> entryList = Arrays
            .stream(entries)
            .collect(Collectors.toList());

    Collections.shuffle(entryList);

    List<Integer> pointListOfTeam = Lists.newArrayList();
    List<List<String>> summonerListOfTeams = Lists.newArrayList();
    List<String> summonerListOfTeam = Lists.newArrayList();

    int teamPoint = 0;
    int loopCount = 0;
    int failLoopCount = 0;

    while (true) {
      for (int i = 0; i < entryList.size(); i++) {
        String entrySummoner = entryList.get(i);
        int summonerPoint = entriesPointMap.get(entrySummoner);

        if (teamPoint + summonerPoint >= averageTeamPoint + PERIOD_POINT) {
          entryList.remove(entrySummoner);
          entryList.add(entrySummoner);
          i--;
          loopCount++;

          if (loopCount >= (LOOP_LIMIT_COUNT * teamSize)) {
            break;
          }

          continue;
        }

        loopCount = 0;
        pointListOfTeam.add(summonerPoint);
        summonerListOfTeam.add(entrySummoner);

        teamPoint = pointListOfTeam
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (pointListOfTeam.size() == 5 && summonerListOfTeam.size() == 5) {
          teamPoint = 0;
          summonerListOfTeams.add(summonerListOfTeam);

          pointListOfTeam = Lists.newArrayList();
          summonerListOfTeam = Lists.newArrayList();
        }
      }

      if (failLoopCount  >= FAIL_LIMIT_COUNT) {
        sendErrorMessage(textChannel, "팀 구성이 실패하였습니다.", Color.RED);
        throw new IllegalArgumentException("Loop Limit Exception");
      } else if (summonerListOfTeams.size() == teamSize) {
        break;
      } else {
        failLoopCount++;
        Collections.shuffle(entryList);
        pointListOfTeam = Lists.newArrayList();
        summonerListOfTeams = Lists.newArrayList();
        summonerListOfTeam = Lists.newArrayList();
        teamPoint = 0;
        loopCount = 0;
      }
    }

    List<List<Object>> generatedTeams = Lists.newArrayList();
    List<List<String>> generatedSummonerListOfTeams = setTeamSummoner(summonerListOfTeams);

    for (int i = 1; i <= generatedSummonerListOfTeams.size(); i++) {
      List<Object> generatedTeam = Lists.newArrayList();

      List<String> generatedSummonerListOfTeam = generatedSummonerListOfTeams.get(i - 1);
      String join = String.format("%s팀: %s", i, String.join(", ", generatedSummonerListOfTeam));
      generatedTeam.add(join);

      String teamBSummonerMostBuilder = setTeamSummonerMostTop3(summonerListOfTeams.get(i - 1));
      generatedTeam.add(teamBSummonerMostBuilder);

      generatedTeams.add(generatedTeam);
    }

    return generatedTeams;
  }

  private List<List<String>> setTeamSummoner(List<List<String>> summonerListOfTeams) {
    Map<String, Integer> tiers = getTiers();
    List<String> summonersInfoInTeam = Lists.newArrayList();
    List<List<String>> summonersInfoInTeams = Lists.newArrayList();

    for (List<String> summonerListOfTeam : summonerListOfTeams) {
      for (String summoner : summonerListOfTeam) {
        LoLienSummoner loLienSummoner = loLienSummonerRepository.findBySummonerName(summoner);
        List<League> leagues = loLienSummoner.getLeagues();

        leagues.sort(Comparator.comparing(League::getSeason, Comparator.reverseOrder()));
        String tier;

        if (leagues.size() == 1) {
          League league = leagues.get(0);
          tier = league.getTier();
        } else {
          League currentSeasonLeague = leagues.get(0);
          String currentTier = currentSeasonLeague.getTier();

          League prevSeasonLeague = leagues.get(1);
          String prevTier = prevSeasonLeague.getTier();

          if (currentTier.equals(DEFAULT_TIER)) {
            tier = prevTier;
          } else {
            int currentTierPoint = tiers.get(currentTier);
            int prevTierPoint = tiers.get(prevTier);

            tier = currentTier;

            if (prevTierPoint > currentTierPoint) {
              tier = prevTier;
            }
          }
        }

        int tierPoint;

        if (tier.equals(DEFAULT_TIER)) {
          int summonerLevel = loLienSummoner.getSummonerLevel();
          tierPoint = getPointByLevel(summonerLevel);
        } else {
          tierPoint = tiers.get(tier);
        }

        String summonerInfo = String.format("%s (%s / %s)", summoner, tier, tierPoint);
        summonersInfoInTeam.add(summonerInfo);
      }
      summonersInfoInTeams.add(summonersInfoInTeam);
      summonersInfoInTeam = Lists.newArrayList();
    }

    return summonersInfoInTeams;
  }

  private String setTeamSummonerMostTop3(List<String> entryList) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String summoner : entryList) {
      LinkedHashMap<Integer, Long> mostChampions = customGameComponent.getMostChamp(summoner, 3);
      List<String> mostChampionsList = Lists.newArrayList();

      for (Map.Entry<Integer, Long> mostChampion : mostChampions.entrySet()) {
        int champId = mostChampion.getKey();
        Champ champ = champRepository.findByKey(champId);
        String championName = champ.getName();
        mostChampionsList.add(championName);
      }

      String mostChamps = String.join(", ", mostChampionsList);
      String mostSummonerChamps = String.format("%s (%s)", summoner, mostChamps);
      stringBuilder
              .append("\n")
              .append(mostSummonerChamps);
    }

    return stringBuilder.toString();
  }

  private Map<String, Integer> getTiers() {
    Map<String, Integer> tiersMap = new HashMap<>();

    int point = 0;
    for (String tier : TIER_LIST) {
      for (String rank : RANK_LIST) {
        String key = tier + "-" + rank;
        tiersMap.put(key, point);
        point++;
      }
    }

    return tiersMap;
  }

  private String getCurrentSeasonTier(String summonerName) {
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    RiotApi riotApi = new RiotApi(config);

    try {
      Summoner summoner = riotApi.getSummonerByName(Platform.KR, summonerName);
      String summonerId = summoner.getId();
      Set<LeagueEntry> leagueEntrySet = riotApi
              .getLeagueEntriesBySummonerId(Platform.KR, summonerId);

      List<LeagueEntry> leagueEntries = Lists.newArrayList(leagueEntrySet);

      String tier = DEFAULT_TIER;

      for (LeagueEntry leagueEntry : leagueEntries) {
        if (leagueEntry.getQueueType().equals("RANKED_SOLO_5x5")) {
          tier = leagueEntry.getTier() + "-" + leagueEntry.getRank();
        }
      }

      return tier;
    } catch (RiotApiException e) {
      logger.error("{}", e);
      throw new IllegalArgumentException("RiotApiException");
    }
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...", Color.RED);
  }
}