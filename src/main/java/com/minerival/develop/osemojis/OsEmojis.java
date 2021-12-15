package com.minerival.develop.osemojis;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public final class OsEmojis extends JavaPlugin implements Listener {

    public Boolean printLoadedEmojis = false;
    public String newEmojiDetectedMessage = "added";
    public String loadingEmojiMessage = "loaded";
    public String noPermisisonMessage = "&cNo permission";
    public static String prefix= "&1[&bO''s &9Emojis&1] ";
    public static String reloadingMessage = "&e&lReloading..";
    public static String reloadedMessage = "&a&lReloaded !";


    public static Map<String, String> loadedEmojis = new HashMap<>();
    Map<String, Object> groupColors = new HashMap<>();
    List<String> registeredCommands = new ArrayList();

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig(true);

    }
    public void loadConfig(Boolean onStarting){

        Map<String, String> toDetectNewEmojis= loadedEmojis;
        String key;
        String value;
        loadedEmojis= null;
        loadedEmojis= new HashMap<>();
        for (String emojisToLoad : getConfig().getStringList("emojis")){
            String[] array = emojisToLoad.split("@");
            key= array[0];
            value= array[1];
            if(printLoadedEmojis && onStarting){
                System.out.println(ChatColor.translateAlternateColorCodes('&',"&b&l"+key + " --> "+ value + loadingEmojiMessage));
            }
            if (!onStarting){
                if (!toDetectNewEmojis.containsKey(key)){
                    System.out.println(key + " --> " + value + " " + newEmojiDetectedMessage);
                }
            }
            loadedEmojis.put(key, value);
        }
        groupColors = getConfig().getConfigurationSection("groups").getValues(false);
        registeredCommands = new ArrayList<>();
        getConfig().getStringList("registeredCommands").stream().forEach(i -> registeredCommands.add(i));
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e){
        String[] commands = e.getMessage().split(" ");
        if (registeredCommands.contains(commands[0])){
            String processedCommand = "";
            for (int i = 1; i < commands.length; i++){
                processedCommand += commands[i] + " ";
            }
            e.setMessage(commands[0] + " " + processMessage(processedCommand, ""));
        }
        return;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e){
        e.setMessage(processMessage(e.getMessage(), getGroupColor(e.getPlayer())));
    }

    public String processMessage(String message, String groupColor){
        if(message.contains(",")){
            String[] exps = getExpressions(message);
            String[] emojis = toEmoji(exps);
            if (exps.length == 0 || emojis.length == 0){
                return message;
            }
            int expCounter = 0;
            String result = "";
            char c;
            for (int i = 0; i < message.length(); i++){
                c = message.charAt(i);
                if(message.charAt(i) == ','){

                    result += ChatColor.WHITE+emojis[expCounter]+translateHexColorCodes(groupColor);
                    i += exps[expCounter].length() + 1;
                    expCounter++;

                }else{
                    result += c;
                }
            }
            message = result;
        }
        return message;
    }
    private String getGroupColor(Player p){
        String group = loadUser(p).getPrimaryGroup();
        String groupColor = (String) groupColors.getOrDefault(group, groupColors.getOrDefault("default", "&f"));
        return groupColor;
    }

    private String colorize(final String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String[] getExpressions(String message){
        List<String> expressions = new ArrayList<>();
        String exp = "";
        Boolean ent = false;
        char c;
        for (int i = 0; i < message.length(); i++){
            c = message.charAt(i);
            if (c == ','){
                if (!ent){
                    ent = true;
                }else{
                    ent = false;
                    if (exp != ""){
                        expressions.add(exp);
                        exp = "";
                    }
                }
            }else if (ent){
                exp += c;
            }
        }
        String[] result = new String[expressions.size()];
        return expressions.toArray(result);
    }

    public String[] toEmoji(String[] exps){
        String[] result = new String[exps.length];
        for (int i = 0 ; i < exps.length; i++){
            result[i] = loadedEmojis.getOrDefault(exps[i], exps[i]);
        }
        return result;
    }
    private String translateHexColorCodes(final String message) {
        final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        final char colorChar = ChatColor.COLOR_CHAR;

        final Matcher matcher = hexPattern.matcher(message);
        final StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        while (matcher.find()) {
            final String group = matcher.group(1);

            matcher.appendReplacement(buffer, colorChar + "x"
                    + colorChar + group.charAt(0) + colorChar + group.charAt(1)
                    + colorChar + group.charAt(2) + colorChar + group.charAt(3)
                    + colorChar + group.charAt(4) + colorChar + group.charAt(5));
        }

        return matcher.appendTail(buffer).toString();
    }

    private LuckPerms getApi() {
        final RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        Validate.notNull(provider);
        return provider.getProvider();
    }
    private User loadUser(final Player player) {
        if (!player.isOnline())
            throw new IllegalStateException("Player is offline!");

        return getApi().getUserManager().getUser(player.getUniqueId());
    }



    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (label.equalsIgnoreCase("emoji")){
            if (args.length == 0){
                if (sender.hasPermission("emoji.reload")){
                    sender.sendMessage(ChatColor.WHITE+"/emoji reload");
                }
                sender.sendMessage(ChatColor.WHITE+"/emoji list");
                return true;
            }
            if (args.length > 0){
                if (args[0].equalsIgnoreCase("reload")){
                    if (!sender.hasPermission("emoji.reload") || !sender.isOp()){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+ noPermisisonMessage));
                        return true;
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',prefix+ reloadingMessage));
                    reloadConfig();
                    saveDefaultConfig();
                    loadConfig(false);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+ reloadedMessage));
                    return true;
                }
                if (args[0].equalsIgnoreCase("list")){
                    /*if (!(sender.hasPermission("emoji.list")) && !(sender.isOp())){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+ noPermisisonMessage));
                        return true;
                    }*/
                    sendEmojis(sender);
                    return true;
                }
            }

        }else if (label.equalsIgnoreCase("el")){
            sendEmojis(sender);
            return true;
        }
        return false;
    }

    public void sendEmojis(CommandSender sender){
        //emojileri sıralayıp gönder
        Iterator<String> keys = loadedEmojis.keySet().iterator();
        Iterator<String> values = loadedEmojis.values().iterator();
        int emojiPerLine = 6;
        //TextComponent[] msg = new TextComponent[emojiPerLine];

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&8&m－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&6You can use emojis typing like &2,:D,&6."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&eEmojis You Can Use: "));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',""));
        while(keys.hasNext()){
            ArrayList<TextComponent> msg = new ArrayList<>();
            for (int i = 0; i < emojiPerLine; i++){
                if (!keys.hasNext()){
                    break;
                }
                String key = keys.next();
                String value = values.next();

                msg.add(i, new TextComponent(colorize(" &2| &f"+value+" &2| ")));
                msg.get(i).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent(colorize("&6Autowrite (&e,"+key+",&6)"))}));
                msg.get(i).setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ","+key+","));
            }
            TextComponent[] msgs = new TextComponent[msg.size()];
            for (int i = 0; i < msgs.length; i++){
                msgs[i] = msg.get(i);
            }
            sender.spigot().sendMessage(msgs);
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&8&m－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－"));

    }



}
