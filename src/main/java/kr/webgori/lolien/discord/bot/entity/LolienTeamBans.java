package kr.webgori.lolien.discord.bot.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "team_bans")
@EqualsAndHashCode(exclude = {"teamStats"})
public class LolienTeamBans {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  @JsonManagedReference(value = "lolienTeamBansTeamStats")
  @ManyToOne
  @JoinColumn(name = "team_stats_idx", nullable = false)
  private LolienTeamStats teamStats;

  @Column(name = "champion_id")
  private Integer championId;
  @Column(name = "pick_turn")
  private Integer pickTurn;
}