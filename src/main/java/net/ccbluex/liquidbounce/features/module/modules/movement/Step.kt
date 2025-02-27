/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.stats.StatList
import net.minecraft.util.MathHelper
import kotlin.math.cos
import kotlin.math.sin

@ModuleInfo(name = "Step", category = ModuleCategory.MOVEMENT)
class Step : Module() {

    /**
     * OPTIONS
     */

    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Jump", "Matrix6.7.0", "NCP", "NCPNew", "MotionNCP", "OldNCP", "OldAAC", "LAAC", "AAC3.3.4", "AAC3.6.4", "AAC4.4.0", "Spartan", "Rewinside"), "NCP")
    private val heightValue = FloatValue("Height", 1F, 0.6F, 10F)
    private val jumpHeightValue = FloatValue("JumpMotion", 0.42F, 0.37F, 0.42F).displayable { modeValue.equals("Jump") || modeValue.equals("TimerJump") }
    private val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val timerValue = FloatValue("Timer", 1F, 0.05F, 1F)
    private val timerDynValue = BoolValue("UseDynamicTimer", false)

    /**
     * VALUES
     */

    private var isStep = false
    private var stepX = 0.0
    private var stepY = 0.0
    private var stepZ = 0.0

    private var ncpNextStep = 0
    private var spartanSwitch = false
    private var isAACStep = false
    var wasTimer = false
    var lastOnGround = false
    var canStep = false

    private val timer = MSTimer()

    override fun onDisable() {
        mc.thePlayer ?: return

        // Change step height back to default (0.5 is default)
        mc.thePlayer.stepHeight = 0.6F
        if (wasTimer) mc.timer.timerSpeed = 1.0F
        wasTimer = false
        lastOnGround = mc.thePlayer.onGround
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround && lastOnGround) {
            canStep = true
            if(modeValue.equals("AAC4.4.0") || modeValue.equals("NCPNew") || modeValue.equals("Matrix6.7.0")) {
                mc.thePlayer.stepHeight = heightValue.get()
            }
        }else {
            canStep = false
            mc.thePlayer.stepHeight = 0.6F
        }
        
        lastOnGround = mc.thePlayer.onGround
        
        if (wasTimer) {
            wasTimer = false
            if(modeValue.equals("AAC4.4.0")) {
                mc.thePlayer.motionX *= 0.913
                mc.thePlayer.motionZ *= 0.913
            }
            mc.timer.timerSpeed = 1.0F
        }
        val mode = modeValue.get()
        

