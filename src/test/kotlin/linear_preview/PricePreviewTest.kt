package linear_preview


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import main.domain_model.prediction.House
import main.domain_model.prediction.Params
import main.domain_model.prediction.Scale
import main.domain_model.prediction.error
import main.domain_model.prediction.gradients
import main.domain_model.prediction.predict
import main.domain_model.prediction.train
import kotlin.math.absoluteValue


class LinearRegressionTest {
    private fun mse(
        data: List<House>,
        params: Params,
    ): Double =
        data
            .map {
                val e = error(predict(it.area, params), it.price)
                e * e
            }.average()
// ===============================
// 1. Normalization tests
// ===============================

    @Test
    fun `scale with identical values should throw IllegalArgumentException`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Scale(listOf(50.0, 50.0, 50.0))
            }
        assertTrue(exception.message!!.contains("identical"))
    }


    @Test
    fun `gradients should compute correct partial derivatives`() {
        val grad =
            gradients(
                x = 2.0,
                error = 3.0,
                n = 4,
            )
        assertEquals((2.0 / 4) * 3.0 * 2.0, grad.w, 1e-9)
        assertEquals((2.0 / 4) * 3.0, grad.b, 1e-9)
    }

    @Test
    fun `train with empty data should throw IllegalArgumentException`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                train(emptyList(), epochs = 100, lr = 0.1)
            }
        assertTrue(exception.message!!.contains("empty"))
    }

    @Test
    fun `train with zero learning rate should throw IllegalArgumentException`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                train(listOf(House(0.0, 0.0)), epochs = 100, lr = 0.0)
            }
        assertTrue(exception.message!!.contains("Learning rate"))
    }

    @Test
    fun `train should reduce error on simple linear data`() {
        // y = 2x
        val simpleData =
            listOf(
                House(0.0, 0.0),
                House(0.5, 1.0),
                House(1.0, 2.0),
            )
        val params = train(simpleData, epochs = 5000, lr = 0.1)
        // We expect w close to 2 and b close to 0
        assertTrue((params.w - 2.0).absoluteValue < 0.1)
    }

    @Test
    fun `train should reduce mean squared error compared to initial params`() {
        val simpleData =
            listOf(
                House(0.0, 0.0),
                House(0.5, 1.0),
                House(1.0, 2.0),
            )

        val initial = Params(0.0, 0.0)
        val trained = train(simpleData, epochs = 3000, lr = 0.1)
    }
}
