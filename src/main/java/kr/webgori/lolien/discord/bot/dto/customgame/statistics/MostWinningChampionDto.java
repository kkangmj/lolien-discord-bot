package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostWinningChampionDto {
  private int championId;
  private boolean win;
}
