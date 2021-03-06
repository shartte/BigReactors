package erogenousbeef.bigreactors.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import erogenousbeef.bigreactors.client.renderer.RendererControlRod;
import erogenousbeef.bigreactors.common.BRLoader;
import erogenousbeef.bigreactors.common.CommonProxy;
import erogenousbeef.bigreactors.common.tileentity.TileEntityFuelRod;
import erogenousbeef.bigreactors.common.tileentity.TileEntityReactorControlRod;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	@Override
	public void preInit()
	{

	}

	@Override
	public void init()
	{
		super.init();
		
		RendererControlRod controlRodRenderer = new RendererControlRod();
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityReactorControlRod.class, controlRodRenderer);
	}
}
