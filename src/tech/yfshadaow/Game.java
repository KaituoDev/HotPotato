package tech.yfshadaow;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Game extends BukkitRunnable implements Listener {
    HotPotato plugin;
    List<Player> players;
    List<Player> playersAlive;
    ItemStack potato;
    ItemStack tnt;
    Random random;
    List<Integer> taskIds;
    long startTime;

    public Game(HotPotato plugin) {
        this.plugin = plugin;
        this.players = plugin.players;
        this.playersAlive = new ArrayList<>();
        this.potato = new ItemStack(Material.BAKED_POTATO,1);
        this.random = new Random();
        this.taskIds = new ArrayList<>();
        this.tnt = new ItemStack(Material.TNT,1);
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
                    ((Player) edbee.getDamager()).getInventory().clear();
                    ((Player) edbee.getEntity()).getInventory().setItem(0,potato);
                    ((Player) edbee.getEntity()).getInventory().setItem(EquipmentSlot.HEAD,tnt);
                    for (Player p : players) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent("§b§l" + edbee.getEntity().getName() + " 接到了山芋！"));
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
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent pqe) {
        players.remove(pqe.getPlayer());
        playersAlive.remove(pqe.getPlayer());
    }

    @Override
    public void run() {
        World world = Bukkit.getWorld("world");
        for (Entity e :world.getNearbyEntities(new Location(world, 1000,12,1000),10,10,10) ) {
            if (e instanceof Player) {
                players.add((Player) e);
                playersAlive.add((Player) e);
            }
        }
        if (players.size() < 2) {
            for (Player p : players) {
                p.sendMessage("§c至少需要2人才能开始游戏！");
            }
            players.clear();
            playersAlive.clear();
        } else {
            startTime = getTime(world);
            world.getBlockAt(1000,13,996).setType(Material.AIR);
            Bukkit.getPluginManager().registerEvents(this,plugin);
            Bukkit.getScheduler().runTask(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 5 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                    p.getInventory().clear();
                }
            });
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 4 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },20);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 3 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },40);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 2 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },60);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                for (Player p : players) {
                    p.sendTitle("§a游戏还有 1 秒开始",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,1f);
                }
            },80);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p: players) {
                    p.teleport(new Location(world, 1001.5,94,993.5));
                    p.sendTitle("§e游戏开始！",null,2,16,2);
                    p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HARP,1f,2f);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,999999,0,false,false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION,999999,0,false,false));
                }

            }, 100);
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->{
                int time = 20 - (int)(((getTime(world) - startTime) / 20) % 20);
                List<Player> playersCopy = new ArrayList<>(players);
                if (time <= 15) {
                    for (Player p : playersCopy) {
                        p.setLevel(time);
                        p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BELL,0.2f,1f);
                    }
                } else {
                    for (Player p : playersCopy) {
                        p.setLevel(0);
                    }
                }
            },100,20));
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->{
                for (Player p : playersAlive) {
                    if (p.getInventory().contains(potato)) {
                        spawnFirework(p.getLocation());
                    }
                }
            },150,100))
            ;
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->{
                int i = random.nextInt(playersAlive.size());
                playersAlive.get(i).getInventory().setItem(0,potato);
                playersAlive.get(i).getInventory().setItem(EquipmentSlot.HEAD,tnt);
                for (Player p : players) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent("§b§l" + playersAlive.get(i).getName() + " 接到了山芋！"));
                }
            },100,400))
            ;
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->{
                Player playerOut = null;
                for (Player p: playersAlive) {
                    if (p.getInventory().contains(potato)) {
                        playerOut = p;
                    }
                }
                if (playerOut != null) {
                    for (Player p : players) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent("§f§l" + playerOut.getName() + " §c爆炸了！"));
                    }
                    playerOut.setGameMode(GameMode.SPECTATOR);
                    world.createExplosion(playerOut.getLocation(), 0.5f, false, false);
                    playersAlive.remove(playerOut);
                }

                },400,400));
            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (playersAlive.size() <= 1) {
                    Player winner = playersAlive.get(0);
                    spawnFirework(winner.getLocation());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },8);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },16);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },24);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        spawnFirework(winner.getLocation());
                    },32);
                    List<Player> playersCopy = new ArrayList<>(players);
                    for (Player p : playersCopy) {
                        p.sendTitle("§e" + playersAlive.get(0).getName() + " §b获胜了！",null,5,50, 5);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.teleport(new Location(world,1000.5,12.0625,999.5));
                            Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p));
                        },100);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                        Block block = world.getBlockAt(1000,13,996);
                        block.setType(Material.OAK_BUTTON);
                        BlockData data = block.getBlockData().clone();
                        ((Directional)data).setFacing(BlockFace.SOUTH);
                        block.setBlockData(data);
                        HandlerList.unregisterAll(this);
                    },100);
                    players.clear();
                    playersAlive.clear();
                    List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
                    taskIds.clear();
                    for (int i : taskIdsCopy) {
                        Bukkit.getScheduler().cancelTask(i);
                    }
                }

            },100,1));
        }
    }
    public static void spawnFirework(Location location){
        Location loc = location;
        loc.setY(loc.getY() + 0.9);
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(2);
        fwm.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }
    public long getTime(World world) {
        return ((CraftWorld)world).getHandle().worldData.getTime();
    }
}
