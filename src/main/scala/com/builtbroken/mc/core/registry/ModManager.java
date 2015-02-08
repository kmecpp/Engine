package com.builtbroken.mc.core.registry;

import com.builtbroken.mc.core.References;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.prefab.tile.BlockTile;
import com.builtbroken.mc.prefab.tile.Tile;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Helper class that can be used to reduce the amount of code used to handle general object registration. Handles basic block and item
 * creation threw reflection. As well follows up by setting a common creative tab, mod prefix, localization name, and texture name.
 *
 * @author DarkGuardsman, Calclavia
 */
public class ModManager
{
    @SidedProxy(clientSide = "com.builtbroken.mc.core.registry.ClientRegistryProxy", serverSide = "com.builtbroken.mc.core.registry.CommonRegistryProxy")
    public static CommonRegistryProxy proxy;

    public final WeakHashMap<Block, String> blocks = new WeakHashMap();
    public final WeakHashMap<Item, String> items = new WeakHashMap();

    public String modPrefix;
    public CreativeTabs defaultTab;

    public ModManager setPrefix(String modPrefix)
    {
        this.modPrefix = modPrefix;
        if (!modPrefix.endsWith(":"))
        {
            this.modPrefix += ":";
        }
        return this;
    }

    public ModManager setTab(CreativeTabs defaultTab)
    {
        this.defaultTab = defaultTab;
        return this;
    }

    /**
     * Creates a new block based on the Tile class provided
     * Handles most common registration and data input tasks for the creation of the block.
     * This includes registering the block, tile, built in item & tile renders. It also inits
     * the instance used for the block itself.
     *
     * @param spatialClass - class that will provide all the data to be used when creating the block.
     * @param args         - arguments needed to create a new instance of the spatial class
     */
    public BlockTile newBlock(Class<? extends Tile> spatialClass, Object... args)
    {
        return newBlock(null, spatialClass, args);
    }

    /**
     * Creates a new block based on the Tile class provided
     * Handles most common registration and data input tasks for the creation of the block.
     * This includes registering the block, tile, built in item & tile renders. It also inits
     * the instance used for the block itself.
     *
     * @param spatialClass - class that will provide all the data to be used when creating the block.
     * @param args         - arguments needed to create a new instance of the spatial class
     */
    public BlockTile newBlock(String name, Class<? extends Tile> spatialClass, Object... args)
    {
        try
        {
            Tile spatial;

            if (args != null && args.length > 0)
            {
                List<Class> paramTypes = new ArrayList();

                for (Object arg : args)
                {
                    paramTypes.add(arg.getClass());
                }

                spatial = spatialClass.getConstructor(paramTypes.toArray(new Class[paramTypes.size()])).newInstance();
            }
            else
            {
                spatial = spatialClass.newInstance();
            }

            return newBlock(name, spatial);
        } catch (Exception e)
        {
            throw new RuntimeException(name() + " Tile [" + spatialClass.getSimpleName() + "] failed to be created:", e);
        }
    }

    /**
     * Creates a new block based on the Tile instance provided
     * Handles most common registration and data input tasks for the creation of the block.
     * This includes registering the block, tile, built in item & tile renders. It also inits
     * the instance used for the block itself.
     *
     * @param spatial - instance of the spatial block used to provide all the data for the block
     */
    public BlockTile newBlock(Tile spatial)
    {
        return newBlock(null, spatial);
    }

    /**
     * Creates a new block based on the Tile instance provided.
     * Handles most common registration and data input tasks for the creation of the block.
     * This includes registering the block, tile, built in item & tile renders. It also inits
     * the instance used for the block itself.
     *
     * @param spatial - instance of the spatial block used to provide all the data for the block
     * @param name    - name the block will use for look up map, registry, texture, etc
     */
    public BlockTile newBlock(String name, Tile spatial)
    {
        if (spatial.name == null || spatial.name.isEmpty())
        {
            References.LOGGER.warn(name() + " Tile: " + spatial + " has no defined name to register with and could cause issues with world loading. In order to prevent the game from crashing we are falling back to using the class name.");
            name = spatial.name != null ? spatial.name : spatial.getClass().getSimpleName();
        }

        BlockTile block = new BlockTile(spatial, modPrefix, defaultTab);
        spatial.setBlock(block);

        block = newBlock(name, block, spatial.itemBlock);

        Tile newTile = spatial.newTile();
        if (newTile != null)
        {
            proxy.registerTileEntity(name, modPrefix, block, newTile);

            if (spatial.renderTileEntity)
            {
                proxy.registerDummyRenderer(newTile.getClass());
            }
        }

        return block;
    }

