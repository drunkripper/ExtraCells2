package extracells.integration.igw

import java.awt.Desktop
import java.io.File
import java.net.{URL, URLConnection}
import java.util.List

import net.minecraft.client.Minecraft
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.{ITextComponent, TextComponentString, TextFormatting}
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.{FMLCommonHandler, Loader, ModContainer}
import net.minecraftforge.fml.relauncher.Side
import org.apache.commons.io.FileUtils

/**
  * This class is meant to be copied to your own mod which implements IGW-Mod. When properly implemented by instantiating a new instance somewhere in your mod
  * loading stage, this will notify the player when it doesn't have IGW in the instance. It also needs to have the config option enabled to
  * notify the player. This config option will be generated in its own config file.
  *
  * @author MineMaarten https://github.com/MineMaarten/IGW-mod
  */
object IGWSupportNotifier {
  private val LATEST_DL_URL: String = "http://minecraft.curseforge.com/mc-mods/223815-in-game-wiki-mod/files/latest"

  private var supportingMod: String = null

  /**
    * Needs to be instantiated somewhere in your mod's loading stage.
    */

  if (FMLCommonHandler.instance.getSide == Side.CLIENT && !Loader.isModLoaded("IGWMod")) {
    val dir: File = new File(".", "config")
    val config: Configuration = new Configuration(new File(dir, "IGWMod.cfg"))
    config.load
    if (config.get(Configuration.CATEGORY_GENERAL, "enable_missing_notification", true, "When enabled, this will notify players when IGW-Mod is not installed even though mods add support.").getBoolean) {
      val mc: ModContainer = Loader.instance.activeModContainer
      val modid: String = mc.getModId
      val loadedMods: List[ModContainer] = Loader.instance.getActiveModList
      import scala.collection.JavaConversions._
      for (container <- loadedMods) {
        if (container.getModId == modid) {
          supportingMod = container.getName
          FMLCommonHandler.instance.bus.register(this)
          ClientCommandHandler.instance.registerCommand(new CommandDownloadIGW)
        }
      }
    }
    config.save
  }


  @SubscribeEvent
  def onPlayerJoin(event: TickEvent.PlayerTickEvent) {
    if (event.player.worldObj.isRemote && event.player == FMLClientHandler.instance.getClientPlayerEntity) {
      event.player.addChatComponentMessage(ITextComponent.Serializer.jsonToComponent("[\"" + TextFormatting.GOLD + "The mod " + supportingMod + " is supporting In-Game Wiki mod. " + TextFormatting.GOLD + "However, In-Game Wiki isn't installed! " + "[\"," + "{\"text\":\"Download Latest\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/igwmod_download\"}}," + "\"]\"]"))
      FMLCommonHandler.instance.bus.unregister(this)
    }
  }

  private class CommandDownloadIGW extends CommandBase {
    override def getRequiredPermissionLevel: Int = {
      return -100
    }

    def getCommandName: String = {
      return "igwmod_download"
    }

    def getCommandUsage(p_71518_1_ : ICommandSender): String = {
      return getCommandName
    }

    def processCommand(p_71515_1_ : ICommandSender, p_71515_2_ : Array[String]) {
      new ThreadDownloadIGW
    }

    override def execute(server: MinecraftServer, sender: ICommandSender, args: Array[String]): Unit = ???
  }

  private class ThreadDownloadIGW extends Thread {

    setName("IGW-Mod Download Thread")
    start


    override def run {
      try {
        if (Minecraft.getMinecraft.thePlayer != null) Minecraft.getMinecraft.thePlayer.addChatMessage(new TextComponentString("Downloading IGW-Mod..."))
        val url: URL = new URL(IGWSupportNotifier.LATEST_DL_URL)
        val connection: URLConnection = url.openConnection
        connection.connect
        val fileName: String = "IGW-Mod.jar"
        val dir: File = new File(".", "mods")
        val f: File = new File(dir, fileName)
        FileUtils.copyURLToFile(url, f)
        if (Minecraft.getMinecraft.thePlayer != null) Minecraft.getMinecraft.thePlayer.addChatMessage(new TextComponentString(TextFormatting.GREEN + "Successfully downloaded. Restart Minecraft to apply."))
        Desktop.getDesktop.open(dir)
        finalize
      }
      catch {
        case e: Throwable => {
          e.printStackTrace
          if (Minecraft.getMinecraft.thePlayer != null) Minecraft.getMinecraft.thePlayer.addChatComponentMessage(new TextComponentString(TextFormatting.RED + "Failed to download"))
          try {
            finalize
          }
          catch {
            case e1: Throwable => {
              e1.printStackTrace
            }
          }
        }
      }
    }
  }

}
