package com.grupo2.pulperiamarin

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @Test
    fun testAddProductAndCheckSummary() {
        // Esperar a que pase el splash (3 segundos)
        Thread.sleep(3500)

        // Ir a Productos
        onView(withId(R.id.btnProducts)).perform(click())

        // Agregar un producto
        onView(withId(R.id.etName)).perform(typeText("Manzanas Test"), closeSoftKeyboard())
        onView(withId(R.id.etPrice)).perform(typeText("10.5"), closeSoftKeyboard())
        onView(withId(R.id.etStock)).perform(typeText("100"), closeSoftKeyboard())
        onView(withId(R.id.etMinStock)).perform(typeText("10"), closeSoftKeyboard())
        onView(withId(R.id.btnSave)).perform(click())

        // Verificar que aparezca en la lista
        onView(withId(R.id.lvProducts)).check(matches(hasDescendant(withText(containsString("Manzanas Test")))))

        // Regresar al menu (asumiendo que hay boton atras o usamos back de android)
        pressBack()

        // Ir a Resumen
        onView(withId(R.id.btnSummary)).perform(click())

        // Verificar valor (10.5 * 100 = 1050.00)
        onView(withId(R.id.tvTotalValue)).check(matches(withText(containsString("1050.00"))))
    }
}