package main.domain.prediction

import main.app.config.createDataSource
import main.app.config.getOptionalSetting
import main.app.config.loadDotEnv
import main.data.impl.caches.HouseInfoCache
import main.data.impl.jdbc.JdbcHouseRepository
import main.data.impl.mem.InMemoryHouseRepository
import main.data.interfaces.HouseRepository
import java.util.Properties
import kotlin.math.roundToLong
import main.domain.house.House as DomainHouse

/*
 Linear Regression in Kotlin
 Predict house price given area in m²
*/

// ===============================
// 1. Data structure
// ===============================

// Internal sample used only as fallback when repos are empty
private val fallbackHouses =
    listOf(
        House(35.0, 120000.0),
        House(52.0, 155000.0),
        House(70.0, 210000.0),
        House(95.0, 260000.0),
        House(140.0, 340000.0),
        House(220.0, 480000.0),
    )

// Model dataset keeps only area and price
data class House(
    val area: Double,
    val price: Double,
)

private val houses: List<House>
    get() = loadTrainingData().houses

enum class TrainingSource {
    DATABASE,
    IN_MEMORY,
    FALLBACK,
}

data class TrainingData(
    val source: TrainingSource,
    val houses: List<House>,
)

data class TrainedModel(
    val areas: Scale,
    val prices: Scale,
    val params: Params,
)

private fun DomainHouse.toLinearSample(): House =
    House(
        area = areaSqMt.toDouble(),
        price = pricePerNight,
    )

fun loadHousesFromRepository(repository: HouseRepository): List<House> = repository.getAll().map { it.toLinearSample() }

fun loadTrainingData(dotEnv: Properties? = loadDotEnv()): TrainingData {
    val jdbcUrl = getOptionalSetting("JDBC_DATABASE_URL", dotEnv)

    if (!jdbcUrl.isNullOrBlank()) {
        val dbSamples =
            runCatching {
                val repository = JdbcHouseRepository(createDataSource(dotEnv), HouseInfoCache(limit = 100))
                loadHousesFromRepository(repository)
            }.getOrElse { emptyList() }

        if (dbSamples.isNotEmpty()) {
            return TrainingData(TrainingSource.DATABASE, dbSamples)
        }
    }

    val memorySamples = loadHousesFromRepository(InMemoryHouseRepository)
    if (memorySamples.isNotEmpty()) {
        return TrainingData(TrainingSource.IN_MEMORY, memorySamples)
    }

    return TrainingData(TrainingSource.FALLBACK, fallbackHouses)
}

// ===============================
// 2. Normalization
// ===============================

class Scale(
    values: List<Double>,
) {
    val min: Double = values.min()
    val max: Double = values.max()
    private val isConstantScale = max == min
    val delta: Double = if (isConstantScale) 1.0 else max - min

    fun normalize(value: Double) = if (isConstantScale) 0.0 else (value - min) / delta

    fun denormalize(value: Double) = if (isConstantScale) min else value * delta + min
}

data class NormalizedData(
    val areas: Scale,
    val prices: Scale,
    val data: List<House>,
)

fun List<House>.normalize(): NormalizedData {
    val areas = Scale(map { it.area })
    val prices = Scale(map { it.price })
    return NormalizedData(
        areas = areas,
        prices = prices,
        data =
            map {
                House(
                    area = areas.normalize(it.area),
                    price = prices.normalize(it.price),
                )
            },
    )
}

// ===============================
// 3. Model
// ===============================

data class Params(
    val w: Double,
    val b: Double,
)

operator fun Params.plus(other: Params) = Params(w + other.w, b + other.b)

fun predict(
    x: Double,
    p: Params,
): Double = p.w * x + p.b

// ===============================
// 4. Helper functions
// ===============================

fun error(
    yPred: Double,
    yReal: Double,
): Double = yPred - yReal

fun gradients(
    x: Double,
    error: Double,
    n: Int,
): Params {
    require(n > 0) { "Cannot compute gradients: empty dataset (n=0)" }
    return Params(
        w = (2.0 / n) * error * x,
        b = (2.0 / n) * error,
    )
}

fun updateParams(
    p: Params,
    delta: Params,
    lr: Double,
) = Params(
    w = p.w - lr * delta.w,
    b = p.b - lr * delta.b,
)

// ===============================
// 5. Training
// ===============================

fun train(
    data: List<House>,
    epochs: Int = 3000,
    lr: Double = 0.05,
): Params {
    require(data.isNotEmpty()) { "Cannot train: empty dataset" }
    require(epochs > 0) { "Epochs must be positive" }
    require(lr > 0) { "Learning rate must be positive" }

    var params = Params(w = 0.0, b = 0.0)

    repeat(epochs) {
        val total =
            data.fold(Params(0.0, 0.0)) { partial, house ->
                val yPred = predict(house.area, params)
                val e = error(yPred, house.price)
                partial + gradients(house.area, e, data.size)
            }
        params = updateParams(params, total, lr)
    }
    return params
}

fun trainModel(
    rawData: List<House>,
    epochs: Int = 3000,
    lr: Double = 0.05,
): TrainedModel {
    val (areas, prices, normalizedData) = rawData.normalize()
    val params = train(normalizedData, epochs, lr)
    return TrainedModel(areas, prices, params)
}

fun predictPriceForArea(
    area: Int,
    model: TrainedModel,
): Long {
    val areaNorm = model.areas.normalize(area.toDouble())
    val priceNorm = predict(areaNorm, model.params)
    return model.prices.denormalize(priceNorm).roundToLong()
}

// ===============================
// 6. Execution
// ===============================

fun mainTrainer() {
    val trainingData = loadTrainingData()
    val model = trainModel(trainingData.houses)

    println("=== Trained model ===")
    println("source = ${trainingData.source} | houses = ${trainingData.houses.size}")
    println("weight = %.3f | bias = %.3f".format(model.params.w, model.params.b))

    val area = 110
    val price = predictPriceForArea(area, model)
    println("Predicted price for a house of $area m²: €$price")
}

// fun main() = mainTrainer() // Teste
