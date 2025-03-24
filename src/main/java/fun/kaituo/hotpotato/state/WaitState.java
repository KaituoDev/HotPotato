package fun.kaituo.hotpotato.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import fun.kaituo.hotpotato.HotPotato;
import fun.kaituo.hotpotato.util.ExplosionSign;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaitState implements GameState, Listener {
    public static final WaitState INST = new WaitState();
    private WaitState() {}

    private HotPotato game;
    private Location startButtonLoc;
    private ExplosionSign explosionSign;

    public void init() {
        game = HotPotato.inst();
        startButtonLoc = game.getLoc("startButton");

        Location ExplosionSignLoc = game.getLoc("ExplosionSign");
        assert ExplosionSignLoc != null;
        explosionSign = new ExplosionSign(game, ExplosionSignLoc);
    }

    public int getExplosionInterval() {
        return explosionSign.getExplosionInterval();
    }

    @EventHandler
    public void onClickStartButton(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Block block = e.getClickedBlock();
        assert block != null;
        if (!block.getLocation().equals(startButtonLoc)) {
            return;
        }
        if (game.getPlayers().size() < 2) {
            e.getPlayer().sendMessage("§c至少需要两名玩家才能开始游戏！");
        } else {
            game.setState(PlayState.INST);
        }
    }

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, game);
        Bukkit.getPluginManager().registerEvents(explosionSign, game);
        for (Player p : game.getPlayers()) {
            addPlayer(p);
        }
    }

    @Override
    public void exit() {
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(explosionSign);
        for (Player p : game.getPlayers()) {
            removePlayer(p);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void addPlayer(Player p) {
        p.getInventory().clear();
        p.getInventory().addItem(Misc.getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.teleport(game.getLocation());
    }

    @Override
    public void removePlayer(Player p) {
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.getInventory().clear();
    }

    @Override
    public void forceStop() {
    }
}