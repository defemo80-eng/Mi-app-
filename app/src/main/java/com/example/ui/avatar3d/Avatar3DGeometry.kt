package com.example.ui.avatar3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MeshBuffers(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val vertexCount: Int,
    val drawMode: Int = GLES20.GL_TRIANGLES
)

data class LoftRing(
    val y: Float,
    val cx: Float = 0f,
    val cz: Float = 0f,
    val rx: Float,
    val rz: Float,
    val frontExtraZ: Float = 0f,
    val backExtraZ: Float = 0f,
    val chestSculpt: Float = 0f
)

object Avatar3DGeometry {

    fun toFloatBuffer(arr: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(arr)
                position(0)
            }
    }

    /**
     * Generates a smooth, watertight 3D mesh from cross-sectional anatomical rings
     * with averaged vertex normals for organic curvature (zero blocky edges).
     */
    fun buildLoftedMesh(
        rings: List<LoftRing>,
        slices: Int = 28,
        capBottom: Boolean = true,
        capTop: Boolean = true
    ): MeshBuffers {
        val ringCount = rings.size
        val grid = Array(ringCount) { r ->
            val ring = rings[r]
            Array(slices) { s ->
                val theta = (2.0 * Math.PI * s / slices).toFloat()
                val cosT = cos(theta)
                val sinT = sin(theta) // +sinT = front (+Z), -sinT = back (-Z)
                var x = ring.cx + ring.rx * cosT
                var z = ring.cz + ring.rz * sinT

                if (sinT > 0f && ring.frontExtraZ != 0f) {
                    z += ring.frontExtraZ * sinT * sinT
                }
                if (sinT < 0f && ring.backExtraZ != 0f) {
                    z -= ring.backExtraZ * sinT * sinT
                }
                if (ring.chestSculpt != 0f && sinT > 0.15f) {
                    // Subtle dual pectoral contour without harsh ridges
                    val chestCurve = kotlin.math.abs(cosT)
                    z += ring.chestSculpt * sinT * (0.4f + 0.6f * sin(chestCurve * Math.PI.toFloat()))
                    x *= 1.0f + 0.03f * ring.chestSculpt * sinT
                }
                floatArrayOf(x, ring.y, z)
            }
        }

        // Accumulate smooth normals per vertex in the grid
        val normals = Array(ringCount) { Array(slices) { FloatArray(3) } }
        fun addFaceNormal(r0: Int, s0: Int, r1: Int, s1: Int, r2: Int, s2: Int) {
            val a = grid[r0][s0]
            val b = grid[r1][s1]
            val c = grid[r2][s2]
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            for ((rr, ss) in listOf(r0 to s0, r1 to s1, r2 to s2)) {
                normals[rr][ss][0] += nx
                normals[rr][ss][1] += ny
                normals[rr][ss][2] += nz
            }
        }

        for (r in 0 until ringCount - 1) {
            for (s in 0 until slices) {
                val nextS = (s + 1) % slices
                addFaceNormal(r, s, r, nextS, r + 1, nextS)
                addFaceNormal(r, s, r + 1, nextS, r + 1, s)
            }
        }

        for (r in 0 until ringCount) {
            for (s in 0 until slices) {
                val n = normals[r][s]
                val len = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]).coerceAtLeast(1e-5f)
                n[0] /= len; n[1] /= len; n[2] /= len
            }
        }

        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        fun push(p: FloatArray, n: FloatArray) {
            verts.add(p[0]); verts.add(p[1]); verts.add(p[2])
            norms.add(n[0]); norms.add(n[1]); norms.add(n[2])
        }

        for (r in 0 until ringCount - 1) {
            for (s in 0 until slices) {
                val nextS = (s + 1) % slices
                val p00 = grid[r][s]; val n00 = normals[r][s]
                val p01 = grid[r][nextS]; val n01 = normals[r][nextS]
                val p11 = grid[r + 1][nextS]; val n11 = normals[r + 1][nextS]
                val p10 = grid[r + 1][s]; val n10 = normals[r + 1][s]

                push(p00, n00); push(p01, n01); push(p11, n11)
                push(p00, n00); push(p11, n11); push(p10, n10)
            }
        }

        if (capBottom && ringCount > 0) {
            val r0 = rings.first()
            val center = floatArrayOf(r0.cx, r0.y, r0.cz)
            val downN = floatArrayOf(0f, -1f, 0f)
            for (s in 0 until slices) {
                val nextS = (s + 1) % slices
                push(center, downN)
                push(grid[0][nextS], downN)
                push(grid[0][s], downN)
            }
        }

        if (capTop && ringCount > 0) {
            val rLast = rings.last()
            val center = floatArrayOf(rLast.cx, rLast.y, rLast.cz)
            val upN = floatArrayOf(0f, 1f, 0f)
            val topIdx = ringCount - 1
            for (s in 0 until slices) {
                val nextS = (s + 1) % slices
                push(center, upN)
                push(grid[topIdx][s], upN)
                push(grid[topIdx][nextS], upN)
            }
        }

        return MeshBuffers(
            vertexBuffer = toFloatBuffer(verts.toFloatArray()),
            normalBuffer = toFloatBuffer(norms.toFloatArray()),
            vertexCount = verts.size / 3
        )
    }

    fun buildSphereMesh(stacks: Int = 20, slices: Int = 28): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        fun pt(phi: Double, theta: Double): FloatArray {
            val x = (sin(phi) * cos(theta)).toFloat()
            val y = cos(phi).toFloat()
            val z = (sin(phi) * sin(theta)).toFloat()
            return floatArrayOf(x, y, z)
        }
        fun addTri(a: FloatArray, b: FloatArray, c: FloatArray) {
            verts.addAll(a.toList()); verts.addAll(b.toList()); verts.addAll(c.toList())
            norms.addAll(a.toList()); norms.addAll(b.toList()); norms.addAll(c.toList())
        }
        for (i in 0 until stacks) {
            val phi1 = Math.PI * i / stacks
            val phi2 = Math.PI * (i + 1) / stacks
            for (j in 0 until slices) {
                val t1 = 2.0 * Math.PI * j / slices
                val t2 = 2.0 * Math.PI * (j + 1) / slices
                val p1 = pt(phi1, t1); val p2 = pt(phi2, t1)
                val p3 = pt(phi2, t2); val p4 = pt(phi1, t2)
                addTri(p1, p2, p3)
                addTri(p1, p3, p4)
            }
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    /**
     * Sculpted human head with chin/jawline taper, cheekbones, brow & occipital dome.
     */
    fun buildSculptedHeadMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.118f, cz = 0.015f, rx = 0.030f, rz = 0.034f, frontExtraZ = 0.014f), // Chin tip
            LoftRing(y = -0.095f, cz = 0.010f, rx = 0.056f, rz = 0.068f, frontExtraZ = 0.020f), // Lower jaw
            LoftRing(y = -0.060f, cz = 0.005f, rx = 0.076f, rz = 0.088f, frontExtraZ = 0.018f), // Jaw / mouth
            LoftRing(y = -0.020f, cz = 0.002f, rx = 0.086f, rz = 0.098f, frontExtraZ = 0.014f), // Cheekbones
            LoftRing(y = 0.022f, cz = 0.000f, rx = 0.088f, rz = 0.102f, frontExtraZ = 0.012f),  // Brow / temples
            LoftRing(y = 0.064f, cz = -0.004f, rx = 0.085f, rz = 0.100f, backExtraZ = 0.008f), // Forehead
            LoftRing(y = 0.098f, cz = -0.006f, rx = 0.070f, rz = 0.085f),                      // Upper cranium
            LoftRing(y = 0.120f, cz = -0.006f, rx = 0.042f, rz = 0.052f),                      // Crown
            LoftRing(y = 0.130f, cz = -0.006f, rx = 0.012f, rz = 0.015f)
        )
        return buildLoftedMesh(rings, slices = 30)
    }

    /**
     * Anatomical neck + trapezius base flare so the head integrates smoothly into the torso.
     */
    fun buildNeckTrapeziusMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.065f, cz = -0.008f, rx = 0.105f, rz = 0.078f, backExtraZ = 0.012f),
            LoftRing(y = -0.032f, cz = -0.004f, rx = 0.064f, rz = 0.060f),
            LoftRing(y = 0.010f, cz = 0.000f, rx = 0.051f, rz = 0.052f),
            LoftRing(y = 0.055f, cz = 0.004f, rx = 0.050f, rz = 0.052f)
        )
        return buildLoftedMesh(rings, slices = 26)
    }

    /**
     * Continuous sculpted human torso (Pelvis -> Waist -> Ribcage -> Chest/Shoulders).
     */
    fun buildSculptedTorsoMesh(shellOffset: Float = 0f, isHoodie: Boolean = false): MeshBuffers {
        val o = shellOffset + if (isHoodie) 0.009f else 0f
        val rings = listOf(
            LoftRing(y = 0.82f, cz = -0.005f, rx = 0.128f + o, rz = 0.095f + o, backExtraZ = 0.018f), // Lower pelvis
            LoftRing(y = 0.89f, cz = -0.004f, rx = 0.156f + o, rz = 0.106f + o, backExtraZ = 0.026f), // Hips / glutes
            LoftRing(y = 0.96f, cz = 0.002f, rx = 0.146f + o, rz = 0.102f + o, backExtraZ = 0.012f),  // Upper hip
            LoftRing(y = 1.04f, cz = 0.008f, rx = 0.132f + o, rz = 0.094f + o, frontExtraZ = 0.008f), // Waist
            LoftRing(y = 1.13f, cz = 0.006f, rx = 0.148f + o, rz = 0.104f + o, frontExtraZ = 0.012f), // Lower ribcage
            LoftRing(y = 1.22f, cz = 0.002f, rx = 0.172f + o, rz = 0.114f + o, chestSculpt = 0.016f, backExtraZ = 0.012f), // Mid chest
            LoftRing(y = 1.31f, cz = -0.002f, rx = 0.188f + o, rz = 0.112f + o, chestSculpt = 0.018f, backExtraZ = 0.014f), // Upper chest
            LoftRing(y = 1.38f, cz = -0.006f, rx = 0.165f + o, rz = 0.094f + o),                      // Collarbone
            LoftRing(y = 1.42f, cz = -0.006f, rx = 0.072f + o * 0.4f, rz = 0.066f + o * 0.4f)        // Neck opening
        )
        return buildLoftedMesh(rings, slices = 32)
    }

    /**
     * Tapered upper arm with deltoid sweep and elbow narrowing.
     */
    fun buildUpperArmMesh(extraRadius: Float = 0f): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.135f, rx = 0.036f + extraRadius, rz = 0.036f + extraRadius), // Elbow
            LoftRing(y = -0.060f, rx = 0.043f + extraRadius, rz = 0.045f + extraRadius), // Lower bicep
            LoftRing(y = 0.025f, rx = 0.049f + extraRadius, rz = 0.052f + extraRadius),  // Mid bicep
            LoftRing(y = 0.095f, rx = 0.056f + extraRadius, rz = 0.058f + extraRadius),  // Deltoid belly
            LoftRing(y = 0.135f, rx = 0.044f + extraRadius, rz = 0.046f + extraRadius)   // Shoulder cap
        )
        return buildLoftedMesh(rings, slices = 22)
    }

    /**
     * Anatomical forearm tapering smoothly to an oval wrist.
     */
    fun buildForearmMesh(extraRadius: Float = 0f): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.125f, rx = 0.025f + extraRadius, rz = 0.030f + extraRadius), // Wrist
            LoftRing(y = -0.055f, rx = 0.031f + extraRadius, rz = 0.034f + extraRadius),
            LoftRing(y = 0.035f, rx = 0.041f + extraRadius, rz = 0.042f + extraRadius),  // Upper forearm muscle
            LoftRing(y = 0.115f, rx = 0.037f + extraRadius, rz = 0.037f + extraRadius)   // Elbow joint
        )
        return buildLoftedMesh(rings, slices = 22)
    }

    /**
     * Sculpted human hand (palm + finger taper) instead of a plain ball.
     */
    fun buildHandMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.072f, cz = 0.008f, rx = 0.014f, rz = 0.024f), // Fingertips
            LoftRing(y = -0.040f, cz = 0.006f, rx = 0.017f, rz = 0.034f), // Knuckles / fingers
            LoftRing(y = 0.000f, cz = 0.003f, rx = 0.019f, rz = 0.039f),  // Palm
            LoftRing(y = 0.036f, cz = 0.000f, rx = 0.022f, rz = 0.032f)   // Wrist heel
        )
        return buildLoftedMesh(rings, slices = 18)
    }

    /**
     * Anatomical thigh tapering smoothly from hip/quadricep down to knee.
     */
    fun buildThighMesh(extraRadius: Float = 0f): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.195f, rx = 0.052f + extraRadius, rz = 0.055f + extraRadius), // Knee
            LoftRing(y = -0.095f, rx = 0.062f + extraRadius, rz = 0.065f + extraRadius, frontExtraZ = 0.006f),
            LoftRing(y = 0.035f, rx = 0.074f + extraRadius, rz = 0.078f + extraRadius, frontExtraZ = 0.008f),  // Mid quad
            LoftRing(y = 0.145f, rx = 0.082f + extraRadius, rz = 0.086f + extraRadius, backExtraZ = 0.010f),   // Upper thigh
            LoftRing(y = 0.195f, rx = 0.072f + extraRadius, rz = 0.074f + extraRadius)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Anatomical calf with gastrocnemius curve tapering to ankle.
     */
    fun buildCalfMesh(extraRadius: Float = 0f): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.190f, rx = 0.035f + extraRadius, rz = 0.038f + extraRadius), // Ankle
            LoftRing(y = -0.090f, rx = 0.042f + extraRadius, rz = 0.046f + extraRadius),
            LoftRing(y = 0.035f, rx = 0.055f + extraRadius, rz = 0.058f + extraRadius, backExtraZ = 0.014f), // Calf muscle curve
            LoftRing(y = 0.135f, rx = 0.053f + extraRadius, rz = 0.055f + extraRadius),
            LoftRing(y = 0.190f, rx = 0.049f + extraRadius, rz = 0.052f + extraRadius)  // Below knee
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Contoured athletic sneaker / shoe upper (sleek curved profile from heel to toe-box).
     */
    fun buildShoeUpperMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = 0.000f, cz = 0.035f, rx = 0.048f, rz = 0.105f, frontExtraZ = 0.025f), // Base rim
            LoftRing(y = 0.026f, cz = 0.032f, rx = 0.046f, rz = 0.100f, frontExtraZ = 0.020f), // Mid vamp
            LoftRing(y = 0.054f, cz = 0.016f, rx = 0.042f, rz = 0.076f, frontExtraZ = 0.010f), // Instep / tongue
            LoftRing(y = 0.084f, cz = -0.012f, rx = 0.039f, rz = 0.046f)                       // Ankle collar
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    fun buildShoeSoleMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.016f, cz = 0.036f, rx = 0.050f, rz = 0.110f, frontExtraZ = 0.024f),
            LoftRing(y = 0.006f, cz = 0.035f, rx = 0.052f, rz = 0.112f, frontExtraZ = 0.024f),
            LoftRing(y = 0.016f, cz = 0.034f, rx = 0.049f, rz = 0.107f, frontExtraZ = 0.022f)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Curved 6-panel athletic cap crown + arched brim (no blocky squares).
     */
    fun buildCapCrownMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = 0.000f, cz = 0.000f, rx = 0.094f, rz = 0.108f),
            LoftRing(y = 0.034f, cz = -0.002f, rx = 0.090f, rz = 0.104f),
            LoftRing(y = 0.068f, cz = -0.004f, rx = 0.072f, rz = 0.084f),
            LoftRing(y = 0.090f, cz = -0.004f, rx = 0.034f, rz = 0.040f),
            LoftRing(y = 0.096f, cz = -0.004f, rx = 0.008f, rz = 0.010f)
        )
        return buildLoftedMesh(rings, slices = 28)
    }

    fun buildCapVisorMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = 0.000f, cz = 0.072f, rx = 0.088f, rz = 0.044f, frontExtraZ = 0.038f),
            LoftRing(y = 0.010f, cz = 0.068f, rx = 0.084f, rz = 0.040f, frontExtraZ = 0.035f)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Wraparound curved sports visor/glasses lens + frame arc.
     */
    fun buildCurvedVisorMesh(widthX: Float = 0.082f, heightY: Float = 0.018f): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -heightY, cz = 0.020f, rx = widthX, rz = 0.074f, frontExtraZ = 0.024f),
            LoftRing(y = 0f, cz = 0.020f, rx = widthX + 0.004f, rz = 0.076f, frontExtraZ = 0.026f),
            LoftRing(y = heightY, cz = 0.020f, rx = widthX, rz = 0.074f, frontExtraZ = 0.024f)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Rounded ergonomic backpack shell (no rectangular brick).
     */
    fun buildBackpackMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.16f, cz = 0f, rx = 0.095f, rz = 0.045f),
            LoftRing(y = -0.11f, cz = 0f, rx = 0.120f, rz = 0.068f, backExtraZ = 0.015f),
            LoftRing(y = 0.00f, cz = 0f, rx = 0.122f, rz = 0.072f, backExtraZ = 0.018f),
            LoftRing(y = 0.11f, cz = 0.004f, rx = 0.106f, rz = 0.064f, backExtraZ = 0.010f),
            LoftRing(y = 0.16f, cz = 0.008f, rx = 0.072f, rz = 0.042f)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    /**
     * Sculpted athletic hair styles (Crown, Tapered Swept Top, Layered Long).
     */
    fun buildSweptHairMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = 0.020f, cz = -0.010f, rx = 0.091f, rz = 0.103f, backExtraZ = 0.006f),
            LoftRing(y = 0.062f, cz = -0.006f, rx = 0.093f, rz = 0.107f, frontExtraZ = 0.012f),
            LoftRing(y = 0.105f, cz = 0.000f, rx = 0.078f, rz = 0.092f, frontExtraZ = 0.016f),
            LoftRing(y = 0.136f, cz = -0.002f, rx = 0.046f, rz = 0.056f),
            LoftRing(y = 0.148f, cz = -0.004f, rx = 0.016f, rz = 0.020f)
        )
        return buildLoftedMesh(rings, slices = 26)
    }

    fun buildLongHairBackMesh(): MeshBuffers {
        val rings = listOf(
            LoftRing(y = -0.125f, cz = -0.060f, rx = 0.088f, rz = 0.040f),
            LoftRing(y = -0.055f, cz = -0.052f, rx = 0.096f, rz = 0.052f),
            LoftRing(y = 0.020f, cz = -0.028f, rx = 0.098f, rz = 0.076f),
            LoftRing(y = 0.072f, cz = -0.012f, rx = 0.092f, rz = 0.092f),
            LoftRing(y = 0.110f, cz = -0.006f, rx = 0.062f, rz = 0.068f)
        )
        return buildLoftedMesh(rings, slices = 24)
    }

    fun buildRingMesh(segments: Int = 48, innerRatio: Float = 0.94f): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        for (i in 0 until segments) {
            val a1 = 2.0 * Math.PI * i / segments
            val a2 = 2.0 * Math.PI * (i + 1) / segments
            val ix1 = (innerRatio * cos(a1)).toFloat(); val iz1 = (innerRatio * sin(a1)).toFloat()
            val ox1 = cos(a1).toFloat(); val oz1 = sin(a1).toFloat()
            val ix2 = (innerRatio * cos(a2)).toFloat(); val iz2 = (innerRatio * sin(a2)).toFloat()
            val ox2 = cos(a2).toFloat(); val oz2 = sin(a2).toFloat()

            verts.addAll(listOf(ix1, 0f, iz1, ox2, 0f, oz2, ox1, 0f, oz1))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
            verts.addAll(listOf(ix1, 0f, iz1, ix2, 0f, iz2, ox2, 0f, oz2))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }

    fun buildDiscMesh(segments: Int = 40): MeshBuffers {
        val verts = ArrayList<Float>()
        val norms = ArrayList<Float>()
        for (i in 0 until segments) {
            val a1 = 2.0 * Math.PI * i / segments
            val a2 = 2.0 * Math.PI * (i + 1) / segments
            verts.addAll(listOf(0f, 0f, 0f, cos(a2).toFloat(), 0f, sin(a2).toFloat(), cos(a1).toFloat(), 0f, sin(a1).toFloat()))
            norms.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
        }
        return MeshBuffers(toFloatBuffer(verts.toFloatArray()), toFloatBuffer(norms.toFloatArray()), verts.size / 3)
    }
}
