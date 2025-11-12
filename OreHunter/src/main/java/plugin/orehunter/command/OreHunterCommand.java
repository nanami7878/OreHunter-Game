package plugin.orehunter.command;
import data.ExecutingPlayer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import plugin.orehunter.PlayerScoreData;
import plugin.orehunter.Main;
import plugin.orehunter.enums.OreInfo;
import plugin.orehunter.mapper.data.PlayerScore;


/**
 * 制限時間内に、ランダムで出現する鉱石を破壊して、高得点を目指すタイムアタックゲームを起動するコマンドです。
 * 点数は鉱石により異なり、破壊した鉱石の合計によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時で保存されます。
 */

public class OreHunterCommand extends BaseCommand implements Listener {

  public static final String LIST = "list";
  public static final int MAX_HEALTH = 20;
  public static final int MAX_FOOD_LEVEL = 20;
  public static final int COUNTDOWN_TIME = 5;
  public static final int GAME_TIME = 20;
  public static final int MAX_SPAWN_ATTEMPTS = 30;
  public static final int SPAWN_COUNT = 2;


  private final Main main;
  private final PlayerScoreData playerScoreData = new PlayerScoreData();
  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<Block> oreBlockList = new ArrayList<>();
  private final Map<Location, Material> brokenBlockTypes = new HashMap<>();



