package com.builtbroken.mc.seven.framework.block.listeners;

import com.builtbroken.mc.api.tile.node.ITileNodeHost;
import com.builtbroken.mc.api.tile.provider.IInventoryProvider;
import com.builtbroken.mc.framework.block.imp.IBlockListener;
import com.builtbroken.mc.framework.block.imp.IDestroyedListener;
import com.builtbroken.mc.prefab.inventory.InventoryIterator;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Handles unloading the inventory onto the group when a tile is broken
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/3/2017.
 */
public class InventoryBreakListener extends TileListener implements IDestroyedListener, IBlockListener
{
    @Override
    public void breakBlock(IBlockState state)
    {
        Object tile = world().unwrap().getTileEntity(pos);
        IInventory inventory = null;
        while (tile != null && inventory == null)
        {
            if (tile instanceof IInventory)
            {
                inventory = (IInventory) tile;
            }
            else if (tile instanceof IInventoryProvider)
            {
                inventory = ((IInventoryProvider) tile).getInventory();
            }

            if (tile instanceof ITileNodeHost)
            {
                tile = ((ITileNodeHost) tile).getTileNode();
            }
            else
            {
                tile = null;
            }
        }

        if (inventory != null)
        {
            InventoryIterator iterator = new InventoryIterator(inventory, true);
            for (ItemStack stack : iterator)
            {
                InventoryUtility.dropItemStack(world().unwrap(), xi(), yi(), zi(), stack, 0, 0);
                inventory.setInventorySlotContents(iterator.slot(), null);
            }
        }
    }
}