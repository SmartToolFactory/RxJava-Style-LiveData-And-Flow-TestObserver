# RxJava-Style-LiveData-TestObserver
TestObserver class for LiveData to test multiple values like ViewState such as loading, and result states or multiple post and setValues


### Implementation

```
class TestObserver<T>(private val liveData: LiveData<T>) : Observer<T> {

//    init {
//        liveData.observeForever(this)
//    }

    private val testValues = mutableListOf<T>()

    override fun onChanged(t: T) {

        if (t != null) testValues.add(t)
        println("⏰ TestObserver onChanged()  testValues $testValues")

    }

    fun assertNoValues(): TestObserver<T> {
        if (testValues.isNotEmpty()) throw AssertionException("Assertion error with actual size ${testValues.size}")
        return this
    }

    fun assertValueCount(count: Int): TestObserver<T> {
        if (count < 0) throw AssertionException("Assert count cannot be smaller than zero")
        if (count != testValues.size) throw AssertionException("Assertion error with expected $count while actual ${testValues.size}")
        return this
    }

    fun assertValues(vararg predicates: T): TestObserver<T> {
        predicates.forEach { predicate ->
            testValues.forEach { testValue ->
                if (predicate != testValue) throw  Exception("Assertion error")
            }
        }
        return this
    }

    fun assertValues(predicate: List<T>.() -> Boolean): TestObserver<T> {
        testValues.predicate()
        return this
    }

    fun values(predicate: List<T>.() -> Unit): TestObserver<T> {
        testValues.predicate()
        return this
    }

    fun values(): List<T> {
        return testValues
    }


    fun dispose() {
        testValues.clear()
        liveData.removeObserver(this)
    }
}


fun <T> LiveData<T>.test(): TestObserver<T> {

    val testObserver = TestObserver(this)

    observeForever(testObserver)

    return testObserver
}

class AssertionException(message: String) : Exception(message) {

}
```

## Usage

```
@Test
fun test() {

    // GIVEN
    val myTestData = MutableLiveData<Int>()
    val testObserver = myTestData.test()

    // WHEN
    myTestData.value = 1
    myTestData.value = 2
    myTestData.value = 3
    
    // THEN
    testObserver.assertValues {
        (this[0] == 1 && this[1] == 2 && this[2] == 3)

    }.assertValueCount(3)

    // 🔥 Do not forget to dispose
    testObserver.dispose()
}

```
