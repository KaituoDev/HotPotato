package fun.kaituo.hotpotato;


import fun.kaituo.gameutils.GameUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


public class HotPotato extends JavaPlugin implements Listener {
    private GameUtils gameUtils;
    static List<Player> players;

    public static HotPotatoGame getGameInstance() {
        return HotPotatoGame.getInstance();
    }

    @EventHandler
    public void onButtonClicked(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!pie.getClickedBlock().getType().equals(Material.OAK_BUTTON)) {
            return;
        }
        if (pie.getClickedBlock().getLocation().equals(new Location(gameUtils.getWorld(), 1000, 13, 996))) {
            HotPotatoGame.getInstance().startGame();
        }
    }

    public void onEnable() {
        gameUtils = (GameUtils) Bukkit.getPluginManager().getPlugin("GameUtils");
        players = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, this);
        gameUtils.registerGame(getGameInstance());
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
        if (players.size() > 0) {
            for (Player p : players) {
                Bukkit.dispatchCommand(p, "join Lobby");
            }
        }
        gameUtils.unregisterGame(getGameInstance());
    }
}