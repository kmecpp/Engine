package com.builtbroken.test.lib.world.explosive;

import com.builtbroken.mc.api.explosive.IExplosiveHandler;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.framework.explosive.ExplosiveRegistry;
import com.builtbroken.mc.prefab.explosive.ExplosiveHandlerGeneric;
import com.builtbroken.mc.prefab.items.ItemNBTExplosive;
import com.builtbroken.mc.prefab.explosive.blast.BlastBasic;
import com.builtbroken.mc.lib.data.item.ItemStackWrapper;
import com.builtbroken.mc.testing.junit.AbstractTest;
import com.builtbroken.mc.testing.junit.VoltzTestRunner;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 1/28/2016.
 */
@RunWith(VoltzTestRunner.class)
public class TestExplosiveItemRegistry extends AbstractTest
{
    private static ItemNBTExplosive explosive;

    @Override
    public void setUpForEntireClass()
    {
        if (explosive == null)
        {
            explosive = new ItemNBTExplosive();
            GameRegistry.registerItem(explosive, "testExplosiveRegExItem");
        }
    }

    @Test
    public void testRegisterExplosiveItem()
    {
        IExplosiveHandler TNT = new ExplosiveHandlerGeneric("tnt", BlastBasic.class, 1);
        ExplosiveRegistry.registerOrGetExplosive(References.DOMAIN, "TNT", TNT);
        ExplosiveRegistry.registerExplosiveItem(new ItemStack(Blocks.tnt), ExplosiveRegistry.get("TNT"), 2);

        assertTrue(ExplosiveRegistry.get("TNT") == TNT);
        assertTrue(ExplosiveRegistry.get(new ItemStack(Blocks.tnt)) == TNT);
        assertEquals(ExplosiveRegistry.getExplosiveSize(new ItemStack(Blocks.tnt)), 2.0);
        assertTrue(ExplosiveRegistry.getItems(TNT).contains(new ItemStackWrapper(new ItemStack(Blocks.tnt))));

        ItemStack stack = new ItemStack(explosive);
        explosive.setExplosive(stack, TNT, 3, null);
        ExplosiveRegistry.registerExplosiveItem(stack);

        assertEquals(TNT, ExplosiveRegistry.get(stack));
        assertEquals(3.0, ExplosiveRegistry.getExplosiveSize(stack));
        assertTrue(ExplosiveRegistry.getItems(TNT).contains(new ItemStackWrapper(stack)));
    }
}
