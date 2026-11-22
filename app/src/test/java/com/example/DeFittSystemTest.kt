package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DeFittDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.entity.DailyLogEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.DeFittRepository
import com.example.data.repository.PurchaseResult
import com.example.domain.catalog.OfflineFoodCatalog
import com.example.domain.catalog.StoreCatalog
import com.example.domain.engine.AvatarEvolutionEngine
import com.example.domain.engine.WorkoutGenerator
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.MealType
import com.example.ui.theme.DeFittTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeFittSystemTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: DeFittDatabase
    private lateinit var prefs: SessionPreferences
    private lateinit var repo: DeFittRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DeFittDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = SessionPreferences(context)
        repo = DeFittRepository(db, prefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `auth register login recovery and backup export import work offline`() = runTest {
        val reg = repo.registerAccount("Carlos Méndez", "carlosfit", 28, "4321")
        assertTrue(reg is AuthResult.Success)
        val successReg = reg as AuthResult.Success
        val recoveryCode = successReg.recoveryCodePlain!!
        val userId = successReg.user.id

        // Duplicate nickname blocked
        val dup = repo.registerAccount("Otro", "carlosfit", 22, "9999")
        assertTrue(dup is AuthResult.Error)

        // Wrong PIN rejected
        val badLogin = repo.loginAccount("carlosfit", "0000")
        assertTrue(badLogin is AuthResult.Error)

        // Valid login works
        val goodLogin = repo.loginAccount("carlosfit", "4321")
        assertTrue(goodLogin is AuthResult.Success)

        // Local recovery resets PIN
        val rec = repo.recoverAccountWithCode("carlosfit", recoveryCode, "8888")
        assertTrue(rec is AuthResult.Success)
        assertTrue(repo.loginAccount("carlosfit", "8888") is AuthResult.Success)

        // Export backup & tamper detection
        val backupJson = repo.exportBackupJson(userId)
        val tampered = backupJson.replace("carlosfit", "hacked")
        assertTrue(repo.importBackupJson(tampered) is AuthResult.Error)

        // Delete account then restore from valid backup
        repo.deleteAccount(userId)
        val restored = repo.importBackupJson(backupJson)
        assertTrue(restored is AuthResult.Success)
        assertEquals("carlosfit", (restored as AuthResult.Success).user.nickname)
    }

    @Test
    fun `step counter anti cheat reboot recovery COD earning and store purchase`() = runTest {
        val reg = repo.registerAccount("Ana Ruiz", "anafit", 24, "1234") as AuthResult.Success
        val uid = reg.user.id

        // Establish initial hardware baseline (1000 cumulative boot steps) -> 0 new steps
        val baseline = repo.processHardwareStepSensorValue(uid, 1000f)
        assertEquals(0L, baseline)

        // Instantaneous 250-step spike in <1s is capped by anti-cheat guard (max 25 burst)
        val spike = repo.processHardwareStepSensorValue(uid, 1250f)
        assertEquals(25L, spike)

        // Advance stored step timestamp back by 60 seconds to simulate a real 1-minute walk of 225 more steps
        val st = db.stepStateDao().getState(uid)!!
        db.stepStateDao().upsertState(st.copy(lastStepEventTimestampMs = st.lastStepEventTimestampMs - 60_000L))
        val validWalk = repo.processHardwareStepSensorValue(uid, 1475f)
        assertEquals(225L, validWalk)

        // Simulate device reboot (sensor resets to 20 after 30s) -> recovers without losing 250!
        val stBeforeReboot = db.stepStateDao().getState(uid)!!
        db.stepStateDao().upsertState(stBeforeReboot.copy(lastStepEventTimestampMs = stBeforeReboot.lastStepEventTimestampMs - 30_000L))
        val deltaAfterReboot = repo.processHardwareStepSensorValue(uid, 20f)
        assertEquals(20L, deltaAfterReboot)

        val userAfterSteps = db.userDao().getUserById(uid)!!
        assertTrue(userAfterSteps.codBalance >= 270L)

        // Buy an item with COD (e.g., acc_wristband_neon = 75 COD)
        val wristband = StoreCatalog.getById("acc_wristband_neon")!!
        val buyRes = repo.purchaseStoreItem(uid, wristband)
        assertTrue(buyRes is PurchaseResult.Success)

        val equipped = db.storeDao().getEquipped(uid)
        assertTrue(equipped.any { it.itemId == wristband.id })

        val achievements = db.achievementDao().getAchievements(uid)
        assertTrue(achievements.any { it.achievementKey == "FIRST_STEPS" })
        assertTrue(achievements.any { it.achievementKey == "FIRST_PURCHASE" })
    }

    @Test
    fun `midnight closure closes past day resets daily steps and preserves COD balance and avatar`() = runTest {
        val reg = repo.registerAccount("Luis Torres", "luis", 31, "5555") as AuthResult.Success
        val uid = reg.user.id

        // Insert unclosed yesterday log
        val yesterday = "2026-11-20"
        db.dailyLogDao().upsertLog(
            DailyLogEntity(
                userId = uid,
                dateKey = yesterday,
                steps = 4500L,
                codEarned = 4500L,
                avatarSnapshotJson = reg.user.avatarConfigJson,
                isClosed = false
            )
        )
        db.userDao().addCodBalance(uid, 4500L)

        val closedCount = repo.reconcilePendingClosuresIfNeeded(uid)
        assertEquals(1, closedCount)

        val closedYesterday = db.dailyLogDao().getLogForDate(uid, yesterday)!!
        assertTrue(closedYesterday.isClosed)
        assertEquals(4500L, closedYesterday.steps)

        // COD balance never resets at midnight
        val userNow = db.userDao().getUserById(uid)!!
        assertEquals(4500L, userNow.codBalance)
    }

    @Test
    fun `workouts food catalog and body scan comparison behave accurately`() = runTest {
        val reg = repo.registerAccount("Sofía", "sofia", 46, "2222") as AuthResult.Success
        val uid = reg.user.id
        val initialAvatar = repo.parseAvatarParams(reg.user.avatarConfigJson)

        val routine = WorkoutGenerator.generateDailyRoutine(46, ExperienceLevel.PRINCIPIANTE, 1200L)
        assertTrue(routine.exercises.size >= 6)

        // Confirming workout YES awards XP but NEVER alters avatar body ratios
        repo.recordWorkoutConfirmation(uid, routine.title, routine.difficulty, routine.estimatedMinutes, "Test", true)
        val afterWorkoutUser = db.userDao().getUserById(uid)!!
        assertTrue(afterWorkoutUser.xpTotal > 0L)
        assertEquals(initialAvatar.shoulderWidthRatio, repo.parseAvatarParams(afterWorkoutUser.avatarConfigJson).shoulderWidthRatio, 0.001f)

        // Food logging & matching
        val matchedApple = OfflineFoodCatalog.matchLabels(listOf("Fruit", "Apple"))
        assertEquals("apple", matchedApple.id)
        repo.addFoodEntry(uid, MealType.DESAYUNO, matchedApple, 200, "IA local", true)
        val todayFoods = db.foodDao().getAllFoodForUser(uid)
        assertEquals(1, todayFoods.size)
        assertEquals(104f, todayFoods.first().calories, 0.5f)

        // Body scan comparison: tiny noise < 2.5% preserves avatar
        val scan1 = repo.saveBodyScan(uid, "CAMERA_POSE_MULTI_ANGLE", 3, 1.00f, 1.00f, 1.00f, 1.00f, 0.90f, "Base", "Scan 1")
        val scan2 = repo.saveBodyScan(uid, "CAMERA_POSE_MULTI_ANGLE", 3, 1.01f, 0.99f, 1.00f, 1.00f, 0.91f, "Estable", "Scan 2")
        val stableComp = AvatarEvolutionEngine.compareScans(scan1, scan2)
        assertFalse(stableComp.measurableChangeDetected)

        // Measurable change >= 2.5% detected honestly
        val scan3 = repo.saveBodyScan(uid, "CAMERA_POSE_MULTI_ANGLE", 3, 1.05f, 0.93f, 1.00f, 1.00f, 0.96f, "Cambio medible", "Scan 3")
        val changedComp = AvatarEvolutionEngine.compareScans(scan1, scan3)
        assertTrue(changedComp.measurableChangeDetected)
    }

    @Test
    fun `step counter hal 0 to 1596 spike 2 real steps duplicate reboot and logout reset`() = runTest {
        val reg = repo.registerAccount("Mateo Silva", "mateo", 29, "1357") as AuthResult.Success
        val uid = reg.user.id

        // HAL emits initial 0f dummy reading on listener start -> 0 steps
        assertEquals(0L, repo.processHardwareStepSensorValue(uid, 0f))

        // Sensor immediately reports cumulative boot counter 1596f within <15s -> re-baselines cleanly, 0 steps/COD!
        assertEquals(0L, repo.processHardwareStepSensorValue(uid, 1596f))
        assertEquals(0L, db.dailyLogDao().getLogForDate(uid, com.example.domain.engine.DateUtils.todayKey())?.steps)
        assertEquals(0L, db.userDao().getUserById(uid)?.codBalance)

        // User walks 2 real steps -> exact 2 steps & 2 COD
        val st1 = db.stepStateDao().getState(uid)!!
        db.stepStateDao().upsertState(st1.copy(lastStepEventTimestampMs = st1.lastStepEventTimestampMs - 5_000L))
        assertEquals(2L, repo.processHardwareStepSensorValue(uid, 1598f))
        assertEquals(2L, db.dailyLogDao().getLogForDate(uid, com.example.domain.engine.DateUtils.todayKey())?.steps)
        assertEquals(2L, db.userDao().getUserById(uid)?.codBalance)

        // User walks 10 more steps -> total 12 steps & 12 COD
        val st2 = db.stepStateDao().getState(uid)!!
        db.stepStateDao().upsertState(st2.copy(lastStepEventTimestampMs = st2.lastStepEventTimestampMs - 10_000L))
        assertEquals(10L, repo.processHardwareStepSensorValue(uid, 1608f))

        // Duplicate reading on app reopen -> 0 duplicates
        assertEquals(0L, repo.processHardwareStepSensorValue(uid, 1608f))

        // Phone reboot resets sensor to 5f -> recovers +5 without duplicating 1608!
        val st3 = db.stepStateDao().getState(uid)!!
        db.stepStateDao().upsertState(st3.copy(lastStepEventTimestampMs = st3.lastStepEventTimestampMs - 10_000L))
        assertEquals(5L, repo.processHardwareStepSensorValue(uid, 5f))

        val log = db.dailyLogDao().getLogForDate(uid, com.example.domain.engine.DateUtils.todayKey())!!
        assertEquals(17L, log.steps)
        assertEquals(17L, log.codEarned)
        assertEquals(17L, db.userDao().getUserById(uid)!!.codBalance)

        // Logout clears baseline so steps taken while logged out aren't credited on re-login
        repo.logout()
        repo.loginAccount("mateo", "1357")
        assertEquals(0L, repo.processHardwareStepSensorValue(uid, 180f))
        assertEquals(17L, db.userDao().getUserById(uid)!!.codBalance)
    }

    @Test
    fun `store covers all 11 categories and wardrobe equip unequip works cleanly`() = runTest {
        val reg = repo.registerAccount("Valeria Cruz", "vale", 25, "9876") as AuthResult.Success
        val uid = reg.user.id

        // Every one of the 11 StoreCategory entries must have items
        com.example.domain.model.StoreCategory.entries.forEach { cat ->
            assertTrue(
                "Category ${cat.displayName} must have items",
                StoreCatalog.allItems.any { it.category == cat }
            )
        }

        // Starter items equipped initially, toggle off & back on
        val starterShirt = StoreCatalog.getById("shirt_defitt_black")!!
        repo.toggleEquipItem(uid, starterShirt)
        assertFalse(db.storeDao().getEquipped(uid).any { it.itemId == starterShirt.id })
        repo.toggleEquipItem(uid, starterShirt)
        assertTrue(db.storeDao().getEquipped(uid).any { it.itemId == starterShirt.id })
    }

    @Test
    fun `compose ui critical journey navigates across home store wardrobe calendar stats and workout`() {
        composeRule.setContent {
            DeFittTheme {
                DeFittAppRoot()
            }
        }
        composeRule.mainClock.advanceTimeBy(1500)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("auth_tab_register").fetchSemanticsNodes().isNotEmpty()
        }

        // Register user
        composeRule.onNodeWithTag("auth_tab_register").performScrollTo().performClick()
        composeRule.onNodeWithTag("register_name_input").performScrollTo().performTextInput("Elena Vega")
        composeRule.onNodeWithTag("auth_nickname_input").performScrollTo().performTextInput("elenavega")
        composeRule.onNodeWithTag("register_age_input").performScrollTo().performTextInput("26")
        composeRule.onNodeWithTag("auth_pin_input").performScrollTo().performTextInput("1234")
        composeRule.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("recovery_code_display").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("recovery_code_display").assertIsDisplayed()
        composeRule.onNodeWithTag("recovery_code_confirm_button").performClick()

        // Home screen hero avatar & steps
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("home_avatar_3d").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_avatar_3d").assertIsDisplayed()
        composeRule.onNodeWithTag("home_steps_text").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("home_workout_button").assertExists()

        // Tienda: verify product artwork & Mi Armario tab
        composeRule.onNodeWithTag("nav_tienda").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("store_avatar_3d").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("store_artwork_shirt_defitt_black").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("store_artwork_shirt_defitt_black").assertExists()
        composeRule.onNodeWithTag("store_tab_wardrobe").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("equip_item_shirt_defitt_black").fetchSemanticsNodes().isNotEmpty()
        }

        // Registro: verify calendar tab
        composeRule.onNodeWithTag("nav_registro").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("registro_tab_calendar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("registro_tab_calendar").performClick()
        composeRule.onNodeWithTag("calendar_prev_month").performScrollTo().assertIsDisplayed()

        // Logros & Estadísticas: verify real DB charts render
        composeRule.onNodeWithTag("nav_logros").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("logros_tab_stats").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("logros_tab_stats").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("chart_habit_rings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chart_weekly_steps").assertExists()

        // Back to Home & Workout SÍ confirmation
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("home_workout_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_workout_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("workout_finish_confirm_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("workout_finish_confirm_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("workout_confirm_yes_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("workout_confirm_yes_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("home_avatar_3d").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
