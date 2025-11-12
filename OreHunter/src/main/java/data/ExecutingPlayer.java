package data;

import lombok.Getter;
import lombok.Setter;

/**
 * OreHunterのゲームを実行する際の、プレイヤー情報を扱うオブジェクトデーターです。
 */

@Getter
@Setter

public class ExecutingPlayer {

  private String playerName;
  private int score;
  private int countdownTime;
  private int gameTime;


  public ExecutingPlayer(String playerName) {
    this.playerName = playerName;
  }
}
