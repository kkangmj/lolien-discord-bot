package kr.webgori.lolien.discord.bot.component;

import java.util.List;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;


import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class HelpComponent {
  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();

    String commandDesc = "┌LoLienBot 도움말 입니다. 자세한 문의사항은 GitHub ( https://github.com/webgori/spring-discord-bot ) 이슈 등록 부탁드립니다.";
    String summonerCommand = "├!소환사 등록 소환사명: 소환사를 Database에 등록합니다.";
    String teamGenerateCommand = "├!팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...: 내전 팀 구성시 평균 티어를 맞추어 팀을 구성합니다.";
    String customGameCommand1 = "├!내전 결과 조회: 최근 5개의 내전 이력을 보여줍니다.";
    String customGameCommand2 = "├!내전 결과 등록 대전기록-URL 참가자목록(순서대로): 내전 후의 결과 데이터를 Database에 등록합니다.";
    String customGameCommand3 = "├!내전 모스트 소환사명: 내전시 해당 소환사가 가장 많이 선택했던 챔프를 1위부터 5위까지 승률과 함께 보여줍니다.";
    String customGameCommand4 = "├!내전 참여횟수: 내전 참여 횟수를 소환사명과 함께 1위부터 5위까지 보여줍니다.";
    String customGameCommand5 = "└!내전 참여횟수 소환사명1, 소환사명2, 소환사명3 ...: 해당 소환사명의 내전 참여 횟수를 보여줍니다.";

    List<String> commands = Lists.newArrayList(commandDesc, summonerCommand, teamGenerateCommand,
            customGameCommand1, customGameCommand2, customGameCommand3, customGameCommand4,
            customGameCommand5);

    for (String command : commands) {
      sendMessage(textChannel, command);
    }
  }
}