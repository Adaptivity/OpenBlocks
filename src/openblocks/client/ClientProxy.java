package openblocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.ServerListenThread;
import net.minecraft.server.ThreadMinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.fluids.FluidStack;
import openblocks.Config;
import openblocks.IProxy;
import openblocks.OpenBlocks;
import openblocks.client.fx.FXLiquidSpray;
import openblocks.client.model.ModelCraneBackpack;
import openblocks.client.renderer.*;
import openblocks.client.renderer.entity.*;
import openblocks.client.renderer.tileentity.*;
import openblocks.common.block.BlockTank;
import openblocks.common.entity.*;
import openblocks.common.tileentity.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy implements IProxy {

	public ClientProxy() {}

	@ForgeSubscribe
	public void textureHook(TextureStitchEvent.Post event) {
		if (event.map.textureType == 0) {
			if (OpenBlocks.Fluids.openBlocksXPJuice != null) {
				OpenBlocks.Fluids.openBlocksXPJuice.setIcons(BlockTank.Icons.xpJuiceStill, BlockTank.Icons.xpJuiceFlowing);
			}
		}
	}

	@Override
	public void init() {
		TickRegistry.registerTickHandler(new ClientTickHandler(), Side.CLIENT);
		MinecraftForge.EVENT_BUS.register(new SoundLoader());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void postInit() {
		SoundEventsManager.instance.init();
	}

	@Override
	public void registerRenderInformation() {

		OpenBlocks.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(new BlockRenderingHandler());

		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGuide.class, new TileEntityGuideRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLightbox.class, new TileEntityLightboxRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTarget.class, new TileEntityTargetRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGrave.class, new TileEntityGraveRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFlag.class, new TileEntityFlagRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTank.class, new TileEntityTankRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrophy.class, new TileEntityTrophyRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBearTrap.class, new TileEntityBearTrapRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySprinkler.class, new TileEntitySprinklerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCannon.class, new TileEntityCannonRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityVacuumHopper.class, new TileEntityVacuumHopperRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityImaginary.class, new TileEntityImaginaryRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFan.class, new TileEntityFanRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityVillageHighlighter.class, new TileEntityVillageHighlighterRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAutoAnvil.class, new TileEntityAutoAnvilRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAutoEnchantmentTable.class, new TileEntityAutoEnchantmentTableRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRopeLadder.class, new TileEntityRopeLadderRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDonationStation.class, new TileEntityDonationStationRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityOreCrusher.class, new TileEntityOreCrusherRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPaintMixer.class, new TileEntityPaintMixerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityProjector.class, new TileEntityProjectorRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityClothTest.class, new TileEntityClothTestRenderer());

		if (OpenBlocks.Blocks.tank != null) MinecraftForgeClient.registerItemRenderer(OpenBlocks.Blocks.tank.blockID, new ItemRendererTank());

		if (OpenBlocks.Items.luggage != null) {
			MinecraftForgeClient.registerItemRenderer(OpenBlocks.Items.luggage.itemID, new ItemRendererLuggage());
			RenderingRegistry.registerEntityRenderingHandler(EntityLuggage.class, new EntityLuggageRenderer());
		}

		if (OpenBlocks.Blocks.paintCan != null) {
			MinecraftForgeClient.registerItemRenderer(OpenBlocks.Blocks.paintCan.blockID, new ItemRendererPaintCan());
		}

		if (OpenBlocks.Items.hangGlider != null) {
			RenderingRegistry.registerEntityRenderingHandler(EntityHangGlider.class, new EntityHangGliderRenderer());

			ItemRendererHangGlider hangGliderRenderer = new ItemRendererHangGlider();
			MinecraftForgeClient.registerItemRenderer(OpenBlocks.Items.hangGlider.itemID, hangGliderRenderer);
			MinecraftForge.EVENT_BUS.register(hangGliderRenderer);

			attachPlayerRenderer();
		}

		if (OpenBlocks.Items.sonicGlasses != null) {
			MinecraftForge.EVENT_BUS.register(SoundEventsManager.instance);
		}

		if (OpenBlocks.Items.craneBackpack != null) {
			ModelCraneBackpack.instance.init();
			RenderingRegistry.registerEntityRenderingHandler(EntityMagnet.class, new EntityMagnetRenderer());
			RenderingRegistry.registerEntityRenderingHandler(EntityBlock.class, new EntityBlockRenderer());
		}

		MinecraftForge.EVENT_BUS.register(new PlayerRenderEventHandler());
		MinecraftForge.EVENT_BUS.register(new EntitySelectionHandler());

		if (OpenBlocks.Items.cartographer != null) {
			RenderingRegistry.registerEntityRenderingHandler(EntityCartographer.class, new EntityCartographerRenderer());
			EntitySelectionHandler.registerRenderer(EntityCartographer.class, new EntityCartographerRenderer.Selection());
		}
	}

	@SuppressWarnings("unchecked")
	private static void attachPlayerRenderer() {
		if (Config.tryHookPlayerRenderer) {
			// Get current renderer and check that it's Mojangs
			Render render = (Render)RenderManager.instance.entityRenderMap.get(EntityPlayer.class);
			if (render.getClass().equals(net.minecraft.client.renderer.entity.RenderPlayer.class)) {
				EntityPlayerRenderer playerRenderer = new EntityPlayerRenderer();
				playerRenderer.setRenderManager(RenderManager.instance);
				RenderManager.instance.entityRenderMap.put(EntityPlayer.class, playerRenderer);
			}
		}
	}

	@Override
	public boolean isServerOnly() {
		return false;
	}

	@Override
	public boolean isServerThread() {
		Thread thr = Thread.currentThread();
		return thr instanceof ThreadMinecraftServer
				|| thr instanceof ServerListenThread;
	}

	@Override
	public void spawnLiquidSpray(World worldObj, FluidStack water, double x, double y, double z, ForgeDirection direction, float angleRadians, float spread) {
		FXLiquidSpray spray = new FXLiquidSpray(worldObj, water, x, y, z, direction, angleRadians, spread);
		Minecraft.getMinecraft().effectRenderer.addEffect(spray);
	}

	@Override
	public EntityPlayer getThePlayer() {
		return FMLClientHandler.instance().getClient().thePlayer;
	}

	@Override
	public IGuiHandler createGuiHandler() {
		return new ClientGuiHandler();
	}

	@Override
	public long getTicks(World worldObj) {
		return ClientTickHandler.getTicks();
	}

	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().theWorld;
	}

	@Override
	public World getServerWorld(int id) {
		return DimensionManager.getWorld(id);
	}

	@Override
	public void sendPacketToPlayer(Player player, Packet packet) {
		if (player instanceof EntityPlayerMP) ((EntityPlayerMP)player).playerNetServerHandler.sendPacketToPlayer(packet);
		else if (player instanceof EntityClientPlayerMP) ((EntityClientPlayerMP)player).sendQueue.addToSendQueue(packet);
		else throw new UnsupportedOperationException("HOW DO I PACKET?");
	}

	@Override
	public void sendPacketToServer(Packet packet) {
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(packet);
	}
}
