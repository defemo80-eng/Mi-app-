package com.example.ui.avatar3d

import android.graphics.Color as AndroidColor
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.domain.catalog.StoreCatalog
import com.example.domain.model.AvatarParams
import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class Avatar3DRenderer : GLSurfaceView.Renderer {

    @Volatile
    var avatarParams: AvatarParams = AvatarParams()

    @Volatile
    var equippedItems: Map<StoreCategory, StoreItem> = emptyMap()

    @Volatile
    var targetYawDegrees: Float = 18f

    @Volatile
    var targetPitchDegrees: Float = 6f

    @Volatile
    var autoRotate: Boolean = false

    private var smoothYaw = 18f
    private var smoothPitch = 6f

    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpMatrixHandle = 0
    private var modelMatrixHandle = 0
    private var colorHandle = 0
    private var keyLightHandle = 0
    private var cameraPosHandle = 0
    private var specStrengthHandle = 0
    private var shininessHandle = 0
    private var alphaHandle = 0
    private var fabricWeaveHandle = 0

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Pre-built sculpted meshes
    private lateinit var sphereMesh: MeshBuffers
    private lateinit var headMesh: MeshBuffers
    private lateinit var neckMesh: MeshBuffers
    private lateinit var torsoBodyMesh: MeshBuffers
    private lateinit var torsoShirtMesh: MeshBuffers
    private lateinit var torsoHoodieMesh: MeshBuffers
    private lateinit var upperArmMesh: MeshBuffers
    private lateinit var upperArmSleeveMesh: MeshBuffers
    private lateinit var forearmMesh: MeshBuffers
    private lateinit var forearmSleeveMesh: MeshBuffers
    private lateinit var handMesh: MeshBuffers
    private lateinit var thighMesh: MeshBuffers
    private lateinit var thighPantsMesh: MeshBuffers
    private lateinit var calfMesh: MeshBuffers
    private lateinit var calfPantsMesh: MeshBuffers
    private lateinit var shoeUpperMesh: MeshBuffers
    private lateinit var shoeSoleMesh: MeshBuffers
    private lateinit var capCrownMesh: MeshBuffers
    private lateinit var capVisorMesh: MeshBuffers
    private lateinit var glassesVisorMesh: MeshBuffers
    private lateinit var backpackMesh: MeshBuffers
    private lateinit var sweptHairMesh: MeshBuffers
    private lateinit var longHairBackMesh: MeshBuffers
    private lateinit var ringMesh: MeshBuffers
    private lateinit var discMesh: MeshBuffers

    private var animPhase = 0f

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.018f, 0.024f, 0.018f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        modelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        keyLightHandle = GLES20.glGetUniformLocation(program, "uKeyLightDir")
        cameraPosHandle = GLES20.glGetUniformLocation(program, "uCameraPos")
        specStrengthHandle = GLES20.glGetUniformLocation(program, "uSpecStrength")
        shininessHandle = GLES20.glGetUniformLocation(program, "uShininess")
        alphaHandle = GLES20.glGetUniformLocation(program, "uAlpha")
        fabricWeaveHandle = GLES20.glGetUniformLocation(program, "uFabricWeave")

        sphereMesh = Avatar3DGeometry.buildSphereMesh(20, 28)
        headMesh = Avatar3DGeometry.buildSculptedHeadMesh()
        neckMesh = Avatar3DGeometry.buildNeckTrapeziusMesh()
        torsoBodyMesh = Avatar3DGeometry.buildSculptedTorsoMesh(shellOffset = 0f, isHoodie = false)
        torsoShirtMesh = Avatar3DGeometry.buildSculptedTorsoMesh(shellOffset = 0.006f, isHoodie = false)
        torsoHoodieMesh = Avatar3DGeometry.buildSculptedTorsoMesh(shellOffset = 0.007f, isHoodie = true)
        upperArmMesh = Avatar3DGeometry.buildUpperArmMesh(0f)
        upperArmSleeveMesh = Avatar3DGeometry.buildUpperArmMesh(0.005f)
        forearmMesh = Avatar3DGeometry.buildForearmMesh(0f)
        forearmSleeveMesh = Avatar3DGeometry.buildForearmMesh(0.005f)
        handMesh = Avatar3DGeometry.buildHandMesh()
        thighMesh = Avatar3DGeometry.buildThighMesh(0f)
        thighPantsMesh = Avatar3DGeometry.buildThighMesh(0.006f)
        calfMesh = Avatar3DGeometry.buildCalfMesh(0f)
        calfPantsMesh = Avatar3DGeometry.buildCalfMesh(0.005f)
        shoeUpperMesh = Avatar3DGeometry.buildShoeUpperMesh()
        shoeSoleMesh = Avatar3DGeometry.buildShoeSoleMesh()
        capCrownMesh = Avatar3DGeometry.buildCapCrownMesh()
        capVisorMesh = Avatar3DGeometry.buildCapVisorMesh()
        glassesVisorMesh = Avatar3DGeometry.buildCurvedVisorMesh()
        backpackMesh = Avatar3DGeometry.buildBackpackMesh()
        sweptHairMesh = Avatar3DGeometry.buildSweptHairMesh()
        longHairBackMesh = Avatar3DGeometry.buildLongHairBackMesh()
        ringMesh = Avatar3DGeometry.buildRingMesh(56, 0.94f)
        discMesh = Avatar3DGeometry.buildDiscMesh(48)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 34f, ratio, 0.4f, 25f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        if (autoRotate) {
            targetYawDegrees = (targetYawDegrees + 0.45f) % 360f
        }
        // Smooth shortest-path angular damping for fluid rotation
        var diffYaw = (targetYawDegrees - smoothYaw) % 360f
        if (diffYaw > 180f) diffYaw -= 360f
        if (diffYaw < -180f) diffYaw += 360f
        smoothYaw = (smoothYaw + diffYaw * 0.22f) % 360f
        smoothPitch += (targetPitchDegrees.coerceIn(-12f, 26f) - smoothPitch) * 0.22f

        animPhase = (animPhase + 0.032f) % (2f * Math.PI.toFloat())
        val breath = sin(animPhase)
        val sway = sin(animPhase * 0.5f)
        val chestBreath = 1f + 0.015f * breath

        val camDist = 3.55f
        val pitchRad = Math.toRadians(smoothPitch.toDouble())
        val camY = (1.02f + sin(pitchRad) * 1.05f).toFloat()
        val camZ = (cos(pitchRad) * camDist).toFloat()

        Matrix.setLookAtM(viewMatrix, 0, 0f, camY, camZ, 0f, 0.92f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(keyLightHandle, 0.52f, 0.85f, 0.78f)
        GLES20.glUniform3f(cameraPosHandle, 0f, camY, camZ)

        val params = avatarParams
        val skinRgb = parseHexRgb(params.skinToneHex, floatArrayOf(0.84f, 0.62f, 0.48f))
        val lipRgb = floatArrayOf(skinRgb[0] * 0.78f, skinRgb[1] * 0.62f, skinRgb[2] * 0.60f)
        val hairRgb = parseHexRgb(params.hairColorHex, floatArrayOf(0.11f, 0.10f, 0.10f))

        val fullSuit = equippedItems[StoreCategory.CONJUNTOS_DEPORTIVOS]
        val hoodie = equippedItems[StoreCategory.SUDADERAS]
        val shirt = equippedItems[StoreCategory.CAMISETAS]
        val pants = equippedItems[StoreCategory.PANTALONES]
        val sneakers = equippedItems[StoreCategory.TENIS_DEPORTIVOS] ?: equippedItems[StoreCategory.ZAPATOS]
        val cap = equippedItems[StoreCategory.GORRAS]
        val glasses = equippedItems[StoreCategory.GAFAS]
        val watch = equippedItems[StoreCategory.RELOJES]
        val acc = equippedItems[StoreCategory.ACCESORIOS]
        val backpack = equippedItems[StoreCategory.MOCHILAS]

        val topItem = fullSuit ?: hoodie ?: shirt
        val topRgb = topItem?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.10f, 0.12f, 0.10f)) }
            ?: floatArrayOf(0.12f, 0.14f, 0.12f)
        val topAccentRgb = topItem?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)

        val pantsItem = fullSuit ?: pants
        val pantsRgb = pantsItem?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.10f, 0.12f, 0.14f)) }
            ?: floatArrayOf(0.11f, 0.13f, 0.15f)
        val pantsAccentRgb = pantsItem?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)
        val isShorts = pants?.styleVariant == 1 && fullSuit == null

        val shoeRgb = sneakers?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.12f, 0.12f, 0.14f)) }
            ?: floatArrayOf(0.14f, 0.14f, 0.16f)
        val shoeAccentRgb = sneakers?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)

        // Studio Floor & Soft Contact Shadow
        drawStagePart(discMesh, 0f, 0.002f, 0f, 0.56f, 1f, 0.56f, 0f, 0f, 0f, floatArrayOf(0.05f, 0.07f, 0.05f), 0.1f, 12f, 0.95f, 0f)
        drawStagePart(ringMesh, 0f, 0.006f, 0f, 0.56f, 1f, 0.56f, 0f, 0f, 0f, floatArrayOf(0.22f, 1.0f, 0.08f), 0.7f, 32f, 0.85f, 0f)
        drawStagePart(ringMesh, 0f, 0.005f, 0f, 0.42f, 1f, 0.42f, 0f, 0f, 0f, floatArrayOf(0.22f, 1.0f, 0.08f), 0.5f, 24f, 0.35f, 0f)
        // Soft ground shadow directly beneath the feet
        drawStagePart(discMesh, 0f, 0.004f, 0.01f, 0.25f, 1f, 0.19f, 0f, 0f, 0f, floatArrayOf(0.0f, 0.0f, 0.0f), 0.0f, 4f, 0.55f, 0f)

        val shoulderScale = params.shoulderWidthRatio.coerceIn(0.84f, 1.22f)
        val waistScale = params.waistRatio.coerceIn(0.82f, 1.24f)
        val hipScale = params.hipWidthRatio.coerceIn(0.86f, 1.18f)
        val limbScale = params.limbThicknessRatio.coerceIn(0.85f, 1.22f)
        val heightScale = (params.heightCm / 172f).coerceIn(0.88f, 1.12f)
        val postureLean = (1f - params.postureAlignment) * 12f

        val torsoWidthScale = (shoulderScale * 0.6f + waistScale * 0.4f)
        val hipX = 0.095f * hipScale

        fun drawHumanPart(
            mesh: MeshBuffers,
            lx: Float, ly: Float, lz: Float,
            sx: Float, sy: Float, sz: Float,
            rx: Float, ry: Float, rz: Float,
            rgb: FloatArray,
            specStrength: Float = 0.22f,
            shininess: Float = 20f,
            fabricWeave: Float = 0f,
            applyPostureLean: Boolean = true
        ) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.rotateM(modelMatrix, 0, smoothYaw, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 1f, heightScale, 1f)
            if (applyPostureLean && ly > 0.88f) {
                Matrix.translateM(modelMatrix, 0, 0f, 0.88f, 0f)
                Matrix.rotateM(modelMatrix, 0, postureLean, 1f, 0f, 0f)
                Matrix.translateM(modelMatrix, 0, 0f, -0.88f, 0f)
            }
            Matrix.translateM(modelMatrix, 0, lx, ly, lz)
            if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
            if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
            if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
            drawCurrent(mesh, rgb, specStrength, shininess, 1.0f, fabricWeave)
        }

        // 1. Sculpted Legs (Thigh -> Knee -> Calf -> Modeled Shoe)
        for (side in listOf(-1f, 1f)) {
            val sx = side * hipX
            val thighMeshToUse = thighPantsMesh
            // Thigh
            drawHumanPart(thighMeshToUse, sx, 0.675f, 0f, limbScale, 1.02f, limbScale, 0f, 0f, side * -1.8f, pantsRgb, 0.14f, 12f, 1f, applyPostureLean = false)
            // Knee joint
            val kneeColor = if (isShorts) skinRgb else pantsRgb
            val kneeSpec = if (isShorts) 0.25f else 0.14f
            drawHumanPart(sphereMesh, sx * 0.97f, 0.475f, 0.008f, 0.052f * limbScale, 0.052f, 0.054f * limbScale, 0f, 0f, 0f, kneeColor, kneeSpec, 18f, if (isShorts) 0f else 1f, applyPostureLean = false)
            // Calf
            val calfColor = if (isShorts) skinRgb else pantsRgb
            val calfMeshToUse = if (isShorts) calfMesh else calfPantsMesh
            drawHumanPart(calfMeshToUse, sx * 0.96f, 0.28f, -0.004f, limbScale, 1.04f, limbScale, 0f, 0f, 0f, calfColor, if (isShorts) 0.25f else 0.14f, 16f, if (isShorts) 0f else 1f, applyPostureLean = false)
            // Side stripe on joggers
            if (!isShorts) {
                drawHumanPart(thighMesh, sx + side * 0.052f * limbScale, 0.675f, 0f, 0.12f, 0.95f, 0.15f, 0f, 0f, side * -1.8f, pantsAccentRgb, 0.3f, 24f, 0f, applyPostureLean = false)
            }
            // Modeled Shoe Sole & Contoured Sneaker Upper
            val footYaw = side * 4f
            drawHumanPart(shoeSoleMesh, sx * 0.95f, 0.025f, 0.0f, 1.0f, 1.0f, 1.0f, 0f, footYaw, 0f, shoeAccentRgb, 0.35f, 24f, 0f, applyPostureLean = false)
            drawHumanPart(shoeUpperMesh, sx * 0.95f, 0.036f, 0.0f, 1.0f, 1.0f, 1.0f, 0f, footYaw, 0f, shoeRgb, 0.28f, 22f, 0.6f, applyPostureLean = false)
        }

        // 2. Sculpted Pelvis & Torso (Under-body + Fitted Garment Shell)
        val upperShell = when {
            hoodie != null || fullSuit != null -> torsoHoodieMesh
            else -> torsoShirtMesh
        }
        // Waistband/Hip pelvis overlap
        drawHumanPart(torsoBodyMesh, 0f, 0f, 0f, hipScale, 1f, waistScale, 0f, 0f, 0f, pantsRgb, 0.15f, 12f, 1f)
        // Upper torso garment with breathing expansion
        drawHumanPart(upperShell, 0f, 0f, 0f, torsoWidthScale * chestBreath, 1f, waistScale * chestBreath, 0f, 0f, 0f, topRgb, 0.18f, 16f, 1f)
        // Subtle sporty chest emblem
        drawHumanPart(sphereMesh, 0.055f * torsoWidthScale, 1.26f, 0.114f * waistScale * chestBreath, 0.022f, 0.022f, 0.008f, 0f, 0f, 0f, topAccentRgb, 0.55f, 48f, 0f)

        // Hoodie hood draped naturally behind neck
        if (hoodie != null || fullSuit != null) {
            drawHumanPart(capCrownMesh, 0f, 1.38f, -0.05f, 1.18f, 0.95f, 1.05f, -35f, 0f, 0f, topRgb, 0.16f, 14f, 1f)
        }

        // 3. Sculpted Arms, Forearms & Full Hands (with gentle natural idle sway)
        val hasSleeves = topItem?.styleVariant != 2 // Tank top = 2
        val longSleeves = hoodie != null || fullSuit != null
        val shoulderAttachX = 0.188f * torsoWidthScale + 0.025f * limbScale
        val armSwayDeg = sway * 1.4f

        for (side in listOf(-1f, 1f)) {
            val sx = side * shoulderAttachX
            val upperArmTilt = side * (-11f) + side * armSwayDeg
            val upperMesh = if (hasSleeves) upperArmSleeveMesh else upperArmMesh
            val upperCol = if (hasSleeves) topRgb else skinRgb
            val upperWeave = if (hasSleeves) 1f else 0f

            // Deltoid & Upper Arm
            drawHumanPart(upperMesh, sx, 1.235f, -0.005f, limbScale, 1f, limbScale, 2f, 0f, upperArmTilt, upperCol, if (hasSleeves) 0.18f else 0.28f, 18f, upperWeave)

            // Elbow joint
            val elbowX = sx + side * 0.032f
            val elbowY = 1.095f
            val foreCol = if (longSleeves) topRgb else skinRgb
            val foreMesh = if (longSleeves) forearmSleeveMesh else forearmMesh
            drawHumanPart(sphereMesh, elbowX, elbowY, 0.002f, 0.038f * limbScale, 0.038f, 0.038f * limbScale, 0f, 0f, 0f, foreCol, 0.25f, 18f, if (longSleeves) 1f else 0f)

            // Forearm (slightly bent forward naturally)
            val foreX = elbowX + side * 0.015f
            val foreY = 0.975f
            drawHumanPart(foreMesh, foreX, foreY, 0.020f, limbScale, 1f, limbScale, -8f, 0f, side * -5f + side * armSwayDeg * 1.2f, foreCol, if (longSleeves) 0.18f else 0.28f, 18f, if (longSleeves) 1f else 0f)

            // Sculpted Hand (palm + finger taper + thumb)
            val handX = foreX + side * 0.008f
            val handY = 0.815f
            drawHumanPart(handMesh, handX, handY, 0.035f, limbScale, 1f, limbScale, -6f, side * -12f, side * -3f, skinRgb, 0.28f, 20f, 0f)
            // Thumb articulation
            drawHumanPart(sphereMesh, handX - side * 0.014f, handY + 0.010f, 0.056f, 0.011f * limbScale, 0.026f, 0.012f * limbScale, -22f, 0f, side * 18f, skinRgb, 0.28f, 20f, 0f)
        }

        // Smartwatch on left wrist
        if (watch != null) {
            val watchBandRgb = parseHexRgb(watch.primaryColorHex, floatArrayOf(0.08f, 0.08f, 0.08f))
            val watchGlowRgb = parseHexRgb(watch.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            val wx = -shoulderAttachX - 0.050f
            drawHumanPart(sphereMesh, wx, 0.865f, 0.032f, 0.032f * limbScale, 0.016f, 0.036f * limbScale, -8f, 0f, 0f, watchBandRgb, 0.35f, 28f, 0f)
            drawHumanPart(sphereMesh, wx - 0.022f, 0.865f, 0.044f, 0.014f, 0.018f, 0.016f, 0f, -20f, 0f, watchGlowRgb, 0.85f, 64f, 0f)
        }

        // Wristbands
        if (acc != null && acc.styleVariant == 0) {
            val accRgb = parseHexRgb(acc.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawHumanPart(sphereMesh, shoulderAttachX + 0.050f, 0.870f, 0.030f, 0.033f * limbScale, 0.022f, 0.036f * limbScale, -8f, 0f, 0f, accRgb, 0.15f, 10f, 1f)
        }

        // Ergonomic Backpack (sculpted dual-curve shell + chest straps)
        if (backpack != null) {
            val bagRgb = parseHexRgb(backpack.primaryColorHex, floatArrayOf(0.14f, 0.16f, 0.14f))
            val bagAccent = parseHexRgb(backpack.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawHumanPart(backpackMesh, 0f, 1.19f, -0.158f * waistScale, 1f, 1f, 1f, 4f, 0f, 0f, bagRgb, 0.25f, 20f, 0.8f)
            drawHumanPart(sphereMesh, 0f, 1.24f, -0.228f * waistScale, 0.068f, 0.010f, 0.012f, 0f, 0f, 0f, bagAccent, 0.7f, 48f, 0f)
            // Shoulder straps hugging front chest
            for (side in listOf(-1f, 1f)) {
                drawHumanPart(sphereMesh, side * 0.092f * torsoWidthScale, 1.27f, 0.068f, 0.018f, 0.115f, 0.032f, 18f, 0f, side * 6f, bagRgb, 0.2f, 16f, 0.6f)
            }
        }

        // 4. Sculpted Neck + Head + Facial Features + Organic Hair
        val headYaw = sway * 2.2f
        drawHumanPart(neckMesh, 0f, 1.44f, -0.002f, 1f, 1f, 1f, 0f, headYaw * 0.4f, 0f, skinRgb, 0.28f, 20f, 0f)

        val headY = 1.595f
        val headZ = 0.008f
        drawHumanPart(headMesh, 0f, headY, headZ, 1f, 1f, 1f, 0f, headYaw, 0f, skinRgb, 0.28f, 22f, 0f)

        // Nose bridge + subtle nostrils
        drawHumanPart(sphereMesh, 0f, headY - 0.010f, headZ + 0.105f, 0.013f, 0.022f, 0.016f, -12f, headYaw, 0f, skinRgb, 0.3f, 24f, 0f)
        // Ears
        for (side in listOf(-1f, 1f)) {
            drawHumanPart(sphereMesh, side * 0.089f, headY - 0.002f, headZ - 0.005f, 0.014f, 0.026f, 0.018f, 0f, headYaw, side * -8f, skinRgb, 0.25f, 18f, 0f)
        }
        // Eyes (sclera + iris + eyebrows) & subtle mouth
        val sclera = floatArrayOf(0.95f, 0.95f, 0.96f)
        val iris = floatArrayOf(0.10f, 0.10f, 0.12f)
        for (side in listOf(-1f, 1f)) {
            val ex = side * 0.034f
            drawHumanPart(sphereMesh, ex, headY + 0.014f, headZ + 0.088f, 0.014f, 0.009f, 0.008f, 0f, headYaw, 0f, sclera, 0.7f, 64f, 0f)
            drawHumanPart(sphereMesh, ex, headY + 0.014f, headZ + 0.094f, 0.008f, 0.008f, 0.005f, 0f, headYaw, 0f, iris, 0.9f, 80f, 0f)
            drawHumanPart(sphereMesh, ex, headY + 0.030f, headZ + 0.092f, 0.019f, 0.004f, 0.006f, 0f, headYaw, side * -5f, hairRgb, 0.15f, 10f, 0f)
        }
        // Mouth contour
        drawHumanPart(sphereMesh, 0f, headY - 0.048f, headZ + 0.090f, 0.024f, 0.005f, 0.008f, 0f, headYaw, 0f, lipRgb, 0.35f, 28f, 0f)

        // Organic 3D Hair (4 sculpted styles, zero blocks)
        when (params.hairStyle) {
            0 -> { // Short athletic swept
                drawHumanPart(sweptHairMesh, 0f, headY + 0.004f, headZ, 1.0f, 0.96f, 1.0f, -4f, headYaw, 0f, hairRgb, 0.22f, 20f, 0.3f)
            }
            1 -> { // Tapered fade with sculpted crown volume
                drawHumanPart(sweptHairMesh, 0f, headY + 0.016f, headZ + 0.004f, 0.94f, 1.10f, 0.98f, 4f, headYaw, 0f, hairRgb, 0.24f, 22f, 0.3f)
            }
            2 -> { // Layered long flowing
                drawHumanPart(sweptHairMesh, 0f, headY + 0.006f, headZ, 1.03f, 1.0f, 1.02f, 0f, headYaw, 0f, hairRgb, 0.25f, 24f, 0.3f)
                drawHumanPart(longHairBackMesh, 0f, headY - 0.018f, headZ, 1.02f, 1.05f, 1.02f, 0f, headYaw, 0f, hairRgb, 0.24f, 22f, 0.3f)
            }
            else -> { // Clean buzz contour
                drawHumanPart(sweptHairMesh, 0f, headY + 0.002f, headZ, 0.97f, 0.88f, 0.97f, 0f, headYaw, 0f, hairRgb, 0.18f, 16f, 0.4f)
            }
        }

        // Headband
        if (acc != null && acc.styleVariant == 1) {
            val accRgb = parseHexRgb(acc.accentColorHex, floatArrayOf(1f, 1f, 1f))
            drawHumanPart(glassesVisorMesh, 0f, headY + 0.044f, headZ - 0.005f, 1.12f, 0.65f, 1.32f, 0f, headYaw, 0f, accRgb, 0.18f, 14f, 1f)
        }

        // Sculpted Cap (6-panel crown + curved brim)
        if (cap != null) {
            val capRgb = parseHexRgb(cap.primaryColorHex, floatArrayOf(0.1f, 0.1f, 0.1f))
            val capAccent = parseHexRgb(cap.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawHumanPart(capCrownMesh, 0f, headY + 0.040f, headZ - 0.005f, 1.02f, 1.02f, 1.02f, -6f, headYaw, 0f, capRgb, 0.20f, 16f, 1f)
            drawHumanPart(capVisorMesh, 0f, headY + 0.042f, headZ + 0.010f, 1.0f, 1.0f, 1.0f, 6f, headYaw, 0f, capRgb, 0.22f, 18f, 0.6f)
            drawHumanPart(sphereMesh, 0f, headY + 0.082f, headZ + 0.098f, 0.016f, 0.012f, 0.006f, 0f, headYaw, 0f, capAccent, 0.6f, 48f, 0f)
        }

        // Wraparound Curved Sport Glasses
        if (glasses != null) {
            val frameRgb = parseHexRgb(glasses.primaryColorHex, floatArrayOf(0.08f, 0.08f, 0.08f))
            val lensRgb = parseHexRgb(glasses.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawHumanPart(glassesVisorMesh, 0f, headY + 0.014f, headZ + 0.005f, 1.02f, 0.92f, 1.0f, 0f, headYaw, 0f, lensRgb, 0.95f, 96f, 0f)
            drawHumanPart(glassesVisorMesh, 0f, headY + 0.028f, headZ + 0.004f, 1.06f, 0.28f, 1.04f, 0f, headYaw, 0f, frameRgb, 0.55f, 48f, 0f)
        }
    }

    private fun drawStagePart(
        mesh: MeshBuffers,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        rx: Float, ry: Float, rz: Float,
        rgb: FloatArray,
        specStrength: Float,
        shininess: Float,
        alpha: Float,
        fabricWeave: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawCurrent(mesh, rgb, specStrength, shininess, alpha, fabricWeave)
    }

    private fun drawCurrent(
        mesh: MeshBuffers,
        rgb: FloatArray,
        specStrength: Float,
        shininess: Float,
        alpha: Float,
        fabricWeave: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(colorHandle, rgb[0], rgb[1], rgb[2])
        GLES20.glUniform1f(specStrengthHandle, specStrength)
        GLES20.glUniform1f(shininessHandle, shininess)
        GLES20.glUniform1f(alphaHandle, alpha)
        GLES20.glUniform1f(fabricWeaveHandle, fabricWeave)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)

        GLES20.glDrawArrays(mesh.drawMode, 0, mesh.vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    private fun parseHexRgb(hex: String, fallback: FloatArray): FloatArray {
        return try {
            val c = AndroidColor.parseColor(hex)
            floatArrayOf(
                AndroidColor.red(c) / 255f,
                AndroidColor.green(c) / 255f,
                AndroidColor.blue(c) / 255f
            )
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    companion object {
        fun resolveEquippedMap(equippedIds: List<String>): Map<StoreCategory, StoreItem> {
            val map = mutableMapOf<StoreCategory, StoreItem>()
            for (id in equippedIds) {
                val item = StoreCatalog.getById(id) ?: continue
                map[item.category] = item
            }
            return map
        }

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uColor;
            uniform vec3 uKeyLightDir;
            uniform vec3 uCameraPos;
            uniform float uSpecStrength;
            uniform float uShininess;
            uniform float uAlpha;
            uniform float uFabricWeave;
            varying vec3 vNormal;
            varying vec3 vWorldPos;
            void main() {
                vec3 n = normalize(vNormal);
                vec3 l = normalize(uKeyLightDir);
                vec3 fillDir = normalize(vec3(-0.7, 0.3, 0.4));
                vec3 v = normalize(uCameraPos - vWorldPos);
                vec3 h = normalize(l + v);

                float keyDiff = max(dot(n, l), 0.0);
                float fillDiff = max(dot(n, fillDir), 0.0) * 0.28;
                float hemi = 0.24 + 0.14 * n.y;
                float spec = pow(max(dot(n, h), 0.0), uShininess) * uSpecStrength;
                float rim = pow(1.0 - max(dot(n, v), 0.0), 2.8) * 0.28;

                // Subtle micro-weave variation for athletic clothing materials
                float weave = 1.0;
                if (uFabricWeave > 0.5) {
                    float pattern = sin(vWorldPos.x * 160.0) * sin(vWorldPos.y * 160.0);
                    weave = 0.96 + 0.04 * pattern;
                }

                vec3 baseColor = uColor * weave;
                vec3 lit = baseColor * (hemi + 0.72 * keyDiff + fillDiff)
                    + vec3(1.0, 0.98, 0.95) * spec
                    + vec3(0.22, 1.0, 0.08) * rim * 0.38;

                gl_FragColor = vec4(clamp(lit, 0.0, 1.0), uAlpha);
            }
        """
    }
}