  public OreHunterCommand(Main main) {
    this.main = main;
  }


  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {
    //最初の引数が「list」だったらスコア一覧を表示して処理を終了します。
    if (args.length == 1 && LIST.equals(args[0])) {
      sendPlayerScoreList(player);
      return false;
    }

    if (args.length != 0) {
      player.sendMessage(ChatColor.RED + "実行できません。コマンド引数を正しく入力する必要があります。[list]");
      return false;
    }

    ExecutingPlayer nowExecutingPlayer = getPlayerScore(player);

    initPlayerStatus(player);

    startGameCountdown(player, nowExecutingPlayer);
    return true;
  }



  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label, String[] args) {
    return false;
  }



  /**
   * 現在登録されているスコアの一覧をメッセージに送ります。
   * @param player プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
  }



  /**
   * プレイヤーが、出現した鉱石にダメージを与えた瞬間に呼ばれるメソッドです。
   * 鉱石を一撃で破壊できるように設定します。
   * @param e　鉱石にダメージを与えた際のイベント情報
   */
  @EventHandler
  public void oreDamage(BlockDamageEvent e) {
    Block damagedBlock = e.getBlock();
    if (oreBlockList.contains(damagedBlock)) {
      e.setInstaBreak(true);
    }
  }



  @EventHandler
  public void oreCrush(BlockBreakEvent e) {
    Block brokenBlock = e.getBlock();
    Player player = e.getPlayer();

    //実行中のプレイヤーがいない(ゲーム中ではない)場合は処理しない。
    if (executingPlayerList.isEmpty())
      return;

    //鉱石以外のブロックを破壊した場合、地形復元用としてブロック情報を記録します。
    Location brokenLocation = brokenBlock.getLocation();
    if (oreBlockList.stream()
        .noneMatch(ore -> ore.equals(brokenBlock))){
      brokenBlockTypes.putIfAbsent(brokenLocation, brokenBlock.getType());
      return;
    }

    executingPlayerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
              OreInfo oreInfo = OreInfo.fromMaterial(brokenBlock.getType());
              if (oreInfo != null) {
                p.setScore(p.getScore() + oreInfo.getScore());
                player.sendMessage(oreInfo.getMessage() + " 現在のスコアは " + p.getScore() + "点！");
              }
        });
    }



  /**
   * 現在実行しているプレイヤーのスコア情報を取得します。
   * @param player コマンドを実行したプレイヤー
   * @return   現在実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer;
    if (executingPlayerList.isEmpty()) {
       executingPlayer = new ExecutingPlayer(player.getName());
      executingPlayerList.add(executingPlayer);
    }else{
      executingPlayer = executingPlayerList.getFirst();
    }

    executingPlayer.setCountdownTime(COUNTDOWN_TIME);
    executingPlayer.setGameTime(GAME_TIME);
    executingPlayer.setScore(0);
    return executingPlayer;
  }



  /**
   *ゲーム開始と同時にプレイヤーの初期状態を設定します。
   * 体力と空腹値が最大になり、採掘道具(ツルハシ)が装備されます。
   * @param player コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    player.setHealth(MAX_HEALTH);
    player.setFoodLevel(MAX_FOOD_LEVEL);

    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
  }



  /**
   * ゲーム開始までのカウントダウン(５〜１,START)を行います。
   * カウントダウン終了後に、ゲームが開始します。
   * @param player　コマンドを実行したプレイヤー
   * @param nowExecutingPlayer　プレイヤーのスコア情報
   */
  private void startGameCountdown(Player player, ExecutingPlayer nowExecutingPlayer) {
    Bukkit.getScheduler().runTaskTimer(main, task -> {
      if (nowExecutingPlayer.getCountdownTime() > 0) {
        player.sendTitle(String.valueOf(nowExecutingPlayer.getCountdownTime()),
            "鉱石をたくさん採掘し高得点を目指そう！", 0, 20, 0);
        nowExecutingPlayer.setCountdownTime(nowExecutingPlayer.getCountdownTime() -1);
        return;
      }


      if (nowExecutingPlayer.getCountdownTime() == 0) {
        player.sendTitle("START！",
            "鉱石をたくさん採掘し高得点を目指そう！", 0, 20, 20);
        nowExecutingPlayer.setCountdownTime(nowExecutingPlayer.getCountdownTime() - 1);

        gamePlay(player, nowExecutingPlayer);
        task.cancel();
      }
    }, 0, 20);
  }



  /**
   * ゲームが開始します。鉱石が出現し、制限時間内に破壊するとスコアが加算されます。
   * ゲーム終了５秒前からカウントダウンを開始します。
   * ゲーム終了後、スコア表示などの処理を行います。
   * @param player　コマンドを実行したプレイヤー
   * @param nowExecutingPlayer　プレイヤーのスコア情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer) {
    Bukkit.getScheduler().runTaskTimer(main, task -> {
      if (nowExecutingPlayer.getGameTime() <= 0) {
        finishGame(player, nowExecutingPlayer, task);
        return;
      }

      if (nowExecutingPlayer.getGameTime() <= COUNTDOWN_TIME) {
        player.sendTitle(String.valueOf(nowExecutingPlayer.getGameTime()),
            "", 0, 20, 0);
      }

      oreSpawn(player);

      nowExecutingPlayer.setGameTime(nowExecutingPlayer.getGameTime() - 1);

    }, 0, 20);
  }



  /**
   * ゲーム終了後の処理を行います。(スコア表示、出現した鉱石の削除、周囲のブロック状態を元に戻す）
   * @param player コマンドを実行したプレイヤー
   * @param nowExecutingPlayer　プレイヤーのスコア情報
   * @param task　ゲーム時間を管理するタイマー
   */
  private void finishGame(Player player, ExecutingPlayer nowExecutingPlayer, BukkitTask task) {
    task.cancel();
    player.sendTitle("ゲーム終了！",
        nowExecutingPlayer.getPlayerName() + "合計 " + nowExecutingPlayer.getScore() + "点！",
        0, 40, 20);


    oreBlockList.forEach(oreBlock
        -> oreBlock.setType(Material.AIR));
    oreBlockList.clear();


    brokenBlockTypes.forEach((restoreLocation, originalBlock)
        -> restoreLocation.getBlock().setType(originalBlock));
    brokenBlockTypes.clear();


    //スコア登録処理
    playerScoreData.insert(
        new PlayerScore(nowExecutingPlayer.getPlayerName()
            ,nowExecutingPlayer.getScore()));

    executingPlayerList.clear();
    }



  /**
   * 鉱石が１秒毎に2個ずつ出現します。
   * @param player コマンドを実行したプレイヤー
   */
  private void oreSpawn(Player player) {
    for (int i = 0; i < SPAWN_COUNT; i++) {
      Block oreSpawnBlock = getOreSpawnBlock(player);
      oreSpawnBlock.setType(getOre());
      oreBlockList.add(oreSpawnBlock);
    }
  }



  /**
   * プレイヤーを中心とし、ランダムで鉱石の出現場所を取得します。
   * 出現場所の取得条件に一致しない場合は、最大30回まで再抽選を行います。
   * @param player コマンドを実行したプレイヤー
   * @return 鉱石の出現場所
   */
  @NotNull
  private Block getOreSpawnBlock(Player player) {
    Location playerLocation = player.getLocation();
    SplittableRandom random = new SplittableRandom();

    for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
      int randomBlockX = random.nextInt(20) - 10;
      int randomBlockZ = random.nextInt(20) - 10;

      int x = playerLocation.getBlockX() + randomBlockX;
      int y = playerLocation.getBlockY();
      int z = playerLocation.getBlockZ() + randomBlockZ;

      Block blockLocation = player.getWorld().getBlockAt(x, y, z);

      if (blockLocation.getType().isAir()
          && blockLocation.getRelative(BlockFace.DOWN).getType().isSolid())
        return blockLocation;
    }
    return playerLocation.getBlock().getRelative(BlockFace.DOWN,2);
  }



  /**
   * 出現させる鉱石の種類をランダムで取得します。
   * @return 鉱石
   */
  private Material getOre() {
    List<Material> oreList = List.of(Material.DIAMOND_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE,Material.IRON_ORE,Material.STONE);
    int random = new SplittableRandom().nextInt(oreList.size());
    return oreList.get(random);
  }

}
