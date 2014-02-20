package openblocks.common.tileentity;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import openblocks.OpenBlocks;
import openblocks.client.stencils.StencilIconPair;
import openblocks.client.stencils.StencilManager;
import openblocks.common.item.ItemPaintBrush;
import openblocks.common.item.ItemSqueegee;
import openblocks.common.item.ItemStencil;
import openblocks.common.sync.SyncableBlockLayers;
import openblocks.common.sync.SyncableBlockLayers.Layer;
import openmods.api.IActivateAwareTile;
import openmods.api.ISpecialDrops;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncableInt;
import openmods.sync.SyncableIntArray;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockUtils;

public class TileEntityCanvas extends SyncedTileEntity implements IActivateAwareTile, ISpecialDrops {

	private static final int BASE_LAYER = -1;

	public static final int[] ALL_SIDES = { 0, 1, 2, 3, 4, 5 };

	/* Used for painting other blocks */
	public SyncableInt paintedBlockId, paintedBlockMeta;

	private SyncableIntArray baseColors;

	public SyncableBlockLayers stencilsUp;
	public SyncableBlockLayers stencilsDown;
	public SyncableBlockLayers stencilsEast;
	public SyncableBlockLayers stencilsWest;
	public SyncableBlockLayers stencilsNorth;
	public SyncableBlockLayers stencilsSouth;

	public SyncableBlockLayers[] allSides;

	@Override
	public void initialize() {}

	public void setupForItemRenderer() {
		createSyncedFields();
	}

	public SyncableIntArray getBaseColors() {
		return baseColors;
	}

	@Override
	protected void createSyncedFields() {
		stencilsUp = new SyncableBlockLayers();
		stencilsDown = new SyncableBlockLayers();
		stencilsEast = new SyncableBlockLayers();
		stencilsWest = new SyncableBlockLayers();
		stencilsNorth = new SyncableBlockLayers();
		stencilsSouth = new SyncableBlockLayers();
		allSides = new SyncableBlockLayers[] {
				stencilsDown, stencilsUp, stencilsNorth, stencilsSouth, stencilsWest, stencilsEast
		};
		baseColors = new SyncableIntArray(new int[] { 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF });
		paintedBlockId = new SyncableInt(0);
		paintedBlockMeta = new SyncableInt(0);
	}

	public SyncableBlockLayers getLayersForSide(int side) {
		return allSides[side];
	}

	@Override
	public void onSynced(Set<ISyncableObject> changes) {
		worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
	}

	public Layer getLayerForSide(int renderSide, int layerId) {
		SyncableBlockLayers layers = getLayersForSide(renderSide);
		if (layers != null) { return layers.getLayer(layerId); }
		return null;
	}

	public int getColorForRender(int renderSide, int layerId) {
		if (layerId == BASE_LAYER) { return baseColors.getValue(renderSide); }
		Layer layer = getLayerForSide(renderSide, layerId);
		if (layer != null) { return layer.getColorForRender(); }
		return 0xCCCCCC;
	}

	public Icon getTextureForRender(int renderSide, int layerId) {
		if (layerId > BASE_LAYER) {
			Layer layer = getLayerForSide(renderSide, layerId);
			if (layer != null) {
				BigInteger bits = layer.getBits();
				if (bits != null) {
					StencilIconPair data = StencilManager.instance.getIcon(bits);
					return layer.hasStencilCover()? data.coverIcon : data.invertedIcon;
				}
			}
		}
		return getBaseTexture(renderSide);
	}

	private Icon getBaseTexture(int side) {
		if (paintedBlockId.getValue() == 0) return OpenBlocks.Blocks.canvas.getIcon(0, 0);
		Block block = Block.blocksList[paintedBlockId.getValue()];
		if (block == null) return OpenBlocks.Blocks.canvas.getIcon(0, 0);
		return block.getIcon(side, paintedBlockMeta.getValue());
	}

	private boolean isBlockUnpainted() {
		for (int i = 0; i < allSides.length; i++) {
			if (!allSides[i].isEmpty() || baseColors.getValue(i) != 0xFFFFFF) return false;
		}
		return true;
	}

	public void applyPaint(int color, int... sides) {
		for (int side : sides) {
			SyncableBlockLayers layer = getLayersForSide(side);
			if (layer.isLastLayerCover()) {
				layer.setLastLayerColor(color);
				layer.moveStencilToNextLayer();
			} else {
				// collapse all layers, since they will be fully covered by
				// paint
				layer.clear();
				baseColors.setValue(side, color);
			}
		}

		if (!worldObj.isRemote) sync();
	}

	private void dropStackFromSide(ItemStack stack, int side) {
		if (worldObj.isRemote) return;
		ForgeDirection dropSide = ForgeDirection.getOrientation(side);

		double dropX = xCoord + dropSide.offsetX;
		double dropY = yCoord + dropSide.offsetY;
		double dropZ = zCoord + dropSide.offsetZ;

		BlockUtils.dropItemStackInWorld(worldObj, dropX, dropY, dropZ, stack);
	}

	public void removePaint(int... sides) {
		for (int side : sides) {
			SyncableBlockLayers layer = getLayersForSide(side);

			// If there is a stencil on top, pop it off.
			if (layer.isLastLayerCover()) {
				BigInteger stencil = layer.getTopStencil();
				ItemStack dropStack = OpenBlocks.Items.stencil.createItemStack(stencil);
				dropStackFromSide(dropStack, side);
			}

			layer.clear();

			baseColors.setValue(side, 0xFFFFFF);
		}

		if (isBlockUnpainted() && paintedBlockId.getValue() != 0) {
			worldObj.setBlock(xCoord, yCoord, zCoord, paintedBlockId.getValue(), paintedBlockMeta.getValue(), BlockNotifyFlags.SEND_TO_CLIENTS);
		}

		if (!worldObj.isRemote) sync();
	}

	public boolean useStencil(int side, BigInteger bits) {
		SyncableBlockLayers layer = getLayersForSide(side);
		if (layer.isLastLayerCover()) {
			BigInteger topStencil = layer.getTopStencil();
			if (topStencil == bits) return false;

			ItemStack dropStack = OpenBlocks.Items.stencil.createItemStack(topStencil);
			dropStackFromSide(dropStack, side);
			layer.setLastLayerStencil(bits);
		} else layer.pushNewStencil(bits);

		if (!worldObj.isRemote) sync();
		return true;
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem();
		if (held != null) {
			Item heldItem = held.getItem();
			if (heldItem instanceof ItemSqueegee || heldItem instanceof ItemPaintBrush || heldItem instanceof ItemStencil) return false;
		}

		SyncableBlockLayers layer = getLayersForSide(side);

		if (layer.isLastLayerCover()) {
			if (player.isSneaking()) {
				BigInteger bits = layer.removeCover();
				if (!worldObj.isRemote) {
					ItemStack dropStack = OpenBlocks.Items.stencil.createItemStack(bits);
					dropStackFromSide(dropStack, side);
				}

			} else getLayersForSide(side).rotateCover();

			if (!worldObj.isRemote) sync();
			return true;
		}

		return false;
	}

	@Override
	public void addDrops(List<ItemStack> drops) {
		for (SyncableBlockLayers sideLayers : allSides) {
			if (sideLayers.isLastLayerCover()) {
				BigInteger bits = sideLayers.getTopStencil();
				if (bits != null) drops.add(OpenBlocks.Items.stencil.createItemStack(bits));
			}
		}
	}
}
