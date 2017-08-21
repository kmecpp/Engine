package com.builtbroken.mc.core.commands.permissions.sub;

import com.builtbroken.mc.core.commands.permissions.GroupProfileHandler;
import com.builtbroken.mc.core.commands.prefab.SubCommand;
import com.builtbroken.mc.framework.access.AccessGroup;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.TextComponentString;

import java.util.List;

/**
 * Created by robert on 2/17/2015.
 */
public class CommandRemoveGroup extends SubCommand
{
    public CommandRemoveGroup()
    {
        super("group");
    }

    @Override
    public boolean handleEntityPlayerCommand(EntityPlayer player, String[] args)
    {
        return handleConsoleCommand(player, args);
    }

    @Override
    public boolean handleConsoleCommand(ICommandSender sender, String[] args)
    {
        if (args.length > 0)
        {
            String name = args[0];
            if (GroupProfileHandler.GLOBAL.getAccessProfile().getGroup(name) != null)
            {
                AccessGroup group = new AccessGroup(name);
                if(GroupProfileHandler.GLOBAL.getAccessProfile().removeGroup(group) != null)
                {
                    if(group.getExtendGroup() != null)
                    {
                        int i = 0;
                        for(AccessGroup g : GroupProfileHandler.GLOBAL.getAccessProfile().getGroups())
                        {
                            if(g.getExtendGroup().getName().equalsIgnoreCase(group.getName()))
                            {
                                i++;
                                g.setToExtend(group.getExtendGroup());
                            }
                        }
                        sender.addChatMessage(new TextComponentString(i + " groups updated"));
                    }
                    sender.addChatMessage(new TextComponentString("Group \'" + name + "\' removed"));
                }
                else
                {
                    sender.addChatMessage(new TextComponentString("Error removing group"));
                }
            }
            else
            {
                sender.addChatMessage(new TextComponentString("Group \'" + name +"\' not found"));
            }
        }
        else
        {
            sender.addChatMessage(new TextComponentString("Missing group name"));
        }
        return true;
    }

    @Override
    public boolean isHelpCommand(String[] args)
    {
        return args != null && args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"));
    }

    @Override
    public void getHelpOutput(ICommandSender sender, List<String> items)
    {
        items.add("[name]");
    }
}
