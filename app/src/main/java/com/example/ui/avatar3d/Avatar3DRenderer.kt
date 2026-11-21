package com.example.ui.avatar3d

import android.graphics.Color as AndroidColor
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.domain.catalog.StoreCatalog
import com.example.domain.model.AvatarParams
import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
    var yawDegrees: Float = 18f

    @Volatile
    var pitchDegrees: Float = 8f

    @Volatile
    var autoRotate: Boolean = false

    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpMatrixHandle = 0
    private var modelMatrixHandle = 0
    private var colorHandle = 0
    private var lightDirHandle = 0

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private lateinit var sphereMesh: MeshBuffers
    private lateinit var cylinderMesh: MeshBuffers
    private lateinit var boxMesh: MeshBuffers
    private lateinit var ringMesh: MeshBuffers

    private var breathPhase = 0f

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.028f, 0.02f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

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
        lightDirHandle = GLES20.glGetUniformLocation(program, "uLightDir")

        sphereMesh = createSphereMesh(18, 24)
        cylinderMesh = createCylinderMesh(24)
        boxMesh = createBoxMesh()
        ringMesh = createRingMesh(36)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 38f, ratio, 0.5f, 20f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        if (autoRotate) {
            yawDegrees = (yawDegrees + 0.45f) % 360f
        }
        breathPhase = (breathPhase + 0.04f) % (2f * Math.PI.toFloat())
        val breathScale = 1f + 0.012f * sin(breathPhase)

        val camDist = 3.55f
        val pitchRad = Math.toRadians(pitchDegrees.coerceIn(-15f, 32f).toDouble())
        val camY = (1.05f + sin(pitchRad) * 1.1f).toFloat()
        val camZ = (cos(pitchRad) * camDist).toFloat()

        Matrix.setLookAtM(viewMatrix, 0, 0f, camY, camZ, 0f, 0.92f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(lightDirHandle, 0.5f, 0.85f, 0.7f)

        val params = avatarParams
        val skinRgb = parseHexRgb(params.skinToneHex, floatArrayOf(0.84f, 0.62f, 0.48f))
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
        val topRgb = topItem?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.1f, 0.12f, 0.1f)) }
            ?: floatArrayOf(0.12f, 0.14f, 0.12f)
        val topAccentRgb = topItem?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)

        val pantsItem = fullSuit ?: pants
        val pantsRgb = pantsItem?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.10f, 0.12f, 0.14f)) }
            ?: floatArrayOf(0.11f, 0.13f, 0.15f)
        val isShorts = pants?.styleVariant == 1 && fullSuit == null

        val shoeRgb = sneakers?.let { parseHexRgb(it.primaryColorHex, floatArrayOf(0.1f, 0.1f, 0.1f)) }
            ?: floatArrayOf(0.14f, 0.14f, 0.16f)
        val shoeAccentRgb = sneakers?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)

        val shoulderW = 0.21f * params.shoulderWidthRatio
        val waistW = 0.14f * params.waistRatio
        val hipW = 0.155f * params.hipWidthRatio
        val limbW = params.limbThicknessRatio
        val heightScale = (params.heightCm / 172f).coerceIn(0.88f, 1.12f)
        val postureLean = (1f - params.postureAlignment) * 14f // forward slouch angle if posture lower

        // Base Pedestal Ring (Neon green + dark disc)
        drawPart(cylinderMesh, 0f, 0.015f, 0f, 0.56f, 0.03f, 0.56f, 0f, 0f, 0f, floatArrayOf(0.06f, 0.08f, 0.06f))
        drawPart(ringMesh, 0f, 0.033f, 0f, 0.57f, 1f, 0.57f, 0f, 0f, 0f, floatArrayOf(0.22f, 1.0f, 0.08f))

        // Helper for rotated body coordinates
        val rootYaw = yawDegrees
        fun drawBodyPart(
            mesh: MeshBuffers,
            lx: Float, ly: Float, lz: Float,
            sx: Float, sy: Float, sz: Float,
            rx: Float, ry: Float, rz: Float,
            rgb: FloatArray
        ) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.rotateM(modelMatrix, 0, rootYaw, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 1f, heightScale, 1f)
            // Apply subtle spinal posture lean above hip height
            if (ly > 0.90f) {
                Matrix.translateM(modelMatrix, 0, 0f, 0.90f, 0f)
                Matrix.rotateM(modelMatrix, 0, postureLean, 1f, 0f, 0f)
                Matrix.translateM(modelMatrix, 0, 0f, -0.90f, 0f)
            }
            Matrix.translateM(modelMatrix, 0, lx, ly, lz)
            if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
            if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
            if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
            drawWithCurrentModel(mesh, rgb)
        }

        // Legs & Feet
        val hipX = hipW * 0.58f
        val thighW = 0.068f * limbW
        val calfW = 0.052f * limbW

        for (side in listOf(-1f, 1f)) {
            val sx = side * hipX
            // Thigh
            drawBodyPart(cylinderMesh, sx, 0.72f, 0f, thighW, 0.38f, thighW, 0f, 0f, side * -2f, pantsRgb)
            // Knee joint
            drawBodyPart(sphereMesh, sx, 0.52f, 0f, thighW * 0.85f, 0.055f, thighW * 0.85f, 0f, 0f, 0f, if (isShorts) skinRgb else pantsRgb)
            // Calf
            val calfColor = if (isShorts) skinRgb else pantsRgb
            drawBodyPart(cylinderMesh, sx, 0.30f, 0f, calfW, 0.40f, calfW, 0f, 0f, 0f, calfColor)
            // Shoe / Sneaker
            drawBodyPart(boxMesh, sx, 0.085f, 0.03f, 0.085f, 0.08f, 0.18f, 0f, 0f, 0f, shoeRgb)
            drawBodyPart(boxMesh, sx, 0.048f, 0.03f, 0.090f, 0.024f, 0.188f, 0f, 0f, 0f, shoeAccentRgb)
        }

        // Pelvis / Hips
        drawBodyPart(cylinderMesh, 0f, 0.92f, 0f, hipW, 0.14f, 0.11f * params.waistRatio, 0f, 0f, 0f, pantsRgb)

        // Waist / Abdomen
        drawBodyPart(cylinderMesh, 0f, 1.07f, 0f, waistW * breathScale, 0.19f, 0.105f * params.waistRatio * breathScale, 0f, 0f, 0f, topRgb)

        // Chest / Upper Torso
        drawBodyPart(cylinderMesh, 0f, 1.26f, 0f, shoulderW * 0.86f * breathScale, 0.24f, 0.125f * breathScale, 0f, 0f, 0f, topRgb)

        // DeFitt chest accent stripe / emblem
        drawBodyPart(boxMesh, 0f, 1.29f, 0.118f, 0.065f, 0.065f, 0.015f, 0f, 0f, 0f, topAccentRgb)

        // Shoulders & Arms
        val hasSleeves = topItem?.styleVariant != 2 // Tank top variant = 2
        val sleeveColor = if (hasSleeves) topRgb else skinRgb
        val armX = shoulderW + 0.035f * limbW
        val upperArmR = 0.048f * limbW
        val forearmR = 0.040f * limbW

        for (side in listOf(-1f, 1f)) {
            val ax = side * armX
            val armTilt = side * -9f
            // Deltoid
            drawBodyPart(sphereMesh, ax, 1.35f, 0f, upperArmR * 1.15f, 0.062f, upperArmR * 1.15f, 0f, 0f, 0f, sleeveColor)
            // Upper arm
            drawBodyPart(cylinderMesh, ax + side * 0.02f, 1.20f, 0f, upperArmR, 0.26f, upperArmR, 0f, 0f, armTilt, sleeveColor)
            // Forearm (hoodie / full suit covers forearm too)
            val forearmColor = if (hoodie != null || fullSuit != null) topRgb else skinRgb
            drawBodyPart(cylinderMesh, ax + side * 0.045f, 0.96f, 0.015f, forearmR, 0.24f, forearmR, -6f, 0f, armTilt * 0.6f, forearmColor)
            // Hand
            drawBodyPart(sphereMesh, ax + side * 0.055f, 0.82f, 0.03f, 0.040f, 0.052f, 0.035f, 0f, 0f, 0f, skinRgb)
        }

        // Smartwatch on left wrist (side = -1f)
        val watchRgb = watch?.let { parseHexRgb(it.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f)) }
            ?: floatArrayOf(0.22f, 1f, 0.08f)
        if (watch != null) {
            val wx = -armX - 0.048f
            drawBodyPart(cylinderMesh, wx, 0.87f, 0.02f, forearmR * 1.18f, 0.038f, forearmR * 1.18f, 0f, 0f, 0f, floatArrayOf(0.08f, 0.08f, 0.08f))
            drawBodyPart(boxMesh, wx - 0.022f, 0.87f, 0.042f, 0.028f, 0.030f, 0.014f, 0f, -25f, 0f, watchRgb)
        }

        // Wristband accessories
        if (acc != null && acc.styleVariant == 0) {
            val accRgb = parseHexRgb(acc.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawBodyPart(cylinderMesh, armX + 0.048f, 0.87f, 0.02f, forearmR * 1.16f, 0.045f, forearmR * 1.16f, 0f, 0f, 0f, accRgb)
        }

        // Backpack (visible when rotating!)
        if (backpack != null) {
            val bagRgb = parseHexRgb(backpack.primaryColorHex, floatArrayOf(0.15f, 0.16f, 0.15f))
            val bagAccent = parseHexRgb(backpack.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawBodyPart(boxMesh, 0f, 1.19f, -0.17f, 0.22f, 0.30f, 0.13f, 0f, 0f, 0f, bagRgb)
            drawBodyPart(boxMesh, 0f, 1.22f, -0.238f, 0.14f, 0.03f, 0.015f, 0f, 0f, 0f, bagAccent)
        }

        // Hoodie back hood
        if (hoodie != null || fullSuit != null) {
            drawBodyPart(sphereMesh, 0f, 1.41f, -0.09f, 0.115f, 0.095f, 0.095f, 0f, 0f, 0f, topRgb)
        }

        // Neck
        drawBodyPart(cylinderMesh, 0f, 1.43f, 0f, 0.052f, 0.10f, 0.052f, 0f, 0f, 0f, skinRgb)

        // Head
        drawBodyPart(sphereMesh, 0f, 1.56f, 0.005f, 0.105f, 0.125f, 0.110f, 0f, 0f, 0f, skinRgb)

        // Eyes (subtle dark eyes for realistic human face orientation)
        val eyeColor = floatArrayOf(0.08f, 0.08f, 0.09f)
        drawBodyPart(sphereMesh, -0.036f, 1.575f, 0.098f, 0.014f, 0.011f, 0.010f, 0f, 0f, 0f, eyeColor)
        drawBodyPart(sphereMesh, 0.036f, 1.575f, 0.098f, 0.014f, 0.011f, 0.010f, 0f, 0f, 0f, eyeColor)

        // Hair (4 styles)
        when (params.hairStyle) {
            0 -> { // Short athletic
                drawBodyPart(sphereMesh, 0f, 1.62f, -0.01f, 0.108f, 0.080f, 0.112f, 0f, 0f, 0f, hairRgb)
            }
            1 -> { // Fade / Top volume
                drawBodyPart(boxMesh, 0f, 1.66f, 0.00f, 0.13f, 0.065f, 0.15f, 0f, 0f, 0f, hairRgb)
                drawBodyPart(sphereMesh, 0f, 1.60f, -0.03f, 0.104f, 0.07f, 0.09f, 0f, 0f, 0f, hairRgb)
            }
            2 -> { // Long
                drawBodyPart(sphereMesh, 0f, 1.61f, -0.02f, 0.116f, 0.10f, 0.12f, 0f, 0f, 0f, hairRgb)
                drawBodyPart(boxMesh, 0f, 1.49f, -0.07f, 0.17f, 0.22f, 0.08f, 0f, 0f, 0f, hairRgb)
            }
            else -> { // Buzz
                drawBodyPart(sphereMesh, 0f, 1.595f, -0.005f, 0.104f, 0.095f, 0.108f, 0f, 0f, 0f, hairRgb)
            }
        }

        // Headband accessory
        if (acc != null && acc.styleVariant == 1) {
            val accRgb = parseHexRgb(acc.accentColorHex, floatArrayOf(1f, 1f, 1f))
            drawBodyPart(cylinderMesh, 0f, 1.615f, 0.005f, 0.110f, 0.028f, 0.114f, 0f, 0f, 0f, accRgb)
        }

        // Cap
        if (cap != null) {
            val capRgb = parseHexRgb(cap.primaryColorHex, floatArrayOf(0.1f, 0.1f, 0.1f))
            val capAccent = parseHexRgb(cap.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawBodyPart(sphereMesh, 0f, 1.64f, -0.005f, 0.114f, 0.075f, 0.118f, 0f, 0f, 0f, capRgb)
            drawBodyPart(boxMesh, 0f, 1.62f, 0.115f, 0.16f, 0.015f, 0.11f, 8f, 0f, 0f, capRgb)
            drawBodyPart(boxMesh, 0f, 1.655f, 0.105f, 0.036f, 0.025f, 0.012f, 0f, 0f, 0f, capAccent)
        }

        // Sport Glasses
        if (glasses != null) {
            val frameRgb = parseHexRgb(glasses.primaryColorHex, floatArrayOf(0.08f, 0.08f, 0.08f))
            val lensRgb = parseHexRgb(glasses.accentColorHex, floatArrayOf(0.22f, 1f, 0.08f))
            drawBodyPart(boxMesh, 0f, 1.578f, 0.108f, 0.155f, 0.036f, 0.022f, 0f, 0f, 0f, lensRgb)
            drawBodyPart(boxMesh, 0f, 1.594f, 0.108f, 0.165f, 0.012f, 0.026f, 0f, 0f, 0f, frameRgb)
        }
    }

    private fun drawPart(
        mesh: MeshBuffers,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        rx: Float, ry: Float, rz: Float,
        rgb: FloatArray
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        if (ry != 0f) Matrix.rotateM(modelMatrix, 0, ry, 0f, 1f, 0f)
        if (rx != 0f) Matrix.rotateM(modelMatrix, 0, rx, 1f, 0f, 0f)
        if (rz != 0f) Matrix.rotateM(modelMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawWithCurrentModel(mesh, rgb)
    }

    private fun drawWithCurrentModel(mesh: MeshBuffers, rgb: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(colorHandle, rgb[0], rgb[1], rgb[2])

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

    private data class MeshBuffers(
        val vertexBuffer: FloatBuffer,
        val normalBuffer: FloatBuffer,
        val vertexCount: Int,
        val drawMode: Int = GLES20.GL_TRIANGLES
    )

    private fun toFloatBuffer(arr: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(arr)
                position(0)
            }
    }

    private fun createSphereMesh(stacks: Int, slices: Int): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        for (i in 0 until stacks) {
            val phi1 = Math.PI * i / stacks
            val phi2 = Math.PI * (i + 1) / stacks
            for (j in 0 until slices) {
                val theta1 = 2.0 * Math.PI * j / slices
                val theta2 = 2.0 * Math.PI * (j + 1) / slices

                val p1 = spherePoint(phi1, theta1)
                val p2 = spherePoint(phi2, theta1)
                val p3 = spherePoint(phi2, theta2)
                val p4 = spherePoint(phi1, theta2)

                addTri(verts, norms, p1, p2, p3)
                addTri(verts, norms, p1, p3, p4)
            }
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    private fun spherePoint(phi: Double, theta: Double): FloatArray {
        val x = (sin(phi) * cos(theta)).toFloat()
        val y = cos(phi).toFloat()
        val z = (sin(phi) * sin(theta)).toFloat()
        return floatArrayOf(x, y, z)
    }

    private fun createCylinderMesh(slices: Int): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        for (j in 0 until slices) {
            val t1 = 2.0 * Math.PI * j / slices
            val t2 = 2.0 * Math.PI * (j + 1) / slices
            val x1 = cos(t1).toFloat()
            val z1 = sin(t1).toFloat()
            val x2 = cos(t2).toFloat()
            val z2 = sin(t2).toFloat()

            // Side quad
            val b1 = floatArrayOf(x1, -0.5f, z1)
            val b2 = floatArrayOf(x2, -0.5f, z2)
            val top2 = floatArrayOf(x2, 0.5f, z2)
            val top1 = floatArrayOf(x1, 0.5f, z1)

            verts.addAll(listOf(b1[0], b1[1], b1[2], b2[0], b2[1], b2[2], top2[0], top2[1], top2[2]))
            norms.addAll(listOf(x1, 0f, z1, x2, 0f, z2, x2, 0f, z2))

            verts.addAll(listOf(b1[0], b1[1], b1[2], top2[0], top2[1], top2[2], top1[0], top1[1], top1[2]))
            norms.addAll(listOf(x1, 0f, z1, x2, 0f, z2, x1, 0f, z1))

            // Top cap
            verts.addAll(listOf(0f, 0.5f, 0f, top2[0], top2[1], top2[2], top1[0], top1[1], top1[2]))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))

            // Bottom cap
            verts.addAll(listOf(0f, -0.5f, 0f, b1[0], b1[1], b1[2], b2[0], b2[1], b2[2]))
            norms.addAll(listOf(0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f))
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    private fun createBoxMesh(): MeshBuffers {
        val h = 0.5f
        val faces = listOf(
            // Front
            Triple(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h)) to floatArrayOf(-h, h, h) to floatArrayOf(0f, 0f, 1f),
            // Back
            Triple(floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h)) to floatArrayOf(h, h, -h) to floatArrayOf(0f, 0f, -1f),
            // Right
            Triple(floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(h, h, -h)) to floatArrayOf(h, h, h) to floatArrayOf(1f, 0f, 0f),
            // Left
            Triple(floatArrayOf(-h, -h, -h), floatArrayOf(-h, -h, h), floatArrayOf(-h, h, h)) to floatArrayOf(-h, h, -h) to floatArrayOf(-1f, 0f, 0f),
            // Top
            Triple(floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h)) to floatArrayOf(-h, h, -h) to floatArrayOf(0f, 1f, 0f),
            // Bottom
            Triple(floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h)) to floatArrayOf(-h, -h, h) to floatArrayOf(0f, -1f, 0f)
        )
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        for ((quad, n) in faces) {
            val (tri, p4) = quad
            val (p1, p2, p3) = tri
            for (p in listOf(p1, p2, p3, p1, p3, p4)) {
                verts.addAll(p.toList())
                norms.addAll(n.toList())
            }
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    private fun createRingMesh(segments: Int): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        val rInner = 0.92f
        val rOuter = 1.0f
        for (i in 0 until segments) {
            val a1 = 2.0 * Math.PI * i / segments
            val a2 = 2.0 * Math.PI * (i + 1) / segments
            val ix1 = (rInner * cos(a1)).toFloat()
            val iz1 = (rInner * sin(a1)).toFloat()
            val ox1 = (rOuter * cos(a1)).toFloat()
            val oz1 = (rOuter * sin(a1)).toFloat()
            val ix2 = (rInner * cos(a2)).toFloat()
            val iz2 = (rInner * sin(a2)).toFloat()
            val ox2 = (rOuter * cos(a2)).toFloat()
            val oz2 = (rOuter * sin(a2)).toFloat()

            verts.addAll(listOf(ix1, 0f, iz1, ox2, 0f, oz2, ox1, 0f, oz1))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
            verts.addAll(listOf(ix1, 0f, iz1, ix2, 0f, iz2, ox2, 0f, oz2))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    private fun addTri(verts: MutableList<Float>, norms: MutableList<Float>, a: FloatArray, b: FloatArray, c: FloatArray) {
        verts.addAll(a.toList())
        verts.addAll(b.toList())
        verts.addAll(c.toList())
        norms.addAll(a.toList())
        norms.addAll(b.toList())
        norms.addAll(c.toList())
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
            void main() {
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uColor;
            uniform vec3 uLightDir;
            varying vec3 vNormal;
            void main() {
                vec3 n = normalize(vNormal);
                vec3 l = normalize(uLightDir);
                float diff = max(dot(n, l), 0.0);
                float rim = pow(1.0 - max(n.z, 0.0), 2.5) * 0.22;
                vec3 lit = uColor * (0.36 + 0.64 * diff) + vec3(0.08, 0.25, 0.04) * rim;
                gl_FragColor = vec4(clamp(lit, 0.0, 1.0), 1.0);
            }
        """
    }
}
