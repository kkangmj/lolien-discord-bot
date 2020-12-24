package kr.webgori.lolien.discord.bot.entity.league;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "lolien_league")
public class LolienLeague {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer idx;

  private String title;

  @JsonBackReference(value = "lolienLeagueMatchLolienLeague")
  @OneToMany(mappedBy = "lolienLeague", cascade = CascadeType.ALL)
  private List<LolienLeagueMatch> lolienLeagueMatches;

  @CreatedDate
  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;
}