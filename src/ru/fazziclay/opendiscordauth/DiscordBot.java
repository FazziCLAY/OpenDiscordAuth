package ru.fazziclay.opendiscordauth;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordBot extends ListenerAdapter {
    public static JDA bot;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Utils.debug("[DiscordBot] onMessageReceived(): message="+event.getMessage().getContentRaw()+"");

        String content           = event.getMessage().getContentRaw();
        User author              = event.getMessage().getAuthor();
        MessageChannel channel   = event.getMessage().getChannel();

        if (channel.getType().isGuild() || author.getId().equals(bot.getSelfUser().getId())) {
            return;
        }

        TempCode tempCode = TempCode.getByValue(0, content);
        Utils.debug("[DiscordBot] onMessageReceived(): (check tempCode) tempCode="+tempCode);

        if (tempCode != null) {
            boolean allow;
            if (Config.roleRequiresEnabled) {
                Guild guild = event.getJDA().getGuildById(Config.roleRequiresGuildId);
                if (guild == null) throw new RuntimeException("config.yml: roleRequiresGuildId guild not found!");
                Member member = guild.getMember(author);
                if (member == null) {
                    allow = false;
                } else {
                    String[] requires = Config.roleRequiresRolesIds.split(",");
                    allow = false;
                    if (Config.roleRequiresLogicMode.equalsIgnoreCase("and")) {
                        boolean n = true;
                        for (String require : requires) {
                            boolean has = false;
                            for (Role role : member.getRoles()) {
                                if (role.getId().equals(require)) {
                                    has = true;
                                    break;
                                }
                            }
                            if (!has) {
                                n = false;
                                break;
                            }
                        }
                        allow = n;

                    } else if (Config.roleRequiresLogicMode.equalsIgnoreCase("or")) {
                        for (String require : requires) {
                            boolean has = false;
                            for (Role role : member.getRoles()) {
                                if (role.getId().equals(require)) {
                                    has = true;
                                    break;
                                }
                            }
                            if (has) {
                                allow = true;
                                break;
                            }
                        }

                    } else {
                        throw new RuntimeException("Unknown config.yml roleRequires.LogicMode '" + Config.roleRequiresLogicMode + "'. Use 'AND' or 'OR'");
                    }
                }

            } else {
                allow = true;
            }

            if (allow) {
                Account account = Account.getByValue(0, tempCode.ownerNickname);

                if (account != null) {
                    Utils.debug("[DiscordBot] onMessageReceived(): (account != null) == true");
                    if (account.ownerDiscord.equals(author.getId())) {
                        LoginManager.login(tempCode.ownerUUID);

                    } else {
                        Utils.sendMessage(channel, Config.messageNotYoursCode);
                    }

                } else {
                    Utils.debug("[DiscordBot] onMessageReceived(): (account != null) == false");
                    Account.create(author, tempCode.ownerNickname);
                }
            } else {
                Utils.sendMessage(channel, Config.messageNotOwnRequiresRoles);
            }

            tempCode.delete();

        } else {
            Utils.sendMessage(channel, Config.messageCodeNotFound);
        }
    }
}
