package ricket.bedtimeban.proxy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import ricket.bedtimeban.*;
import ricket.bedtimeban.commands.BedtimeCommand;
import ricket.bedtimeban.commands.CancelBanCommand;
import ricket.bedtimeban.commands.SetTimezoneCommand;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerProxy implements IProxy {

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();

    public static final ScheduledExecutorService banExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("BedtimeBanHammer").build());

    private final TimeParser timeParser = new TimeParser();
    private final BanScheduler banScheduler = new BanScheduler();
    private BanHammer banHammer;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // register network handlers, etc
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        MinecraftServerBanUtils banUtils = new MinecraftServerBanUtils(server);
        banHammer = new BanHammer(banExecutor, server, banUtils, banScheduler);

        event.registerServerCommand(new BedtimeCommand(timeParser, banScheduler));
        event.registerServerCommand(new CancelBanCommand(banUtils, banScheduler));
        event.registerServerCommand(new SetTimezoneCommand(banScheduler));
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        banHammer.start();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLoggedIn(PlayerLoggedInEvent event) {
        String reminder = banScheduler.makeBanReminderString(event.player.getUniqueID());
        if (reminder != null) {
            event.player.sendMessage(new TextComponentString(reminder));
        }
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerLoggedOutEvent event) {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void entityStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.isCanceled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity == null) {
            BedtimeBanMod.logger.warn("EntityStruckByLightningEvent with no entity");
            return;
        }
        BedtimeBanMod.logger.info("Entity struck by lightning: " + entity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void entityDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) {
            BedtimeBanMod.logger.warn("LivingDeathEvent with no entity");
            return;
        }
        DamageSource source = event.getSource();
        BedtimeBanMod.logger.info("Entity died: " + entity + " ; message: " + damageSourceToString(source, entity));
    }

    private String damageSourceToString(DamageSource damageSource, EntityLivingBase entity) {
        if (damageSource == null) {
            return "null";
        }
        ITextComponent deathMessage = damageSource.getDeathMessage(entity);
        return deathMessage.getUnformattedText();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void logForcedDespawns(LivingSpawnEvent parentEvent) {
        if (!(parentEvent instanceof AllowDespawn) || parentEvent.isCanceled()) {
            return;
        }
        AllowDespawn event = (AllowDespawn) parentEvent;
        Result result = event.getResult();
        if (result == Result.ALLOW) {
            // ALLOW will force the mob to despawn
            EntityLivingBase entity = event.getEntityLiving();
            if (entity == null) {
                BedtimeBanMod.logger.warn("AllowDespawn event with no entity");
                return;
            }
            BedtimeBanMod.logger.info("Entity being forced to despawn: " + entity);
        }
    }
}
