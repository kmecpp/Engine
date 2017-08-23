package com.builtbroken.mc.client.effects.providers;

import com.builtbroken.mc.client.SharedAssets;
import com.builtbroken.mc.client.effects.VisualEffectProvider;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.network.packet.PacketSpawnParticle;
import com.builtbroken.mc.lib.render.fx.FXShockWave;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Spawns a shock wave effect at the location
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 2/15/2017.
 */
public class VEProviderShockWave extends VisualEffectProvider
{
    public VEProviderShockWave()
    {
        super("shockwave");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void displayEffect(World world, double x, double y, double z, double mx, double my, double mz, NBTTagCompound otherData)
    {
        FXShockWave fx = new FXShockWave(world, x, y, z, otherData.getFloat("red"), otherData.getFloat("green"), otherData.getFloat("blue"), otherData.getFloat("scale"), otherData.getFloat("distance"));
        fx.texture = SharedAssets.GREY_TEXTURE_40pAlpha;
        fx.setMotion(mx, my, mz);
        FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
    }

    public static void spawnEffect(World world, double x, double y, double z, double motionX, double motionY, double motionZ, float red, float green, float blue, float scale, float distance)
    {
        PacketSpawnParticle packet = new PacketSpawnParticle("VEP_shockwave", world.provider.getDimension(), x, y, z, motionX, motionY, motionZ);
        packet.otherData = new NBTTagCompound();
        packet.otherData.setFloat("red", red);
        packet.otherData.setFloat("green", green);
        packet.otherData.setFloat("blue", blue);
        packet.otherData.setFloat("scale", scale);
        packet.otherData.setFloat("distance", distance);
        Engine.packetHandler.sendToAllAround(packet, world, x, y, z, 100);
    }
}