    /**
     * Creates a new instance of the block class as long as it has a default constructor
     */
    public Block newBlock(Class<? extends Block> blockClazz, Class<? extends ItemBlock> itemBlockClass)
    {
        return newBlock(blockClazz.getSimpleName(), blockClazz, itemBlockClass);
    }

    /**
     * Creates a new instance of the block class as long as it has a default constructor
     */
    public Block newBlock(Class<? extends Block> blockClazz)
    {
        return newBlock(blockClazz.getSimpleName(), blockClazz);
    }

    /**
     * Creates a new instance of the block class as long as it has a default constructor
     */
    public Block newBlock(String name, Class<? extends Block> blockClazz)
    {
        return newBlock(name, blockClazz, null);
    }

    /**
     * Creates a new instance of the block class as long as it has a default constructor
     */
    public Block newBlock(String name, Class<? extends Block> blockClazz, Class<? extends ItemBlock> itemBlockClass)
    {
        try
        {
            return newBlock(name, blockClazz.newInstance(), itemBlockClass);
        } catch (InstantiationException e)
        {
            throw new RuntimeException(name() + " Failed to create block from class " + blockClazz, e);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(name() + " Failed to access class " + blockClazz, e);
        }
    }

    /**
     * Doesn't create a new block but applies the required blockName, textureName, and registers the block
     *
     * @param name  - name the block will be registed with, localization, and texture name
     * @param block - instance of the block
     */
    public <C extends Block> C newBlock(String name, C block)
    {
        return newBlock(name, block, null);
    }

    /**
     * Doesn't create a new block but applies the required blockName, textureName, and registers the block
     *
     * @param name      - name the block will be registed with, localization, and texture name
     * @param block     - instance of the block
     * @param itemBlock - item block used with the block
     */
    public <C extends Block> C newBlock(String name, C block, Class<? extends ItemBlock> itemBlock)
    {
        // Register block with item block
        proxy.registerBlock(this, name, modPrefix, block, itemBlock);
        blocks.put(block, name);
        return block;
    }

    /**
     * Creates a new Item instance using the class provided
     *
     * @param clazz - class to create an instance from
     * @param args  - arguments needed to create a new instance
     */
    public <C extends Item> C newItem(Class<C> clazz, Object... args)
    {
        return newItem(null, clazz, args);
    }

    /**
     * Creates a new item using reflection as well runs it threw some check to activate any
     * interface methods
     *
     * @param name  - name to register the item with //@param modid - mods that the item comes from
     * @param clazz - item class
     * @return the new item
     */
    public <C extends Item> C newItem(String name, Class<C> clazz, Object... args)
    {
        try
        {
            Item item;

            if (args != null && args.length > 0)
            {
                List<Class> paramTypes = new ArrayList();

                for (Object arg : args)
                {
                    paramTypes.add(arg.getClass());
                }

                item = clazz.getConstructor(paramTypes.toArray(new Class[paramTypes.size()])).newInstance();
            }
            else
            {
                item = clazz.getConstructor().newInstance();
            }

            return (C) newItem(name, item);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(name() + "Failed to create item [" + name + "] due to invalid constructor", e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(name() + "Failed to create item [" + name + "]", e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(name() + "Failed to create item [" + name + "] due to access issue", e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(name() + "Failed to create item [" + name + "] when invoking constructor", e);
        }
    }

    /** Registers a new item using its class as the registry name
     * @param item - item instance to register
     * @return item instance
     */
    public Item newItem(Item item)
    {
        return newItem(null, item);
    }

    /**
     * Registers a new item
     * @param name - name to register with, as well use for icon & lang name if missing
     * @param item - item instance to register
     * @return item instance
     */
    public Item newItem(String name, Item item)
    {
        if(name == null || name.isEmpty())
        {
            References.LOGGER.warn(name() +" Registry name was not provided for item " + item + " using class name to prevent game from crashing. This may cause world loading issues in the future.");
            name = LanguageUtility.decapitalizeFirst(item.getClass().getSimpleName().replace("Item", ""));
        }
        proxy.registerItem(this, name, modPrefix, item);
        items.put(item, name);
        return item;
    }


    public String name()
    {
        return "ModManager[" + (modPrefix == null || modPrefix.isEmpty() ? hashCode() : modPrefix) + "]";
    }
}
