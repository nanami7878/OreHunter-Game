package plugin.orehunter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.orehunter.command.OreHunterCommand;

public final class Main extends JavaPlugin {


  @Override
  public void onEnable() {
    OreHunterCommand oreHunterCommand = new OreHunterCommand(this);
    Bukkit.getPluginManager().registerEvents(oreHunterCommand,this);
    getCommand("oreHunter").setExecutor(oreHunterCommand);

  }

}
