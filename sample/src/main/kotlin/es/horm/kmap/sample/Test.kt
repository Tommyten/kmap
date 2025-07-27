
import com.example.generated.toTestModel
import es.horm.kmap.runtime.KmapTo
import es.horm.kmap.runtime.Mapping

@KmapTo(
    target = BusinessModel::class,
    mappings = [Mapping("c", "test")]
)
@KmapTo(TestModel::class)
data class Dto(
    val a: Int,
    val b: Int,
    val c: Int,
)

data class BusinessModel(
    val a: Int,
    val b: Int,
    val test: Int,
)
data class TestModel(
    val b: Int,
    val c: Int
)

fun main() {
    Dto(1,2,3).toTestModel()
    println("Ran!")
}
