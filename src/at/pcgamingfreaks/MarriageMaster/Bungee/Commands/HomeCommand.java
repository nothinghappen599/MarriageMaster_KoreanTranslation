/*
 *   Copyright (C) 2019 GeorgH93
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.pcgamingfreaks.MarriageMaster.Bungee.Commands;

import at.pcgamingfreaks.Bungee.Message.Message;
import at.pcgamingfreaks.MarriageMaster.Bungee.API.Marriage;
import at.pcgamingfreaks.MarriageMaster.Bungee.API.MarriagePlayer;
import at.pcgamingfreaks.MarriageMaster.Bungee.API.MarryCommand;
import at.pcgamingfreaks.MarriageMaster.Bungee.Listener.PluginChannelCommunicator;
import at.pcgamingfreaks.MarriageMaster.Bungee.MarriageMaster;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HomeCommand extends MarryCommand
{
	private final Message messagePlayerNoHome, messageNoHome, messageHomeBlockedTo, messageHomeBlockedFrom;
	private final Set<String> blockedFrom, blockedTo;
	private final boolean delayed;
	private final PluginChannelCommunicator communicator;

	public HomeCommand(MarriageMaster plugin)
	{
		super(plugin, "home", plugin.getLanguage().getTranslated("Commands.Description.Home"), "marry.home", false, true, plugin.getLanguage().getCommandAliases("Home"));

		messageNoHome          = plugin.getLanguage().getMessage("Ingame.Home.NoHome");
		messagePlayerNoHome    = plugin.getLanguage().getMessage("Ingame.Home.PlayerNoHome");
		messageHomeBlockedTo   = plugin.getLanguage().getMessage("Ingame.Home.BlockedTo");
		messageHomeBlockedFrom = plugin.getLanguage().getMessage("Ingame.Home.BlockedFrom");

		delayed     = plugin.getConfig().isHomeDelayed();
		blockedTo   = plugin.getConfig().getHomeBlackListedServersTo();
		blockedFrom = plugin.getConfig().getHomeBlackListedServersFrom();

		communicator = plugin.getPluginChannelCommunicator();
		communicator.setHomeCommand(this);
	}

	@Override
	public void close()
	{
		communicator.setHomeCommand(null);
	}

	@Override
	public void execute(@NotNull CommandSender sender, @NotNull String mainCommandAlias, @NotNull String alias, @NotNull String[] args)
	{
		MarriagePlayer player = getMarriagePlugin().getPlayerData((ProxiedPlayer) sender);
		if(player.isMarried() || (sender.hasPermission("marry.home.others") && ((!getMarriagePlugin().areMultiplePartnersAllowed() && args.length == 1) || (getMarriagePlugin().areMultiplePartnersAllowed() && args.length == 2))))
		{
			Marriage marriage = getTargetedMarriage(sender, player, args);
			if(marriage != null)
			{
				if(!marriage.isHomeSet()) // no home set
				{
					if(sender.hasPermission("marry.home.others") && !marriage.getPartner1().equals(player) && !marriage.getPartner2().equals(player))
					{
						messagePlayerNoHome.send(sender);
					}
					else
					{
						messageNoHome.send(sender);
					}
				}
				else
				{
					if(!blockedFrom.contains(((ProxiedPlayer) sender).getServer().getInfo().getName().toLowerCase()))
					{
						//noinspection ConstantConditions
						if(!blockedTo.contains(marriage.getHome().getHomeServer().toLowerCase()))
						{
							if(delayed && !player.hasPermission("marry.skiptpdelay"))
							{
								//noinspection ConstantConditions
								communicator.sendMessage(((ProxiedPlayer) sender).getServer().getInfo(), "delayHome", player.getUUID().toString(), marriage.getPartner(player).getUUID().toString());
							}
							else
							{
								sendHome(player.getPlayer(), marriage);
							}
						}
						else
						{
							messageHomeBlockedTo.send(sender);
						}
					}
					else
					{
						messageHomeBlockedFrom.send(sender);
					}
				}
			}
		}
		else
		{
			((MarriageMaster) getMarriagePlugin()).messageNotMarried.send(sender);
		}
	}

	@Override
	public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String mainCommandAlias, @NotNull String alias, @NotNull String[] args)
	{
		List<String> names = getMarriagePlugin().getCommandManager().getSimpleTabComplete(sender, args);
		if(sender.hasPermission("marry.home.others"))
		{
			if(names == null) names = new LinkedList<>();
			String arg = args[args.length - 1].toLowerCase();
			for(ProxiedPlayer player : plugin.getProxy().getPlayers())
			{
				if(!names.contains(player.getName()) && !sender.getName().equals(player.getName()) && player.getName().toLowerCase().startsWith(arg))
				{
					names.add(player.getName());
				}
			}
			if(names.isEmpty())
			{
				names = null;
			}
		}
		return names;
	}

	@Override
	public boolean canUse(@NotNull CommandSender sender)
	{
		return super.canUse(sender) && (sender.hasPermission("marry.home.others") || getMarriagePlugin().getPlayerData((ProxiedPlayer) sender).isMarried());
	}

	private Marriage getTargetedMarriage(CommandSender sender, MarriagePlayer player, String[] args)
	{
		Marriage marriage;
		if(getMarriagePlugin().areMultiplePartnersAllowed())
		{
			if(args.length == 2 && sender.hasPermission("marry.home.others"))
			{
				MarriagePlayer target1 = getMarriagePlugin().getPlayerData(args[0]), target2 = getMarriagePlugin().getPlayerData(args[1]);
				if(target1.isMarried() && target2.isMarried() && target1.isPartner(target2))
				{
					marriage = target1.getMarriageData(target2);
				}
				else
				{
					if(!target1.isMarried() || !target2.isMarried())
					{
						MarriagePlayer t = (!target1.isMarried()) ? target1 : target2;
						((MarriageMaster) getMarriagePlugin()).messagePlayerNotMarried.send(sender, t.getName());
					}
					else
					{
						((MarriageMaster) getMarriagePlugin()).messagePlayersNotMarried.send(sender);
					}
					return null;
				}
			}
			else if(args.length == 1)
			{
				MarriagePlayer partner = getMarriagePlugin().getPlayerData(args[0]);
				if(!player.isPartner(partner))
				{
					((MarriageMaster) getMarriagePlugin()).messageTargetPartnerNotFound.send(sender);
					return null;
				}
				marriage = player.getMarriageData(partner);
			}
			else
			{
				marriage = player.getMarriageData();
			}
		}
		else
		{
			if(args.length == 1 && sender.hasPermission("marry.home.others"))
			{
				MarriagePlayer target = getMarriagePlugin().getPlayerData(args[0]);
				if(target.isMarried())
				{
					marriage = target.getMarriageData();
				}
				else
				{
					((MarriageMaster) getMarriagePlugin()).messagePlayerNotMarried.send(sender, target.getName());
					return null;
				}
			}
			else
			{
				marriage = player.getMarriageData();
			}
		}
		return marriage;
	}

	public void sendHome(final @NotNull UUID playerUUID, final @NotNull UUID partnerUUID)
	{
		ProxiedPlayer pPlayer = plugin.getProxy().getPlayer(playerUUID);
		if(pPlayer != null)
		{
			MarriagePlayer player = getMarriagePlugin().getPlayerData(pPlayer);
			Marriage marriage;
			if(getMarriagePlugin().areMultiplePartnersAllowed())
			{
				MarriagePlayer partner = getMarriagePlugin().getPlayerData(partnerUUID);
				marriage = player.getMarriageData(partner);
			}
			else
			{
				marriage = player.getMarriageData();
			}
			if(marriage != null)
			{
				sendHome(pPlayer, marriage);
			}
		}
	}

	public void sendHome(final ProxiedPlayer player, final Marriage marriage)
	{
		if(marriage.getHome() == null) return;
		final ServerInfo homeServer = plugin.getProxy().getServerInfo(marriage.getHome().getHomeServer());
		if(!player.getServer().getInfo().getName().equals(homeServer.getName()))
		{
			player.connect(homeServer);
			plugin.getProxy().getScheduler().schedule(plugin, () -> sendHome(homeServer, player.getUniqueId(), (player.equals(marriage.getPartner1().getPlayer()) ? marriage.getPartner2() : marriage.getPartner1()).getUUID()), 1L, TimeUnit.SECONDS);
		}
		else
		{
			sendHome(homeServer, player.getUniqueId(), (player.equals(marriage.getPartner1().getPlayer()) ? marriage.getPartner2() : marriage.getPartner1()).getUUID());
		}
	}

	public void sendHome(final ServerInfo homeServer, final UUID player, final UUID partner)
	{
		communicator.sendMessage(homeServer, "home", player.toString(), partner.toString());
	}
}