package fun.kaituo.hotpotato;

import fun.kaituo.gameutils.Game;
import fun.kaituo.gameutils.event.PlayerChangeGameEvent;
import fun.kaituo.gameutils.event.PlayerEndGameEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HotPotatoGame extends Game implements Listener {
    private static final HotPotatoGame instance = new HotPotatoGame((HotPotato) Bukkit.getPluginManager().getPlugin("HotPotato"));
    List<Player> playersAlive;
    ItemStack potato;
    ItemStack tnt;
    long startTime;
    int countDownSeconds = 5;

    private HotPotatoGame(HotPotato plugin) {
        this.plugin = plugin;
        players = HotPotato.players;
        playersAlive = new ArrayList<>();
        potato = new ItemStack(Material.BAKED_POTATO, 1);
        tnt = new ItemStack(Material.TNT, 1);
        initializeGame(plugin, "HotPotato", "§e烫手山芋", new Location(world, 1000, 12, 1000));
        initializeButtons(new Location(world, 1000, 13, 996),
                BlockFace.SOUTH, new Location(world, 1004, 13, 1000), BlockFace.WEST);
        initializeGameRunnable();
    }

    public static HotPotatoGame getInstance() {
        return instance;
    }

    @EventHandler
    public void preventDamage(EntityDamageEvent ede) {
        if (ede.getEntity() instanceof Player) {
            if (playersAlive.contains(ede.getEntity())) {
                ede.setDamage(0);
            }
        }
    }

    @EventHandler
    public void preventDroppingItem(PlayerDropItemEvent pdie) {
        if (playersAlive.contains(pdie.getPlayer())) {
            pdie.setCancelled(true);
        }
    }

    @EventHandler
    public void preventClickingInventory(InventoryClickEvent ice) {
        if (ice.getWhoClicked() instanceof Player) {
            if (playersAlive.contains(ice.getWhoClicked())) {
                ice.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void transferPotato(EntityDamageByEntityEvent edbee) {
        if (edbee.getDamager() instanceof Firework) {
            edbee.setCancelled(true);
            return;
        }
        if (edbee.getDamager() instanceof Player && edbee.getEntity() instanceof Player) {
            if (playersAlive.contains(edbee.getDamager()) && playersAlive.contains(edbee.getEntity())) {
                if (((Player) edbee.getDamager()).getInventory().contains(potato)) {
                    ((Player) edbee.getDamager()).removePotionEffect(PotionEffectType.SPEED);
                    ((Player) edbee.getDamager()).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
                    ((Player) edbee.getDamager()).getInventory().clear();
                    ((Player) edbee.getEntity()).getInventory().setItem(0, potato);
                    ((Player) edbee.getEntity()).getInventory().setItem(EquipmentSlot.HEAD, tnt);
                    ((Player) edbee.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false));
                    for (Player p : players) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§b§l" + edbee.getEntity().getName() + " 接到了山芋！"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
        playersAlive.remove(pcge.getPlayer());
    }


    private void initializeGameRunnable() {
        gameRunnable = () -> {

            Collection<Player> startingPlayers = getPlayersNearHub(50, 50, 50);
            players.addAll(startingPlayers);
            playersAlive.addAll(startingPlayers);
            if (players.size() < 2) {
                for (Player p : players) {
                    p.sendMessage("§c至少需要2人才能开始游戏！");
                }
                players.clear();
                playersAlive.clear();
            } else {
                startTime = getTime(world);
                removeStartButton();
                Bukkit.getPluginManager().registerEvents(this, plugin);
                startCountdown(countDownSeconds);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player p : players) {
                        p.getInventory().clear();
                    }
                });
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    placeSpectateButton();
                    for (Player p : players) {
                        p.teleport(new Location(world, 1001.5, 94, 993.5));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
                    }

                }, countDownSeconds * 20);
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    int time = 20 - (int) (((getTime(world) - startTime) / 20) % 20);
                    List<Player> playersCopy = new ArrayList<>(players);
                    if (time <= 15) {
                        for (Player p : playersCopy) {
                            p.setLevel(time);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.2f, 1f);
                        }
                    } else {
                        for (Player p : playersCopy) {
                            p.setLevel(0);
                        }
                    }
                }, countDownSeconds * 20, 20));
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    for (Player p : playersAlive) {
                        if (p.getInventory().contains(potato)) {
                            spawnFirework(p);
                        }
                    }
                }, countDownSeconds * 20 + 50, 100))
                ;
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    int i = random.nextInt(playersAlive.size());
                    playersAlive.get(i).getInventory().setItem(0, potato);
                    playersAlive.get(i).getInventory().setItem(EquipmentSlot.HEAD, tnt);
                    playersAlive.get(i).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false));
                    for (Player p : players) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§b§l" + playersAlive.get(i).getName() + " 接到了山芋！"));
                    }
                }, countDownSeconds * 20, 400))
                ;
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    Player playerOut = null;
                    for (Player p : playersAlive) {
                        if (p.getInventory().contains(potato)) {
                            playerOut = p;
                        }
                    }
                    if (playerOut != null) {
                        for (Player p : players) {
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f§l" + playerOut.getName() + " §c爆炸了！"));
                        }
                        playerOut.setGameMode(GameMode.SPECTATOR);
                        world.createExplosion(playerOut.getLocation(), 0.5f, false, false);
                        playersAlive.remove(playerOut);
                    }

                }, countDownSeconds * 20 + 300, 400));
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (playersAlive.size() <= 1) {
                        Player winner = playersAlive.get(0);
                        spawnFireworks(winner);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : playersCopy) {
                            p.sendTitle("§e" + playersAlive.get(0).getName() + " §b获胜了！", null, 5, 50, 5);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(new Location(world, 1000.5, 12.0625, 999.5));
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, this));
                            }, 100);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            placeStartButton();
                            removeSpectateButton();
                            HandlerList.unregisterAll(this);
                        }, 100);
                        players.clear();
                        playersAlive.clear();
                        List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
                        taskIds.clear();
                        for (int i : taskIdsCopy) {
                            Bukkit.getScheduler().cancelTask(i);
                        }
                    }

                }, 100, 1));
            }
        };
    }

    @Override
    protected void quit(Player p) throws IOException {
        players.remove(p);
        playersAlive.remove(p);
    }

    @Override
    protected boolean rejoin(Player player) {
        return false;
    }

    @Override
    protected boolean join(Player player) {
        player.setBedSpawnLocation(hubLocation, true);
        player.teleport(hubLocation);
        return true;
    }

    @Override
    protected void forceStop() {
        if (playersAlive.isEmpty()) {
            return;
        }
        List<Player> playersCopy = new ArrayList<>(players);
        for (Player p : playersCopy) {
            p.sendTitle("§c游戏被强制停止", null, 5, 50, 5);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.teleport(new Location(world, 1000.5, 12.0625, 999.5));
                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, this));
            }, 100);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            placeStartButton();
            removeSpectateButton();
            HandlerList.unregisterAll(this);
        }, 100);
        players.clear();
        playersAlive.clear();
        List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
        taskIds.clear();
        for (int i : taskIdsCopy) {
            Bukkit.getScheduler().cancelTask(i);
        }
    }
}
