package extracells.part.gas

import java.util

import appeng.api.config.Actionable
import appeng.api.networking.security.MachineSource
import appeng.api.storage.IMEMonitor
import appeng.api.storage.data.IAEFluidStack
import extracells.integration.Integration
import extracells.integration.mekanism.gas.MekanismGas
import extracells.part.fluid.PartFluidImport
import extracells.util.{AEUtils, GasUtil}
import mekanism.api.gas.{Gas, GasStack, IGasHandler}
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.{Fluid, FluidStack}
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.fml.common.Optional.{Interface, Method}


@Interface(iface = "mekanism.api.gas.IGasHandler", modid = "MekanismAPI|gas", striprefs = true)
class PartGasImport extends PartFluidImport with IGasHandler {

  private val isMekanismEnabled = Integration.Mods.MEKANISMGAS.isEnabled

  override def doWork(rate: Int, TicksSinceLastCall: Int): Boolean = {
    if ((!isMekanismEnabled) || getFacingGasTank == null || !isActive) return false
    var empty: Boolean = true
    val filter: util.List[Fluid] = new util.ArrayList[Fluid]
    filter.add(this.filterFluids(4))
    if (this.filterSize >= 1) {
      {
        var i: Byte = 1
        while (i < 9) {
          {
            if (i != 4) {
              filter.add(this.filterFluids(i))
            }
          }
          i = (i + 2).toByte
        }
      }
    }
    if (this.filterSize >= 2) {
      {
        var i: Byte = 0
        while (i < 9) {
          {
            if (i != 4) {
              filter.add(this.filterFluids(i))
            }
          }
          i = (i + 2).toByte
        }
      }
    }
    import scala.collection.JavaConversions._
    for (fluid <- filter) {
      if (fluid != null) {
        empty = false
        if (fillToNetwork(fluid, rate * TicksSinceLastCall)) {
          return true
        }
      }
    }
    empty && fillToNetwork(null, rate * TicksSinceLastCall)
  }

  @Optional.Method(modid = "MekanismAPI|gas")
  override protected def fillToNetwork(fluid: Fluid, toDrain: Int): Boolean = {
    var drained: GasStack = null
    val facingTank: IGasHandler = getFacingGasTank
    val side: EnumFacing = getFacing
    val gasType = {
      if (fluid == null)
        null
      else {
        val gasStack = GasUtil.getGasStack(new FluidStack(fluid, toDrain))
        if (gasStack == null)
          null
        else
          gasStack.getGas
      }
    }
    if (gasType == null) {
      drained = facingTank.drawGas(side.getOpposite, toDrain, false)
    }
    else if (facingTank.canDrawGas(side.getOpposite, gasType)) {
      drained = facingTank.drawGas(side.getOpposite, toDrain, false)
    }
    if (drained == null || drained.amount <= 0 || drained.getGas == null) return false
    val toFill: IAEFluidStack = AEUtils.createFluidStack(GasUtil.getFluidStack(drained))
    val notInjected: IAEFluidStack = injectGas(toFill, Actionable.MODULATE)
    if (notInjected != null) {
      val amount: Int = (toFill.getStackSize - notInjected.getStackSize).toInt
      if (amount > 0) {
        facingTank.drawGas(side.getOpposite, amount, true)

        true
      }
      else {
        false
      }
    }
    else {
      facingTank.drawGas(side.getOpposite, toFill.getFluidStack.amount, true)
      true
    }
  }

  @Method(modid = "MekanismAPI|gas")
  override def receiveGas(side: EnumFacing, stack: GasStack, doTransfer: Boolean): Int = {
    if (stack == null || stack.amount <= 0 || !canReceiveGas(side, stack.getGas))
      return 0
    val amount = Math.min(stack.amount, 125 + this.speedState * 125)
    val gasStack = GasUtil.createAEFluidStack(stack.getGas, amount)
    val notInjected = {
      if (getGridBlock == null) {
        gasStack
      } else {
        val monitor: IMEMonitor[IAEFluidStack] = getGridBlock.getFluidMonitor
        if (monitor == null)
          gasStack
        else
          monitor.injectItems(gasStack, if (true) Actionable.MODULATE else Actionable.SIMULATE, new MachineSource(this))
      }
    }
    if (notInjected == null)
      amount
    else
      amount - notInjected.getStackSize.toInt
  }

  @Method(modid = "MekanismAPI|gas")
  override def drawGas(side: EnumFacing, amount: Int, doTransfer: Boolean): GasStack = null

  @Method(modid = "MekanismAPI|gas")
  override def canDrawGas(side: EnumFacing, gasType: Gas): Boolean = false

  @Method(modid = "MekanismAPI|gas")
  override def canReceiveGas(side: EnumFacing, gasType: Gas): Boolean = {
    val fluid = MekanismGas.getFluidGasMap.get(gasType)
    var isEmpty = true
    for (filter <- filterFluids) {
      if (filter != null) {
        isEmpty = false
        if (filter == fluid)
          return true
      }
    }
    isEmpty
  }
}
