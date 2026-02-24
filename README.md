# 鉱石採掘ゲーム 🪨⛏️　 
 
## 💫 はじめに
本リポジトリは、Javaを用いて作成したMinecraftプラグイン『鉱石採掘ゲーム（OreHunter）』に関するものです。  
Javaでの機能を実装し、制限時間内にランダムで出現する鉱石を採掘し、高得点を目指すゲームにしました。  
スコアはデータベースに保存されます。
 
※ ご利用に関するトラブル等につきましては、一切の責任を負いかねますことを予めご了承ください。    
<br/> 

---

## 🎥 制作背景  

---

## ⚙️ 使用技術・開発環境
| |技術・環境   |
|-----|-----|
| バックエンド |![Java](https://img.shields.io/badge/Java-21-grey.svg?style=plastic&labelColor=red) |
| アプリケーション |![Minecraft](https://img.shields.io/badge/Minecraft-1.21.5-grey.svg?style=plastic&labelColor=success) |
| サーバー |![Spigot](https://img.shields.io/badge/Spigot-1.21.5-grey.svg?style=plastic&logo=spigotmc&labelColor=ED8106&logoColor=white) |
| データベース |![MySQL](https://img.shields.io/badge/MySQL-8.0.43-grey.svg?style=plastic&logo=mysql&labelColor=4479A1&logoColor=white) | 
| 使用ツール |![MyBatis](https://img.shields.io/badge/MyBatis-3.5.13-grey.svg?style=plastic&labelColor=DD0700)&nbsp;![GitHub](https://img.shields.io/badge/GitHub-black?logo=github)&nbsp;![IntelliJ IDEA](https://img.shields.io/badge/intellij%20IDEA-2025.1.3-grey.svg?style=plastic&logo=intellijidea&labelColor=000000)| 
 
<br/> 

---

## 🎬 プレイ動画と実装方法  
Javaでの機能を実装し、制限時間のカウントと同時に鉱石がランダムで出現する仕様にしました。

https://github.com/user-attachments/assets/9e402620-06f3-4a73-a32d-03ed0cc0ff24

〈鉱石の出現〉 
- 5種類の鉱石をリストで定義し、そのリストの中からランダムで１種類 抽選することで、拡張しやすい構成にしました。
```java
 private Material getOre() {
    List<Material> oreList = List.of(Material.DIAMOND_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE,Material.IRON_ORE,Material.STONE);
    int random = new SplittableRandom().nextInt(oreList.size());
    return oreList.get(random);
  }
```
<br/>  

- 出現場所は、プレイヤーを中心として出現範囲の条件が指定されており、その中からランダムで抽選します。<br/>
抽選した場所が条件に満たない場合は、再抽選を行うことで、ゲームの進行性を保つようにしました。
```java
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
          && blockLocation.getRelative(BlockFace.DOWN).getType().isSolid()) {
        return blockLocation;
      }
    }
    return playerLocation.getBlock().getRelative(BlockFace.DOWN,2);
  }
```
<br/>


〈鉱石ごとの情報管理〉
- 列挙型(enum)を用いて、鉱石ごとに異なる点数とメッセージを一元管理しています。<br/>
fromMaterial メソッドにより、Material から対応する鉱石情報を取得できるようにしています。<br/>
これにより、if文などの条件分岐を増やすことなく、後から情報を追加する場合でも対応しやすく拡張性を持たせた設計にしました。
```java
public enum OreInfo {
  DIAMOND(Material.DIAMOND_ORE, 100, "ダイヤモンド鉱石！"),
  LAPIS(Material.LAPIS_ORE, 50, "ラピスラズリ鉱石！"),
  REDSTONE(Material.REDSTONE_ORE, 30, "レッドストーン鉱石！"),
  IRON(Material.IRON_ORE, 10, "鉄鉱石！"),
  STONE(Material.STONE, -50, "残念！石は-50点！");

public static OreInfo fromMaterial(Material material) {
  for (OreInfo oreInfo : values()) {
    if (oreInfo.getMaterial() == material) {
      return oreInfo;
     }
   }
   return null;
 }
}
```
<br/> 


〈スコア加算〉
- 出現した鉱石のみをスコア加算対象として判定し、鉱石が破壊(採掘)された瞬間にスコアが加算されるよう実装しました。<br/>
スコア加算対象の判定方法は、出現した鉱石をリストで管理しておき、そのリストとゲーム中に破壊(採掘)したブロックが一致するかどうかで判定しています。<br/>
特定のスコア加算対象にすることで、ゲーム性を保つよう実装しました。なお、リストと一致しないブロックが破壊された場合はスコア加算対象とせず、ゲーム終了後に地形復元として復元できるようにブロック情報を記録します。
```java
public void oreCrush(BlockBreakEvent e) {
    Block brokenBlock = e.getBlock();
    Player player = e.getPlayer();

    if (executingPlayerList.isEmpty()){
      return;
    }

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
```
<br/>


〈地形復元〉
- スコア加算対象外のブロックが破壊された場合、破壊されたブロックの位置とその位置に元々あったブロックの種類を<br/>
Map<Location, Material>で記録します。ゲーム終了後に、出現した鉱石を削除した上で、記録しておいた情報をもとに各ブロックを元の状態へ戻すことで、ゲーム開始前の地形を保つ設計としました。
```java
Location brokenLocation = brokenBlock.getLocation();
    if (oreBlockList.stream()
        .noneMatch(ore -> ore.equals(brokenBlock))){
      brokenBlockTypes.putIfAbsent(brokenLocation, brokenBlock.getType());
      return;
    }


oreBlockList.forEach(oreBlock
        -> oreBlock.setType(Material.AIR));
    oreBlockList.clear();

brokenBlockTypes.forEach((restoreLocation, originalBlock)
        -> restoreLocation.getBlock().setType(originalBlock));
    brokenBlockTypes.clear();
```
<br/>


〈時間管理〉  
ゲームの進行は、スケジューラーを用いて「開始前カウントダウン」「ゲーム本編（制限時間管理）」「終了処理」の3段階に分けて管理しています。
メソッド抽出を行い、各段階の責務を明確に分けて処理することで、処理の見通しを良くし拡張や修正を行いやすい構成にしました。
- 開始前カウントダウン
```java
private void startGameCountdown(Player player, ExecutingPlayer nowExecutingPlayer) {
    Bukkit.getScheduler().runTaskTimer(main, task -> {
      if (nowExecutingPlayer.getCountdownTime() > 0) {
        player.sendTitle(String.valueOf(nowExecutingPlayer.getCountdownTime()),
            "鉱石をたくさん採掘し高得点を目指そう！", 0, STAY_TIME, 0);
        nowExecutingPlayer.setCountdownTime(nowExecutingPlayer.getCountdownTime() -1);
        return;
      }

      if (nowExecutingPlayer.getCountdownTime() == 0) {
        player.sendTitle("START！",
            "鉱石をたくさん採掘し高得点を目指そう！", 0, STAY_TIME, FADE_OUT_TIME);
        nowExecutingPlayer.setCountdownTime(nowExecutingPlayer.getCountdownTime() - 1);

        gamePlay(player, nowExecutingPlayer);
        task.cancel();
      }
    }, 0, 20);
  }
```
- ゲーム本編(制限時間管理)
```java
private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer) {
    Bukkit.getScheduler().runTaskTimer(main, task -> {
      if (nowExecutingPlayer.getGameTime() <= 0) {
        finishGame(player, nowExecutingPlayer, task);
        return;
      }

      if (nowExecutingPlayer.getGameTime() <= COUNTDOWN_TIME) {
        player.sendTitle(String.valueOf(nowExecutingPlayer.getGameTime()),
            "", 0, STAY_TIME, 0);
      }

      oreSpawn(player);

      nowExecutingPlayer.setGameTime(nowExecutingPlayer.getGameTime() - 1);

    }, 0, 20);
  }
```
- 終了処理
```java
private void finishGame(Player player, ExecutingPlayer nowExecutingPlayer, BukkitTask task) {
    task.cancel();
    player.sendTitle("ゲーム終了！",
        nowExecutingPlayer.getPlayerName() + "合計 " + nowExecutingPlayer.getScore() + "点！",
        0, STAY_LONG_TIME, FADE_OUT_TIME);
}
```

---

## 🤖 スコア確認動画とデータベースについて  
スコアはデータベースに保存されます。<br/>
コマンドで`/orehunter list`を実行することで、データベースに保存された過去のスコア履歴を確認することができます。

【ゲーム開始前のスコア確認動画】<br/>

https://github.com/user-attachments/assets/2d9aee91-485d-40fe-ae91-15e3eb785852

【ゲーム終了後のスコア確認動画】<br/>

https://github.com/user-attachments/assets/052271e2-ccd3-4999-afb4-006ac72b88c3 

## 🤖 データベースについて  
### ◇ データベースの接続方法
１. ご自身のローカル環境でMySQLに接続してください。  
２. 以下のSQLコマンドを順番に実行してください。  
<br/>  

① データベース作成
```
CREATE DATABASE orehunter_game;
```  
② データベース選択
```
USE orehunter_game;
```
③ テーブル作成（Mac か Windows どちらか一方を実行してください）  

######  ＊　Macの場合
```
CREATE TABLE player_score(id int auto_increment, player_name varchar(100), score int, registered_at datetime, primary key(id));
```  
######  ＊　Windowsの場合
```
CREATE TABLE player_score(id int auto_increment, player_name varchar(100), score int, registered_at datetime, primary key(id)) DEFAULT CHARSET=utf8;
```
<br/>  
３. MySQLの接続情報（ url , username , password ）は、ご自身のローカル環境に合わせて「 mybatis-config.xml 」に設定してください。  
<br/>  
<br/>  
<br/>  

### ◇ データベースの構成
データベース名：orehunter_game  
テーブル名　　：player_score  
|カラム名|詳細|
|---|---|
|id|主キー（AUTO_INCREMENT）|
|player_name|プレイヤー名|
|score|ゲームでの獲得スコア|
|registered_at|登録日時|  
<br/>   


---


## 📖 コマンド一覧
|コマンド|詳細|
|---|---|
|`/orehunter`|ゲーム開始|
|`/orehunter list`|プレイヤーのスコア履歴表示|  
<br/>   

---

##  📝 工夫した点
#### ◇ カウントダウンの演出 
- ゲーム開始時と終了時に５秒間のカウントダウンを表示することで、ゲームのタイミングを把握できるようにしました。

#### ◇ 鉱石の出現パターンと得点設定
- ランダム配置で時間の経過と共に次々と出現させることで、「反射神経✖️判断力」が求められるスピード感を重視したゲームにしました。
- 鉱石の中に、マイナスポイントの「石」を混在させることで、プレイヤーのスコアを左右するようゲーム性をもたせました。

#### ◇ ブロック復元処理 
- 鉱石が出現した場所や、採掘時に誤って破壊してしまったブロックの元の状態を記録しておき、ゲーム終了後に自動で復元される仕組みにしました。
- プレイヤーは、地形や建物を破壊してしまっても心配することなく、好きな場所でゲームを楽しむことができます。
  
#### ◇ 採掘方法の設定 
- 出現した鉱石は、1回叩くだけで採掘できるように設定し、次々と出現する鉱石をテンポよく採掘できるようにしました。
<br/>   

---

## 🗝️ ゲーム概要
次々と出現する鉱石を、20秒の制限時間内に採掘して高得点を目指すゲームです。  
鉱石の種類によって獲得ポイントが異なり、石はマイナスポイントなので採掘時には注意しましょう。

進行方向に鉱石が突如出現し、行く手を阻むこともあります。  
そんな時は、「採掘するか・回避するか」プレイヤーの瞬発力が試されます。

どんな時でも、鉱石を素早く見極める力が高得点への鍵となります。

ゲーム終了後のスコアはデータベースに保存され、いつでもスコア履歴を確認することができます。  

※このゲームは、シングルプレイ用として作成しています。  
<br/>  

---

## 🎬 プレイ動画  

https://github.com/user-attachments/assets/2d9aee91-485d-40fe-ae91-15e3eb785852

https://github.com/user-attachments/assets/9e402620-06f3-4a73-a32d-03ed0cc0ff24

https://github.com/user-attachments/assets/052271e2-ccd3-4999-afb4-006ac72b88c3  

<br/> 

※ 動画容量の関係で画質を調整している為、一部文字が見えづらい箇所がございます。

<br/>  

---


## 🎮 ゲーム詳細
### ◇ ゲーム開始の準備  
１. コマンドで`/orehunter`と入力し、エンターキーを押します。
- プレイヤーの体力と空腹度は最大値に設定され、採掘道具(ツルハシ)が装備されます。
- ゲーム開始までのカウントダウンが始まり、「5 , 4 , 3 , 2 , 1 , START！」と表示されます。

　※ 広く平らな地形でのコマンド実行を推奨します。  
　※ メインハンドのアイテムは上書きされますので、何も持たない状態でのコマンド実行を推奨します。  
　<br/> 
 
 ### ◇ ゲーム開始
１. 20秒間のゲームが開始します。  
<br/>  

２. 出現する鉱石を採掘します。
- 鉱石は、1秒毎に2個ずつランダムで出現し、種類によって獲得ポイントが異なります。( 詳細は、［鉱石別得点一覧］をご参照下さい )
- 出現場所は、プレイヤーを中心とした約20✖️20ブロックの範囲にランダムで出現します。
- 採掘方法は、ツルハシで1回叩くだけで採掘可能です。
- 採掘時には、ポイントが即時加算され、採掘した鉱石に応じて「 鉱石名 , 獲得ポイント , 合計スコア 」が表示されます。
<br/>  

３. ゲーム終了 5秒前からカウントダウンが始まり、「5 , 4 , 3 , 2 , 1 , ゲーム終了！」と表示されます。  
<br/>

### ◇ ゲーム終了後
１. スコアが表示され、周囲のブロック状態は元の状態に戻されます。  
- 出現した鉱石はエリア内から消え、誤って破壊してしまった地形ブロックなどは復元されます。

　※ プレイヤーが、復元対象のブロックと同じ位置にいた場合、復元と同時に地中に埋まってしまう可能性がありますのでご注意ください。  
　<br/> 
 
２. スコアはデータベースに保存されます。
- コマンドで`/orehunter list`と入力することで確認できます。

　※ 誤ったコマンド引数を入力した場合、「 実行できません。コマンド引数を正しく入力する必要があります。[list] 」と表示されます。    
<br/>   

---

##  📝 工夫した点
#### ◇ カウントダウンの演出 
- ゲーム開始時と終了時に５秒間のカウントダウンを表示することで、ゲームのタイミングを把握できるようにしました。

#### ◇ 鉱石の出現パターンと得点設定
- ランダム配置で時間の経過と共に次々と出現させることで、「反射神経✖️判断力」が求められるスピード感を重視したゲームにしました。
- 鉱石の中に、マイナスポイントの「石」を混在させることで、プレイヤーのスコアを左右するようゲーム性をもたせました。

#### ◇ ブロック復元処理 
- 鉱石が出現した場所や、採掘時に誤って破壊してしまったブロックの元の状態を記録しておき、ゲーム終了後に自動で復元される仕組みにしました。
- プレイヤーは、地形や建物を破壊してしまっても心配することなく、好きな場所でゲームを楽しむことができます。
  
#### ◇ 採掘方法の設定 
- 出現した鉱石は、1回叩くだけで採掘できるように設定し、次々と出現する鉱石をテンポよく採掘できるようにしました。
<br/>   

---

## 💎 鉱石別得点一覧
|鉱石名|ポイント|
|---|---|
|ダイヤモンド鉱石|100 ポイント|
|ラピスラズリ鉱石|50 ポイント|
|レッドストーン鉱石|30 ポイント|
|鉄鉱石|10 ポイント|
|石|-50 ポイント|  
<br/>   

---

## 🍀 おわりに  
このプラグインは、Javaのオブジェクト指向・データベース連携・コードの可読性など、学んだ内容を意識し開発しました。  
学習からプラグイン開発を通し、設計・実装・改善を繰り返しながらコードを組み立て、必要な情報を調べながら進めることができたと感じています。 

引き続き学習を継続しながら知識を深め、より良いコードを書けるよう努めて参ります。  

また、ご意見やお気づきの点がございましたら、お手数ですが Xアカウント（[@nap_7878](https://x.com/nap_7878)）までご連絡いただけましたら幸いです。  

最後までご覧いただきありがとうございました。  








 

  



