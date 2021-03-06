package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienSummonerRepository extends JpaRepository<LolienSummoner, Integer> {
  boolean existsBySummonerName(String summonerName);

  boolean existsByAccountId(String accountId);

  LolienSummoner findBySummonerName(String summonerName);

  Optional<LolienSummoner> findBySummonerNameIs(String summonerName);

  LolienSummoner findByAccountId(String accountId);

  List<LolienSummoner> findTop5ByIdxNotIn(Set<Integer> summonersIdx);

  List<LolienSummoner> findTop5By();

  List<LolienSummoner> findByMmrNotNullOrderByMmrAsc();

  List<LolienSummoner> findByMmrNotNullOrderByMmrDesc();

  long countByIdIn(List<String> ids);
}
