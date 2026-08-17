package org.macekill.addon.modules;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.macekill.addon.Addon;

/**
 * Mace Killer Module
 *
 * @author kybe236
 */
public class MaceKiller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("The maximum height to teleport to.")
        .defaultValue(170)
        .min(1)
        .max(170)
        .sliderMax(170)
        .build()
    );

    private final Setting<Boolean> maximum = sgGeneral.add(new BoolSetting.Builder()
        .name("maximum")
        .description("Automatically uses the maximum safe height.")
        .defaultValue(true)
        .build()
    );

    public MaceKiller() {
        super(Addon.CATEGORY, "mace-killer", "Makes more damage with the mace.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null) return;

        try {
            if (mc.player.getMainHandStack().getItem() != Items.MACE) return;

            Entity targetEntity = event.entity;
            if (event.isCancelled() || targetEntity.isInvulnerable()) return;

            double prevX = mc.player.getX();
            double prevY = mc.player.getY();
            double prevZ = mc.player.getZ();

            int blocks = getMaxHeightAbovePlayer();
            if (blocks == 0) {
                info("No suitable position found");
                return;
            }

            int packetsRequired = blocks / 10;
            if (packetsRequired > 20) {
                packetsRequired = 1;
            }

            if (mc.player.getVehicle() != null) {
                Entity vehicle = mc.player.getVehicle();

                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.getNetworkHandler().sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle));
                }

                vehicle.setPosition(vehicle.getX(), vehicle.getY() + blocks, vehicle.getZ());
                mc.getNetworkHandler().sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle));
            } else {
                for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
                }

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ(), false, mc.player.horizontalCollision));
            }

            if (mc.player.getVehicle() != null) {
                Entity vehicle = mc.player.getVehicle();

                vehicle.setPosition(prevX, prevY, prevZ);
                mc.getNetworkHandler().sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle));

                // Do it again to be sure it happens
                vehicle.setPosition(prevX, prevY, prevZ);
                mc.getNetworkHandler().sendPacket(VehicleMoveC2SPacket.fromVehicle(vehicle));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, prevY, prevZ, false, mc.player.horizontalCollision));

                // Do it again to be sure it happens
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, prevY, prevZ, false, mc.player.horizontalCollision));

                // and again because it failed sometimes
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, prevY, prevZ, false, mc.player.horizontalCollision));
            }
        } catch (Exception e) {
            error("Error: " + e.getMessage());
        }
    }

    private int getMaxHeightAbovePlayer() {
        if (mc.player == null) {
            return 0;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int maxHeight = maximum.get() ? 170 : height.get();
        maxHeight += playerPos.getY();

        for (int i = maxHeight; i > playerPos.getY(); i--) {
            BlockPos isopenair1 = new BlockPos(playerPos.getX(), i, playerPos.getZ());
            BlockPos isopenair2 = isopenair1.up();
            if (isSafeBlock(isopenair1) && isSafeBlock(isopenair2)) {
                return i - playerPos.getY();
            }
        }

        return 0;
    }

    private boolean isSafeBlock(BlockPos pos) {
        if (mc.world == null) {
            return false;
        }

        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.AIR || block == Blocks.WATER || block == Blocks.POWDER_SNOW;
    }
}
