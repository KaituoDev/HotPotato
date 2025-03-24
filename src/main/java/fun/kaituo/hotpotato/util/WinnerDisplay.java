package fun.kaituo.hotpotato.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

public class WinnerDisplay {
    @Getter
    @Setter
    private Location displayLocation;

    public WinnerDisplay(Location displayLocation) {
        this.displayLocation = displayLocation;
    }

    public void displayWinner(Player winner) {
        if (displayLocation == null) {
            return;
        }

        Block block = displayLocation.getBlock();
        block.setType(Material.PLAYER_HEAD);

        if (block.getState() instanceof Skull skull) {
            PlayerProfile profile = winner.getPlayerProfile();
            skull.setOwnerProfile(profile);
            skull.update(true);
        }
    }
}
