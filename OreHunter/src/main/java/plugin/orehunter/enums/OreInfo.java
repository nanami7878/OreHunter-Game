package plugin.orehunter.enums;

import lombok.Getter;
import org.bukkit.Material;

/**
 *出現した鉱石の種類毎に鉱石情報を設定し、管理する列挙型です。
 */


@Getter
public enum OreInfo {
  DIAMOND(Material.DIAMOND_ORE, 100, "ダイヤモンド鉱石！"),
  LAPIS(Material.LAPIS_ORE, 50, "ラピスラズリ鉱石！"),
  REDSTONE(Material.REDSTONE_ORE, 30, "レッドストーン鉱石！"),
  IRON(Material.IRON_ORE, 10, "鉄鉱石！"),
  STONE(Material.STONE, -50, "残念！石は-50点！");


  private final Material material;
  private final int score;
  private final String message;




  OreInfo(Material material, int score, String message) {
    this.material = material;
    this.score = score;
    this.message = message;
  }


  public static OreInfo fromMaterial(Material material) {
    for (OreInfo oreInfo : values()) {
      if (oreInfo.getMaterial() == material) {
        return oreInfo;
      }
    }
    return null;
  }

}