package kr.webgori.lolien.discord.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SummonerMostChampDto {
  private int championId;
  private long count;
}
