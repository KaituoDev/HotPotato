package fun.kaituo.hotpotato.util;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionSign extends AbstractSignListener {
    @Getter
    private int explosionInterval = 20;

    public ExplosionSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "§0§l爆炸间隔");
        lines.set(2, "§b§l" + explosionInterval + "§0§l 秒");
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        if (explosionInterval < 30) {
            explosionInterval += 5;
            lines.set(2, "§b§l" + explosionInterval + "§0§l 秒");
            update();
        }
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        if (explosionInterval > 10) {
            explosionInterval -= 5;
            lines.set(2, "§b§l" + explosionInterval + "§0§l 秒");
            update();
        }
    }
}
