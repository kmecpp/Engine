package com.builtbroken.mc.core.registry;

import com.builtbroken.mc.api.items.ISimpleItemRenderer;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.lib.helper.ReflectionUtility;
import com.builtbroken.mc.lib.render.block.ItemRenderHandler;
import com.builtbroken.mc.lib.render.block.RenderTileDummy;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;

public class ClientRegistryProxy extends CommonRegistryProxy
{
    @Override
    public void registerBlock(ModManager manager, String name, String modPrefix, Block block, Class<? extends ItemBlock> itemBlock)
    {
        super.registerBlock(manager, name, modPrefix, block, itemBlock);
        if(block.getUnlocalizedName() == null || block.getUnlocalizedName().contains("null"))
        {
            block.setBlockName(name);
        }

        //Set texture name, reflection is used to prevent overriding the blocks existing name
        try
        {
            Field field = ReflectionUtility.getMCField(Block.class, "textureName");
            if (field.get(block) == null)
            {
                block.setBlockTextureName(modPrefix + name);
            }
        }
        catch (IllegalAccessException e)
        {
            References.LOGGER.error(manager.name() + " Failed to access textureName field for block " + name);
        }

        //Sets creative tab
        if(manager.defaultTab != null)
        {
            try
            {
                Field field = ReflectionUtility.getMCField(Block.class, "displayOnCreativeTab");
                if (field.get(block) == null)
                {
                    block.setCreativeTab(manager.defaultTab);
                }
            }
            catch (IllegalAccessException e)
            {
                References.LOGGER.error(manager.name() + " Failed to access creativeTab field for block " + name);
            }
        }

        if(block instanceof IRegistryInit)
        {
            ((IRegistryInit) block).onClientRegistered();
        }
    }

    @Override
    public void registerItem(ModManager manager, String name, String modPrefix, Item item)
    {
        super.registerItem(manager, name, modPrefix, item);
        if (modPrefix != null)
        {
            item.setUnlocalizedName(modPrefix + name);

            if (ReflectionHelper.getPrivateValue(Item.class, item, "iconString", "field_111218_cA") == null)
            {
                item.setTextureName(modPrefix + name);
            }
        }

        if (manager.defaultTab != null)
        {
            item.setCreativeTab(manager.defaultTab);
        }

        if(item instanceof IRegistryInit)
        {
            ((IRegistryInit) item).onClientRegistered();
        }
    }

    @Override
    public void registerTileEntity(String name, String prefix, Block block, TileEntity tile)
    {
        super.registerTileEntity(name, prefix, block, tile);
        if (tile instanceof ISimpleItemRenderer)
        {
            ItemRenderHandler.register(new ItemStack(block).getItem(), (ISimpleItemRenderer) tile);
        }
    }

    @Override
    public void registerDummyRenderer(Class<? extends TileEntity> clazz)
    {
        if (!TileEntityRendererDispatcher.instance.mapSpecialRenderers.containsKey(clazz))
        {
            ClientRegistry.bindTileEntitySpecialRenderer(clazz, new RenderTileDummy());
        }
    }
}
