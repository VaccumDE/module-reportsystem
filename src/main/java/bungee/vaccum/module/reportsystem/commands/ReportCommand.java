package bungee.vaccum.module.reportsystem.commands;

import bungee.vaccum.api.cloud.CloudHandler;
import bungee.vaccum.api.cloud.ReportManager;
import bungee.vaccum.api.objects.Report;
import bungee.vaccum.api.utils.ReportReason;
import bungee.vaccum.module.reportsystem.ReportSystem;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.chat.ComponentSerializer;
import org.apache.commons.lang3.EnumUtils;

import java.io.IOException;
import java.util.*;

public class ReportCommand extends Command {

    ReportManager reportManager = ReportSystem.getInstance().getBungeeAPI().getReportManager();
    CloudHandler cloudHandler = ReportSystem.getInstance().getBungeeAPI().getCloudHandler();
    private final IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);

    public ReportCommand(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ReportSystem.getInstance().getProxy().getScheduler().runAsync(ReportSystem.getInstance(), () -> {
            try {
                if (sender instanceof ProxiedPlayer) {
                    ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

                    if (proxiedPlayer.hasPermission("vaccum.report.process")) {
                        if(args.length >= 1) {
                            if (args[0].equalsIgnoreCase("login")) {
                                if (!reportManager.getReportStatus(proxiedPlayer)) {
                                    reportManager.setReportStatus(proxiedPlayer, true);
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aDu wurdest eingeloggt.");
                                } else {
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDu bist bereits eingeloggt!");
                                }
                            } else if (args[0].equalsIgnoreCase("logout")) {
                                if (reportManager.getReportStatus(proxiedPlayer)) {
                                    reportManager.setReportStatus(proxiedPlayer, false);
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aDu wurdest ausgeloggt.");
                                } else {
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDu bist bereits ausgeloggt!");
                                }
                            } else if (args[0].equalsIgnoreCase("togglelogin")) {
                                if (reportManager.getReportAutoLoginStatus(proxiedPlayer)) {
                                    reportManager.setReportAutoLoginStatus(proxiedPlayer, false);
                                    reportManager.setReportStatus(proxiedPlayer, false);
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Dein Autologin wurde §cdeaktiviert§7!");
                                } else {
                                    reportManager.setReportAutoLoginStatus(proxiedPlayer, true);
                                    reportManager.setReportStatus(proxiedPlayer, true);
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Dein Autologin wurde §aaktiviert§7!");
                                }
                            } else if (args[0].equalsIgnoreCase("list")) {
                                if (reportManager.getReportStatus(proxiedPlayer)) {
                                    int site = 1;
                                    if(args.length == 1) {
                                        sendList(site, proxiedPlayer);
                                    } else if(args.length == 2 && !args[1].isEmpty()) {
                                        try {
                                            site = Integer.parseInt(args[1]);
                                        } catch (NumberFormatException exception) {
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report list <seite>");
                                        }
                                        sendList(site, proxiedPlayer);
                                    } else {
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report list <seite>");
                                    }
                                }
                            } else if (args[0].equalsIgnoreCase("accept")) {
                                if (reportManager.getReportStatus(proxiedPlayer)) {
                                    if (!reportManager.isEditorAlreadyInCase(proxiedPlayer)) {
                                        if (args.length == 2 && !args[1].isEmpty()) {
                                            if (reportManager.isCaseOpen(args[1])) {
                                                Report report = reportManager.getReport(args[1]);
                                                reportManager.setEditor(report.getCaseId(), proxiedPlayer);
                                                proxiedPlayer.sendMessage(ReportSystem.prefix + "Du hast den Case §e" + report.getCaseId() + " §7übernommen.");

                                                ICloudPlayer suspect = playerManager.getOnlinePlayer(UUID.fromString(report.getSuspect()));

                                                if(suspect == null) {
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Die Case wurde wieder eröffnet, da der Verdächtige offline ist!");
                                                    reportManager.setEditorToNull(report.getCaseId());
                                                } else {
                                                    for(UUID uuid : reportManager.getOnlineReportPlayers()) {
                                                        ICloudPlayer all = playerManager.getOnlinePlayer(uuid);
                                                        all.getPlayerExecutor().sendChatMessage(ReportSystem.prefix + cloudHandler.getDisplayName(proxiedPlayer) + " §7hat den Report Case §e" + report.getCaseId() + " §aübernommen§7.");
                                                    }
                                                    ICloudPlayer cloudPlayer = playerManager.getOnlinePlayer(proxiedPlayer.getUniqueId());
                                                    cloudPlayer.getPlayerExecutor().connect(suspect.getConnectedService().getServerName());
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Der Name des Spielers lautet §e" + suspect.getName());
                                                }
                                            } else {
                                                proxiedPlayer.sendMessage(ReportSystem.prefix + "Dieser Case ist nicht mehr offen.");
                                            }
                                        } else if(args.length == 1) {
                                            try {
                                                Random randomGenerator = new Random();
                                                int index = randomGenerator.nextInt(reportManager.getOpenCases().size());
                                                Report report = reportManager.getOpenCases().get(index);
                                                reportManager.setEditor(report.getCaseId(), proxiedPlayer);
                                                ICloudPlayer suspect = playerManager.getOnlinePlayer(UUID.fromString(report.getSuspect()));

                                                if (suspect == null) {
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Die Case wurde wieder eröffnet, da der Verdächtige offline ist!");
                                                    reportManager.setEditorToNull(report.getCaseId());
                                                } else {
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Du hast den Case §e" + report.getCaseId() + " §7übernommen.");
                                                    for (UUID uuid : reportManager.getOnlineReportPlayers()) {
                                                        ICloudPlayer all = playerManager.getOnlinePlayer(uuid);
                                                        all.getPlayerExecutor().sendChatMessage(ReportSystem.prefix + cloudHandler.getDisplayName(proxiedPlayer) + " §7hat den Report Case §e" + report.getCaseId() + " §aübernommen§7.");
                                                    }
                                                    proxiedPlayer.sendMessage(" ");
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aCaseID: §e" + report.getCaseId());
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aAussteller: §e" + cloudHandler.getUsername(report.getIssuer()));
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aVerdächtiger: §e" + cloudHandler.getUsername(report.getSuspect()));
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aGrund: §e" + report.getReportReason());
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aAnfrage: §e" + report.getCreationTime());
                                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aServer: §e" + suspect.getConnectedService().getServerName());
                                                    proxiedPlayer.sendMessage(" ");
                                                    playerManager.getPlayerExecutor(proxiedPlayer.getUniqueId()).connect(suspect.getConnectedService().getServerName());
                                                }
                                            } catch (Exception exception) {
                                                proxiedPlayer.sendMessage(ReportSystem.prefix + "Es ist aktuell keine Case offen.");
                                            }
                                        } else {
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report accept <caseid>");
                                        }
                                    } else {
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDu bearbeitest aktuell noch eine Case!");
                                    }
                                } else {
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDu bist nicht eingeloggt!");
                                }
                            } else if (args[0].equalsIgnoreCase("mycase")) {
                                if (reportManager.isEditorAlreadyInCase(proxiedPlayer)) {
                                    Report report = reportManager.getCaseOfEditor(proxiedPlayer);
                                    ICloudPlayer suspect = playerManager.getOnlinePlayer(UUID.fromString(report.getSuspect()));
                                    proxiedPlayer.sendMessage(" ");
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aCaseID: §e" + report.getCaseId());
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aAussteller: §e" + cloudHandler.getUsername(report.getIssuer()));
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aVerdächtiger: §e" + cloudHandler.getUsername(report.getSuspect()));
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aGrund: §e" + report.getReportReason());
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§aAnfrage: §e" + report.getCreationTime());
                                    if(suspect != null)
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§aServer: §e" + suspect.getConnectedService().getServerName());
                                    else
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§aServer: §cOFFLINE");
                                    proxiedPlayer.sendMessage(" ");
                                } else {
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Du befindest dich aktuell nicht in einem Case.");
                                }
                            } else if(args[0].equalsIgnoreCase("close")) {
                                if(reportManager.getReportStatus(proxiedPlayer)) {
                                    if(args.length == 1 && !args[0].isEmpty()) {
                                        if(reportManager.isEditorAlreadyInCase(proxiedPlayer)) {
                                            Report report = reportManager.getCaseOfEditor(proxiedPlayer);
                                            reportManager.closeReport(report.getCaseId());
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du hast die Case §e" + report.getCaseId() + " §7geschlossen.");
                                            for(UUID uuid : reportManager.getOnlineReportPlayers()) {
                                                ICloudPlayer all = playerManager.getOnlinePlayer(uuid);
                                                all.getPlayerExecutor().sendChatMessage(ReportSystem.prefix + cloudHandler.getDisplayName(proxiedPlayer) + " §7hat den Report Case §e" + report.getCaseId() + " §cgeschlossen§7.");
                                            }
                                        } else
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du bearbeitest aktuell keine Case... Falls du ein Report mit der CaseID schließen willst, dann nutze §7§o/report close <caseid>");
                                    } else if(args.length == 2 && !args[0].isEmpty() && !args[1].isEmpty()) {
                                        try {
                                            Report report = reportManager.getReport(args[1]);
                                            reportManager.closeReport(report.getCaseId());
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du hast die Case §e" + report.getCaseId() + " §7geschlossen.");
                                            for (UUID uuid : reportManager.getOnlineReportPlayers()) {
                                                ICloudPlayer all = playerManager.getOnlinePlayer(uuid);
                                                all.getPlayerExecutor().sendChatMessage(ReportSystem.prefix + cloudHandler.getDisplayName(proxiedPlayer) + " §7hat den Report Case §e" + report.getCaseId() + " §cgeschlossen§7.");
                                            }
                                        } catch (Exception exception) {
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Die angegebene Case ist nicht mehr offen...");
                                        }
                                    } else
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§Falsche Eingabe: §7§o/report close <(caseid)>");
                                } else
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "Du bist nicht eingeloggt!");
                            } else if(args[0].equalsIgnoreCase("jump")) {
                                if(reportManager.getReportStatus(proxiedPlayer)) {
                                    if(args.length == 1) {
                                        if(reportManager.isEditorAlreadyInCase(proxiedPlayer)) {
                                            Report report = reportManager.getCaseOfEditor(proxiedPlayer);

                                            playerManager.getPlayerExecutor(proxiedPlayer.getUniqueId()).connect(playerManager.getOnlinePlayer(UUID.fromString(report.getSuspect())).getConnectedService().getServerName());
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du wurdest zum Server von §e" + cloudHandler.getUsername(report.getSuspect()) + " §7verbunden.");
                                        } else {
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du befindest dich aktuell nicht in einem Case.");
                                        }
                                    } else {
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report jump");
                                    }
                                } else {
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDu bist nicht eingeloggt!");
                                }
                            } else {
                                proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report <login/logout/togglelogin/list/accept/mycase/close/jump>");
                            }
                        } else
                            proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report <login/logout/togglelogin/list/accept/mycase/close/jump>");
                    } else {
                        if(args.length == 2 && !args[1].isEmpty()) {
                            if(EnumUtils.isValidEnumIgnoreCase(ReportReason.class, args[1])) {
                                ICloudPlayer cloudPlayer = playerManager.getFirstOnlinePlayer(args[0]);

                                if(cloudPlayer != null) {
                                    if (cloudPlayer.getUniqueId().equals(proxiedPlayer.getUniqueId()))
                                        proxiedPlayer.sendMessage(ReportSystem.prefix + "Du darfst dich nicht selber melden!");
                                    else {
                                        IPermissionUser permissionUser = CloudNetDriver.getInstance().getPermissionManagement().getUser(cloudPlayer.getUniqueId());
                                        if (!permissionUser.hasPermission("vaccum.report.bypass").asBoolean()) {
                                            if (!reportManager.isPlayerAlreadyReported(cloudPlayer.getUniqueId())) {
                                                String caseId = reportManager.createCase(proxiedPlayer.getUniqueId(), cloudPlayer.getUniqueId(), EnumUtils.getEnumIgnoreCase(ReportReason.class, args[1]));
                                                proxiedPlayer.sendMessage(ReportSystem.prefix + "§e" + cloudPlayer.getName() + " §7wurde §aerfolgreich §7gemeldet, vielen Dank für deine Unterstützung!");

                                                Report report = reportManager.getReport(caseId);

                                                for (UUID uuid : reportManager.getOnlineReportPlayers()) {
                                                    ICloudPlayer reportMember = playerManager.getOnlinePlayer(uuid);
                                                    TextComponent textComponent = new TextComponent(ReportSystem.prefix + "§e" + cloudHandler.getUsername(report.getSuspect()) + " §7wurde von §a"
                                                            + cloudHandler.getUsername(proxiedPlayer.getUniqueId().toString()) + " §7wegen §c" + report.getReportReason() + " §7gemeldet. §8[" + report.getCaseId() + "]");
                                                    textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eKlicke hier, um diesen Case zu übernehmen").create()));
                                                    textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report accept " + report.getCaseId()));
                                                    sendMessage(reportMember.getUniqueId(), textComponent);
                                                }
                                            } else
                                                proxiedPlayer.sendMessage(ReportSystem.prefix + "Dieser Spieler wurde bereits gemeldet!");
                                        } else
                                            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du kannst diesen Spieler nicht reporten!");
                                    }
                                } else
                                    proxiedPlayer.sendMessage(ReportSystem.prefix + "§cDieser Spieler ist offline!");
                            } else {
                                String reasons = Arrays.asList(ReportReason.values()).toString().replace("[", "").replace("]", "");
                                proxiedPlayer.sendMessage(ReportSystem.prefix + "§7Bitte gebe einen der folgenden Gründe an: §e" + reasons);
                            }
                        } else {
                            proxiedPlayer.sendMessage(ReportSystem.prefix + "§cFalsche Eingabe: §7§o/report <Spieler> <Grund>");
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private void sendMessage(UUID uuid, TextComponent messages) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument().append("uniqueId", uuid).append("messages", ComponentSerializer.toString(messages)));
    }

    private void sendList(int site, ProxiedPlayer proxiedPlayer) throws Exception {
        int lines = 3;
        int allReports = reportManager.getOpenCases().size();
        int allPages = allReports / lines;
        int oneSitePlus = site + 1;
        int oneSiteMinus = site - 1;

        if(allReports % lines > 0)
            allPages++;
        if(site > allPages || site == 0) {
            proxiedPlayer.sendMessage(ReportSystem.prefix + "Es ist aktuell keine Case offen oder diese Seite exestiert nicht");
            return;
        }
        proxiedPlayer.sendMessage(" ");
        if(site == 1) {
            proxiedPlayer.sendMessage(ReportSystem.prefix + "Es sind aktuell §e" + allReports + " §7Cases offen.");
        } else {
            proxiedPlayer.sendMessage(ReportSystem.prefix + "Du befindest dich auf der Seite §e" + site);
        }
        proxiedPlayer.sendMessage(" ");

        List<Report> reports = reportManager.getOpenCases(lines, site);

        for(Report report : reports) {
            TextComponent textComponent = new TextComponent(ReportSystem.prefix + "§e" + cloudHandler.getUsername(report.getSuspect()) + " §7wurde aufgrund von §c" + report.getReportReason() + " §7um " + report.getCreationTime() + " gemeldet §8[" + report.getCaseId() + "]");
            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eKlicke hier, um diesen Case zu übernehmen").create()));
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report accept " + report.getCaseId()));
            proxiedPlayer.sendMessage(textComponent);
        }
        arrows(oneSitePlus, oneSiteMinus, proxiedPlayer);
    }

    private void arrows(int oneSitePlus, int oneSiteMinus, ProxiedPlayer proxiedPlayer) {
        TextComponent textComponent = new TextComponent(ReportSystem.prefix);

        TextComponent textComponent1 = new TextComponent("§f«« ");
        textComponent1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report list " + oneSiteMinus));
        textComponent1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§8(§7Seite: §e" + oneSiteMinus + "§8)").create()));
        textComponent.addExtra(textComponent1);

        TextComponent textComponent2 = new TextComponent("            ");
        textComponent2.setClickEvent(null);
        textComponent2.setHoverEvent(null);
        textComponent.addExtra(textComponent2);

        TextComponent textComponent3 = new TextComponent(" §f»»");
        textComponent3.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report list " + oneSitePlus));
        textComponent3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§8(§7Seite: §e" + oneSitePlus + "§8)").create()));
        textComponent.addExtra(textComponent3);
        proxiedPlayer.sendMessage(textComponent);
    }
}
