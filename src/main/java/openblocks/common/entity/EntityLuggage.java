package openblocks.common.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import openblocks.OpenBlocks;
import openblocks.OpenBlocksGuiHandler;
import openblocks.common.entity.ai.EntityAICollectItem;
import openmods.inventory.GenericInventory;
import openmods.inventory.IInventoryProvider;
import openmods.inventory.transfer.SinkFactory;
import openmods.inventory.transfer.sinks.IItemStackSink;

import com.google.common.base.Strings;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityLuggage extends EntityTameable implements IInventoryProvider, IEntityAdditionalSpawnData {

	protected GenericInventory inventory = new GenericInventory("luggage", false, 27);
	public boolean special;

	public int lastSound = 0;

	public EntityLuggage(World world) {
		super(world);
		setSize(0.5F, 0.5F);
		setAIMoveSpeed(0.7F);
		setMoveForward(0);
		setTamed(true);
		func_110163_bv(); // set persistent
		getNavigator().setAvoidsWater(true);
		getNavigator().setCanSwim(true);
		this.tasks.addTask(1, new EntityAISwimming(this));
		this.tasks.addTask(2, new EntityAIFollowOwner(this, getAIMoveSpeed(), 10.0F, 2.0F));
		this.tasks.addTask(3, new EntityAICollectItem(this));
		this.dataWatcher.addObject(18, Integer.valueOf(inventory.getSizeInventory())); // inventory
	}

	public void setSpecial() {
		if (special) return;
		special = true;
		GenericInventory inventory = new GenericInventory("luggage", false, 54);
		inventory.copyFrom(this.inventory);
		if (this.dataWatcher != null) {
			this.dataWatcher.updateObject(18, Integer.valueOf(inventory.getSizeInventory()));
		}
		this.inventory = inventory;
	}

	public boolean isSpecial() {
		if (worldObj.isRemote) { return inventory.getSizeInventory() > 27; }
		return special;
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (worldObj.isRemote) {
			int inventorySize = dataWatcher.getWatchableObjectInt(18);
			if (inventory.getSizeInventory() != inventorySize) {
				inventory = new GenericInventory("luggage", false, inventorySize);
			}
		}
		lastSound++;
	}

	@Override
	public boolean isAIEnabled() {
		return true;
	}

	@Override
	public GenericInventory getInventory() {
		return inventory;
	}

	@Override
	public EntityAgeable createChild(EntityAgeable entityageable) {
		return null;
	}

	@Override
	public boolean interact(EntityPlayer player) {
		if (!worldObj.isRemote && !isDead) {
			if (player.isSneaking()) {
				ItemStack luggageItem = new ItemStack(OpenBlocks.Items.luggage);
				NBTTagCompound tag = new NBTTagCompound();
				inventory.writeToNBT(tag);
				luggageItem.setTagCompound(tag);

				String nameTag = getCustomNameTag();
				if (!Strings.isNullOrEmpty(nameTag)) luggageItem.setStackDisplayName(nameTag);

				if (player.inventory.addItemStackToInventory(luggageItem)) setDead();
			} else {
				player.openGui(OpenBlocks.instance, OpenBlocksGuiHandler.GuiId.luggage.ordinal(), player.worldObj, getEntityId(), 0, 0);
			}
		}
		return true;
	}

	public boolean canConsumeStackPartially(ItemStack stack) {
		IItemStackSink sink = SinkFactory.wrap(inventory);
		sink.accept(stack);
		return sink.hasChanges();
	}

	@Override
	protected void func_145780_a(int x, int y, int z, Block block) {
		playSound("openblocks:luggage.walk", 0.3F, 0.7F + (worldObj.rand.nextFloat() * 0.5f));
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setBoolean("shiny", special);
		inventory.writeToNBT(tag);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		if (tag.hasKey("shiny") && tag.getBoolean("shiny")) setSpecial();
		inventory.readFromNBT(tag);
	}

	@Override
	public void onStruckByLightning(EntityLightningBolt lightning) {
		setSpecial();
	}

	@Override
	public boolean isEntityInvulnerable() {
		return true;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	@Override
	public void writeSpawnData(ByteBuf data) {
		data.writeInt(inventory.getSizeInventory());
	}

	@Override
	public void readSpawnData(ByteBuf data) {
		inventory = new GenericInventory("luggage", false, data.readInt());
	}

	@Override
	public double getMountedYOffset() {
		return 0.825;
	}
}
