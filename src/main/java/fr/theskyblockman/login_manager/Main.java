package fr.theskyblockman.login_manager;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public final class Main extends JavaPlugin implements Listener {
    public static MysqlDataSource dbSource;
    public static Connection openConnection() {
        try {
            return dbSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void onEnable() {
        dbSource = new MysqlConnectionPoolDataSource();
        dbSource.setServerName(getConfig().getString(getConfig().getString("database.ip")));
        dbSource.setDatabaseName(getConfig().getString(getConfig().getString("database.name")));
        dbSource.setUser(getConfig().getString(getConfig().getString("database.user")));
        dbSource.setPassword(getConfig().getString(getConfig().getString("database.password")));

        getCommand("password").setExecutor(new PasswordCommand());
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    public final String playerPasswordMatch = "SELECT * FROM players_logins WHERE username = ? AND password = ?";
    public final String playerPasswordExists = "SELECT * FROM players_logins WHERE username = ?";
    public final String addPlayerPassword = "INSERT INTO players_logins VALUES (?, ?)";
    public static HashMap<String, Boolean> currentlyVerifyingPlayers = new HashMap<>();


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException {
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if(Objects.equals(onlinePlayer.getName(), event.getPlayer().getName())) {
                event.getPlayer().kickPlayer(ChatColor.RED + "Someone else already uses this username.");
                return;
            }
        }
        Connection connection = openConnection();
        assert connection != null;
        PreparedStatement passwordExists = connection.prepareStatement(playerPasswordExists);
        passwordExists.setString(1, event.getPlayer().getName());
        ResultSet rs = passwordExists.executeQuery();
        boolean hasPassword = rs.next();
        currentlyVerifyingPlayers.put(event.getPlayer().getName(), hasPassword);
        event.getPlayer().sendMessage(ChatColor.BLUE + (hasPassword ? "Please enter your password." : "Please define your password."));
        passwordExists.close();
        connection.close();
    }

    @EventHandler
    public void onPlayerSendsMessage(AsyncPlayerChatEvent event) throws SQLException {
        if(currentlyVerifyingPlayers.containsKey(event.getPlayer().getName())) {
            boolean hasPlayerPassword = currentlyVerifyingPlayers.get(event.getPlayer().getName());
            Connection connection = openConnection();
            assert connection != null;

            if(hasPlayerPassword) {
                PreparedStatement isPasswordCorrect = connection.prepareStatement(playerPasswordMatch);
                isPasswordCorrect.setString(1, event.getPlayer().getName());
                isPasswordCorrect.setString(2, event.getMessage());

                ResultSet rs = isPasswordCorrect.executeQuery();
                currentlyVerifyingPlayers.remove(event.getPlayer().getName());

                if(rs.next()) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Welcome !");
                } else {
                    event.getPlayer().kickPlayer(ChatColor.RED + "Wrong password !");
                }
                rs.close();
            } else {
                if(event.getMessage().length() > 32 || event.getMessage().contains(" ")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Your password must not use any spaces and be at max 32 characters.");
                    connection.close();
                    return;
                }
                PreparedStatement setPassword = connection.prepareStatement(addPlayerPassword);
                setPassword.setString(1, event.getPlayer().getName());
                setPassword.setString(2, event.getMessage());
                ResultSet rs = setPassword.executeQuery();
                rs.close();

                event.getPlayer().sendMessage(ChatColor.GREEN + "Welcome, your password has been saved !");
            }
            connection.close();
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        currentlyVerifyingPlayers.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerMoves(PlayerMoveEvent event) {
        event.setCancelled(currentlyVerifyingPlayers.containsKey(event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerInteracts(PlayerInteractEvent event) {
        event.setCancelled(currentlyVerifyingPlayers.containsKey(event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerIsTeleported(PlayerTeleportEvent event) {
        event.setCancelled(currentlyVerifyingPlayers.containsKey(event.getPlayer().getName()));
    }
}