        // Motion steps
        when {
            mode.equals("jump", true) && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround
                    && !mc.gameSettings.keyBindJump.isKeyDown -> {
                fakeJump()
                mc.thePlayer.motionY = jumpHeightValue.get().toDouble()
            }

            mode.equals("timerjump", true) -> {
                mc.timer.timerSpeed = 1f
                if (mc.thePlayer.isCollidedHorizontally) {
                    if (mc.thePlayer.onGround) {
                        fakeJump()
                        mc.thePlayer.motionY = jumpHeightValue.get().toDouble()
                        isStep = true
                    } else if (isStep) {
                        mc.timer.timerSpeed = if (mc.thePlayer.motionY> 0) {
                            (1 - (mc.thePlayer.motionY / 1.8)).toFloat()
                        } else {
                            1.25f
                        }
                    }
                } else {
                    isStep = false
                }
            }

            mode.equals("laac", true) -> if (mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder &&
                !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb) {
                if (mc.thePlayer.onGround && timer.hasTimePassed(delayValue.get().toLong())) {
                    isStep = true

                    fakeJump()
                    mc.thePlayer.motionY += 0.620000001490116

                    val f = mc.thePlayer.rotationYaw * 0.017453292F
                    mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2
                    mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2
                    timer.reset()
                }

                mc.thePlayer.onGround = true
            } else {
                isStep = false
            }

            mode.equals("aac3.6.4", true) -> if (mc.thePlayer.isCollidedHorizontally &&
                MovementUtils.isMoving()) {
                if (mc.thePlayer.onGround && couldStep()) {
                    mc.thePlayer.motionX *= 1.12
                    mc.thePlayer.motionZ *= 1.12
                    mc.thePlayer.jump()
                    isAACStep = true
                }

                if (isAACStep) {
                    mc.thePlayer.motionY -= 0.015

                    if (!mc.thePlayer.isUsingItem && mc.thePlayer.movementInput.moveStrafe == 0F) {
                        mc.thePlayer.jumpMovementFactor = 0.3F
                    }
                }
            } else {
                isAACStep = false
            }

            mode.equals("aac3.3.4", true) -> if (mc.thePlayer.isCollidedHorizontally &&
                MovementUtils.isMoving()) {
                if (mc.thePlayer.onGround && couldStep()) {
                    mc.thePlayer.motionX *= 1.26
                    mc.thePlayer.motionZ *= 1.26
                    mc.thePlayer.jump()
                    isAACStep = true
                }

                if (isAACStep) {
                    mc.thePlayer.motionY -= 0.015

                    if (!mc.thePlayer.isUsingItem && mc.thePlayer.movementInput.moveStrafe == 0F) {
                        mc.thePlayer.jumpMovementFactor = 0.3F
                    }
                }
            } else {
                isAACStep = false
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val mode = modeValue.get()

        // Motion steps
        when {
            mode.equals("motionncp", true) && mc.thePlayer.isCollidedHorizontally && !mc.gameSettings.keyBindJump.isKeyDown -> {
                when {
                    mc.thePlayer.onGround && couldStep() -> {
                        fakeJump()
                        mc.thePlayer.motionY = 0.0
                        event.y = 0.41999998688698
                        ncpNextStep = 1
                    }

                    ncpNextStep == 1 -> {
                        event.y = 0.7531999805212 - 0.41999998688698
                        ncpNextStep = 2
                    }

                    ncpNextStep == 2 -> {
                        val yaw = MovementUtils.direction

                        event.y = 1.001335979112147 - 0.7531999805212
                        event.x = -sin(yaw) * 0.7
                        event.z = cos(yaw) * 0.7

                        ncpNextStep = 0
                    }
                }
            }
        }
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        mc.thePlayer ?: return
        val mode = modeValue.get()

        if (event.eventState == EventState.PRE) {
            // Phase should disable step
            if (LiquidBounce.moduleManager[Phase::class.java]!!.state) {
                event.stepHeight = 0F
                return
            }
            if (mode.equals("AAC4.4.0", ignoreCase = true) || mode.equals("NCPNew", ignoreCase = true) || modeValue.equals("Matrix6.7.0")) {
                if (event.stepHeight > 0.6F && !canStep) return
                if (event.stepHeight <= 0.6F) return
            }

            // Set step to default in some cases
            if (!mc.thePlayer.onGround || !timer.hasTimePassed(delayValue.get().toLong()) ||
                mode.equals("Jump", ignoreCase = true) || mode.equals("MotionNCP", ignoreCase = true) ||
                mode.equals("LAAC", ignoreCase = true) || mode.equals("AAC3.3.4", ignoreCase = true) ||
                mode.equals("TimerJump", ignoreCase = true)) {
                mc.thePlayer.stepHeight = 0.6F
                event.stepHeight = 0.6F
                return
            }

            // Set step height
            val height = heightValue.get()

            // Detect possible step
            
            mc.thePlayer.stepHeight = height
            event.stepHeight = height
            
            if (event.stepHeight > 0.6F) {
                isStep = true
                stepX = mc.thePlayer.posX
                stepY = mc.thePlayer.posY
                stepZ = mc.thePlayer.posZ
            }
            
        } else {
            if (!isStep) { // Check if step
                return
            }

            if (mc.thePlayer.entityBoundingBox.minY - stepY > 0.6) { // Check if full block step
                if (timerValue.get()<1.0) {
                    wasTimer = true
                    mc.timer.timerSpeed = timerValue.get()
                    if (timerDynValue.get()) {
                        mc.timer.timerSpeed = (mc.timer.timerSpeed / Math.sqrt(mc.thePlayer.entityBoundingBox.minY - stepY)).toFloat()
                    }
                }
                when {
                    mode.equals("NCP", ignoreCase = true) || mode.equals("OldAAC", ignoreCase = true) -> {
                        fakeJump()

                        // Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.41999998688698, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.7531999805212, stepZ, false))
                        timer.reset()
                    }
                    
                    mode.equals("NCPNew", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        when {
                            rstepHeight > 2.019 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.919)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 2.019 && rstepHeight > 1.869 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 1.869 && rstepHeight > 1.5 -> {
                                val stpPacket = arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 1.5 && rstepHeight > 1.015 -> {
                                val stpPacket = arrayOf(0.42, 0.7532, 1.01, 1.093, 1.015)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                            
                            rstepHeight <= 1.015 && rstepHeight > 0.875 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                            
                            rstepHeight <= 0.875 && rstepHeight > 0.6 -> {
                                val stpPacket = arrayOf(0.39, 0.6938)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                        }
                        timer.reset()
                    }
                    
                    mode.equals("Matrix6.7.0", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        when {
                            
                            rstepHeight <= 2.005 && rstepHeight > 1.5 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212, 1.17319996740818, 1.5064, 1.75454, 1.91931)
                                stpPacket.forEach {
                                    if(it >= 0.753 && it <= 0.754) {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, true))
                                    }else {
                                        mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                            stepY + it, stepZ, false))
                                    }
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                                mc.timer.timerSpeed = (mc.timer.timerSpeed / 1.5).toFloat()
                                wasTimer = true
                            }
                            
                            rstepHeight <= 1.5 && rstepHeight > 1.0 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212, 1.001335979112147)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                                mc.thePlayer.motionX = 0.0
                                mc.thePlayer.motionZ = 0.0
                            }
                            
                            rstepHeight <= 1.0 && rstepHeight > 0.6 -> {
                                val stpPacket = arrayOf(0.41999998688698, 0.7531999805212)
                                stpPacket.forEach {
                                    mc.thePlayer.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                        stepY + it, stepZ, false))
                                }
                            }
                        }
                        timer.reset()
                    }

                    mode.equals("Spartan", ignoreCase = true) -> {
                        fakeJump()

                        if (spartanSwitch) {
                            // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.41999998688698, stepZ, false))
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.7531999805212, stepZ, false))
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 1.001335979112147, stepZ, false))
                        } else { // Force step
                            mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                stepY + 0.6, stepZ, false))
                        }

                        // Spartan allows one unlegit step so just swap between legit and unlegit
                        spartanSwitch = !spartanSwitch

                        // Reset timer
                        timer.reset()
                    }

                    mode.equals("AAC4.4.0", ignoreCase = true) -> {
                        val rstepHeight = mc.thePlayer.entityBoundingBox.minY - stepY
                        fakeJump()
                        timer.reset()
                        when {
                            rstepHeight >= 1.0 - 0.015625 && rstepHeight < 1.5 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.4, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.7, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.9, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.0, stepZ, true))
                            }
                            rstepHeight >= 1.5 - 0.015625 && rstepHeight < 2.0 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.42, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.7718, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.0556, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.2714, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.412, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.50, stepZ, true))
                            }
                            rstepHeight >= 2.0 - 0.015625 -> {
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.45, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 0.84375, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.18125, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.4625, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.6875, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.85625, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                                    stepY + 1.96875, stepZ, false))
                                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX + mc.thePlayer.motionX * 0.5,
                                    stepY + 2.0000, stepZ + mc.thePlayer.motionZ * 0.5, true))
                            }
                        }
                        /*

                            AAC4 Step Code By Co丶Dynamic
                            NO SKIDDER PLEASE

                        */
                    }

                    mode.equals("Rewinside", ignoreCase = true) -> {
                        fakeJump()

                        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.41999998688698, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 0.7531999805212, stepZ, false))
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(stepX,
                            stepY + 1.001335979112147, stepZ, false))

                        // Reset timer
                        timer.reset()
                    }
                }
            }

            isStep = false
            stepX = 0.0
            stepY = 0.0
            stepZ = 0.0
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && isStep && modeValue.equals("OldNCP")) {
            packet.y += 0.07
            isStep = false
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    private fun fakeJump() {
        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    private fun couldStep(): Boolean {
        val yaw = MovementUtils.direction
        val x = -sin(yaw) * 0.32
        val z = cos(yaw) * 0.32

        return mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(x, 1.001335979112147, z))
            .isEmpty()
    }

    override val tag: String
        get() = modeValue.get()
}
