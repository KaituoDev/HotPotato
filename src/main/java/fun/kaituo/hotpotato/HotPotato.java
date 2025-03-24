package fun.kaituo.hotpotato;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.hotpotato.state.WaitState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HotPotato extends Game {
    private static HotPotato instance;
    public static HotPotato inst() { return instance; }

    public final Set<UUID> playerIds = new HashSet<>();
    private ItemStack hotPotato;

    public Set<Player> getPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playerIds) {
            Player p = Bukkit.getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    public ItemStack getHotPotato() {
        if (hotPotato == null) {
            hotPotato = new ItemStack(Material.BAKED_POTATO);
            ItemMeta meta = hotPotato.getItemMeta();
            meta.setDisplayName("§c§l烫手山芋");
            List<String> lore = new ArrayList<>();
            lore.add("§e§o快把它传给别人！");
            meta.setLore(lore);
            hotPotato.setItemMeta(meta);
        }
        return hotPotato;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addPlayer(Player p) {
        // Check if player is in another game and remove them
        if (GameUtils.inst().getGame(p) != this) {
            GameUtils.inst().getGame(p).removePlayer(p);
        }

        // Add player to this game
        playerIds.add(p.getUniqueId());
        p.sendTitle("§c§l烫手§e§l山芋","",10,30,20);
        super.addPlayer(p);

        if (state == WaitState.INST) {
            WaitState.INST.addPlayer(p);
        }
    }
    @Override
    public void removePlayer(Player p) {
        playerIds.remove(p.getUniqueId());
        super.removePlayer(p);
    }

    @Override
    public void forceStop() {
        if(state != WaitState.INST){
            setState(WaitState.INST);
        }

    }

    @Override
    public void tick() {
        super.tick();
    }


    private void initStates() {
        WaitState.INST.init();
        //PlayState.INST.init();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        updateExtraInfo("§c烫手山芋", getLoc("hubLoc"));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            initStates();
            setState(WaitState.INST);
        }, 1);
    }

    @Override
    public void onDisable() {
        for (Player p : getPlayers()) {
            removePlayer(p);
            GameUtils.inst().join(p, GameUtils.inst().getLobby());
        }
        this.state.exit();
        Bukkit.getScheduler().cancelTasks(this);
        super.onDisable();
    }
}