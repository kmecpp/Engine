package com.builtbroken.mc.framework.block;

import com.builtbroken.jlib.data.Colors;
import com.builtbroken.mc.api.tile.access.IGuiTile;
import com.builtbroken.mc.api.tile.listeners.*;
import com.builtbroken.mc.api.tile.provider.IInventoryProvider;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.registry.ModManager;
import com.builtbroken.mc.core.registry.implement.IRegistryInit;
import com.builtbroken.mc.framework.logic.imp.ITileNodeHost;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.lib.helper.WrenchUtility;
import com.builtbroken.mc.lib.json.IJsonGenMod;
import com.builtbroken.mc.lib.json.imp.IJsonGenObject;
import com.builtbroken.mc.prefab.inventory.InventoryIterator;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.prefab.items.ItemBlockAbstract;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Block generated through a json based file format... Used to reduce dependency on code
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public class BlockBase extends BlockContainer implements IRegistryInit, IJsonGenObject, ITileEntityProvider
{
    public final BlockPropertyData data;
    public IJsonGenMod mod;

    protected boolean registered = false;

    private final HashMap<String, List<ITileEventListener>> listeners = new HashMap();

    public BlockBase(BlockPropertyData data)
    {
        super(data.getMaterial());
        this.data = data;
        this.setBlockName(data.localization.replace("${name}", data.name));
        this.setResistance(data.getResistance());
        this.setHardness(data.getHardness());
    }

    @Override
    public String getLoader()
    {
        return "block";
    }

    @Override
    public String getMod()
    {
        return data.MOD;
    }

    @Override
    public void register(IJsonGenMod mod, ModManager manager)
    {
        if (!registered)
        {
            this.mod = mod;
            registered = true;
            manager.newBlock(data.registryKey, this, ItemBlockAbstract.class);
            if (data.tileEntityProvider != null)
            {
                data.tileEntityProvider.register(this, mod, manager);
            }
        }
    }

    @Override
    public void onRegistered()
    {
        if (data.oreName != null)
        {
            OreDictionary.registerOre(data.oreName, new ItemStack(this));
        }
    }

    @Override
    public void onClientRegistered()
    {

    }

    @Override
    public String toString()
    {
        return "Block[" + data.name + "]";
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        if (data.tileEntityProvider != null)
        {
            return data.tileEntityProvider.createNewTileEntity(this, world, meta);
        }
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta)
    {
        return createNewTileEntity(world, meta);
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z)
    {
        //TODO implement
        return data.getHardness();
    }

    @Override
    public void fillWithRain(World world, int x, int y, int z)
    {
        if (listeners.containsKey("rain"))
        {
            for (ITileEventListener listener : listeners.get("rain"))
            {
                if (listener instanceof IFillRainListener)
                {
                    if (listener instanceof IBlockListener)
                    {
                        ((IBlockListener) listener).inject(world, x, y, z);
                        ((IFillRainListener) listener).onFilledWithRain();
                        ((IBlockListener) listener).eject();
                    }
                    else
                    {
                        ((IFillRainListener) listener).onFilledWithRain();
                    }
                }
            }
        }
    }

    @Override
    public float getExplosionResistance(Entity entity)
    {
        if (listeners.containsKey("resistance"))
        {
            float re = -1;
            for (ITileEventListener listener : listeners.get("resistance"))
            {
                if (listener instanceof IResistanceListener)
                {
                    float value = ((IResistanceListener) listener).getExplosionResistance(entity);
                    if (value >= 0 && (value < re || re < 0))
                    {
                        re = value;
                    }
                }
            }
            if (re >= 0)
            {
                return re;
            }
        }
        return data.getResistance() / 5.0F;
    }

    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ)
    {
        if (listeners.containsKey("resistance"))
        {
            float re = -1;
            for (ITileEventListener listener : listeners.get("resistance"))
            {
                if (listener instanceof IResistanceListener)
                {
                    float value;
                    if (listener instanceof IBlockListener)
                    {
                        ((IBlockListener) listener).inject(world, x, y, z);
                        value = ((IResistanceListener) listener).getExplosionResistance(entity, explosionX, explosionY, explosionZ);
                        ((IBlockListener) listener).eject();
                    }
                    else
                    {
                        value = ((IResistanceListener) listener).getExplosionResistance(entity, explosionX, explosionY, explosionZ);
                    }

                    if (value >= 0 && (value < re || re < 0))
                    {
                        re = value;
                    }
                }
            }
            if (re >= 0)
            {
                return re;
            }
        }
        return getExplosionResistance(entity);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z)
    {
        //TODO implement
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack)
    {
        //TODO implement
    }

    @Override
    public void onPostBlockPlaced(World world, int x, int y, int z, int metadata)
    {
        //TODO implement
    }

    /**
     * Called upon the block being destroyed by an explosion
     */
    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion ex)
    {
        //TODO implement
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int par6)
    {
        //TODO implement
        Object tile = world.getTileEntity(x, y, z);
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
                InventoryUtility.dropItemStack(world, x, y, z, stack, 0, 0);
                inventory.setInventorySlotContents(iterator.slot(), null);
            }
        }
        super.breakBlock(world, x, y, z, block, par6);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
    {
        //TODO implement
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random)
    {
        //TODO implement
        return 1;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        //TODO implement
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)
    {
        //TODO implement
        return super.canPlaceBlockOnSide(world, x, y, z, side);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        //TODO implement
        return super.canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ)
    {
        //TODO implement
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        Object tile = getTile(world, x, y, z);
        if (tile instanceof IActivationListener)
        {
            ((IActivationListener) tile).onPlayerClicked(player);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        try
        {
            Object tile = getTile(world, x, y, z);
            if (WrenchUtility.isUsableWrench(player, player.inventory.getCurrentItem(), x, y, z)
                    && tile instanceof IWrenchListener && ((IWrenchListener) tile).handlesWrenchRightClick())
            {
                if (((IWrenchListener) tile).onPlayerRightClickWrench(player, side, hitX, hitY, hitZ))
                {
                    WrenchUtility.damageWrench(player, player.inventory.getCurrentItem(), x, y, z);
                    return true;
                }
                return false;
            }
            else if (tile instanceof IGuiTile && ((IGuiTile) tile).shouldOpenOnRightClick(player))
            {
                player.openGui(mod, ((IGuiTile) tile).getDefaultGuiID(player), world, x, y, z);
                return true;
            }
            else if (tile instanceof IActivationListener)
            {
                return ((IActivationListener) tile).onPlayerActivated(player, side, hitX, hitY, hitZ);
            }
            return false;
        }
        catch (Exception e)
        {
            outputError(world, x, y, z, "while right click block on side " + side, e);
            player.addChatComponentMessage(new ChatComponentText(Colors.RED.code + LanguageUtility.getLocal("blockTile.error.onBlockActivated")));
        }
        return false;
    }

    protected Object getTile(World world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ITileNodeHost)
        {
            return ((ITileNodeHost) tile).getTileNode();
        }
        return tile;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random par5Random)
    {
        //TODO implement
    }

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random par5Random)
    {
        //TODO implement
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        //TODO implement
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB aabb, List list, Entity entity)
    {
        super.addCollisionBoxesToList(world, x, y, z, aabb, list, entity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z)
    {
        //TODO implement
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
    {
        //TODO implement
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return super.shouldSideBeRendered(access, x, y, z, side);
    }

    @Override
    public boolean isBlockSolid(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return super.isBlockSolid(access, x, y, z, side);
    }

    @Override
    public int getLightValue(IBlockAccess access, int x, int y, int z)
    {
        //TODO implement
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride()
    {
        //TODO implement
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        //TODO implement
        return super.isOpaqueCube();
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        //TODO implement
        return super.renderAsNormalBlock();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType()
    {
        //TODO implement
        return 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return super.getIcon(access, x, y, z, side);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta)
    {
        //TODO implement
        return super.getIcon(side, meta);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        super.registerBlockIcons(iconRegister);
        //TODO implement
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockAccess access, int x, int y, int z)
    {
        //TODO implement
        return super.colorMultiplier(access, x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBlockColor()
    {
        //TODO implement
        return super.getBlockColor();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderColor(int i)
    {
        //TODO implement
        return super.getRenderColor(i);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player)
    {
        //TODO implement
        return super.getPickBlock(target, world, x, y, z, player);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
        //TODO implement
        return new ArrayList<ItemStack>();
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list)
    {
        super.getSubBlocks(item, creativeTabs, list);
        //TODO implement
    }

    /**
     * Redstone interaction
     */
    @Override
    public boolean canProvidePower()
    {
        //TODO implement
        return false;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return 0;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z)
    {
        //TODO implement
    }

    @Override
    public void setBlockBoundsForItemRender()
    {
        //TODO implement
    }

    @Override
    protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack itemStack)
    {
        //TODO implement
    }

    @Override
    public int getRenderBlockPass()
    {
        //TODO implement
        return super.getRenderBlockPass();
    }

    @Override
    public int tickRate(World world)
    {
        //TODO implement
        return super.tickRate(world);
    }

    /**
     * Outputs an error to console with location data
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param msg
     * @param e
     */
    protected void outputError(World world, int x, int y, int z, String msg, Throwable e)
    {
        String dim = "null";
        if (world != null && world.provider != null)
        {
            dim = "" + world.provider.dimensionId;
        }
        Engine.logger().error("Error: " + msg + " \nLocation[" + dim + "w " + x + "x " + y + "y " + z + "z" + "]", e);
    }
}
