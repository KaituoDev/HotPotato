package fun.kaituo.hotpotato.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.hotpotato.HotPotato;
import fun.kaituo.hotpotato.util.WinnerDisplay;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PlayState implements GameState, Listener {
    private HotPotato game;
    public static final PlayState INST = new PlayState();
    private PlayState() {}

    private final Set<Integer> taskIds = new HashSet<>();
    private final Random random = new Random();
    private final Set<UUID> playersAlive = new HashSet<>();
    @Getter
    private Location playfield;
    private BossBar countdownBar;
    private final int explosionInterval = WaitState.INST.getExplosionInterval();
    private final Set<Player> potatoHolders = new HashSet<>();
    private org.bukkit.scoreboard.Scoreboard scoreboard;
    private org.bukkit.scoreboard.Team potatoHolderTeam;

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        String POTATO_TEAM_NAME = "hotPotatoHolder";
        potatoHolderTeam = scoreboard.registerNewTeam(POTATO_TEAM_NAME);
        potatoHolderTeam.setColor(org.bukkit.ChatColor.RED);
        potatoHolderTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
    }

    public void init(){
        game = HotPotato.inst();
        playfield = game.getLoc("playfield");
        setupScoreboard();
    }

    public Set<Player> getPlayersAlive() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playersAlive) {
            Player p = game.getServer().getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    private void checkForEnd() {
        if (getPlayersAlive().size() == 1) {
            Player winner = getPlayersAlive().iterator().next();
            for (Player p : game.getPlayers()) {
                p.sendTitle("§a游戏结束", "§e" + winner.getName() + " §a获胜！", 10, 30, 20);
            }

            Location fireworkLoc = game.getLoc("firework");
            if (fireworkLoc != null) {
                spawnVictoryFireworks(fireworkLoc);
            }

            // 有获胜者时才更新头颅
            Location displayLocation = game.getLoc("winnerHead");
            if (displayLocation != null) {
                WinnerDisplay display = new WinnerDisplay(displayLocation);
                display.displayWinner(winner);
            }

            game.setState(WaitState.INST);
        } else if (getPlayersAlive().isEmpty()) {
            for (Player p : game.getPlayers()) {
                p.sendTitle("§a游戏结束", "§c无人存活！", 10, 30, 20);
            }

            game.setState(WaitState.INST);
        }
    }

    @Override
    public void enter() {
        // Make sure playfield is initialized
        if (playfield == null) {
            init();
        }

        playersAlive.clear();

        for (Player p : game.getPlayers()) {
            playersAlive.add(p.getUniqueId());
            // Add effects
            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 3, false, false));

            playersAlive.add(p.getUniqueId());
            p.teleport(playfield);
            p.setGameMode(GameMode.ADVENTURE);
            game.getLogger().info("Player " + p.getName() + " teleported to playfield");
        }

        Bukkit.getPluginManager().registerEvents(this, game);

        countdownBar = Bukkit.createBossBar(
                "游戏即将开始: 5",
                BarColor.BLUE,
                BarStyle.SOLID
        );
        countdownBar.setProgress(1.0);

        for (Player p : game.getPlayers()) {
            countdownBar.addPlayer(p);
        }

        for (int i = 5; i >= 1; i--) {
            final int time = i;
            BukkitTask taskId = Bukkit.getScheduler().runTaskLater(game, () -> {
                // Update bossbar
                countdownBar.setTitle("游戏即将开始: " + time);
                countdownBar.setProgress(time / 5.0);

                // Send title to all players
                for (Player p : game.getPlayers()) {
                    p.sendTitle("§c" + time , "", 0, 20, 0);

                    float pitch = (time == 1) ? 2.0F : 1.0F;
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, pitch);
                }
            }, (5 - i) * 20L); // 20 ticks = 1 second
            taskIds.add(taskId.getTaskId());
        }

        BukkitTask startGameTaskId = Bukkit.getScheduler().runTaskLater(game, () -> {
            countdownBar.removeAll();

            for (Player p : game.getPlayers()) {
                p.sendTitle("§c游戏开始！", "", 10, 20, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            }

            startGame();
        }, 5 * 20L);
        taskIds.add(startGameTaskId.getTaskId());
    }

    private void startGame() {
        Set<Player> alivePlayers = getPlayersAlive();
        potatoHolders.clear(); // Clear previous holders

        // Determine how many potatoes to distribute
        int potatoCount = alivePlayers.size() >= 8 ? 2 : 1;

        // Select random players as potato holders
        for (int i = 0; i < potatoCount; i++) {
            if (alivePlayers.isEmpty()) break;

            Player randomPlayer = (Player) alivePlayers.toArray()[random.nextInt(alivePlayers.size())];
            randomPlayer.getInventory().addItem(HotPotato.inst().getHotPotato());
            potatoHolders.add(randomPlayer);
            alivePlayers.remove(randomPlayer); // Ensure the same player doesn't get multiple potatoes

            // Announce who got the potato
            for (Player p : game.getPlayers()) {
                p.sendMessage("§e§l" + randomPlayer.getName() + " §c获得了烫手山芋！");
            }
        }

        // Update the display for all potato holders
        updatePotatoHolders();

        countdownBar.setTitle("§c山芋爆炸倒计时: " + explosionInterval + "秒");
        countdownBar.setProgress(1.0);
        countdownBar.setColor(BarColor.RED);

        potatoTimer(explosionInterval);
    }

    private void potatoTimer(int seconds) {
        final int totalSeconds = seconds;
        final int[] timeLeft = {seconds};

        BukkitTask timerTask = Bukkit.getScheduler().runTaskTimer(game, () -> {
            timeLeft[0]--;

            for (Player p : game.getPlayers()) {
                countdownBar.addPlayer(p);
            }

            // 更新BossBar
            if (timeLeft[0] > 0) {
                countdownBar.setTitle("§c山芋爆炸倒计时: " + timeLeft[0] + "秒");
                countdownBar.setProgress((double) timeLeft[0] / totalSeconds);
            } else {
                potatoBoom();
            }
        }, 20L, 20L); // 每秒执行一次

        taskIds.add(timerTask.getTaskId());
    }

    private void potatoBoom() {
        Set<Player> toEliminate = new HashSet<>(potatoHolders);
        boolean anyEliminated = false;

        for (int id : new HashSet<>(taskIds)) {
            if (Bukkit.getScheduler().isQueued(id)) {
                Bukkit.getScheduler().cancelTask(id);
                taskIds.remove(id);
            }
        }

        for (Player holder : toEliminate) {
            if (holder != null && playersAlive.contains(holder.getUniqueId())) {
                // Play explosion effects
                Location explosionLoc = holder.getLocation();
                holder.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 10, 0.5, 0.5, 0.5, 0.1);
                holder.getWorld().spawnParticle(org.bukkit.Particle.FLAME, explosionLoc, 15, 0.5, 0.5, 0.5, 0.2);

                // Play explosion sound for everyone
                for (Player p : game.getPlayers()) {
                    p.playSound(explosionLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                }

                // Send message to all players
                for (Player p : game.getPlayers()) {
                    p.sendMessage(holder.getName() + " §c被烫死了！");
                }

                // Eliminate holder
                eliminatePlayer(holder);
                anyEliminated = true;
            }
        }

        potatoHolders.clear();

        // Continue to next round if game isn't over
        if (anyEliminated && getPlayersAlive().size() > 1) {
            // Update BossBar to show "Distributing potatoes" message
            countdownBar.setTitle("§e发放土豆中...");
            countdownBar.setProgress(1.0);
            countdownBar.setColor(BarColor.YELLOW);

            BukkitTask newRoundTask = Bukkit.getScheduler().runTaskLater(game, this::startGame, 60L);
            taskIds.add(newRoundTask.getTaskId());
        }
    }

    private void eliminatePlayer(Player player) {
        potatoHolders.remove(player);
        potatoHolderTeam.removeEntry(player.getName());

        playersAlive.remove(player.getUniqueId());
        player.getInventory().clear();

        player.setGameMode(GameMode.SPECTATOR);

        checkForEnd();
    }

    @EventHandler
    public void onTransferPotato(EntityDamageByEntityEvent event) {
        if(!playersAlive.contains(event.getEntity().getUniqueId())){
            return;
        }

        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        // Check if attacker has potato and target is alive
        if (hasHotPotato(attacker)) {
            // Remove potato from attacker
            removeHotPotato(attacker);
            potatoHolders.remove(attacker);

            // Give potato to target
            target.getInventory().addItem(HotPotato.inst().getHotPotato());
            potatoHolders.add(target);
            updatePotatoHolders();

            // Broadcast message
            for (Player p : game.getPlayers()) {
                p.sendMessage("§e" + attacker.getName() + " §c将烫手山芋传给了 §e" + target.getName() + "§c！");
            }
        }
    }

    private boolean hasHotPotato(Player player) {
        return player.getInventory().contains(HotPotato.inst().getHotPotato());
    }

    private void removeHotPotato(Player player) {
        player.getInventory().remove(HotPotato.inst().getHotPotato());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFallDamage(EntityDamageEvent event) {
        // Check if it's a player and the damage is from falling
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Check if player is in our game
            if (game.getPlayers().contains(player)) {
                // Cancel the fall damage
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void exit() {
        HandlerList.unregisterAll(this);
        for (Player p : game.getPlayers()) {
            p.removePotionEffect(PotionEffectType.SATURATION);
            p.removePotionEffect(PotionEffectType.RESISTANCE);
            p.removePotionEffect(PotionEffectType.GLOWING);
        }
        for (Player p : game.getPlayers()) {
            removePlayer(p);
        }
        for (int id : taskIds) {
            game.getServer().getScheduler().cancelTask(id);
        }
        HandlerList.unregisterAll(this);
        taskIds.clear();  // Also clear taskIds
        playersAlive.clear();
        try {
            if (potatoHolderTeam != null) {
                for (String entry : potatoHolderTeam.getEntries()) {
                    potatoHolderTeam.removeEntry(entry);
                }
            }
        } catch (IllegalStateException e) {
            // Team might already be unregistered
            game.getLogger().warning("Failed to unregister team: " + e.getMessage());
        }
        countdownBar.removeAll();
    }

    @Override
    public void tick() {
        checkForEnd();
    }

    @Override
    public void addPlayer(Player p) {
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(PlayState.INST.getPlayfield());
    }

    @Override
    public void removePlayer(Player p) {
        playersAlive.remove(p.getUniqueId());
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.removePotionEffect(PotionEffectType.GLOWING);
        p.removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    public void forceStop() {
        HandlerList.unregisterAll(this);

        // Cancel any scheduled tasks
        Bukkit.getScheduler().cancelTasks(game);

        exit();
    }

    private void updatePotatoHolders() {
        // 先移除所有玩家的红色发光效果
        for (Player p : getPlayersAlive()) {
            potatoHolderTeam.removeEntry(p.getName());
            p.removePotionEffect(PotionEffectType.GLOWING);
            p.removePotionEffect(PotionEffectType.SPEED);

            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0, false, false));
        }

        // 给新的持有者添加红色发光效果
        for (Player holder : potatoHolders) {
            if (holder != null && playersAlive.contains(holder.getUniqueId())) {
                potatoHolderTeam.addEntry(holder.getName());
                holder.removePotionEffect(PotionEffectType.GLOWING);
                holder.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0, false, false));
                holder.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0, false, false));
            }
        }

        // 让所有玩家看到发光效果
        for (Player p : game.getPlayers()) {
            p.setScoreboard(scoreboard);
        }
    }
    private void spawnVictoryFireworks(Location location) {
        for (int i = 0; i < 10; i++) {
            // Schedule fireworks with increasing delay for better visual effect
            Bukkit.getScheduler().runTaskLater(game, () -> {
                org.bukkit.entity.Firework firework = location.getWorld().spawn(location, org.bukkit.entity.Firework.class);
                org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();

                // Random firework colors
                org.bukkit.Color[] colors = {
                        org.bukkit.Color.RED,
                        org.bukkit.Color.GREEN,
                        org.bukkit.Color.BLUE,
                        org.bukkit.Color.YELLOW,
                        org.bukkit.Color.AQUA,
                        org.bukkit.Color.PURPLE
                };

                // Create random firework effect
                org.bukkit.FireworkEffect.Type[] types = org.bukkit.FireworkEffect.Type.values();
                org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                        .with(types[new Random().nextInt(types.length)])
                        .withColor(colors[new Random().nextInt(colors.length)])
                        .withFade(colors[new Random().nextInt(colors.length)])
                        .withTrail()
                        .withFlicker()
                        .build();

                meta.addEffect(effect);
                meta.setPower(1 + new Random().nextInt(2)); // Random height
                firework.setFireworkMeta(meta);
            }, i * 8L); // Launch fireworks with slight delay between each
        }
    }
}



