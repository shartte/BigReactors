package erogenousbeef.bigreactors.common.tileentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import erogenousbeef.bigreactors.common.block.BlockReactorPart;
import erogenousbeef.core.common.CoordTriplet;
import erogenousbeef.core.multiblock.MultiblockControllerBase;
import erogenousbeef.core.power.buildcraft.PowerProviderBeef;
import universalelectricity.core.block.IConductor;
import universalelectricity.core.block.IConnector;
import universalelectricity.core.block.IVoltage;
import universalelectricity.core.electricity.ElectricityNetworkHelper;
import universalelectricity.core.electricity.IElectricityNetwork;
import buildcraft.api.core.SafeTimeTracker;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class TileEntityReactorPowerTap extends TileEntityReactorPart implements IVoltage, IConnector, IPowerReceptor {

	public static final float uePowerFactor = 0.01f;	 // 100 UE watts per 1 internal power
	public static final float bcPowerFactor = 1.00f;  // 1 MJ per 1 internal power
	
	boolean isConnected;
	IPowerProvider powerProvider;
	
	ForgeDirection out;
	
	public TileEntityReactorPowerTap() {
		super();
		
		isConnected = false;
		
		powerProvider = new PowerProviderBeef();
		
		out = ForgeDirection.UNKNOWN;
	}
	
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
		if(isConnected()) {
			checkForConnections(world, x, y, z);
		}
	}
	
	// IMultiblockPart
	@Override
	public void onAttached(MultiblockControllerBase newController) {
		super.onAttached(newController);
		
		checkOutwardDirection();
		checkForConnections(this.worldObj, xCoord, yCoord, zCoord);
	}
	
	@Override
	public void onMachineAssembled() {
		super.onMachineAssembled();

		if(this.worldObj.isRemote) { return; } 
		
		checkOutwardDirection();
		checkForConnections(this.worldObj, xCoord, yCoord, zCoord);
	}

	// Custom PowerTap methods
	/**
	 * Discover which direction is normal to the multiblock face.
	 */
	protected void checkOutwardDirection() {
		MultiblockControllerBase controller = this.getMultiblockController();
		CoordTriplet minCoord = controller.getMinimumCoord();
		CoordTriplet maxCoord = controller.getMaximumCoord();
		
		if(this.xCoord == minCoord.x) {
			out = ForgeDirection.WEST;
		}
		else if(this.xCoord == maxCoord.x){
			out = ForgeDirection.EAST;
		}
		else if(this.zCoord == minCoord.z) {
			out = ForgeDirection.NORTH;
		}
		else if(this.zCoord == maxCoord.z) {
			out = ForgeDirection.SOUTH;
		}
		else if(this.yCoord == minCoord.y) {
			// Just in case I end up making omnidirectional taps.
			out = ForgeDirection.DOWN;
		}
		else if(this.yCoord == maxCoord.y){
			// Just in case I end up making omnidirectional taps.
			out = ForgeDirection.UP;
		}
		else {
			// WTF BRO
			out = ForgeDirection.UNKNOWN;
		}
	}
	
	/**
	 * Check for a world connection, if we're assembled.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void checkForConnections(World world, int x, int y, int z) {
		boolean wasConnected = isConnected;
		if(out == ForgeDirection.UNKNOWN) {
			wasConnected = false;
			isConnected = false;
		}
		else {
			// See if our adjacent non-reactor coordinate has a TE
			TileEntity te = world.getBlockTileEntity(x + out.offsetX, y + out.offsetY, z + out.offsetZ);
			if(te == null || te instanceof TileEntityReactorPowerTap) {
				isConnected = false;
			}
			else if(te instanceof IPowerReceptor) {
				isConnected = true;
			}
			else if(te instanceof IConnector) {
				IConnector connector = (IConnector)te;
				if(connector.canConnect(out.getOpposite())) {
					isConnected = true;
				}
				else {
					isConnected = false;
				}
			}
		}
		
		if(wasConnected != isConnected) {
			if(isConnected) {
				// Newly connected
				world.setBlockMetadataWithNotify(x, y, z, BlockReactorPart.POWERTAP_METADATA_BASE+1, 2);
			}
			else {
				// No longer connected
				world.setBlockMetadataWithNotify(x, y, z, BlockReactorPart.POWERTAP_METADATA_BASE, 2);
			}
		}
	}

	/** This will be called by the Reactor Controller when this tap should be providing power.
	 * @return Power units remaining after consumption.
	 */
	public int onProvidePower(int units) {
		ArrayList<CoordTriplet> deadCoords = null;
		
		if(!isConnected) {
			return units;
		}
		
		TileEntity te = this.worldObj.getBlockTileEntity(xCoord + out.offsetX, yCoord + out.offsetY, zCoord + out.offsetZ);
		if(te instanceof IPowerReceptor) {
			// Buildcraft
			int mjAvailable = (int)Math.floor((float)units / bcPowerFactor);
			
			IPowerReceptor ipr = (IPowerReceptor)te;
			IPowerProvider ipp = ipr.getPowerProvider();
			ForgeDirection approachDirection = out.getOpposite();
			
			if(ipp != null && ipp.preConditions(ipr) && ipp.getMinEnergyReceived() <= mjAvailable) {
				int energyUsed = Math.min(Math.min(ipp.getMaxEnergyReceived(), mjAvailable), ipp.getMaxEnergyStored() - (int)Math.floor(ipp.getEnergyStored()));
				ipp.receiveEnergy(energyUsed, approachDirection);
				
				units -= (int)((float)energyUsed * bcPowerFactor);
			}
		}
		else if(te instanceof IConnector) { 
			// Universal Electricity
			ForgeDirection approachDirection = out.getOpposite();
			IElectricityNetwork network = ElectricityNetworkHelper.getNetworkFromTileEntity(te, approachDirection);
			if(network != null) {
				double wattsAvailable = (double)units / (double)uePowerFactor;
				double wattsWanted = network.getRequest().getWatts();
				if(wattsWanted > 0) {
					if(wattsWanted > wattsAvailable) {
						wattsWanted = wattsAvailable;
					}
					
					network.startProducing(this, wattsWanted/getVoltage(), getVoltage());
					 // Rounding up to prevent free energy. Sorry bro, thermodynamics says so.
					units -= (int)Math.ceil(wattsWanted * uePowerFactor);
				} else {
					network.stopProducing(this);
				}
			}
		}
		else {
			// Handle burnt-out connections that didn't trigger a neighbor update (UE, mostly)
			isConnected = false;
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, BlockReactorPart.POWERTAP_METADATA_BASE, 2);
		}
		
		return units;
	}
	
	@Override
	public void invalidate()
	{
		ElectricityNetworkHelper.invalidate(this); // Universal Electricity
		super.invalidate();
	}
	
	// Universal Electricity
	
	@Override
	public boolean canConnect(ForgeDirection direction) {
		return direction == out;
	}

	@Override
	public double getVoltage() {
		// TODO: Make this selectable?
		return 120;
	}

	// Buildcraft methods
	
	@Override
	public void setPowerProvider(IPowerProvider provider) {
		this.powerProvider = provider;
	}

	@Override
	public IPowerProvider getPowerProvider() {
		return this.powerProvider;
	}

	@Override
	public void doWork() { }

	@Override
	public int powerRequest(ForgeDirection from) { return 0; }
}
