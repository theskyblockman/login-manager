package fr.theskyblockman.login_manager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PasswordCommand implements TabExecutor {
    public final String updatePlayerPassword = "UPDATE players_logins SET password = ? WHERE username = ?";
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;

        Player player = (Player) sender;

        if(args.length >= 2) {
            if(Objects.equals(args[0], "set")) {
                if(Main.currentlyVerifyingPlayers.containsKey(player.getName())) {
                    player.sendMessage(ChatColor.RED + "Please connect yourself before editing your password.");
                    return false;
                }

                if(args.length != 2 || args[1].length() > 32) {
                    player.sendMessage(ChatColor.RED + "Your password must not use any spaces and be at max 32 characters.");
                    return false;
                }

                try {
                    Connection connection = Main.openConnection();
                    assert connection != null;
                    PreparedStatement passwordUpdateStatement = connection.prepareStatement(updatePlayerPassword);
                    passwordUpdateStatement.setString(1, args[1]);
                    passwordUpdateStatement.setString(2, player.getName());
                    passwordUpdateStatement.executeQuery();
                    connection.close();

                    player.sendMessage(ChatColor.GREEN + "Your password has been edited !");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> argsToReturn = new ArrayList<>();

        if(args.length == 1) {
            argsToReturn.add("set");
        }

        return argsToReturn;
    }
}
