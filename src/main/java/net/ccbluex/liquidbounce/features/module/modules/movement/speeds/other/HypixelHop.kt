/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class HypixelHop : SpeedMode("HypixelHop") {
    override fun onMotion() {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()

                var speed = if (MovementUtils.getSpeed() < 0.56f) MovementUtils.getSpeed() * 1.045f else 0.56f
                if (mc.thePlayer.onGround && mc.thePlayer.isPotionActive(Potion.moveSpeed))
                    speed *= 1f + 0.13f * (1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier)

                MovementUtils.strafe(speed)
                return
            } else if (mc.thePlayer.motionY < 0.2) {
                mc.thePlayer.motionY -= 0.02
            }

            MovementUtils.strafe(MovementUtils.getSpeed() * 1.01889f)
        } else {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
        }
    }
}